package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.NotificationRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.AppNotification
import com.example.brainracer.domain.entities.AppNotificationType
import com.example.brainracer.domain.entities.ChallengeStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    /** Вызовы с этими id не показываем (отклонён, отменён, завершён, истёк) — дублирует очистку в Firestore. */
    val hiddenChallengeIds: Set<String> = emptySet(),
    /**
     * 'true' после того, как [syncHiddenChallengeIds] отработал для текущего снимка —
     * до этого список вызовов на экране не показываем (иначе мелькают все уведомления,
     * а после фильтра остаются только актуальные).
     */
    val challengesTabBadgeReady: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val repository = NotificationRepositoryImpl()
    private val challengeRepository = ChallengeRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private var enrichJob: Job? = null
    private var hiddenSyncGeneration: Int = 0

    init {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Войдите в аккаунт",
                    challengesTabBadgeReady = true
                )
            }
        } else {
            viewModelScope.launch {
                try {
                    repository.observeNotificationsForUser(uid).collect { list ->
                        hiddenSyncGeneration++
                        val gen = hiddenSyncGeneration
                        // Не сбрасываем challengesTabBadgeReady здесь: иначе при каждом снимке
                        // снова показываются «все» вызовы до конца sync (мигание старых карточек).
                        _uiState.update {
                            it.copy(
                                items = list,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        enrichJob?.cancel()
                        enrichJob = viewModelScope.launch {
                            syncHiddenChallengeIds(list, gen)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Ошибка загрузки",
                            challengesTabBadgeReady = true
                        )
                    }
                }
            }
        }
    }

    private suspend fun syncHiddenChallengeIds(list: List<AppNotification>, generation: Int) {
        val challengeIds = list
            .filter { it.type == AppNotificationType.CHALLENGE }
            .mapNotNull { it.challengeId }
            .distinct()
        if (challengeIds.isEmpty()) {
            if (generation != hiddenSyncGeneration) return
            _uiState.update {
                it.copy(hiddenChallengeIds = emptySet(), challengesTabBadgeReady = true)
            }
            return
        }
        val now = Date()
        val hidden = mutableSetOf<String>()
        for (cid in challengeIds) {
            when (val r = challengeRepository.getChallenge(cid)) {
                is Result.Success -> {
                    val ch = r.data
                    val expiredByTime = !ch.expiresAt.toDate().after(now)
                    when (ch.status) {
                        ChallengeStatus.DECLINED,
                        ChallengeStatus.CANCELLED,
                        ChallengeStatus.COMPLETED,
                        ChallengeStatus.EXPIRED -> hidden.add(cid)
                        else -> {
                            // В БД часто остаётся PENDING после истечения срока — не показываем такие вызовы.
                            if (expiredByTime) hidden.add(cid)
                        }
                    }
                }
                is Result.Error -> {
                    // Документ вызова удалён или нет доступа — считаем уведомление неактуальным.
                    hidden.add(cid)
                }
            }
        }
        if (generation != hiddenSyncGeneration) return
        _uiState.update {
            it.copy(hiddenChallengeIds = hidden, challengesTabBadgeReady = true)
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (repository.markAsRead(notificationId)) {
                is Result.Error -> { }
                is Result.Success -> { }
            }
        }
    }
}
