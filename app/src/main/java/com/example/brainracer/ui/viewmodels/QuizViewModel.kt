package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.UserAnswer
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.fold
import com.example.brainracer.ui.utils.QuizUIState
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
    private var currentQuiz: Quiz? = null
    private var userAnswers = mutableListOf<UserAnswer>()
    private var currentUserId: String? = null
    private var totalTimeSpent: Int = 0
    private var questionStartTime: Long = 0L
    private fun loadQuiz(quizId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try{
                quizRepository.getQuiz(quizId).fold(
                    onSuccess = {
                        quiz ->
                        currentQuiz = quiz
                        userAnswers.clear()
                        totalTimeSpent = 0
                        if (quiz.questions.isNotEmpty()){
                            questionStartTime = System.currentTimeMillis()
                            val firstQuestion = quiz.questions[0]
                            _uiState.value = QuizUIState(
                                isLoading = false,
                                question = firstQuestion.questionText,
                                options = firstQuestion.options,
                                totalQuestions = quiz.questions.size,
                                currentQuestionIndex = 0,
                                attachedImageUrl = firstQuestion.imageUrl ?: firstQuestion.gifUrl
                            )
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "В викторине нет вопросов"
                                )
                            }
                        }
                    },
                    onFailure = {
                        error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки викторины: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception){
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectAnswer(answerIndex: Int) {
        if (_uiState.value.isAnswerSubmitted) return
        _uiState.update {
            it.copy(selectedAnswerIndex = answerIndex)
        }
    }

    fun submitAnswer() {
        val currentState = _uiState.value
        val quiz = currentQuiz
        val selectedIndex = currentState.selectedAnswerIndex

        if (quiz == null || selectedIndex == null){
            _uiState.update {
                it.copy(errorMessage = "Выберите ответ перед отправкой")
            }
            return
        }
        if (currentState.isAnswerSubmitted) {
            return
        }
        val currentQuestionIndex = currentState.currentQuestionIndex
        val currentQuestion = quiz.questions.getOrNull(currentQuestionIndex)
        if (currentQuestion == null) {
            _uiState.update {
                it.copy(errorMessage = "Ошибка: вопрос не найден")
            }
            return
        }

        // Затраченное время на вопрос
        val timeSpentOnQuestion = ((System.currentTimeMillis() - questionStartTime) / 1000).toInt()
        totalTimeSpent += timeSpentOnQuestion

        val isCorrect = selectedIndex == currentQuestion.correctAnswerIndex

        userAnswers.add(
            UserAnswer(
                questionId = currentQuestion.id,
                selectedAnswerIndex = selectedIndex,
                isCorrect = isCorrect,
                timeSpent = timeSpentOnQuestion
            )
        )

        val newScore = currentState.score + if (isCorrect) currentQuestion.points else 0
        val newCorrectAnswers = currentState.correctAnswers + if (isCorrect) 1 else 0
        val newIncorrectAnswers = currentState.incorrectAnswers + if (!isCorrect) 1 else 0

        _uiState.update {
            it.copy(
                isAnswerSubmitted = true,
                isAnswerCorrect = isCorrect,
                score = newScore,
                correctAnswers = newCorrectAnswers,
                incorrectAnswers = newIncorrectAnswers
            )
        }
        if (currentQuestionIndex >= quiz.questions.size - 1) {
            saveQuizResults()
        }
    }

    fun attachImage(imageUrl: String) {
        _uiState.update {
            it.copy(attachedImageUrl = imageUrl)
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val quiz = currentQuiz ?: return
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex >= quiz.questions.size) {
            //Викторина завершена
            _uiState.update {
                it.copy(
                    isQuizCompleted = true,
                    showResults = true
                )
            }
        } else {
            val nextQuestion = quiz.questions[nextIndex]
            // Отсчет времени для нового вопроса
            questionStartTime = System.currentTimeMillis()
            _uiState.update{
                it.copy(
                    currentQuestionIndex = nextIndex,
                    selectedAnswerIndex = null,
                    isAnswerSubmitted = false,
                    attachedImageUrl = nextQuestion.imageUrl ?: nextQuestion.gifUrl,
                    isAnswerCorrect = null,
                    errorMessage = null,
                    question = nextQuestion.questionText,
                    options = nextQuestion.options
                )
            }
        }
    }

    private fun saveQuizResults() {
        viewModelScope.launch {
            val quiz = currentQuiz ?: return@launch
            val userId = currentUserId ?: return@launch

            if (userAnswers.isEmpty()) return@launch

            try {
                val correctCount = userAnswers.count { it.isCorrect }
                val totalQuestions = quiz.questions.size
                val accuracy = if (totalQuestions > 0) {
                    (correctCount.toDouble() / totalQuestions) * 100
                } else 0.0

                // Cреднее время на вопрос
                val averageTime = if (totalQuestions > 0) {
                    totalTimeSpent.toDouble() / totalQuestions
                } else 0.0

                var username = "Игрок"
                when (val userResult = userRepository.getUser(userId)) {
                    is Result.Success -> {
                        username = userResult.data.nickname
                    }
                    is Result.Error -> { }
                }

                val quizResult = com.example.brainracer.domain.entities.ChallengeResult(
                    quizId = quiz.id,
                    userId = userId,
                    userNickname = username,
                    score = _uiState.value.score,
                    totalQuestions = totalQuestions,
                    correctAnswers = correctCount,
                    incorrectAnswers = totalQuestions - correctCount,
                    timeSpent = totalTimeSpent,
                    averageTimePerQuestion = averageTime,
                    answers = userAnswers,
                    pointsEarned = _uiState.value.score
                )

                when (val saveResult = quizRepository.recordQuizResult(quizResult)) {
                    is Result.Success -> {
                        when (val updateResult = userRepository.updateUserStats(userId, quizResult)) {
                            is Result.Success -> {
                                _uiState.update {
                                    it.copy(
                                        showResults = true,
                                        accuracy = accuracy
                                    )
                                }
                            }
                            is Result.Error -> {
                                _uiState.update {
                                    it.copy(
                                        errorMessage = "Ошибка обновления статистики: ${updateResult.exception.message}"
                                    )
                                }
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Ошибка сохранения результатов: ${saveResult.exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun restartQuiz() {
        currentQuiz?.id?.let {
            quizId ->
            _uiState.value = QuizUIState()
            loadQuiz(quizId)
        }
    }

    fun closeResults() {
        _uiState.update {
            it.copy(showResults = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}