package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val questions: List<Question> = emptyList(),
    val stats: QuizStats = QuizStats(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isPublic: Boolean = true,
    val timePerQuestion: Int = 30,
    var totalTime: Int = 0
) {
    val questionCount: Int
        get() = questions.size

    init {
        totalTime = questions.size * timePerQuestion
    }
}

data class QuizStats(
    @PropertyName("times_taken")
    val timesTaken: Int = 0,
    @PropertyName("average_score")
    val averageScore: Double = 0.0,
    @PropertyName("total_attempts")
    val totalAttempts: Int = 0,
    @PropertyName("completion_rate")
    val completionRate: Double = 0.0,
    @PropertyName("ratings_count")
    val ratingsCount: Int = 0,
    @PropertyName("average_rating")
    val averageRating: Double = 0.0
)

enum class QuizDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

enum class QuestionType {
    MULTIPLE_CHOICE, TRUE_FALSE, INPUT_ANSWER
}

