package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.domain.entities.Category
import com.example.brainracer.ui.utils.HomeUiState
import com.example.brainracer.ui.utils.QuizItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    private val quizRepository = QuizRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    // добавлено свойство
    private val firestore = FirebaseFirestore.getInstance()

    // Кэш полного списка викторин — нужен, чтобы фильтрация по категории
    // не «съедала» записи безвозвратно (логическая ошибка в оригинале).
    private var allQuizzes: List<QuizItem> = emptyList()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            println("DEBUG: loadInitialData started")

            val userId = auth.currentUser?.uid
            println("DEBUG: Current user ID = $userId")

            if (userId != null) {
                loadUserData(userId)
            } else {
                _uiState.update { it.copy(userName = "Гость") }
                println("DEBUG: No user, setting to 'Гость'")
            }

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
                        val userName = when {
                            !user.nickname.isNullOrBlank() -> user.nickname!!
                            !auth.currentUser?.displayName.isNullOrBlank() ->
                                auth.currentUser?.displayName ?: "Гость"
                            !user.email.isNullOrBlank() ->
                                user.email!!.split("@").firstOrNull() ?: "Гость"
                            else -> "Гость"
                        }
                        _uiState.update { state ->
                            state.copy(userName = userName, userStats = user.stats, isLoading = false)
                        }
                    },
                    onFailure = { exception ->
                        val userName = when {
                            !auth.currentUser?.displayName.isNullOrBlank() ->
                                auth.currentUser?.displayName ?: "Гость"
                            !auth.currentUser?.email.isNullOrBlank() ->
                                auth.currentUser?.email?.split("@")?.firstOrNull() ?: "Гость"
                            else -> "Гость"
                        }
                        _uiState.update { state -> state.copy(userName = userName, isLoading = false) }
                    }
                )
            } catch (e: Exception) {
                val userName = auth.currentUser?.displayName
                    ?: auth.currentUser?.email?.split("@")?.firstOrNull()
                    ?: "Гость"
                _uiState.update { state -> state.copy(userName = userName, isLoading = false) }
            }
        }
    }

    fun refreshUserName() {
        auth.currentUser?.uid?.let { loadUserData(it) }
    }

    // ── Загрузка викторин ─────────────────────────────────────────────────────

    fun loadQuizzes() {
        println("DEBUG: loadQuizzes() called")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = quizRepository.getPopularQuizzes(limit = 100)
                result.fold(
                    onSuccess = { quizzes ->
                        val quizItems = quizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.categoryId,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name,
                                description = quiz.description,
                                rating = quiz.stats.averageRating,
                                playCount = quiz.stats.timesTaken
                            )
                        }

                        // Сохраняем полный список в кэш
                        allQuizzes = quizItems

                        if (quizItems.isEmpty()) {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    quizzes = emptyList(),
                                    errorMessage = "Викторин нет. Нажмите ➕ чтобы добавить"
                                )
                            }
                        } else {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    quizzes = quizItems,
                                    selectedCategory = "Все"
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки: ${exception.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = "Ошибка: ${e.localizedMessage}")
                }
            }
        }
    }

    // ── Фильтрация по категории ───────────────────────────────────────────────
    //
    // Исправление логической ошибки: фильтруем allQuizzes (полный кэш),
    // а не _uiState.value.quizzes (который уже мог быть обрезан).
    // Теперь переключение обратно на «Все» корректно восстанавливает список.

    fun loadQuizzesByCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, isLoading = true) }

        viewModelScope.launch {
            delay(300) // Небольшая задержка для ощущения отклика
            val filtered = if (category == "Все") allQuizzes
            else allQuizzes.filter { it.category == category }
            _uiState.update { state ->
                state.copy(quizzes = filtered, isLoading = false)
            }
        }
    }

    // ── Загрузка категорий из Firestore ───────────────────────────────────────

    private suspend fun loadCategories() {
        // Исправление: firestore теперь объявлен как свойство класса (см. выше).
        // Исправление вложенного it: явно именуем параметр внешней лямбды `state`,
        // иначе Kotlin не может разобраться, к какому типу относится `it` —
        // к HomeUiState (update) или к Category (map), и бросает ошибку компиляции.
        val snapshot = firestore.collection("categories")
            .orderBy("order")
            .get()
            .await()                                    // await() теперь работает — импорт добавлен

        val categoryNames = snapshot.documents
            .mapNotNull { doc -> doc.toObject(Category::class.java) }
            .map { category -> category.name }          // явный параметр — нет конфликта с `it`

        _uiState.update { state ->                      // `state` вместо `it` — устраняет shadowing
            state.copy(categories = categoryNames)
        }
    }

    // ── Добавление демо-викторин ──────────────────────────────────────────────

    fun addDemoQuizzes() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                    ?: "demo_user_${System.currentTimeMillis()}"

                val testQuizzes = listOf(
                    com.example.brainracer.domain.entities.Quiz(
                        id = "test_quiz_${System.currentTimeMillis()}",
                        title = "Тест: Основы биологии",
                        description = "Простая тестовая викторина",
                        categoryId = "Биология",
                        difficulty = com.example.brainracer.domain.entities.QuizDifficulty.EASY,
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
                        timePerQuestion = 30
                    )
                )

                var successCount = 0
                for (quiz in testQuizzes) {
                    try {
                        quizRepository.createQuiz(quiz).fold(
                            onSuccess = { successCount++ },
                            onFailure = { /* игнорируем */ }
                        )
                    } catch (e: Exception) {
                        // Продолжаем несмотря на ошибки
                    }
                    delay(500)
                }

                if (successCount > 0) {
                    delay(2000)
                    loadQuizzes()
                    _uiState.update { state ->
                        state.copy(isLoading = false, errorMessage = "Демо-викторины добавлены!")
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(isLoading = false, errorMessage = "Не удалось добавить викторины")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = "Ошибка: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}