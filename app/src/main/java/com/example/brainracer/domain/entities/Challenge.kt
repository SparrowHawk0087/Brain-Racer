package com.example.brainracer.domain.entities
import java.time.LocalDateTime

enum class ChallengeStatus {
    PENDING, ACCEPTED, COMPLETED, EXPIRED, DECLINED
}

// Сущность для хранения результатов соревнования
data class Challenge(
    val id: String,
    val quizId: String,
    val challengerUserId: String,
    val challengedUserId: String,
    val status: ChallengeStatus,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val results: Map<String, ChallengeResult> // userId -> result
)
