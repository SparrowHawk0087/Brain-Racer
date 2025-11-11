package com.example.brainracer.domain.entities

import java.time.LocalDateTime

data class UserAnswer(
    val questionId: String,
    val selectedAnswerId: String,
    val isCorrect: Boolean,
    val timeSpent: Long
)
