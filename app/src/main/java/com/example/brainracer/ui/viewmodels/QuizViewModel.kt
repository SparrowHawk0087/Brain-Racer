package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.UserAnswer
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.fold
import com.example.brainracer.ui.utils.QuizUIState
import com.example.brainracer.ui.utils.XpBreakdown
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUIState())
    val uiState: StateFlow<QuizUIState> = _uiState.asStateFlow()

    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()
    private val auth           = FirebaseAuth.getInstance()

    private var currentQuiz: Quiz? = null
    private val userAnswers        = mutableListOf<UserAnswer>()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private var totalTimeSpent    = 0
    private var questionStartTime = 0L

    // ── Загрузка викторины ────────────────────────────────────────────────

    fun loadQuiz(quizId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            quizRepository.getQuiz(quizId).fold(
                onSuccess = { quiz ->
                    currentQuiz = quiz
                    userAnswers.clear()
                    totalTimeSpent = 0

                    if (quiz.questions.isNotEmpty()) {
                        questionStartTime = System.currentTimeMillis()
                        val first = quiz.questions[0]
                        _uiState.value = QuizUIState(
                            isLoading                = false,
                            question                 = first.questionText,
                            options                  = first.options,
                            totalQuestions           = quiz.questions.size,
                            currentQuestionTimeLimit = first.timeLimit.coerceAtLeast(5),
                            attachedImageUrl         = first.imageUrl ?: first.gifUrl
                        )
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "В викторине нет вопросов")
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Ошибка загрузки: ${error.message}")
                    }
                }
            )
        }
    }

    // ── Выбор ответа ──────────────────────────────────────────────────────

    fun selectAnswer(answerIndex: Int) {
        if (_uiState.value.isAnswerSubmitted) return
        _uiState.update { it.copy(selectedAnswerIndex = answerIndex) }
    }

    // ── Подтверждение ответа ──────────────────────────────────────────────

    fun submitAnswer() {
        val state    = _uiState.value
        val quiz     = currentQuiz ?: return
        val selected = state.selectedAnswerIndex ?: run {
            _uiState.update { it.copy(errorMessage = "Выберите ответ") }
            return
        }
        if (state.isAnswerSubmitted) return

        recordAnswer(
            quiz         = quiz,
            state        = state,
            selectedIdx  = selected,
            usedFullTime = false
        )
    }

    /**
     * Вызывается UI когда таймер истёк и пользователь не успел ответить.
     * Засчитывает вопрос как неверный, не требует выбранного ответа.
     */
    fun timeoutQuestion() {
        val state = _uiState.value
        val quiz  = currentQuiz ?: return
        if (state.isAnswerSubmitted) return   // уже обработан

        recordAnswer(
            quiz         = quiz,
            state        = state,
            selectedIdx  = -1,     // -1 = не выбрано
            usedFullTime = true
        )
    }

    /**
     * Общая логика записи ответа (как явного, так и по тайм-ауту).
     */
    private fun recordAnswer(
        quiz: Quiz,
        state: QuizUIState,
        selectedIdx: Int,
        usedFullTime: Boolean
    ) {
        val qIndex   = state.currentQuestionIndex
        val question = quiz.questions.getOrNull(qIndex) ?: return

        val timeSpent = if (usedFullTime) {
            question.timeLimit
        } else {
            ((System.currentTimeMillis() - questionStartTime) / 1000).toInt()
        }
        totalTimeSpent += timeSpent

        val isCorrect = selectedIdx != -1 && selectedIdx == question.correctAnswerIndex

        userAnswers.add(
            UserAnswer(
                questionId          = question.id,
                selectedAnswerIndex = selectedIdx,
                isCorrect           = isCorrect,
                timeSpent           = timeSpent
            )
        )

        _uiState.update {
            it.copy(
                isAnswerSubmitted = true,
                isAnswerCorrect   = isCorrect,
                score             = it.score + if (isCorrect) question.points else 0,
                correctAnswers    = it.correctAnswers   + if (isCorrect) 1 else 0,
                incorrectAnswers  = it.incorrectAnswers + if (!isCorrect) 1 else 0
            )
        }

        if (qIndex >= quiz.questions.size - 1) {
            saveQuizResults()
        }
    }

    // ── Следующий вопрос ──────────────────────────────────────────────────

    /**
     * Переходит к следующему вопросу.
     * Защита от двойного вызова: игнорирует, если ответ ещё не подтверждён.
     */
    fun nextQuestion() {
        val state = _uiState.value
        // Защита: нельзя перейти дальше пока ответ не засчитан
        // (предотвращает гонку между авто-переходом и ручным нажатием)
        if (!state.isAnswerSubmitted) return

        val quiz    = currentQuiz ?: return
        val nextIdx = state.currentQuestionIndex + 1

        if (nextIdx >= quiz.questions.size) {
            _uiState.update { it.copy(isQuizCompleted = true, showResults = true) }
            return
        }

        val next = quiz.questions[nextIdx]
        questionStartTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                currentQuestionIndex     = nextIdx,
                selectedAnswerIndex      = null,
                isAnswerSubmitted        = false,
                isAnswerCorrect          = null,
                errorMessage             = null,
                question                 = next.questionText,
                options                  = next.options,
                currentQuestionTimeLimit = next.timeLimit.coerceAtLeast(5),
                attachedImageUrl         = next.imageUrl ?: next.gifUrl
            )
        }
    }

    // ── Сохранение результатов + расчёт XP ───────────────────────────────

    private fun saveQuizResults() {
        viewModelScope.launch {
            val quiz   = currentQuiz ?: return@launch
            val userId = currentUserId ?: run {
                finishWithXp(quiz, xpBefore = 0, saveToFirestore = false)
                return@launch
            }

            val xpBefore = when (val r = userRepository.getUser(userId)) {
                is Result.Success -> r.data.stats.totalPoints
                is Result.Error   -> 0
            }

            finishWithXp(quiz, xpBefore, saveToFirestore = true, userId = userId)
        }
    }

    private suspend fun finishWithXp(
        quiz: Quiz,
        xpBefore: Int,
        saveToFirestore: Boolean,
        userId: String? = null
    ) {
        val totalQ   = quiz.questions.size
        val correct  = userAnswers.count { it.isCorrect }
        val accuracy = if (totalQ > 0) correct.toDouble() / totalQ * 100 else 0.0

        val xpResult = LevelSystem.calculateQuizXp(
            answers    = userAnswers,
            questions  = quiz.questions,
            difficulty = quiz.difficulty,
            xpBefore   = xpBefore
        )

        val breakdown = XpBreakdown(
            baseXp          = xpResult.baseXp,
            speedBonusXp    = xpResult.speedBonusXp,
            accuracyBonusXp = xpResult.accuracyBonusXp,
            difficultyLabel = "${xpResult.difficultyMultiplierLabel} (${quiz.difficulty.name})",
            totalXp         = xpResult.totalXp
        )

        val newTotalXp  = xpBefore + xpResult.totalXp
        val newLevel    = LevelSystem.levelFromXp(newTotalXp)
        val newProgress = LevelSystem.levelProgress(newTotalXp)

        _uiState.update {
            it.copy(
                showResults      = true,
                isQuizCompleted  = true,
                accuracy         = accuracy,
                xpEarned         = xpResult.totalXp,
                xpBreakdown      = breakdown,
                leveledUp        = xpResult.leveledUp,
                newLevel         = newLevel,
                newLevelProgress = newProgress,
                reviewQuestions  = quiz.questions,
                reviewAnswers    = userAnswers.toList()
            )
        }

        if (!saveToFirestore || userId == null) return

        val avgTime  = if (totalQ > 0) totalTimeSpent.toDouble() / totalQ else 0.0
        val nickname = when (val r = userRepository.getUser(userId)) {
            is Result.Success -> r.data.nickname
            is Result.Error   -> "Игрок"
        }

        val quizResult = com.example.brainracer.domain.entities.ChallengeResult(
            quizId                 = quiz.id,
            userId                 = userId,
            userNickname           = nickname,
            score                  = _uiState.value.score,
            totalQuestions         = totalQ,
            correctAnswers         = correct,
            incorrectAnswers       = totalQ - correct,
            timeSpent              = totalTimeSpent,
            averageTimePerQuestion = avgTime,
            answers                = userAnswers.toList(),
            pointsEarned           = xpResult.totalXp
        )

        quizRepository.recordQuizResult(quizResult)
        userRepository.updateUserStats(userId, quizResult)
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    fun restartQuiz() {
        currentQuiz?.id?.let { id ->
            userAnswers.clear()
            totalTimeSpent = 0
            _uiState.value = QuizUIState()
            loadQuiz(id)
        }
    }

    fun closeResults() {
        _uiState.update { it.copy(showResults = false) }
    }

    fun attachImage(imageUrl: String) {
        _uiState.update { it.copy(attachedImageUrl = imageUrl) }
    }
}