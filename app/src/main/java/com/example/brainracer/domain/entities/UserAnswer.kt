package com.example.brainracer.domain.entities

// Сущность ответа пользователя
data class UserAnswer(
    val questionId: String = "",
    val selectedAnswerIndex: Int = -1,
    val isCorrect: Boolean = false,
    val timeSpent: Int = 0
)