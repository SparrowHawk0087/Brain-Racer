package com.example.brainracer.domain.entities

data class UserStats(
    val totalQuizzesTaken: Int = 0,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val averageScore: Double = 0.0,
    val quizzesCreated: Int = 0
) {
    constructor() : this(0, 0, 0, 0, 0, 0, 0, 0.0, 0)
}