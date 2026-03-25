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

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    /** Вызовы с этими id не показываем (отклонён, отменён, завершён, истёк) — дублирует очистку в Firestore. */
    val hiddenChallengeIds: Set<String> = emptySet(),
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

    init {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Войдите в аккаунт")
            }
        } else {
            viewModelScope.launch {
                try {
                    repository.observeNotificationsForUser(uid).collect { list ->
                        _uiState.update {
                            it.copy(items = list, isLoading = false, errorMessage = null)
                        }
                        enrichJob?.cancel()
                        enrichJob = viewModelScope.launch {
                            syncHiddenChallengeIds(list)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Ошибка загрузки")
                    }
                }
            }
        }
    }

    private suspend fun syncHiddenChallengeIds(list: List<AppNotification>) {
        val challengeIds = list
            .filter { it.type == AppNotificationType.CHALLENGE }
            .mapNotNull { it.challengeId }
            .distinct()
        if (challengeIds.isEmpty()) {
            _uiState.update { it.copy(hiddenChallengeIds = emptySet()) }
            return
        }
        val hidden = mutableSetOf<String>()
        for (cid in challengeIds) {
            when (val r = challengeRepository.getChallenge(cid)) {
                is Result.Success -> {
                    when (r.data.status) {
                        ChallengeStatus.DECLINED,
                        ChallengeStatus.CANCELLED,
                        ChallengeStatus.COMPLETED,
                        ChallengeStatus.EXPIRED -> hidden.add(cid)
                        else -> { }
                    }
                }
                is Result.Error -> { }
            }
        }
        _uiState.update { it.copy(hiddenChallengeIds = hidden) }
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
