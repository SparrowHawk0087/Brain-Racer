package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.ui.utils.QuizUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUIState())
    val uiState: StateFlow<QuizUIState> = _uiState.asStateFlow()


    private fun loadQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                question = "Вопрос",
                options = listOf("", "", "", ""),
                totalQuestions = 10 /* тут 10 вопросов всего в каждом по 4 ответа для примера 10 поменять можно */
            )
        }
    }


    fun selectAnswer(answerIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedAnswerIndex = answerIndex)
    }


    fun submitAnswer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnswerSubmitted = true,
                score = _uiState.value.score +1
            )
        }
    }


    fun attachImage(imageUrl: String) {
        _uiState.value = _uiState.value.copy(attachedImageUrl = imageUrl)
    }

    fun nextQuestion() {
        viewModelScope.launch {
            val nextIndex = _uiState.value.currentQuestionIndex + 1
            if (nextIndex >= _uiState.value.totalQuestions) {
                _uiState.value = _uiState.value.copy(isQuizCompleted = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    currentQuestionIndex = nextIndex,
                    selectedAnswerIndex = null,
                    isAnswerSubmitted = false,
                    attachedImageUrl = null
                )
            }
        }
    }


    fun restartQuiz() {
        _uiState.value = QuizUIState()
        loadQuiz()
    }

    override fun onCleared() {
        super.onCleared()
    }
}