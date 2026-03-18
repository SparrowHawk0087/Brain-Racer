package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.data.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChallengeRepositoryImpl : ChallengeRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val challengesCollection = firestore.collection("challenges")
    private val usersCollection = firestore.collection("users")

    // Чтение
    override suspend fun getChallengesForUser(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("status", ChallengeStatus.PENDING.name)
            .whereIn("challengedUserId", listOf(userId))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val challenges = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getIncomingChallenges(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("challengedUserId", userId)
            .whereEqualTo("status", ChallengeStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        val challenges = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getOutgoingChallenges(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("challengerUserId", userId)
            .whereIn("status", listOf(
                ChallengeStatus.PENDING.name,
                ChallengeStatus.ACCEPTED.name,
                ChallengeStatus.COMPLETED.name
            ))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        val challenges = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getActiveChallenges(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("status", ChallengeStatus.ACCEPTED.name)
            .whereIn("challengedUserId", listOf(userId))
            .whereGreaterThan("expiresAt", Timestamp.now())
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .get()
            .await()

        val challenges = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getCompletedChallenges(userId: String, limit: Int): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("status", ChallengeStatus.COMPLETED.name)
            .whereIn("challengedUserId", listOf(userId))
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val challenges = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getChallenge(challengeId: String): Result<Challenge> = try {
        val doc = challengesCollection.document(challengeId).get().await()

        if (doc.exists()) {
            val challenge = doc.toObject(Challenge::class.java)
            if (challenge != null) {
                Result.success(challenge.copy(id = doc.id))
            } else {
                Result.error(Exception("Challenge data is null"))
            }
        } else {
            Result.error(Exception("Challenge not found"))
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    // Создание

    override suspend fun createChallenge(challenge: Challenge): Result<String> {
        return try {
            // Проверка: не существует ли уже активный вызов между этими пользователями на эту викторину
            val existingSnapshot = challengesCollection
                .whereEqualTo("challengerUserId", challenge.challengerUserId)
                .whereEqualTo("challengedUserId", challenge.challengedUserId)
                .whereEqualTo("quizId", challenge.quizId)
                .whereIn("status", listOf(
                    ChallengeStatus.PENDING.name,
                    ChallengeStatus.ACCEPTED.name
                ))
                .get()
                .await()

            if (!existingSnapshot.isEmpty) {
                return Result.error(Exception("Уже есть активный вызов на эту викторину"))
            }

            // Получаем никнеймы для денормализации
            val challengerDoc = usersCollection.document(challenge.challengerUserId).get().await()
            val challengedDoc = usersCollection.document(challenge.challengedUserId).get().await()

            val challengeWithDetails = challenge.copy(
                challengerNickname = challengerDoc.getString("nickname") ?: "Игрок",
                challengedNickname = challengedDoc.getString("nickname") ?: "Игрок"
            )

            val docRef = challengesCollection.document()
            val challengeWithId = challengeWithDetails.copy(id = docRef.id)

            docRef.set(challengeWithId).await()

            // TODO: Отправить FCM уведомление challenged пользователю
            // sendChallengeNotification(challengeWithId)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // Обновление статуса

    override suspend fun acceptChallenge(challengeId: String): Result<Unit> = try {
        challengesCollection.document(challengeId)
            .update("status", ChallengeStatus.ACCEPTED.name)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun declineChallenge(challengeId: String): Result<Unit> = try {
        challengesCollection.document(challengeId)
            .update("status", ChallengeStatus.DECLINED.name)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun cancelChallenge(challengeId: String): Result<Unit> = try {
        challengesCollection.document(challengeId)
            .update("status", ChallengeStatus.CANCELLED.name)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // Результаты

    override suspend fun submitChallengeResult(
        challengeId: String,
        userId: String,
        result: ChallengeResult
    ): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val challengeRef = challengesCollection.document(challengeId)
            val challengeDoc = transaction.get(challengeRef)

            // Проверка: существует ли вызов
            if (!challengeDoc.exists()) {
                throw Exception("Вызов не найден")
            }

            // Проверка: может ли пользователь отправить результат
            val challengerId = challengeDoc.getString("challengerUserId")
            val challengedId = challengeDoc.getString("challengedUserId")

            if (userId != challengerId && userId != challengedId) {
                throw Exception("Пользователь не участвует в этом вызове")
            }

            // Определяем поле для результата
            val resultField = if (userId == challengerId) "challengerResult" else "challengedResult"

            // Проверяем, не отправлен ли уже результат
            val existingResult = challengeDoc.get(resultField)
            if (existingResult != null) {
                throw Exception("Результат уже отправлен")
            }

            // Обновляем результат
            transaction.update(challengeRef, resultField, result)

            // Проверяем, есть ли оба результата
            val challengerResult = challengeDoc.get("challengerResult") as? ChallengeResult
            val challengedResult = challengeDoc.get("challengedResult") as? ChallengeResult

            // Если оба результата есть — определяем победителя
            if (challengerResult != null && challengedResult != null) {
                val winnerId = when {
                    challengerResult.score > challengedResult.score -> challengerId
                    challengedResult.score > challengerResult.score -> challengedId
                    else -> "draw"
                }

                val updates = mapOf(
                    "status" to ChallengeStatus.COMPLETED.name,
                    "completedAt" to Timestamp.now(),
                    "winnerId" to winnerId,
                    "isDraw" to (winnerId == "draw")
                )

                transaction.update(challengeRef, updates)
            }

            null // Transaction не возвращает значение
        }.await()

        // Обновляем статистику пользователя
        val userRepository = UserRepositoryImpl()
        userRepository.updateUserStats(userId, result)

        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // Утилиты

    override suspend fun checkExpiredChallenges(): Result<Unit> = try {
        val expiredSnapshot = challengesCollection
            .whereEqualTo("status", ChallengeStatus.PENDING.name)
            .whereLessThan("expiresAt", Timestamp.now())
            .get()
            .await()

        val batch = firestore.batch()
        expiredSnapshot.documents.forEach { doc ->
            batch.update(doc.reference, "status", ChallengeStatus.EXPIRED.name)
        }
        batch.commit().await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // FCM уведомления  (TODO) ====================

    private suspend fun sendChallengeNotification(challenge: Challenge) {
        // Для реализации потребуется Firebase Cloud Functions
        // Или Firebase Admin SDK на бэкенде
        // Примерная логика:
        /*
        val userDoc = usersCollection.document(challenge.challengedUserId).get().await()
        val fcmToken = userDoc.getString("fcmToken")

        if (fcmToken != null) {
            // Отправка через Firebase Cloud Messaging
            val message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("🎯 Новый вызов!")
                    .setBody("${challenge.challengerNickname} бросил вам вызов в ${challenge.quizTitle}")
                    .build())
                .putData("challengeId", challenge.id)
                .putData("type", "challenge")
                .build()

            FirebaseMessaging.getInstance().send(message)
        }
        */
    }
}