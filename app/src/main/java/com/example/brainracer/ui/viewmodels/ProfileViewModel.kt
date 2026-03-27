package com.example.brainracer.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.ImageOptimizerUtil
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.User as DomainUser
import com.example.brainracer.ui.utils.PassedQuizUi
import com.example.brainracer.ui.utils.ProfileAchievements
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.ui.utils.ProfileGoalBadges
import com.example.brainracer.ui.utils.ProfileUIState
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.toQuizItem
import com.example.brainracer.ui.utils.TopicStatUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val profileLoadMutex = Mutex()
    private var profileCacheUserId: String? = null
    private var profileCacheLoadedAtMs: Long = 0L

    companion object {
        private const val MAX_BIO_LENGTH = 400
        /** Не полная перезагрузка с ON_RESUME чаще этого интервала; после игры срабатывает ProfileAfterQuizRefresh. */
        private const val PROFILE_CACHE_TTL_MS = 20_000L
    }

    /**
     * @param forceRefresh `true` — смена userId или первый заход; `false` — ON_RESUME (учитывается TTL).
     */
    fun loadUserProfile(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            profileLoadMutex.withLock {
                if (!forceRefresh &&
                    userId == profileCacheUserId &&
                    (System.currentTimeMillis() - profileCacheLoadedAtMs) < PROFILE_CACHE_TTL_MS &&
                    _uiState.value.userId == userId &&
                    _uiState.value.username.isNotBlank()
                ) {
                    return@withLock
                }

                _uiState.update { it.copy(isLoading = true, errorMessage = null, deletingQuizId = null) }
                try {
                    when (val ur = userRepository.getUser(userId)) {
                        is Result.Success -> loadProfileData(ur.data)
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Ошибка загрузки профиля: ${ur.exception.message}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка загрузки профиля: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun invalidateProfileCache() {
        profileCacheUserId = null
        profileCacheLoadedAtMs = 0L
    }

    private suspend fun loadProfileData(user: DomainUser) {
        val stats = user.stats
        val totalXp = stats.totalPoints
        val level = LevelSystem.levelFromXp(totalXp)
        val progress = LevelSystem.levelProgress(totalXp)
        val rankName = LevelSystem.rankForLevel(level).displayName

        val displayName = when {
            user.nickname.isNotBlank() -> user.nickname
            user.email.isNotBlank() -> user.email.substringBefore("@")
            else -> "Игрок"
        }

        val (createdQuizItems, resultsData, quizHistoryErr) = coroutineScope {
            val createdDeferred = async { quizRepository.getQuizzesByUser(user.id) }
            val resultsDeferred = async { quizRepository.getRecentResultsForUser(user.id, 50) }
            val createdRes = createdDeferred.await()
            val resultsRes = resultsDeferred.await()

            val items = when (createdRes) {
                is Result.Success -> createdRes.data.map { it.toQuizItem() }
                is Result.Error -> emptyList()
            }
            val (rows, histErr) = when (resultsRes) {
                is Result.Success -> resultsRes.data to null
                is Result.Error ->
                    emptyList<com.example.brainracer.domain.entities.ChallengeResult>() to
                        "История прохождений не загрузилась: ${resultsRes.exception.message}"
            }
            Triple(items, rows, histErr)
        }

        val ids = resultsData.map { it.quizId }.distinct()
        val quizById = when (val batch = quizRepository.getQuizzesByIds(ids)) {
            is Result.Success -> batch.data
            is Result.Error -> emptyMap()
        }

        val passed = mutableListOf<PassedQuizUi>()
        val byCategory = linkedMapOf<String, MutableList<Double>>()

        for (r in resultsData) {
            val q = quizById[r.quizId]
            if (q != null) {
                passed.add(
                    PassedQuizUi(
                        quizId = r.quizId,
                        title = q.title,
                        category = q.categoryId,
                        accuracyPercent = r.accuracy.toInt().coerceIn(0, 100),
                        pointsEarned = r.pointsEarned,
                        completedAtEpochMs = r.completedAt.toDate().time
                    )
                )
                byCategory.getOrPut(q.categoryId) { mutableListOf() }.add(r.accuracy)
            } else {
                passed.add(
                    PassedQuizUi(
                        quizId = r.quizId,
                        title = "Викторина",
                        category = "—",
                        accuracyPercent = r.accuracy.toInt().coerceIn(0, 100),
                        pointsEarned = r.pointsEarned,
                        completedAtEpochMs = r.completedAt.toDate().time
                    )
                )
            }
        }

        val topicStats = byCategory.entries
            .map { (name, accs) ->
                val avg = if (accs.isNotEmpty()) accs.average().toFloat() else 0f
                TopicStatUi(
                    categoryName = name,
                    percent = avg.coerceIn(0f, 100f),
                    paletteIndex = 0
                )
            }
            .sortedByDescending { it.percent }
            .mapIndexed { idx, t -> t.copy(paletteIndex = idx) }

        val achievements = ProfileAchievements.compute(stats, topicStats, createdQuizItems.size)

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                quizHistoryLoadError = quizHistoryErr,
                userId = user.id,
                username = displayName,
                email = user.email,
                avatarUrl = user.avatarUrl,
                registrationDate = user.createdAt.toDate().toString(),
                userLevel = level,
                levelProgress = progress,
                rankName = rankName,
                currentRank = rankName,
                userStats = stats,
                createdQuizzes = createdQuizItems,
                likedQuizzes = emptyList(),
                passedAttempts = passed,
                topicStats = topicStats,
                achievements = achievements,
                achievementsCount = achievements.count { a -> a.unlocked },
                friendsCount = user.friends.size,
                bio = user.bio,
                interests = user.interests
            )
        }
        profileCacheUserId = user.id
        profileCacheLoadedAtMs = System.currentTimeMillis()
    }

    fun updateUserAvatar(userId: String, avatarUrl: String) {
        viewModelScope.launch {
            when (val result = userRepository.updateUserAvatar(userId, avatarUrl)) {
                is Result.Success -> {
                    _uiState.update { it.copy(avatarUrl = avatarUrl) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Ошибка обновления аватара: ${result.exception.message}") }
                }
            }
        }
    }

    fun uploadAvatar(context: Context, userId: String, uri: Uri) {
        val uid = auth.currentUser?.uid
        if (uid == null || uid != userId) {
            _uiState.update { it.copy(errorMessage = "Нужна авторизация для смены аватара") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, errorMessage = null) }
            try {
                val optimized = ImageOptimizerUtil.optimize(context, uri, isCover = false)
                val ext = if (optimized.mimeType == "image/gif") "gif" else optimized.mimeType.substringAfter('/')
                val path = "avatars/$userId/${UUID.randomUUID()}.$ext"
                val ref = storage.reference.child(path)
                ref.putBytes(optimized.bytes).await()
                val url = ref.downloadUrl.await().toString()
                when (val result = userRepository.updateUserAvatar(userId, url)) {
                    is Result.Success -> _uiState.update { it.copy(avatarUrl = url) }
                    is Result.Error -> _uiState.update {
                        it.copy(errorMessage = "Ошибка сохранения аватара: ${result.exception.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ошибка загрузки аватара: ${e.message}") }
            }
            _uiState.update { it.copy(isUploadingAvatar = false) }
        }
    }

    fun saveBioAndGoalBadges(
        userId: String,
        bio: String,
        badgeIds: List<String>,
        onFinished: (success: Boolean) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null || uid != userId) {
            _uiState.update { it.copy(errorMessage = "Нужна авторизация для сохранения профиля") }
            onFinished(false)
            return
        }
        val trimmedBio = bio.trim().take(MAX_BIO_LENGTH)
        val badges = ProfileGoalBadges.normalizeBadgeIds(badgeIds)
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, errorMessage = null) }
            when (val bioRes = userRepository.updateUserBio(userId, trimmedBio)) {
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            errorMessage = "Ошибка сохранения: ${bioRes.exception.message}"
                        )
                    }
                    onFinished(false)
                    return@launch
                }
                is Result.Success -> Unit
            }
            when (val intRes = userRepository.updateUserInterests(userId, badges)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            bio = trimmedBio,
                            interests = badges
                        )
                    }
                    onFinished(true)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            errorMessage = "Био сохранено, но бейджи не обновились: ${intRes.exception.message}"
                        )
                    }
                    onFinished(false)
                }
            }
        }
    }

    fun updateUsername(userId: String, newUsername: String) {
        viewModelScope.launch {
            when (val userResult = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val u = userResult.data
                    val updatedUser = u.copy(nickname = newUsername)

                    when (val updateResult = userRepository.updateUser(updatedUser)) {
                        is Result.Success -> {
                            _uiState.update { it.copy(username = newUsername) }
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(errorMessage = "Ошибка обновления имени: ${updateResult.exception.message}")
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = "Ошибка обновления имени: ${userResult.exception.message}") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearQuizHistoryError() {
        _uiState.update { it.copy(quizHistoryLoadError = null) }
    }

    /**
     * Удаляет викторину из Firestore и из списка `createdQuizzes` у пользователя.
     * [profileUserId] должен совпадать с текущим uid (свой профиль).
     */
    fun deleteCreatedQuiz(
        quizId: String,
        profileUserId: String,
        onFinished: (success: Boolean, errorMessage: String?) -> Unit = { _, _ -> }
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != profileUserId) {
            onFinished(false, "Войдите в аккаунт")
            return
        }
        if (quizId.isBlank()) {
            onFinished(false, "Некорректная викторина")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(deletingQuizId = quizId) }
            when (val loaded = quizRepository.getQuiz(quizId)) {
                is Result.Success -> {
                    val creator = loaded.data.createdBy
                    if (creator.isNotBlank() && creator != uid) {
                        _uiState.update { it.copy(deletingQuizId = null) }
                        onFinished(false, "Это не ваша викторина")
                        return@launch
                    }
                }
                is Result.Error -> {
                    // Сеть / парсинг: пробуем удалить — Firestore rules отсекут чужое
                }
            }
            when (val del = quizRepository.deleteQuiz(quizId)) {
                is Result.Success -> {
                    removeCreatedQuizFromState(quizId)
                    ProfileAfterQuizRefresh.notify(uid)
                    onFinished(true, null)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(deletingQuizId = null) }
                    onFinished(false, del.exception.message)
                }
            }
        }
    }

    private fun removeCreatedQuizFromState(quizId: String) {
        _uiState.update { s ->
            val newList = s.createdQuizzes.filter { it.id != quizId }
            val achievements = s.userStats?.let { stats ->
                ProfileAchievements.compute(stats, s.topicStats, newList.size)
            } ?: s.achievements
            s.copy(
                createdQuizzes = newList,
                achievements = achievements,
                achievementsCount = achievements.count { a -> a.unlocked },
                deletingQuizId = null
            )
        }
    }
}
