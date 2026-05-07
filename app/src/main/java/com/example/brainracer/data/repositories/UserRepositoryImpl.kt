package com.example.brainracer.data.repositories

import android.util.Log
import com.example.brainracer.data.storage.EvolutionStorageRepositoryImpl
import com.example.brainracer.data.storage.QuizDraftRepositoryImpl
import com.example.brainracer.data.storage.StorageConfig
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.ChallengeWinnerXpOutcome
import com.example.brainracer.domain.entities.ChallengeXpPolicy
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.FriendRequest
import com.example.brainracer.domain.entities.FriendshipStatus
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.UserRank
import com.example.brainracer.domain.entities.normalizeNicknameForStorage
import com.example.brainracer.data.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await


class UserRepositoryImpl : UserRepository {
    companion object {
        const val NICKNAME_TAKEN_ERROR_CODE = "NICKNAME_TAKEN"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val nicknameIndexCollection = firestore.collection("nickname_index")
    private val challengesCollection = firestore.collection("challenges")
    private val quizzesCollection = firestore.collection("quizzes")
    private val friendRequestsCollection = firestore.collection("friend_requests")

    private fun intFromStatsMap(m: Map<String, Any?>?, key: String): Int =
        (m?.get(key) as? Number)?.toInt() ?: 0

    private fun longFromStatsMap(m: Map<String, Any?>?, key: String): Long =
        (m?.get(key) as? Number)?.toLong() ?: 0L

    private fun isPermissionDenied(e: Throwable): Boolean {
        val fs = e as? FirebaseFirestoreException
        return fs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
    }

    @Suppress("UNCHECKED_CAST")
    private fun paidPairMapFromStats(raw: Any?): Map<String, Int> {
        val m = raw as? Map<*, *> ?: return emptyMap()
        return m.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val intVal = when (v) {
                is Int -> v
                is Long -> v.toInt()
                else -> null
            } ?: return@mapNotNull null
            key to intVal
        }.toMap()
    }

    // ── Получить пользователя ─────────────────────────────────────────────
    override suspend fun getUser(userId: String): Result<User> = try {
        val document = usersCollection.document(userId).get().await()
        if (document.exists()) {
            val raw = document.toObject(User::class.java)
            val user = raw?.copy(id = userId)
            if (user != null) Result.success(user)
            else Result.error(Exception("User data is null"))
        } else {
            Result.error(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun countUsersWithNicknameNormalized(
        normalized: String,
        excludeUserId: String?
    ): Result<Int> = try {
        if (normalized.isBlank()) {
            Result.success(0)
        } else {
            val snap = usersCollection
                .whereEqualTo("nickname_normalized", normalized)
                .get()
                .await()
            val n = if (excludeUserId != null) {
                snap.documents.count { it.id != excludeUserId }
            } else {
                snap.documents.size
            }
            Result.success(n)
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun mergeNicknameNormalized(userId: String, normalized: String): Result<Unit> = try {
        usersCollection.document(userId)
            .set(mapOf("nickname_normalized" to normalized), SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun backfillNicknameNormalizedForNickname(
        rawNickname: String,
        normalized: String
    ): Result<Int> = try {
        if (rawNickname.isBlank() || normalized.isBlank()) {
            Result.success(0)
        } else {
            val snapshot = usersCollection
                .whereEqualTo("nickname", rawNickname)
                .limit(100)
                .get()
                .await()

            val docsToUpdate = snapshot.documents.filter { doc ->
                val current = doc.getString("nickname_normalized").orEmpty().trim()
                current.isBlank() || current != normalized
            }
            if (docsToUpdate.isEmpty()) {
                Result.success(0)
            } else {
                val batch = firestore.batch()
                docsToUpdate.forEach { doc ->
                    batch.set(
                        doc.reference,
                        mapOf("nickname_normalized" to normalized),
                        SetOptions.merge()
                    )
                }
                batch.commit().await()
                Result.success(docsToUpdate.size)
            }
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Создать пользователя ──────────────────────────────────────────────
    override suspend fun createUser(user: User): Result<Unit> = try {
        val normalized = user.nicknameNormalized.ifBlank {
            normalizeNicknameForStorage(user.nickname)
        }
        val userToWrite = if (user.nicknameNormalized == normalized) user else {
            user.copy(nicknameNormalized = normalized)
        }
        val userRef = usersCollection.document(user.id)
        val nickRef = normalized.takeIf { it.isNotBlank() }?.let { nicknameIndexCollection.document(it) }

        try {
            firestore.runTransaction { tx ->
                if (nickRef != null) {
                    val nickDoc = tx.get(nickRef)
                    val ownerId = nickDoc.getString("userId")
                    if (!ownerId.isNullOrBlank() && ownerId != user.id) {
                        throw IllegalStateException(NICKNAME_TAKEN_ERROR_CODE)
                    }
                }

                tx.set(userRef, userToWrite)
                if (nickRef != null) {
                    tx.set(
                        nickRef,
                        mapOf(
                            "userId" to user.id,
                            "nickname" to user.nickname,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }
                null
            }.await()
        } catch (e: Exception) {
            // Если rules ещё не открыли коллекцию nickname_index, регистрация не блокируется
            if (!isPermissionDenied(e)) throw e
            userRef.set(userToWrite).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Обновить пользователя ─────────────────────────────────────────────
    override suspend fun updateUser(user: User): Result<Unit> = try {
        val userRef = usersCollection.document(user.id)
        val normalizedNew = user.nicknameNormalized.ifBlank {
            normalizeNicknameForStorage(user.nickname)
        }
        val userToWrite = if (user.nicknameNormalized == normalizedNew) user else {
            user.copy(nicknameNormalized = normalizedNew)
        }
        val newNickRef = normalizedNew.takeIf { it.isNotBlank() }?.let { nicknameIndexCollection.document(it) }

        try {
            firestore.runTransaction { tx ->
                val oldUserDoc = tx.get(userRef)
                val oldNormalized = oldUserDoc.getString("nickname_normalized")
                    ?.trim()
                    .orEmpty()
                    .ifBlank {
                        normalizeNicknameForStorage(oldUserDoc.getString("nickname").orEmpty())
                    }

                if (newNickRef != null) {
                    val newNickDoc = tx.get(newNickRef)
                    val ownerId = newNickDoc.getString("userId")
                    if (!ownerId.isNullOrBlank() && ownerId != user.id) {
                        throw IllegalStateException(NICKNAME_TAKEN_ERROR_CODE)
                    }
                }

                tx.set(userRef, userToWrite, SetOptions.merge())
                if (newNickRef != null) {
                    tx.set(
                        newNickRef,
                        mapOf(
                            "userId" to user.id,
                            "nickname" to user.nickname,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }

                if (oldNormalized.isNotBlank() && oldNormalized != normalizedNew) {
                    val oldNickRef = nicknameIndexCollection.document(oldNormalized)
                    val oldNickDoc = tx.get(oldNickRef)
                    val oldOwnerId = oldNickDoc.getString("userId")
                    if (oldOwnerId == user.id) {
                        tx.delete(oldNickRef)
                    }
                }
                null
            }.await()
        } catch (e: Exception) {
            // Fallback для сред с закрытыми rules на nickname_index
            if (!isPermissionDenied(e)) throw e
            userRef.set(userToWrite, SetOptions.merge()).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Соло: один зачёт XP на quizId ─────────────────────────────────────
    override suspend fun applySoloQuizCompletion(
        userId: String,
        quizResult: ChallengeResult,
        profileSessionXp: Int
    ): Result<Int> = try {
        val userRef = usersCollection.document(userId)
        val awardedHolder = intArrayOf(0)
        firestore.runTransaction { transaction ->
            awardedHolder[0] = 0
            val userDoc = transaction.get(userRef)
            val currentStats = userDoc.get("stats") as? Map<String, Any?> ?: mapOf()

            val soloIds = (currentStats["solo_completed_quiz_ids"] as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList()
            val alreadyDone = quizResult.quizId in soloIds
            val awarded = if (alreadyDone) 0 else profileSessionXp.coerceAtLeast(0)
            awardedHolder[0] = awarded

            val newTotalQuizzes = longFromStatsMap(currentStats, "total_quizzes_taken") + 1
            val newTotalQuestions = longFromStatsMap(currentStats, "total_questions_answered") + quizResult.totalQuestions
            val newCorrect = longFromStatsMap(currentStats, "correct_answers") + quizResult.correctAnswers
            val newIncorrect = longFromStatsMap(currentStats, "incorrect_answers") + quizResult.incorrectAnswers
            val prevPoints = longFromStatsMap(currentStats, "total_points")
            val newTotalPoints = prevPoints + awarded

            val currentAverage = currentStats["average_score"] as? Double ?: 0.0
            val nq = newTotalQuizzes.toInt()
            val newAverageScore = if (nq > 0)
                (currentAverage * (nq - 1) + quizResult.accuracy) / nq
            else quizResult.accuracy

            val newSoloIds = if (!alreadyDone) soloIds + quizResult.quizId else soloIds

            val updates = mutableMapOf<String, Any>(
                "stats.total_quizzes_taken" to newTotalQuizzes,
                "stats.total_questions_answered" to newTotalQuestions,
                "stats.correct_answers" to newCorrect,
                "stats.incorrect_answers" to newIncorrect,
                "stats.total_points" to newTotalPoints,
                "stats.average_score" to newAverageScore,
                "stats.solo_completed_quiz_ids" to newSoloIds,
                "rank" to calculateRank(newTotalPoints.toInt()).name
            )
            transaction.update(userRef, updates)
            null
        }.await()
        Result.success(awardedHolder[0])
    } catch (e: Exception) {
        Result.error(e)
    }

    // ── Дуэль: только попытка в статистике ────────────────────────────────
    override suspend fun applyChallengeQuizParticipation(
        userId: String,
        quizResult: ChallengeResult
    ): Result<Unit> = try {
        val userRef = usersCollection.document(userId)
        firestore.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            val currentStats = userDoc.get("stats") as? Map<String, Any?> ?: mapOf()

            val newTotalQuizzes = longFromStatsMap(currentStats, "total_quizzes_taken") + 1
            val newTotalQuestions = longFromStatsMap(currentStats, "total_questions_answered") + quizResult.totalQuestions
            val newCorrect = longFromStatsMap(currentStats, "correct_answers") + quizResult.correctAnswers
            val newIncorrect = longFromStatsMap(currentStats, "incorrect_answers") + quizResult.incorrectAnswers

            val currentAverage = currentStats["average_score"] as? Double ?: 0.0
            val nq = newTotalQuizzes.toInt()
            val newAverageScore = if (nq > 0)
                (currentAverage * (nq - 1) + quizResult.accuracy) / nq
            else quizResult.accuracy

            val updates = mapOf(
                "stats.total_quizzes_taken" to newTotalQuizzes,
                "stats.total_questions_answered" to newTotalQuestions,
                "stats.correct_answers" to newCorrect,
                "stats.incorrect_answers" to newIncorrect,
                "stats.average_score" to newAverageScore
            )
            transaction.update(userRef, updates)
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun tryGrantChallengeWinnerXp(challengeId: String): Result<ChallengeWinnerXpOutcome?> = try {
        val outcomeHolder = arrayOfNulls<ChallengeWinnerXpOutcome>(1)
        firestore.runTransaction { transaction ->
            outcomeHolder[0] = null
            val chRef = challengesCollection.document(challengeId)
            val chDoc = transaction.get(chRef)
            if (!chDoc.exists()) return@runTransaction null

            val challenge = chDoc.toObject(Challenge::class.java) ?: return@runTransaction null
            if (challenge.status != ChallengeStatus.COMPLETED) return@runTransaction null
            if (challenge.isDraw) return@runTransaction null
            val winnerId = challenge.winnerId ?: return@runTransaction null
            if (challenge.winnerXpGranted) return@runTransaction null

            val winnerResult = if (winnerId == challenge.challengerUserId) {
                challenge.challengerResult
            } else {
                challenge.challengedResult
            } ?: return@runTransaction null

            val opponentId = if (winnerId == challenge.challengerUserId) {
                challenge.challengedUserId
            } else {
                challenge.challengerUserId
            }

            val qRef = quizzesCollection.document(challenge.quizId)
            val qDoc = transaction.get(qRef)
            val quiz = qDoc.toObject(Quiz::class.java) ?: throw Exception("quiz missing for challenge XP")

            val userRef = usersCollection.document(winnerId)
            val uDoc = transaction.get(userRef)
            val currentStats = uDoc.get("stats") as? Map<String, Any?> ?: mapOf()

            val todayStart = ChallengeXpPolicy.utcDayStartMillis()
            var dayMillis = longFromStatsMap(currentStats, "challenge_xp_day_utc_millis")
            var earnedToday = intFromStatsMap(currentStats, "challenge_xp_earned_today")
            var paidMap = paidPairMapFromStats(currentStats["challenge_pair_paid_today"])

            if (dayMillis != todayStart) {
                dayMillis = todayStart
                earnedToday = 0
                paidMap = emptyMap()
            }

            val pairKey = ChallengeXpPolicy.pairKeyQuiz(winnerId, opponentId, challenge.quizId)
            val paidCount = paidMap[pairKey] ?: 0
            val mult = ChallengeXpPolicy.multiplierForPaidAttemptIndex(paidCount)

            val totalPoints = intFromStatsMap(currentStats, "total_points")
            val sessionProfileXp = LevelSystem.calculateQuizXp(
                winnerResult.answers,
                quiz.questions,
                quiz.difficulty,
                totalPoints
            ).profileTotalXp

            val raw = (sessionProfileXp * mult).toInt()
            val capRem = (ChallengeXpPolicy.DAILY_CHALLENGE_XP_CAP - earnedToday).coerceAtLeast(0)
            val awarded = minOf(capRem, raw).coerceAtLeast(0)

            val newTotalPoints = totalPoints + awarded
            val newEarnedToday = earnedToday + awarded
            val newPaidMap: Map<String, Int> = if (awarded > 0) {
                paidMap.toMutableMap().apply { put(pairKey, paidCount + 1) }
            } else {
                paidMap
            }

            val userUpdates = mutableMapOf<String, Any>(
                "stats.total_points" to newTotalPoints,
                "stats.challenge_xp_day_utc_millis" to dayMillis,
                "stats.challenge_xp_earned_today" to newEarnedToday,
                "stats.challenge_pair_paid_today" to newPaidMap,
                "rank" to calculateRank(newTotalPoints).name
            )
            transaction.update(userRef, userUpdates)
            transaction.update(chRef, "winnerXpGranted", true)

            outcomeHolder[0] = ChallengeWinnerXpOutcome(winnerId = winnerId, xpAdded = awarded)
            null
        }.await()
        Result.success(outcomeHolder[0])
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

    override suspend fun updateUserBio(userId: String, bio: String): Result<Unit> = try {
        usersCollection.document(userId).update("bio", bio).await()
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

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = try {
        usersCollection.document(userId).update("fcmToken", token).await()
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
    // Транзакция убирает userId из массива friends у friendId и наоборот
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

    override suspend fun deleteUserAccountData(userId: String): Result<Unit> = try {
        if (userId.isBlank()) {
            Result.error(IllegalArgumentException("userId is blank"))
        } else {
            val userDoc = usersCollection.document(userId).get().await()
            val normalized = userDoc.getString("nickname_normalized")
                ?.trim()
                .orEmpty()
                .ifBlank { normalizeNicknameForStorage(userDoc.getString("nickname").orEmpty()) }
            val friends = (userDoc.get("friends") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val avatarUrl = userDoc.getString("avatarUrl").orEmpty()
            val createdQuizIds = (userDoc.get("createdQuizzes") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()

            // Удаляем профиль, индекс ника, ссылки у друзей
            val profileBatch = firestore.batch()
            profileBatch.delete(usersCollection.document(userId))
            if (normalized.isNotBlank()) {
                profileBatch.delete(nicknameIndexCollection.document(normalized))
            }
            friends.forEach { friendId ->
                profileBatch.update(usersCollection.document(friendId), "friends", FieldValue.arrayRemove(userId))
            }
            profileBatch.commit().await()

            // Удаляем все friend requests, где пользователь sender/receiver
            suspend fun deleteRequestsByField(field: String) {
                while (true) {
                    val snapshot = friendRequestsCollection
                        .whereEqualTo(field, userId)
                        .limit(200)
                        .get()
                        .await()
                    if (snapshot.isEmpty) break
                    val batch = firestore.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                    if (snapshot.size() < 200) break
                }
            }
            deleteRequestsByField("senderId")
            deleteRequestsByField("receiverId")

            when (val draftCleanup = QuizDraftRepositoryImpl().deleteAllDrafts(userId)) {
                is Result.Error ->
                    Log.w("UserRepository", "Evolution drafts cleanup failed: ${draftCleanup.exception.message}")
                is Result.Success -> Unit
            }

            // Удаляем созданные пользователем викторины
            if (createdQuizIds.isNotEmpty()) {
                val quizRepo = QuizRepositoryImpl()
                createdQuizIds.forEach { quizId ->
                    when (val r = quizRepo.deleteQuiz(quizId)) {
                        is Result.Error ->
                            Log.w("UserRepository", "Quiz $quizId cleanup failed: ${r.exception.message}")
                        is Result.Success -> Unit
                    }
                }
            }

            // Удаляем аватарку из bucket'а пользователя
            // Делается после удаления Firestore-данных, чтобы клиенты больше не ссылались на него
            val storage = EvolutionStorageRepositoryImpl()
            val avatarKey = StorageConfig.extractObjectKeyPublic(avatarUrl)
                ?: avatarUrl.takeIf { it.startsWith("avatars/") }
            if (!avatarKey.isNullOrBlank()) {
                when (val res = storage.delete(StorageConfig.BUCKET_AVATARS, avatarKey)) {
                    is Result.Error ->
                        Log.w("UserRepository", "Avatar cleanup failed: ${res.exception.message}")
                    is Result.Success ->
                        Log.d("UserRepository", "Avatar deleted: ${StorageConfig.BUCKET_AVATARS}/$avatarKey")
                }
            } else {
                // Если в профиле не было avatarUrl, то подчищаем по конвенции ключа avatars/{userId}.{ext}
                listOf("jpeg", "jpg", "png", "gif", "webp").forEach { ext ->
                    storage.delete(StorageConfig.BUCKET_AVATARS, "avatars/$userId.$ext")
                }
            }

            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    // Рассчет ранга
    private fun calculateRank(points: Int): UserRank {
        return UserRank.entries
            .sortedByDescending { it.minPoints }
            .first { points >= it.minPoints }
    }
}