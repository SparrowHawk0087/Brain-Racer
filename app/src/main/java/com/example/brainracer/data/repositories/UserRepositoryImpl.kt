package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.UserRank
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    // Firestore instance и коллекция пользователей
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")

    // Получаем user из Firestore
    override suspend fun getUser(userId: String): Result<User> = try {
        val document = usersCollection.document(userId).get().await()
        if (document.exists()) {
            val user = document.toObject(User::class.java)
            Result.success(user ?: throw Exception("User data is null"))
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Создаем user в Firestore
    override suspend fun createUser(user: User) : Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Обновляем user в Firestore
    override suspend fun updateUser(user: User) : Result<Unit> = try {
        usersCollection.document(user.id).set(user, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Обновляем статистику пользователя в Firestore
    override suspend fun updateUserStats(
        userId: String,
        quizResult: ChallengeResult
    ): Result<Unit>  = try {
        val userRef = usersCollection.document(userId)
        firestore.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            val currentStats = userDoc.get("stats") as? Map<String, Any?> ?: mapOf()

            // Обновляем статистику
            val newTotalQuizzes = (currentStats["total_quizzes_taken"] as? Long ?: 0) + 1
            val newTotalQuestions = (currentStats["total_questions_answered"] as? Long ?: 0) + quizResult.totalQuestions
            val newCorrectAnswers = (currentStats["correct_answers"] as? Long ?: 0) + quizResult.correctAnswers
            val newIncorrectAnswers = (currentStats["incorrect_answers"] as? Long ?: 0) + quizResult.incorrectAnswers
            val newTotalPoints = (currentStats["total_points"] as? Long ?: 0) + quizResult.pointsEarned

            // Рассчитываем новое среднее значение
            val currentAverage = currentStats["average_score"] as? Double ?: 0.0
            val newAverageScore = if (newTotalQuizzes > 0) {
                (currentAverage * (newTotalQuizzes - 1) + quizResult.accuracy) / newTotalQuizzes
            } else quizResult.accuracy

            val updates = mapOf(
                "stats.total_quizzes_taken" to newTotalQuizzes,
                "stats.total_questions_answered" to newTotalQuestions,
                "stats.correct_answers" to newCorrectAnswers,
                "stats.incorrect_answers" to newIncorrectAnswers,
                "stats.total_points" to newTotalPoints,
                "stats.average_score" to newAverageScore
            )

            transaction.update(userRef, updates)

            // Обновляем ранг
            val newRank = calculateRank(newTotalPoints.toInt())
            transaction.update(userRef, "rank", newRank.name)

        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Функция поиска пользователей из Firestore
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
        Result.failure(e)
    }

    // Обновляем интересы пользователя в Firestore
    override suspend fun updateUserInterests(userId: String, interests: List<String>): Result<Unit> = try {
        usersCollection.document(userId)
            .update("interests", interests)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Обновляем аватар пользователя в Firestore
    override suspend fun updateUserAvatar(userId: String, avatarUrl: String): Result<Unit> = try {
        usersCollection.document(userId)
            .update("avatarUrl", avatarUrl)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Расчет ранга пользователя
    private fun calculateRank(points: Int): UserRank {
        return UserRank.entries
            .sortedByDescending { it.minPoints }
            .first { points >= it.minPoints }
    }
}