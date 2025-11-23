package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.ui.utils.ProfileUIState
import com.example.brainracer.ui.utils.QuizItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String? = null) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val mockProfile = createMockProfile()
                _uiState.update { mockProfile.copy(isLoading = false) }
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

    fun toggleQuizLike(quizId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val updatedLikedQuizzes = currentState.likedQuizzes.toMutableList()
                currentState.copy(likedQuizzes = updatedLikedQuizzes)
            }
        }
    }

    fun deleteCreatedQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val updatedCreatedQuizzes = currentState.createdQuizzes
                    .filter { it.id != quizId }
                currentState.copy(createdQuizzes = updatedCreatedQuizzes)
            }
        }
    }

    fun updateUserAvatar(avatarUrl: String) {
        _uiState.update { it.copy(avatarUrl = avatarUrl) }
    }

    fun updateUsername(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun updateGameStats(isWin: Boolean, pointsEarned: Int) {
        _uiState.update { currentState ->
            val newGamesPlayed = currentState.gamesPlayed + 1
            val newGamesWon = currentState.gamesWon + if (isWin) 1 else 0
            val newWinRate = if (newGamesPlayed > 0) {
                (newGamesWon.toDouble() / newGamesPlayed) * 100
            } else 0.0
            val newTotalPoints = currentState.totalPoints + pointsEarned

            currentState.copy(
                gamesPlayed = newGamesPlayed,
                gamesWon = newGamesWon,
                winRate = newWinRate,
                totalPoints = newTotalPoints
            )
        }
    }

    private fun createMockProfile(): ProfileUIState {
        return ProfileUIState(
            userId = "user_123",
            username = "BrainMaster",
            email = "brainmaster@example.com",
            avatarUrl = "https://example.com/avatar.jpg",
            registrationDate = "2024-01-15",
            gamesPlayed = 150,
            gamesWon = 95,
            winRate = 63.3,
            totalPoints = 12500,
            likedQuizzes = listOf(
                QuizItem(
                    id = "quiz_1",
                    title = "История Древнего Рима",
                    category = "История",
                    questionCount = 15
                )
            ),
            createdQuizzes = listOf(
                QuizItem(
                    id = "quiz_2",
                    title = "Космос и вселенная",
                    category = "Наука",
                    questionCount = 20
                )
            )
        )
    }
}