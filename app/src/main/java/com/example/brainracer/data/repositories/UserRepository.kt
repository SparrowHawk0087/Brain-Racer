package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.FriendRequest
import com.example.brainracer.domain.entities.User
import com.example.brainracer.data.utils.Result

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    suspend fun createUser(user: User): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateUserStats(userId: String, quizResult: ChallengeResult): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateUserInterests(userId: String, interests: List<String>): Result<Unit>
    suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit>
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<Unit>
    suspend fun getFriendRequests(userId: String): Result<List<FriendRequest>>
    suspend fun acceptFriendRequest(requestId: String, userId: String, friendId: String): Result<Unit>
    suspend fun declineFriendRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit>
}