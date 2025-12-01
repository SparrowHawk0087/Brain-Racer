package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.ChallengeResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class QuizRepositoryImpl: QuizRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    // Firestore коллекции
    private val quizzesCollection = firestore.collection("quizzes")
    private val usersCollection = firestore.collection("users")
    private val quizResultsCollection = firestore.collection("quiz_results")

    //Получение квиза по id
    override suspend fun getQuiz(quizId: String): Result<Quiz> = try {
        val document = quizzesCollection.document(quizId).get().await()
        if (document.exists()) {
            val quiz = document.toObject(Quiz::class.java)
            Result.success(quiz ?: throw Exception("Quiz data is null"))
        } else {
            Result.failure(Exception("Quiz not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    //Получение квизов по категории
    override suspend fun getQuizzesByCategory(category: String, limit: Int): Result<List<Quiz>> = try {
        val res = quizzesCollection
            .whereEqualTo("category",category)
            .whereEqualTo("isPublic",true)
            .limit(limit.toLong())
            .get()
            .await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java)}
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.failure(e)
    }
    // Получение квизов, созданных конкретным юзером
    override suspend fun getQuizzesByUser(userId: String): Result<List<Quiz>> = try {
        val res = quizzesCollection
            .whereEqualTo("createdBy",userId)
            .orderBy("createdAt",Query.Direction.DESCENDING)
            .get()
            .await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java)}
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Создание квиза
    override suspend fun createQuiz(quiz: Quiz): Result<Unit> = try {
        val quizWithId = if (quiz.id.isBlank()){
            quiz.copy(id = quizzesCollection.document().id)
        } else
            quiz
        quizzesCollection.document(quizWithId.id).set(quizWithId).await()
        usersCollection.document(quizWithId.createdBy)
            .update("createdQuizzes",FieldValue.arrayUnion(quizWithId.id))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Обновить существующий квиз
    override suspend fun updateQuiz(quiz: Quiz): Result<Unit> = try {
        quizzesCollection.document(quiz.id).set(quiz, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
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
        Result.failure(e)
    }

    // Поиск квизов по названию (+ категории)
    override suspend fun searchQuizzes(query: String, category: String?): Result<List<Quiz>> = try {
        var queryRef = quizzesCollection
            .whereEqualTo("isPublic", true)
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + "\uf8ff")
            .limit(20)
        if (!category.isNullOrBlank())
            queryRef = queryRef.whereEqualTo("category", category)
        val res = queryRef.get().await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java) }
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Запись результатов прохождения квиза
    override suspend fun recordQuizResult(quizResult: ChallengeResult): Result<Unit> = try {
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
        Result.failure(e)
    }

    // Получение популярных квизов ( отбор по количеству прохождений)
    override suspend fun getPopularQuizzes(limit: Int): Result<List<Quiz>> = try {
        val res = quizzesCollection
            .whereEqualTo("isPublic", true)
            .orderBy("stats.times_taken", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        val quizzes = res.documents.mapNotNull { it.toObject(Quiz::class.java) }
        Result.success(quizzes)
    } catch (e: Exception) {
        Result.failure(e)
    }
}