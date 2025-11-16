package com.example.brainracer.domain.entities

import java.time.LocalDateTime

enum class QuizStatus {
    DRAFT, UNDER_REVIEW, PUBLISHED, ARCHIVED
}
enum class QuizDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

// Сущность "Викторина"
data class Quiz(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: QuizDifficulty,
    val questions: List<Question>,
    val authorId: String,
    val status: QuizStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val playCount: Int,
    val averageRating: Double
) {
    fun getQuestionCount(): Int = questions.size
    fun getTotalTime(): Long = questions.sumOf { it.timeLimit }
}

