package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeWinnerXpOutcome
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.getOrNull
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await

class QuizRepositoryImpl: QuizRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    // Firestore коллекции
    private val quizzesCollection = firestore.collection("quizzes")
    private val usersCollection = firestore.collection("users")
    private val quizResultsCollection = firestore.collection("quiz_results")
    private val quizStatsCollection = firestore.collection("quiz_stats")

    private suspend fun mergeQuizPlayStats(quiz: Quiz): Quiz {
        return try {
            val snap = quizStatsCollection.document(quiz.id).get().await()
            if (!snap.exists()) return quiz
            val tt = (snap.get("times_taken") as? Number)?.toInt() ?: return quiz
            val avg = (snap.get("average_score") as? Number)?.toDouble() ?: quiz.stats.averageScore
            quiz.copy(stats = quiz.stats.copy(timesTaken = tt, averageScore = avg))
        } catch (_: Exception) {
            quiz
        }
    }

    //Получение квиза по id
    override suspend fun getQuiz(quizId: String): Result<Quiz> = try {
        val document = quizzesCollection.document(quizId).get().await()
        if (document.exists()) {
            val parsed = document.toObject(Quiz::class.java)
                ?: throw Exception("Quiz data is null")
            val quiz = if (parsed.id.isBlank()) parsed.copy(id = document.id) else parsed
            Result.success(mergeQuizPlayStats(quiz))
        } else {
            Result.error(Exception("Quiz not found"))
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    /** Firestore: не более 30 значений в одном `in` по полю. */
    private companion object {
        const val WHERE_IN_CHUNK = 30
        /** Сколько публичных викторин прочитать для клиентского поиска по названию. */
        const val SEARCH_SCAN_LIMIT = 500
        const val SEARCH_MAX_RESULTS = 50
    }

    override suspend fun getQuizzesByIds(
        quizIds: List<String>,
        maxConcurrentChunkQueries: Int
    ): Result<Map<String, Quiz>> {
        return try {
            val distinct = quizIds.filter { it.isNotBlank() }.distinct()
            if (distinct.isEmpty()) {
                Result.success(emptyMap())
            } else {
                val chunks = distinct.chunked(WHERE_IN_CHUNK)
                val concurrency = maxConcurrentChunkQueries.coerceIn(1, 32)
                val semaphore = Semaphore(concurrency)
                val snapshots = coroutineScope {
                    chunks.map { chunk ->
                        async {
                            semaphore.withPermit {
                                quizzesCollection
                                    .whereIn(FieldPath.documentId(), chunk)
                                    .get()
                                    .await()
                            }
                        }
                    }.awaitAll()
                }
                val out = LinkedHashMap<String, Quiz>()
                for (snap in snapshots) {
                    for (doc in snap.documents) {
                        doc.toObject(Quiz::class.java)?.let { out[doc.id] = it }
                    }
                }
                Result.success(out)
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    //Получение квизов по категории
    override suspend fun getQuizzesByCategory(category: String, limit: Int): Result<List<Quiz>> = try {
        val res = quizzesCollection
            .whereEqualTo("categoryId", category)
            .whereEqualTo("public", true)  // Изменить здесь
            .limit(limit.toLong())
            .get()
            .await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java)}
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.error(e)
    }
    // Получение квизов, созданных конкретным юзером
    override suspend fun getQuizzesByUser(userId: String): Result<List<Quiz>> {
        if (userId.isBlank()) return Result.success(emptyList())
        return try {
            try {
                val res = quizzesCollection
                    .whereEqualTo("createdBy", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                Result.success(res.documents.mapNotNull { it.toObject(Quiz::class.java) })
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.w(
                        "QuizRepository",
                        "getQuizzesByUser: нет индекса createdBy+createdAt — запасной запрос без orderBy",
                        e
                    )
                    val res = quizzesCollection
                        .whereEqualTo("createdBy", userId)
                        .limit(100)
                        .get()
                        .await()
                    val list = res.documents
                        .mapNotNull { it.toObject(Quiz::class.java) }
                        .sortedByDescending { it.createdAt.toDate().time }
                    Result.success(list)
                } else {
                    Result.error(e)
                }
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // Создание квиза
    override suspend fun createQuiz(quiz: Quiz): Result<Unit> {
        return try {
            // Просто сохраняем викторину, НЕ создаем пользователя
            quizzesCollection.document(quiz.id).set(quiz).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Обновить существующий квиз
    override suspend fun updateQuiz(quiz: Quiz): Result<Unit> = try {
        quizzesCollection.document(quiz.id).set(quiz, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // Удаление квиза
    override suspend fun deleteQuiz(quizId: String): Result<Unit> = try {
        val quiz = getQuiz(quizId).getOrNull()
        quizzesCollection.document(quizId).delete().await()
        quiz?.createdBy?.let { creatorId ->
            usersCollection.document(creatorId)
                .update("createdQuizzes", FieldValue.arrayRemove(quizId))
                .await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    // Поиск по названию: префиксный range + public + category требует составного индекса в Firestore и часто падает
    // без него. Надёжный вариант выборка публичных документов (одно поле) и фильтр по подстроке на клиенте
    override suspend fun searchQuizzes(query: String, category: String?): Result<List<Quiz>> {
        return try {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                Result.success(emptyList())
            } else {
                val res = quizzesCollection
                    .whereEqualTo("public", true)
                    .limit(SEARCH_SCAN_LIMIT.toLong())
                    .get()
                    .await()

                val needle = trimmed.lowercase()
                var list = res.documents
                    .mapNotNull { it.toObject(Quiz::class.java) }
                    .filter { it.title.lowercase().contains(needle) }

                if (!category.isNullOrBlank()) {
                    list = list.filter { it.categoryId == category }
                }

                Result.success(list.take(SEARCH_MAX_RESULTS))
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    // Запись результатов прохождения квиза
    /*override suspend fun recordQuizResult(quizResult: ChallengeResult): Result<Unit> = try {
        val resWithId = quizResult.copy( id = quizResultsCollection.document().id )
        quizResultsCollection.document(resWithId.id).set(resWithId).await()
        val quizRef = quizzesCollection.document(quizResult.quizId)
        firestore.runTransaction { transaction ->
            val quizDoc = transaction.get(quizRef)
            val currentStats = quizDoc.get("stats") as? Map<String, Any> ?: mapOf()
            val newTimesTaken = (currentStats["times_taken"] as? Long ?: 0) + 1
            val currentAverage = currentStats["average_score"] as? Double ?: 0.0
            val newAverageScore = (currentAverage * (newTimesTaken - 1) + quizResult.accuracy) / newTimesTaken
            val updates = mapOf(
                "stats.times_taken" to newTimesTaken,
                "stats.average_score" to newAverageScore
            )
            transaction.update(quizRef, updates)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }*/

    // Получение популярных квизов ( отбор по количеству прохождений)
    // В QuizRepositoryImpl.kt изменить:
    override suspend fun getPopularQuizzes(limit: Int): Result<List<Quiz>> = try {
        println("DEBUG QuizRepositoryImpl: Querying with field 'public' instead of 'isPublic'")

        val res = quizzesCollection
            .whereEqualTo("public", true)  // Изменить isPublic → public
            .limit(limit.toLong())
            .get()
            .await()

        println("DEBUG QuizRepositoryImpl: Got ${res.documents.size} documents")

        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java) }
        Result.success(quizzes)
    } catch (e: Exception) {
        println("DEBUG QuizRepositoryImpl: Error: ${e.message}")
        Result.error(e)
    }

    override suspend fun getPublicCustomQuizzes(limit: Int): Result<List<Quiz>> = try {
        val res = quizzesCollection
            .orderBy(FieldPath.documentId())
            .startAt("quiz_custom_")
            .endAt("quiz_custom_\uf8ff")
            .limit(limit.toLong())
            .get()
            .await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java) }
            .filter { it.isPublic }
            .sortedByDescending { it.createdAt.toDate().time }
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.error(e)
    }

    /**
     * Десериализация результата: при «битых» вложенных [ChallengeResult.answers] toObject() может вернуть null —
     * тогда собираем документ вручную (для профиля достаточно метаданных и точности).
     */
    private fun parseChallengeResultDoc(doc: DocumentSnapshot): ChallengeResult? {
        if (!doc.exists()) return null
        val fromPojo = try {
            doc.toObject(ChallengeResult::class.java)
        } catch (e: RuntimeException) {
            Log.w("QuizRepository", "toObject ChallengeResult id=${doc.id}", e)
            null
        }
        if (fromPojo != null && fromPojo.quizId.isNotBlank() && fromPojo.userId.isNotBlank()) {
            return fromPojo.copy(id = doc.id)
        }
        val quizId = doc.getString("quizId") ?: return null
        val uid = doc.getString("userId") ?: return null
        fun numInt(key: String): Int = when (val v = doc.get(key)) {
            is Number -> v.toInt()
            else -> 0
        }
        return ChallengeResult(
            id = doc.id,
            quizId = quizId,
            userId = uid,
            userNickname = doc.getString("userNickname").orEmpty(),
            score = numInt("score"),
            totalQuestions = numInt("totalQuestions"),
            correctAnswers = numInt("correctAnswers"),
            incorrectAnswers = numInt("incorrectAnswers"),
            timeSpent = numInt("timeSpent"),
            averageTimePerQuestion = (doc.getDouble("averageTimePerQuestion") ?: 0.0),
            answers = emptyList(),
            pointsEarned = numInt("pointsEarned"),
            completedAt = doc.getTimestamp("completedAt") ?: Timestamp.now(),
            challengeId = doc.getString("challengeId")
        )
    }

    override suspend fun getRecentResultsForUser(userId: String, limit: Int): Result<List<ChallengeResult>> {
        if (userId.isBlank()) return Result.success(emptyList())
        val safeLimit = limit.coerceIn(1, 100)
        val fallbackFetchCap = (safeLimit * 8).coerceIn(80, 500)

        suspend fun finalize(docs: List<DocumentSnapshot>): List<ChallengeResult> =
            docs.mapNotNull { parseChallengeResultDoc(it) }
                .sortedByDescending { it.completedAt.toDate().time }
                .take(safeLimit)

        return try {
            try {
                val res = quizResultsCollection
                    .whereEqualTo("userId", userId)
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .limit(safeLimit.toLong())
                    .get()
                    .await()
                Result.success(finalize(res.documents))
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.w(
                        "QuizRepository",
                        "quiz_results: нет составного индекса userId+completedAt — запасной запрос без orderBy",
                        e
                    )
                    val res = quizResultsCollection
                        .whereEqualTo("userId", userId)
                        .limit(fallbackFetchCap.toLong())
                        .get()
                        .await()
                    Result.success(finalize(res.documents))
                } else {
                    Result.error(e)
                }
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun countSavedResultsForUserAndQuiz(userId: String, quizId: String): Result<Int> = try {
        if (userId.isBlank() || quizId.isBlank()) {
            Result.success(0)
        } else {
            val snap = quizResultsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("quizId", quizId)
                .get()
                .await()
            Result.success(snap.documents.size)
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun getUserQuizPlayCount(userId: String, quizId: String): Result<Int> = try {
        if (userId.isBlank() || quizId.isBlank()) {
            Result.success(0)
        } else {
            val ref = usersCollection.document(userId).collection("quiz_play_counts").document(quizId)
            val snap = ref.get().await()
            if (snap.exists()) {
                val n = (snap.get("count") as? Number)?.toInt() ?: 0
                Result.success(n)
            } else {
                countSavedResultsForUserAndQuiz(userId, quizId)
            }
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun recordUserQuizSessionFinished(
        userId: String,
        quizId: String,
        sessionId: String,
        savedResultToQuizResults: Boolean
    ): Result<Unit> = try {
        if (userId.isBlank() || quizId.isBlank() || sessionId.isBlank()) {
            Result.success(Unit)
        } else {
            val userRef = usersCollection.document(userId)
            val countRef = userRef.collection("quiz_play_counts").document(quizId)
            val sessionRef = userRef.collection("quiz_sessions").document(sessionId)
            firestore.runTransaction { tx ->
                val sessionSnap = tx.get(sessionRef)
                if (sessionSnap.exists()) {
                    return@runTransaction null
                }

                val countSnap = tx.get(countRef)
                if (!countSnap.exists()) {
                    tx.set(countRef, mapOf("count" to 1L), SetOptions.merge())
                } else {
                    tx.update(countRef, "count", FieldValue.increment(1))
                }

                tx.set(
                    sessionRef,
                    mapOf(
                        "quizId" to quizId,
                        "savedResultToQuizResults" to savedResultToQuizResults,
                        "finishedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                null
            }.await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.error(e)
    }

    // Запись результатов прохождения квиза с поддержкой вызовов
    /** Обновляет pointsEarned у записи победителя, если он завершил дуэль раньше соперника (в архиве было 0). */
    private suspend fun patchWinnerChallengeQuizResultPoints(
        winnerUserId: String,
        challengeId: String,
        pointsEarned: Int
    ) {
        val snap = quizResultsCollection
            .whereEqualTo("userId", winnerUserId)
            .whereEqualTo("challengeId", challengeId)
            .get()
            .await()
        for (doc in snap.documents) {
            try {
                quizResultsCollection.document(doc.id).update("pointsEarned", pointsEarned).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("QuizRepository", "patch winner quiz_result points: ${e.code} ${e.message}")
                }
            }
        }
    }

    override suspend fun recordQuizResult(
        quizResult: ChallengeResult,
        profileSessionXpForSolo: Int?
    ): Result<Int> {
        return try {
            val userRepo = UserRepositoryImpl()
            var duelXpOutcome: ChallengeWinnerXpOutcome? = null

            // 1) Дуэль: сначала вызов — иначе при PERMISSION_DENIED на quiz_results/users квиз не засчитается.
            if (!quizResult.challengeId.isNullOrBlank()) {
                val challengeRepository = ChallengeRepositoryImpl()
                when (
                    val sub = challengeRepository.submitChallengeResult(
                        challengeId = quizResult.challengeId!!,
                        userId = quizResult.userId,
                        result = quizResult
                    )
                ) {
                    is Result.Error   -> return Result.error(sub.exception)
                    is Result.Success -> Unit
                }
            }

            val returnXp: Int
            val storedPoints: Int
            if (quizResult.challengeId.isNullOrBlank()) {
                val profileXp = profileSessionXpForSolo ?: quizResult.pointsEarned
                when (val solo = userRepo.applySoloQuizCompletion(quizResult.userId, quizResult, profileXp)) {
                    is Result.Error -> return Result.error(solo.exception)
                    is Result.Success -> {
                        returnXp = solo.data
                        storedPoints = solo.data
                    }
                }
            } else {
                when (val part = userRepo.applyChallengeQuizParticipation(quizResult.userId, quizResult)) {
                    is Result.Error -> {
                        val code = (part.exception as? FirebaseFirestoreException)?.code
                            ?: (part.exception.cause as? FirebaseFirestoreException)?.code
                        if (code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            return Result.error(part.exception)
                        }
                    }
                    is Result.Success -> Unit
                }

                // Начисление XP победителю — до записи в quiz_results, чтобы в архиве и в профиле был фактический XP.
                when (val grant = userRepo.tryGrantChallengeWinnerXp(quizResult.challengeId!!)) {
                    is Result.Error -> {
                        val code = (grant.exception as? FirebaseFirestoreException)?.code
                            ?: (grant.exception.cause as? FirebaseFirestoreException)?.code
                        if (code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            return Result.error(grant.exception)
                        }
                    }
                    is Result.Success -> {
                        duelXpOutcome = grant.data
                        val o = grant.data
                        if (o != null && o.xpAdded > 0) {
                            ProfileAfterQuizRefresh.notify(o.winnerId)
                        }
                    }
                }

                val myDuelXp = duelXpOutcome?.takeIf { it.winnerId == quizResult.userId }?.xpAdded ?: 0
                returnXp = myDuelXp
                storedPoints = myDuelXp
            }

            val resWithId = quizResult.copy(
                id = quizResultsCollection.document().id,
                pointsEarned = storedPoints
            )

            // 2) Архив прохождения (в правилах обязателен match /quiz_results/{id} allow create)
            try {
                quizResultsCollection.document(resWithId.id).set(resWithId).await()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }

            // Победитель уже сыграл первым: у его записи было 0 — дописываем pointsEarned со второго устройства.
            val grant = duelXpOutcome
            val cid = quizResult.challengeId
            if (grant != null && grant.xpAdded > 0 && grant.winnerId != quizResult.userId && !cid.isNullOrBlank()) {
                patchWinnerChallengeQuizResultPoints(
                    winnerUserId = grant.winnerId,
                    challengeId = cid,
                    pointsEarned = grant.xpAdded
                )
            }

            // 3) Счётчики прохождений — коллекция quiz_stats (доступна любому auth; документ квиза правит только создатель)
            val quizRef = quizzesCollection.document(quizResult.quizId)
            val statRef = quizStatsCollection.document(quizResult.quizId)
            try {
                firestore.runTransaction { tx ->
                    val quizSnap = tx.get(quizRef)
                    if (!quizSnap.exists()) return@runTransaction

                    val embedded = quizSnap.get("stats") as? Map<String, Any> ?: mapOf()
                    val prevEmbedded = (embedded["times_taken"] as? Number)?.toLong() ?: 0L
                    val avgEmbedded = (embedded["average_score"] as? Number)?.toDouble() ?: 0.0

                    val statSnap = tx.get(statRef)
                    val (prevTaken, currentAverage) = if (statSnap.exists()) {
                        val pt = (statSnap.get("times_taken") as? Number)?.toLong() ?: 0L
                        val ca = (statSnap.get("average_score") as? Number)?.toDouble() ?: 0.0
                        pt to ca
                    } else {
                        prevEmbedded to avgEmbedded
                    }

                    val newTimesTaken = prevTaken + 1
                    val newAverageScore =
                        (currentAverage * prevTaken + quizResult.accuracy) / newTimesTaken

                    tx.set(
                        statRef,
                        mapOf(
                            "times_taken" to newTimesTaken,
                            "average_score" to newAverageScore
                        )
                    )
                }.await()
            } catch (e: FirebaseFirestoreException) {
                Log.e("QuizRepository", "quiz_stats write failed: ${e.code} ${e.message}")
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }

            Result.success(returnXp)
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}