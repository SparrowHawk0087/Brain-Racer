package com.example.brainracer.domain.entities

enum class AchievementType {
    QUIZZES_COMPLETED, CORRECT_ANSWERS, STREAK, CATEGORY_MASTERY, SOCIAL
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val type: AchievementType,
    val targetValue: Int
)