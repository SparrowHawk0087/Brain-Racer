package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.NotificationRepositoryImpl
import com.example.brainracer.data.repositories.UserChallengeSides
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.QuizStats
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.ui.utils.HomeUiState
import com.example.brainracer.ui.utils.QuizItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val quizRepository       = QuizRepositoryImpl()
    private val userRepository        = UserRepositoryImpl()
    private val challengeRepository   = ChallengeRepositoryImpl()
    private val notificationRepository = NotificationRepositoryImpl()
    private val auth                  = FirebaseAuth.getInstance()

    // Кэш полного списка викторин для фильтрации по категории
    private var allQuizzes: List<QuizItem> = emptyList()

    init {
        loadInitialData()
    }

    // ── Начальная загрузка ────────────────────────────────────────────────

    private fun loadInitialData() {
        viewModelScope.launch {
            // loadUserData и loadQuizzes теперь suspend — вызываем напрямую
            val userId = auth.currentUser?.uid
            if (userId != null) {
                _uiState.update { it.copy(currentUserId = userId) }
                loadUserData(userId)
            } else {
                _uiState.update { it.copy(userName = "Гость") }
            }
            loadQuizzes()
            if (userId != null) {
                launch {
                    try {
                        challengeRepository.observeUserChallengeSides(userId).collect { sides ->
                            applyHomeChallengeSides(sides)
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(errorMessage = "Вызовы: ${e.message ?: "ошибка сети"}")
                        }
                    }
                }
                launch {
                    try {
                        notificationRepository.observeNotificationsForUser(userId).collect { list ->
                            val unread = list.count { !it.read }
                            _uiState.update { it.copy(unreadNotificationsCount = unread) }
                        }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(unreadNotificationsCount = 0) }
                    }
                }
            }
        }
    }

    /** Сохраняет FCM-токен в профиль для будущих push (Cloud Functions). */
    fun syncFcmTokenToProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNotBlank()) userRepository.updateFcmToken(uid, token)
            } catch (_: Exception) { }
        }
    }

    // ── Загрузка данных пользователя (suspend) ────────────────────────────

    /**
     * Загружает пользователя из Firestore и вычисляет уровень через LevelSystem.
     * Suspend-функция — вызывать только из корутина.
     */
    private suspend fun loadUserData(userId: String) {
        _uiState.update { it.copy(isLoading = true) }

        when (val result = userRepository.getUser(userId)) {
            is Result.Success -> {
                val user = result.data

                val userName = when {
                    user.nickname.isNotBlank()                     -> user.nickname
                    !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser!!.displayName!!
                    user.email.isNotBlank()                        -> user.email.split("@").first()
                    else                                           -> "Гость"
                }

                val totalXp  = user.stats.totalPoints
                val level    = LevelSystem.levelFromXp(totalXp)
                val progress = LevelSystem.levelProgress(totalXp)
                val rank     = LevelSystem.rankForLevel(level)

                _uiState.update { state ->
                    state.copy(
                        isLoading     = false,
                        userName      = userName,
                        userStats     = user.stats,
                        userLevel     = level,
                        levelProgress = progress,
                        rankName      = rank.displayName
                    )
                }
            }

            is Result.Error -> {
                val fallback = auth.currentUser?.displayName
                    ?: auth.currentUser?.email?.split("@")?.first()
                    ?: "Гость"
                _uiState.update { it.copy(isLoading = false, userName = fallback) }
            }
        }
    }

    // ── Загрузка викторин (suspend) ───────────────────────────────────────

    private suspend fun loadQuizzes() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        when (val result = quizRepository.getPopularQuizzes(limit = 100)) {
            is Result.Success -> {
                val items = result.data.map { quiz ->
                    QuizItem(
                        id            = quiz.id,
                        title         = quiz.title,
                        category      = quiz.categoryId,
                        questionCount = quiz.questions.size,
                        difficulty    = quiz.difficulty.name,
                        description   = quiz.description,
                        rating        = quiz.stats.averageRating,
                        playCount     = quiz.stats.timesTaken
                    )
                }
                allQuizzes = items
                if (items.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, quizzes = emptyList(),
                            errorMessage = "Викторин нет. Нажмите ➕ чтобы добавить")
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, quizzes = items, selectedCategory = "Все")
                    }
                }
            }

            is Result.Error -> {
                _uiState.update {
                    it.copy(isLoading = false,
                        errorMessage = "Ошибка загрузки: ${result.exception.message}")
                }
            }
        }
    }

    // ── Публичные методы ──────────────────────────────────────────────────

    /**
     * Вызывать при возвращении на HomeScreen (например после прохождения викторины),
     * чтобы подтянуть свежую статистику и пересчитать уровень.
     */
    fun refreshUserStats() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadUserData(userId)
            when (val r = challengeRepository.fetchUserChallengeSides(userId)) {
                is Result.Success -> applyHomeChallengeSides(r.data)
                is Result.Error   -> { }
            }
        }
    }

    fun refreshUserName() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch { loadUserData(userId) }
    }

    fun refreshChallenges() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            when (val r = challengeRepository.fetchUserChallengeSides(userId)) {
                is Result.Success -> applyHomeChallengeSides(r.data)
                is Result.Error   -> { }
            }
        }
    }

    // ── Вызовы (данные с merge на клиенте, синхронно со snapshot в репозитории) ─

    private fun applyHomeChallengeSides(sides: UserChallengeSides) {
        val incoming = sides.asChallenged.filter { it.status == ChallengeStatus.PENDING }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )
        val outgoing = sides.asChallenger.filter {
            it.status == ChallengeStatus.PENDING ||
                    it.status == ChallengeStatus.ACCEPTED ||
                    it.status == ChallengeStatus.COMPLETED
        }
        val outgoingPending = outgoing.filter { it.status == ChallengeStatus.PENDING }
        val nowDate = Timestamp.now().toDate()
        val activeAccepted = (sides.asChallenged + sides.asChallenger)
            .distinctBy { it.id }
            .filter {
                it.status == ChallengeStatus.ACCEPTED &&
                        it.expiresAt.toDate().after(nowDate)
            }
        val completed = (sides.asChallenged + sides.asChallenger)
            .filter { it.status == ChallengeStatus.COMPLETED }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Challenge> { it.completedAt?.seconds ?: 0L }
                    .thenByDescending { it.completedAt?.nanoseconds ?: 0 }
            )
            .take(20)

        val homeActive = (incoming + outgoingPending + activeAccepted)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )

        _uiState.update {
            it.copy(
                pendingChallenges      = incoming,
                homeActiveChallenges   = homeActive,
                homeFinishedChallenges = completed
            )
        }
    }

    /** Перезагружает список викторин из Firestore. */
    fun reloadQuizzes() {
        viewModelScope.launch {
            loadQuizzes()
        }
    }

    /** Фильтрация по категории — работает с кэшем, без сетевого запроса. */
    fun loadQuizzesByCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, isLoading = true) }
        viewModelScope.launch {
            delay(300) // небольшая задержка для ощущения отклика
            val filtered = if (category == "Все") allQuizzes
            else allQuizzes.filter { it.category == category }
            _uiState.update { it.copy(quizzes = filtered, isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadChallengePickerData() {
        viewModelScope.launch {
            _uiState.update { it.copy(challengePickerLoading = true) }
            val userId = auth.currentUser?.uid ?: run {
                _uiState.update { it.copy(challengePickerLoading = false) }
                return@launch
            }
            val friends = when (val me = userRepository.getUser(userId)) {
                is Result.Success -> me.data.friends.mapNotNull { fid ->
                    when (val fr = userRepository.getUser(fid)) {
                        is Result.Success -> fr.data.copy(id = fid)
                        else -> null
                    }
                }
                else -> emptyList()
            }
            val quizzes = when (val r = quizRepository.getPopularQuizzes(limit = 80)) {
                is Result.Success -> r.data.map { quiz ->
                    QuizItem(
                        id            = quiz.id,
                        title         = quiz.title,
                        category      = quiz.categoryId,
                        questionCount = quiz.questions.size,
                        difficulty    = quiz.difficulty.name,
                        description   = quiz.description,
                        rating        = quiz.stats.averageRating,
                        playCount     = quiz.stats.timesTaken
                    )
                }
                is Result.Error -> emptyList()
            }
            val quizList = quizzes.ifEmpty { allQuizzes }
            _uiState.update {
                it.copy(
                    friendsForChallenge    = friends,
                    challengePickerQuizzes = quizList,
                    challengePickerLoading = false
                )
            }
        }
    }

    fun sendChallengeToFriend(friendId: String, quizId: String, quizTitle: String) {
        viewModelScope.launch {
            val challengerId = auth.currentUser?.uid ?: return@launch
            val challenge = Challenge(
                quizId           = quizId,
                quizTitle        = quizTitle,
                challengerUserId = challengerId,
                challengedUserId = friendId,
                status           = ChallengeStatus.PENDING
            )
            challengeRepository.createChallenge(challenge).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            errorMessage         = null,
                            challengeSentMessage = "Вызов отправлен"
                        )
                    }
                    viewModelScope.launch {
                        when (val r = challengeRepository.fetchUserChallengeSides(challengerId)) {
                            is Result.Success -> applyHomeChallengeSides(r.data)
                            is Result.Error   -> { }
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun consumeChallengeSentMessage() {
        _uiState.update { it.copy(challengeSentMessage = null) }
    }

    // ── Добавление демо-викторин ──────────────────────────────────────────

    fun addDemoQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val userId = auth.currentUser?.uid
                    ?: "demo_user_${System.currentTimeMillis()}"

                val demoQuiz = Quiz(
                    id          = "test_quiz_${System.currentTimeMillis()}",
                    title       = "Тест: Основы биологии",
                    description = "Простая тестовая викторина",
                    categoryId  = "Биология",
                    difficulty  = QuizDifficulty.EASY,
                    questions   = listOf(
                        com.example.brainracer.domain.entities.Question(
                            id                 = "q1",
                            questionText       = "Сколько хромосом у человека?",
                            questionType       = QuestionType.MULTIPLE_CHOICE,
                            options            = listOf("23", "46", "48", "64"),
                            correctAnswerIndex = 1,
                            explanation        = "У человека 46 хромосом",
                            points             = 10,
                            timeLimit          = 30
                        )
                    ),
                    stats       = QuizStats(),
                    createdBy   = userId,
                    createdAt   = Timestamp.now(),
                    timePerQuestion = 30
                )

                when (quizRepository.createQuiz(demoQuiz)) {
                    is Result.Success -> {
                        delay(1000)
                        loadQuizzes()
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Демо-викторины добавлены!")
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Не удалось добавить викторины")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Ошибка: ${e.localizedMessage}")
                }
            }
        }
    }
}