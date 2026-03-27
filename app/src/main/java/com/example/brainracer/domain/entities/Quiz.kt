package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val questions: List<Question> = emptyList(),
    val stats: QuizStats = QuizStats(),
    val createdBy: String = "",
    /** Никнейм автора (для кастомных викторин, пишется при публикации). */
    val creatorNickname: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val timePerQuestion: Int = 30,
    var totalTime: Int = 0,
    val imageUrl: String = "",

    // Поле для фильтрации публичных викторин.
    // @PropertyName нужен, потому что "public" — ключевое слово Kotlin,
    // поэтому Kotlin-поле называется isPublic, а в Firestore хранится как "public"
    @get:PropertyName("public")
    @set:PropertyName("public")
    var isPublic: Boolean = true
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

