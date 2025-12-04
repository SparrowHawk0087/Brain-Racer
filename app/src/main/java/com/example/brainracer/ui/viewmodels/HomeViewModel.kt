package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.ui.utils.HomeUiState
import com.example.brainracer.ui.utils.QuizItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val quizRepository = QuizRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            println("DEBUG: loadInitialData started")

            // 1. Загружаем пользователя
            val userId = auth.currentUser?.uid
            println("DEBUG: Current user ID = $userId")

            userId?.let {
                loadUserData(it)
            } ?: run {
                // Если пользователь не авторизован, показываем "Гость"
                _uiState.update { it.copy(userName = "Гость") }
                println("DEBUG: No user, setting to 'Гость'")
            }

            // 2. Загружаем викторины с задержкой
            delay(1000)
            println("DEBUG: Loading quizzes...")
            loadQuizzes()
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val userResult = userRepository.getUser(userId)

                userResult.fold(
                    onSuccess = { user ->
                        // Получаем имя из nickname или email - гарантируем String (не nullable)
                        val userName = when {
                            !user.nickname.isNullOrBlank() -> user.nickname!!
                            !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName ?: "Гость"
                            !user.email.isNullOrBlank() -> {
                                val nameFromEmail = user.email!!.split("@").firstOrNull()
                                nameFromEmail ?: "Гость"
                            }
                            else -> "Гость"
                        }

                        _uiState.update {
                            it.copy(
                                userName = userName,
                                userStats = user.stats,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        // Если не удалось загрузить из Firestore, используем данные из Firebase Auth
                        val userName = when {
                            !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName ?: "Гость"
                            !auth.currentUser?.email.isNullOrBlank() -> {
                                val nameFromEmail = auth.currentUser?.email?.split("@")?.firstOrNull()
                                nameFromEmail ?: "Гость"
                            }
                            else -> "Гость"
                        }

                        _uiState.update {
                            it.copy(
                                userName = userName,
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                // Используем данные из Firebase Auth при ошибке
                val userName = when {
                    !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName ?: "Гость"
                    !auth.currentUser?.email.isNullOrBlank() -> {
                        val nameFromEmail = auth.currentUser?.email?.split("@")?.firstOrNull()
                        nameFromEmail ?: "Гость"
                    }
                    else -> "Гость"
                }

                _uiState.update {
                    it.copy(
                        userName = userName,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshUserName() {
        auth.currentUser?.uid?.let { userId ->
            loadUserData(userId)
        }
    }

    /// ЗАГРУЗКА ВИКТОРИН
    fun loadQuizzes() {
        println("DEBUG: loadQuizzes() called")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                println("DEBUG: Calling quizRepository.getPopularQuizzes()")
                val result = quizRepository.getPopularQuizzes(limit = 20)
                println("DEBUG: Got result from getPopularQuizzes")

                result.fold(
                    onSuccess = { quizzes ->
                        println("DEBUG: Success! Got ${quizzes.size} quizzes")

                        val quizItems = quizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.category,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name,
                                description = quiz.description,
                                rating = quiz.stats.averageRating,
                                playCount = quiz.stats.timesTaken
                            )
                        }

                        println("DEBUG: Created ${quizItems.size} quiz items")

                        if (quizItems.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    quizzes = emptyList(),
                                    errorMessage = "Викторин нет. Нажмите ➕ чтобы добавить"
                                )
                            }
                            println("DEBUG: No quiz items found")
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    quizzes = quizItems,
                                    selectedCategory = "Все"
                                )
                            }
                            println("DEBUG: Updated UI state with ${quizItems.size} quizzes")
                        }
                    },
                    onFailure = { exception ->
                        println("DEBUG: Error loading quizzes: ${exception.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                println("DEBUG: Exception in loadQuizzes: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // МЕТОД ДЛЯ ДОБАВЛЕНИЯ ДЕМО-ВИКТОРИН
    fun addDemoQuizzes() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Создаем простые тестовые викторины
                val currentUserId = auth.currentUser?.uid ?: "demo_user_${System.currentTimeMillis()}"

                val testQuizzes = listOf(
                    com.example.brainracer.domain.entities.Quiz(
                        id = "test_quiz_${System.currentTimeMillis()}",
                        title = "Тест: Основы биологии",
                        description = "Простая тестовая викторина",
                        category = "Биология",
                        difficulty = com.example.brainracer.domain.entities.QuizDifficulty.EASY,
                        tags = listOf("тест", "биология"),
                        questions = listOf(
                            com.example.brainracer.domain.entities.Question(
                                id = "q1",
                                questionText = "Сколько хромосом у человека?",
                                questionType = com.example.brainracer.domain.entities.QuestionType.MULTIPLE_CHOICE,
                                options = listOf("23", "46", "48", "64"),
                                correctAnswerIndex = 1,
                                explanation = "У человека 46 хромосом",
                                points = 10,
                                timeLimit = 30
                            )
                        ),
                        stats = com.example.brainracer.domain.entities.QuizStats(
                            timesTaken = 0,
                            averageScore = 0.0,
                            totalAttempts = 0,
                            completionRate = 0.0,
                            ratingsCount = 0,
                            averageRating = 0.0
                        ),
                        createdBy = currentUserId,
                        createdAt = com.google.firebase.Timestamp.now(),
                        isPublic = true,
                        timePerQuestion = 30
                    )
                )

                var successCount = 0
                for (quiz in testQuizzes) {
                    try {
                        quizRepository.createQuiz(quiz).fold(
                            onSuccess = {
                                successCount++
                            },
                            onFailure = {
                                // Игнорируем ошибку
                            }
                        )
                    } catch (e: Exception) {
                        // Продолжаем несмотря на ошибки
                    }
                    delay(500)
                }

                if (successCount > 0) {
                    // Ждем и перезагружаем
                    delay(2000)
                    loadQuizzes()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Демо-викторины добавлены!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Не удалось добавить викторины"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadQuizzesByCategory(category: String) {
        // Фильтруем уже загруженные викторины
        _uiState.update {
            it.copy(
                selectedCategory = category,
                isLoading = true
            )
        }

        viewModelScope.launch {
            delay(500) // Имитация загрузки

            val currentQuizzes = _uiState.value.quizzes
            val filtered = if (category == "Все") {
                currentQuizzes
            } else {
                currentQuizzes.filter { it.category == category }
            }

            _uiState.update {
                it.copy(
                    quizzes = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}