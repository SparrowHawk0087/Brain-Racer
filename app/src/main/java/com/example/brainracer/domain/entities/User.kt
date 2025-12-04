package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",
    val email: String = "",
    val nickname: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val stats: UserStats = UserStats(),
    val interests: List<String> = emptyList(),
    val rank: UserRank = UserRank.BEGINNER,
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val createdQuizzes: List<String> = emptyList(),
    val friends: List<String> = emptyList()
) {
    @get:Exclude
    val accuracy: Double
        get() = if (stats.totalQuestionsAnswered > 0) {
            stats.correctAnswers.toDouble() / stats.totalQuestionsAnswered * 100
        } else 0.0
}

enum class UserRank(val displayName: String, val minPoints: Int) {
    BEGINNER("Новичок", 0),
    EXPLORER("Исследователь", 100),
    SCHOLAR("Ученый", 500),
    MASTER("Мастер", 1500),
    GRANDMASTER("Великий мастер", 3000)
}
