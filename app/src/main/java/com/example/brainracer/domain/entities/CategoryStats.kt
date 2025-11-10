package com.example.brainracer.domain.entities

data class CategoryStats(
    val category: String,
    val improvement: Double,
    val quizzesCompleted: Int,
    val correctAnswers: Int,
    val averageScore: Double
)