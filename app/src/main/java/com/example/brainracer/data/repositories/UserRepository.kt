package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.User

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    suspend fun createUser(user: User): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateUserStats(userId: String, quizResult: ChallengeResult): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateUserInterests(userId: String, interests: List<String>): Result<Unit>
    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit>
}