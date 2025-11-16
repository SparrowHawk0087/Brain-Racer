package com.example.brainracer.domain.entities

import java.time.LocalDateTime

data class ChallengeResult(
    val userId: String,
    val challengeId: String,
    val score: Int,
    val correctAnswers: Int,
    val timeSpent: Long,
    val completedAt: LocalDateTime,
    val userAnswers: List<UserAnswer>
) {
    fun calculateAccuracy(): Double =
        if (userAnswers.isEmpty()) 0.0
        else userAnswers.count { it.isCorrect }.toDouble() / userAnswers.size
}