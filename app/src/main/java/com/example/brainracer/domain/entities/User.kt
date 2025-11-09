package com.example.brainracer.domain.entities

import java.time.LocalDateTime

// Сущность пользователя
data class User(
    val id: String,
    val email: String,
    val username: String,
    val password: String,
    val stats: UserStats,
    val createdAt: LocalDateTime,
    val lastActiveAt: LocalDateTime
) {
    fun getTotalStats(): Int = stats.totalScore
}

