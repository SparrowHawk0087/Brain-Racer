package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.QuizStats
import com.example.brainracer.domain.entities.QuestionType
import com.example.brainracer.ui.utils.HomeUiState
import com.example.brainracer.ui.utils.QuizItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val quizRepository = QuizRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val auth           = FirebaseAuth.getInstance()

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
                loadUserData(userId)
            } else {
                _uiState.update { it.copy(userName = "Гость") }
            }
            loadQuizzes()
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
        }
    }

    fun refreshUserName() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadUserData(userId)
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