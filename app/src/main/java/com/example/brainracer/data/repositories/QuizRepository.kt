package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.data.utils.Result

/**
 * [recordQuizResult] пишет в `challenges` (дуэль), `quiz_results`, `users` (stats+rank), `quizzes` (stats).
 *
 * Обязательно в правилах должны быть:
 * - `match /quiz_results/{resultId} { allow read, create: if request.auth != null; }` — без блока запись падает с PERMISSION_DENIED.
 * - `quizzes`: update для чужих прохождений только по полю `stats` (см. ниже).
 * - `users`: явный `allow update` для владельца документа (любые поля), отдельно — чужой update только `friends`.
 */
interface QuizRepository {
    suspend fun getQuiz(quizId: String): Result<Quiz>
    suspend fun getQuizzesByCategory(category: String,limit: Int = 20): Result<List<Quiz>>
    suspend fun getQuizzesByUser(userId: String): Result<List<Quiz>>
    suspend fun createQuiz(quiz: Quiz): Result<Unit>
    suspend fun updateQuiz(quiz: Quiz): Result<Unit>
    suspend fun deleteQuiz(quizId: String): Result<Unit>
    suspend fun searchQuizzes(query: String, category: String? = null): Result<List<Quiz>>
    suspend fun recordQuizResult(quizResult: ChallengeResult): Result<Unit>
    //Требует индекс Firestore: collection `quiz_results` — поля `userId` (Ascending) + `completedAt` (Descending), если подскажет консоль.
    suspend fun getRecentResultsForUser(userId: String, limit: Int = 40): Result<List<ChallengeResult>>
    suspend fun getPopularQuizzes(limit: Int = 10): Result<List<Quiz>>
}