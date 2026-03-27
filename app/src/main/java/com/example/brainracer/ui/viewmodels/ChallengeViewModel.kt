package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserChallengeSides
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.toQuizItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChallengeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val incomingChallenges: List<Challenge> = emptyList(),
    val outgoingChallenges: List<Challenge> = emptyList(),
    val activeChallenges: List<Challenge> = emptyList(),
    val completedChallenges: List<Challenge> = emptyList(),
    val friendsForChallenge: List<User> = emptyList(),
    val challengePickerQuizzes: List<QuizItem> = emptyList(),
    val challengePickerLoading: Boolean = false,
    val challengeSentMessage: String? = null
)

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val challengeRepository = ChallengeRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    init {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Пользователь не авторизован")
                }
                return@launch
            }
            try {
                challengeRepository.observeUserChallengeSides(userId).collect { sides ->
                    _uiState.update { mergeChallengeSides(it, sides, userId) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Вызовы: ${e.message ?: "ошибка доступа к Firestore"}")
                }
            }
        }
    }

    /** Разовая подгрузка (например после сбоя слушателя). Списки обычно обновляются через Firestore snapshot. */
    fun loadChallenges() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            when (val r = challengeRepository.fetchUserChallengeSides(userId)) {
                is Result.Success ->
                    _uiState.update { mergeChallengeSides(it, r.data, userId) }
                is Result.Error ->
                    _uiState.update { s ->
                        s.copy(isLoading = false, errorMessage = r.exception.message)
                    }
            }
        }
    }

    private fun mergeChallengeSides(
        state: ChallengeUiState,
        sides: UserChallengeSides,
        @Suppress("UNUSED_PARAMETER") userId: String
    ): ChallengeUiState {
        val incoming = sides.asChallenged.filter { it.status == ChallengeStatus.PENDING }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )
        val outgoingAllowed = setOf(
            ChallengeStatus.PENDING,
            ChallengeStatus.ACCEPTED,
            ChallengeStatus.COMPLETED
        )
        val outgoing = sides.asChallenger.filter { it.status in outgoingAllowed }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )
        val nowDate = Timestamp.now().toDate()
        val active = (sides.asChallenged + sides.asChallenger)
            .distinctBy { it.id }
            .filter {
                it.status == ChallengeStatus.ACCEPTED &&
                    it.expiresAt.toDate().after(nowDate)
            }
            .sortedWith(
                compareBy<Challenge> { it.expiresAt.seconds }.thenBy { it.expiresAt.nanoseconds }
            )
        val completed = (sides.asChallenged + sides.asChallenger)
            .filter { it.status == ChallengeStatus.COMPLETED }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Challenge> { it.completedAt?.seconds ?: 0L }
                    .thenByDescending { it.completedAt?.nanoseconds ?: 0 }
            )
            .take(50)
        return state.copy(
            isLoading            = false,
            incomingChallenges   = incoming,
            outgoingChallenges   = outgoing,
            activeChallenges     = active,
            completedChallenges  = completed
        )
    }

    fun createChallenge(challengedUserId: String, quizId: String, quizTitle: String) {
        viewModelScope.launch {
            val challengerUserId = auth.currentUser?.uid ?: run {
                _uiState.update { it.copy(errorMessage = "Пользователь не авторизован") }
                return@launch
            }
            val challenge = Challenge(
                quizId = quizId,
                quizTitle = quizTitle,
                challengerUserId = challengerUserId,
                challengedUserId = challengedUserId
            )
            challengeRepository.createChallenge(challenge).fold(
                onSuccess = {
                    _uiState.update { it.copy(errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun acceptChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.acceptChallenge(challengeId).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun declineChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.declineChallenge(challengeId).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun cancelChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.cancelChallenge(challengeId).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
        }
    }

    fun submitChallengeResult(challengeId: String, result: ChallengeResult) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            challengeRepository.submitChallengeResult(challengeId, uid, result).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            )
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
                is Result.Success -> r.data.map { it.toQuizItem() }
                is Result.Error -> emptyList()
            }
            _uiState.update {
                it.copy(
                    friendsForChallenge    = friends,
                    challengePickerQuizzes = quizzes,
                    challengePickerLoading = false
                )
            }
        }
    }

    fun sendChallengeFromPicker(friendId: String, quizId: String, quizTitle: String) {
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
}