package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.fold
import com.example.brainracer.data.utils.getOrNull
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChallengeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val incomingChallenges: List<Challenge> = emptyList(),
    val outgoingChallenges: List<Challenge> = emptyList(),
    val activeChallenges: List<Challenge> = emptyList(),
    val completedChallenges: List<Challenge> = emptyList()
)

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val challengeRepository = ChallengeRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadChallenges()
    }

    fun loadChallenges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = auth.currentUser?.uid ?: run {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Пользователь не авторизован") }
                return@launch
            }

            // Загружаем все типы вызовов параллельно
            val incoming = challengeRepository.getIncomingChallenges(userId)
            val outgoing = challengeRepository.getOutgoingChallenges(userId)
            val active = challengeRepository.getActiveChallenges(userId)
            val completed = challengeRepository.getCompletedChallenges(userId)

            // Обновляем состояние
            _uiState.update {
                it.copy(
                    isLoading = false,
                    incomingChallenges = incoming.getOrNull() ?: emptyList(),
                    outgoingChallenges = outgoing.getOrNull() ?: emptyList(),
                    activeChallenges = active.getOrNull() ?: emptyList(),
                    completedChallenges = completed.getOrNull() ?: emptyList()
                )
            }
        }
    }

    fun createChallenge(challengedUserId: String, quizId: String, quizTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val challengerUserId = auth.currentUser?.uid ?: run {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Пользователь не авторизован")
                }
                return@launch
            }

            val challenge = Challenge(
                quizId = quizId,
                quizTitle = quizTitle,
                challengerUserId = challengerUserId,
                challengedUserId = challengedUserId
            )

            challengeRepository.createChallenge(challenge).fold(
                onSuccess = { challengeId ->
                    _uiState.update { it.copy(isLoading = false) }
                    loadChallenges() // Обновляем список
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
            )
        }
    }

    fun acceptChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.acceptChallenge(challengeId).fold(
                onSuccess = { loadChallenges() },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun declineChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.declineChallenge(challengeId).fold(
                onSuccess = { loadChallenges() },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun cancelChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.cancelChallenge(challengeId).fold(
                onSuccess = { loadChallenges() },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun submitChallengeResult(challengeId: String, result: ChallengeResult) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            challengeRepository.submitChallengeResult(challengeId, userId, result).fold(
                onSuccess = { loadChallenges() },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}