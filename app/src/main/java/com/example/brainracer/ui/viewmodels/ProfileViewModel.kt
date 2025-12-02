package com.example.brainracer.ui.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.ui.utils.fold
import com.example.brainracer.domain.entities.User as DomainUser
import com.example.brainracer.ui.utils.ProfileUIState
import com.example.brainracer.ui.utils.QuizItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.brainracer.ui.utils.Result

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()

    fun loadUserProfile(userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                userRepository.getUser(userId).fold(
                    onSuccess = { user ->
                        loadUserQuizzes(user)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки профиля: ${error.message}"
                            )
                        }
                    }
                )
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

    private fun loadUserQuizzes(user: DomainUser) {
        viewModelScope.launch {
            try {
                // Загружаем созданные викторины
                when (val createdQuizzesResult = quizRepository.getQuizzesByUser(user.id)) {
                    is Result.Success -> {
                        val createdQuizzes = createdQuizzesResult.data

                        // Конвертируем в QuizItem для UI
                        val createdQuizItems = createdQuizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.category,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name
                            )
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userId = user.id,
                                username = user.nickname,
                                email = user.email,
                                avatarUrl = user.avatarUrl,
                                registrationDate = user.createdAt.toDate().toString(),
                                gamesPlayed = user.stats.totalQuizzesTaken,
                                gamesWon = user.stats.correctAnswers,
                                winRate = user.stats.averageScore,
                                totalPoints = user.stats.totalPoints,
                                createdQuizzes = createdQuizItems,
                                likedQuizzes = emptyList()
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки викторин: ${createdQuizzesResult.exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка загрузки викторин: ${e.message}"
                    )
                }
            }
        }
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

    fun updateUsername(userId: String, newUsername: String) {
        viewModelScope.launch {
            when (val userResult = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val user = userResult.data
                    val updatedUser = user.copy(nickname = newUsername)

                    when (val updateResult = userRepository.updateUser(updatedUser)) {
                        is Result.Success -> {
                            _uiState.update { it.copy(username = newUsername) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(errorMessage = "Ошибка обновления имени: ${updateResult.exception.message}") }
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
}


    /* fun toggleQuizLike(quizId: String) {
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
}*/