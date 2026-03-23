package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.FriendRequest        // ← добавлен импорт
import com.example.brainracer.domain.entities.FriendshipStatus     // ← добавлен импорт (вместо несуществующего FriendRequestStatus)
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.UserRank
import com.example.brainracer.data.utils.Result
import com.google.firebase.Timestamp                               // ← добавлен импорт
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


class UserRepositoryImpl : UserRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")
    private val friendRequestsCollection = firestore.collection("friend_requests")

    // ── Получить пользователя ─────────────────────────────────────────────
    override suspend fun getUser(userId: String): Result<User> = try {
        val document = usersCollection.document(userId).get().await()
        if (document.exists()) {
            val user = document.toObject(User::class.java)
            if (user != null) Result.success(user)
            else Result.error(Exception("User data is null"))
        } else {
            Result.error(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Создать пользователя ──────────────────────────────────────────────
    override suspend fun createUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Обновить пользователя ─────────────────────────────────────────────
    override suspend fun updateUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Обновить статистику ───────────────────────────────────────────────
    override suspend fun updateUserStats(
        userId: String,
        quizResult: ChallengeResult
    ): Result<Unit> = try {
        val userRef = usersCollection.document(userId)
        firestore.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            val currentStats = userDoc.get("stats") as? Map<String, Any?> ?: mapOf()

            val newTotalQuizzes    = (currentStats["total_quizzes_taken"]      as? Long ?: 0) + 1
            val newTotalQuestions  = (currentStats["total_questions_answered"]  as? Long ?: 0) + quizResult.totalQuestions
            val newCorrectAnswers  = (currentStats["correct_answers"]           as? Long ?: 0) + quizResult.correctAnswers
            val newIncorrectAnswers = (currentStats["incorrect_answers"]        as? Long ?: 0) + quizResult.incorrectAnswers
            val newTotalPoints     = (currentStats["total_points"]              as? Long ?: 0) + quizResult.pointsEarned

            val currentAverage = currentStats["average_score"] as? Double ?: 0.0
            val newAverageScore = if (newTotalQuizzes > 0)
                (currentAverage * (newTotalQuizzes - 1) + quizResult.accuracy) / newTotalQuizzes
            else quizResult.accuracy

            val updates = mapOf(
                "stats.total_quizzes_taken"      to newTotalQuizzes,
                "stats.total_questions_answered" to newTotalQuestions,
                "stats.correct_answers"          to newCorrectAnswers,
                "stats.incorrect_answers"        to newIncorrectAnswers,
                "stats.total_points"             to newTotalPoints,
                "stats.average_score"            to newAverageScore,
                "rank"                           to calculateRank(newTotalPoints.toInt()).name
            )
            transaction.update(userRef, updates)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Поиск пользователей ───────────────────────────────────────────────
    override suspend fun searchUsers(query: String): Result<List<User>> = try {
        val result = usersCollection
            .whereGreaterThanOrEqualTo("nickname", query)
            .whereLessThanOrEqualTo("nickname", query + "\uf8ff")
            .limit(20)
            .get()
            .await()
        val users = result.documents.mapNotNull { it.toObject(User::class.java) }
        Result.success(users)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Обновить интересы ─────────────────────────────────────────────────
    override suspend fun updateUserInterests(userId: String, interests: List<String>): Result<Unit> = try {
        usersCollection.document(userId).update("interests", interests).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Обновить аватар ───────────────────────────────────────────────────
    override suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit> = try {
        usersCollection.document(userId).update("avatarUrl", avatarUrl).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Отправить запрос в друзья ─────────────────────────────────────────
    override suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<Unit> = try {
        val requestRef = friendRequestsCollection.document()
        val request = FriendRequest(
            id = requestRef.id,
            senderId = senderId,
            receiverId = receiverId,
            // ✅ Исправлено: FriendshipStatus вместо несуществующего FriendRequestStatus
            status = FriendshipStatus.PENDING,
            createdAt = Timestamp.now(),   // ✅ Timestamp теперь импортирован
            updatedAt = Timestamp.now()
        )
        requestRef.set(request).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Получить список запросов в друзья для пользователя ────────────────
    // Метод возвращает все входящие PENDING-запросы, адресованные userId.
    override suspend fun getFriendRequests(userId: String): Result<List<FriendRequest>> = try {
        val snapshot = friendRequestsCollection
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", FriendshipStatus.PENDING.name)
            .get()
            .await()
        val requests = snapshot.documents.mapNotNull { it.toObject(FriendRequest::class.java) }
        Result.success(requests)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Принять запрос в друзья ───────────────────────────────────────────
    // Выполняется в одной транзакции: статус запроса → ACCEPTED,
    // userId и friendId добавляются в списки друзей друг друга.
    override suspend fun acceptFriendRequest(
        requestId: String,
        userId: String,
        friendId: String
    ): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val requestRef  = friendRequestsCollection.document(requestId)
            val userRef     = usersCollection.document(userId)
            val friendRef   = usersCollection.document(friendId)

            transaction.update(requestRef, mapOf(
                "status"    to FriendshipStatus.ACCEPTED.name,
                "updatedAt" to Timestamp.now()
            ))
            // FieldValue.arrayUnion безопасно добавляет элемент, даже если массива ещё нет
            transaction.update(userRef,   "friends", FieldValue.arrayUnion(friendId))
            transaction.update(friendRef, "friends", FieldValue.arrayUnion(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Отклонить запрос в друзья ─────────────────────────────────────────
    // Меняем статус на BLOCKED — документ сохраняется в истории.
    override suspend fun declineFriendRequest(requestId: String): Result<Unit> = try {
        friendRequestsCollection.document(requestId)
            .update(mapOf(
                "status"    to FriendshipStatus.BLOCKED.name,
                "updatedAt" to Timestamp.now()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Удалить из друзей ─────────────────────────────────────────────────
    // Транзакция убирает userId из массива friends у friendId и наоборот.
    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val userRef   = usersCollection.document(userId)
            val friendRef = usersCollection.document(friendId)

            transaction.update(userRef,   "friends", FieldValue.arrayRemove(friendId))
            transaction.update(friendRef, "friends", FieldValue.arrayRemove(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Вспомогательный метод расчёта ранга ──────────────────────────────
    private fun calculateRank(points: Int): UserRank {
        return UserRank.entries
            .sortedByDescending { it.minPoints }
            .first { points >= it.minPoints }
    }
}