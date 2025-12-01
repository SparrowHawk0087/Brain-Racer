package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp

data class ChallengeResult(
    val id: String = "",
    val quizId: String = "",
    val userId: String = "",
    val userNickname: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val timeSpent: Int = 0,
    val averageTimePerQuestion: Double = 0.0,
    val answers: List<UserAnswer> = emptyList(),
    val pointsEarned: Int = 0,
    val completedAt: Timestamp = Timestamp.now(),
    val challengeId: String? = null
) {
    val accuracy: Double
        get() = if (totalQuestions > 0) {
            correctAnswers.toDouble() / totalQuestions * 100
        } else 0.0
}

