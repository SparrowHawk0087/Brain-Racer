package com.example.brainracer.domain.entities

// Сущность ответа пользователя
data class UserAnswer(
    val questionId: String,
    val selectedAnswerId: String,
    val isCorrect: Boolean,
    val timeSpent: Long
)
