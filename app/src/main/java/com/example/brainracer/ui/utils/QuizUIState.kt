package com.example.brainracer.ui.utils

data class QuizUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val question: String = "",
    val options: List<String> = emptyList(),
    val selectedAnswerIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val isAnswerCorrect: Boolean? = null,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val isQuizCompleted: Boolean = false,
    val attachedImageUrl: String? = null,
    val showResults: Boolean = false,
    val accuracy: Double = 0.0
)