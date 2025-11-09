package com.example.brainracer.domain.entities
import java.time.LocalDateTime
enum class FriendshipStatus {
    PENDING, ACCEPTED, BLOCKED
}

// Сущность для системы дружбы между пользователями
data class Friendship(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val status: FriendshipStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
