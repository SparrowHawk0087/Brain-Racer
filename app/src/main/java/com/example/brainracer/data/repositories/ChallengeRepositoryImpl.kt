package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.data.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChallengeRepositoryImpl : ChallengeRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val challengesCollection = firestore.collection("challenges")
    private val usersCollection = firestore.collection("users")

    private fun mapDocsToChallenges(docs: Iterable<com.google.firebase.firestore.DocumentSnapshot>): List<Challenge> =
        docs.mapNotNull { doc ->
            doc.toObject(Challenge::class.java)?.copy(id = doc.id)
        }

    // Чтение — только одно поле в запросе + фильтр на клиенте (не нужны составные индексы).

    override suspend fun getChallengesForUser(userId: String): Result<List<Challenge>> =
        getIncomingChallenges(userId)

    override suspend fun getIncomingChallenges(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("challengedUserId", userId)
            .limit(100)
            .get()
            .await()
        val challenges = mapDocsToChallenges(snapshot.documents)
            .filter { it.status == ChallengeStatus.PENDING }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getOutgoingChallenges(userId: String): Result<List<Challenge>> = try {
        val snapshot = challengesCollection
            .whereEqualTo("challengerUserId", userId)
            .limit(100)
            .get()
            .await()
        val allowed = setOf(
            ChallengeStatus.PENDING,
            ChallengeStatus.ACCEPTED,
            ChallengeStatus.COMPLETED
        )
        val challenges = mapDocsToChallenges(snapshot.documents)
            .filter { it.status in allowed }
            .sortedWith(
                compareByDescending<Challenge> { it.createdAt.seconds }
                    .thenByDescending { it.createdAt.nanoseconds }
            )
        Result.success(challenges)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getActiveChallenges(userId: String): Result<List<Challenge>> = try {
        val now = Timestamp.now()
        val asChallenged = challengesCollection
            .whereEqualTo("status", ChallengeStatus.ACCEPTED.name)
            .whereEqualTo("challengedUserId", userId)
            .get()
            .await()
        val asChallenger = challengesCollection
            .whereEqualTo("status", ChallengeStatus.ACCEPTED.name)
            .whereEqualTo("challengerUserId", userId)
            .get()
            .await()

        val merged = (asChallenged.documents + asChallenger.documents)
            .distinctBy { it.id }
            .mapNotNull { doc ->
                doc.toObject(Challenge::class.java)?.copy(id = doc.id)
            }
            .filter { it.expiresAt.toDate().after(now.toDate()) }
            .sortedWith(compareBy<Challenge> { it.expiresAt.seconds }.thenBy { it.expiresAt.nanoseconds })
        Result.success(merged)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getCompletedChallenges(userId: String, limit: Int): Result<List<Challenge>> = try {
        val lim = 80L
        val asChallenged = challengesCollection
            .whereEqualTo("challengedUserId", userId)
            .limit(lim)
            .get()
            .await()
        val asChallenger = challengesCollection
            .whereEqualTo("challengerUserId", userId)
            .limit(lim)
            .get()
            .await()

        val merged = (mapDocsToChallenges(asChallenged.documents) + mapDocsToChallenges(asChallenger.documents))
            .filter { it.status == ChallengeStatus.COMPLETED }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Challenge> { it.completedAt?.seconds ?: 0L }
                    .thenByDescending { it.completedAt?.nanoseconds ?: 0 }
            )
            .take(limit)
        Result.success(merged)
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

    override suspend fun fetchUserChallengeSides(userId: String): Result<UserChallengeSides> = try {
        val s1 = challengesCollection
            .whereEqualTo("challengedUserId", userId)
            .limit(100)
            .get()
            .await()
        val s2 = challengesCollection
            .whereEqualTo("challengerUserId", userId)
            .limit(100)
            .get()
            .await()
        Result.success(
            UserChallengeSides(
                asChallenged = mapDocsToChallenges(s1.documents),
                asChallenger = mapDocsToChallenges(s2.documents)
            )
        )
    } catch (e: Exception) {
        Result.error(e)
    }

    override fun observeUserChallengeSides(userId: String): Flow<UserChallengeSides> = callbackFlow {
        val lock = Any()
        var challenged: List<Challenge> = emptyList()
        var challenger: List<Challenge> = emptyList()

        fun emitSides() {
            val snap = synchronized(lock) {
                UserChallengeSides(
                    challenged.toList(),
                    challenger.toList()
                )
            }
            trySend(snap)
        }

        val regChallenged = challengesCollection
            .whereEqualTo("challengedUserId", userId)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    synchronized(lock) {
                        challenged = mapDocsToChallenges(snapshot.documents)
                    }
                    emitSides()
                }
            }

        val regChallenger = challengesCollection
            .whereEqualTo("challengerUserId", userId)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    synchronized(lock) {
                        challenger = mapDocsToChallenges(snapshot.documents)
                    }
                    emitSides()
                }
            }

        awaitClose {
            regChallenged.remove()
            regChallenger.remove()
        }
    }

    // Создание

    override suspend fun createChallenge(challenge: Challenge): Result<String> {
        return try {
            val existingSnapshot = challengesCollection
                .whereEqualTo("challengerUserId", challenge.challengerUserId)
                .limit(80)
                .get()
                .await()

            val hasConflict = existingSnapshot.documents.any { doc ->
                val c = doc.toObject(Challenge::class.java) ?: return@any false
                c.challengedUserId == challenge.challengedUserId &&
                    c.quizId == challenge.quizId &&
                    (c.status == ChallengeStatus.PENDING || c.status == ChallengeStatus.ACCEPTED)
            }
            if (hasConflict) {
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

            val parsed = challengeDoc.toObject(Challenge::class.java)
                ?: throw Exception("Не удалось прочитать вызов")

            if (parsed.status != ChallengeStatus.ACCEPTED) {
                throw Exception("Вызов не в статусе «принят»")
            }

            // Определяем поле для результата
            val resultField = if (userId == challengerId) "challengerResult" else "challengedResult"

            // Проверяем, не отправлен ли уже результат
            val alreadySent = if (userId == challengerId) parsed.challengerResult != null
            else parsed.challengedResult != null
            if (alreadySent) {
                return@runTransaction null
            }

            transaction.update(challengeRef, resultField, result)

            val newChallenger = if (userId == challengerId) result else parsed.challengerResult
            val newChallenged = if (userId == challengedId) result else parsed.challengedResult

            if (newChallenger != null && newChallenged != null) {
                val isDraw = newChallenger.score == newChallenged.score
                val winnerId: String? = when {
                    isDraw -> null
                    newChallenger.score > newChallenged.score -> challengerId
                    else -> challengedId
                }

                if (winnerId != null) {
                    transaction.update(
                        challengeRef,
                        "status", ChallengeStatus.COMPLETED.name,
                        "completedAt", Timestamp.now(),
                        "winnerId", winnerId,
                        "isDraw", isDraw
                    )
                } else {
                    transaction.update(
                        challengeRef,
                        "status", ChallengeStatus.COMPLETED.name,
                        "completedAt", Timestamp.now(),
                        "winnerId", FieldValue.delete(),
                        "isDraw", isDraw
                    )
                }
            }

            null
        }.await()

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