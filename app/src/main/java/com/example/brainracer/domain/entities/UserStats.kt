package com.example.brainracer.domain.entities

data class UserStats(
    val totalQuizzes: Int,
    val correctAnswers: Int,
    val averageScore: Double,
    val totalScore: Int,
    val categoryStats: List<CategoryStats>,
    val achievements: List<UserAchievement>
) {
    fun calculateLevel(): Int = (totalScore / 1000) + 1
}
