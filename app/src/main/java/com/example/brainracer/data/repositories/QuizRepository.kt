package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.data.utils.Result
interface QuizRepository {
    suspend fun getQuiz(quizId: String): Result<Quiz>
    suspend fun getQuizzesByIds(
        quizIds: List<String>,
        maxConcurrentChunkQueries: Int = 10
    ): Result<Map<String, Quiz>>
    suspend fun getQuizzesByCategory(category: String,limit: Int = 20): Result<List<Quiz>>
    suspend fun getQuizzesByUser(userId: String): Result<List<Quiz>>
    suspend fun createQuiz(quiz: Quiz): Result<Unit>
    suspend fun updateQuiz(quiz: Quiz): Result<Unit>
    suspend fun deleteQuiz(quizId: String): Result<Unit>
    suspend fun searchQuizzes(query: String, category: String? = null): Result<List<Quiz>>
    /**
     * @param profileSessionXpForSolo XP без speed для соло; для дуэли игнорируется.
     * @return фактически зачтённый в профиль XP за эту запись (соло — после правила «один раз»; дуэль — 0 здесь, победа обрабатывается отдельно).
     */
    suspend fun recordQuizResult(
        quizResult: ChallengeResult,
        profileSessionXpForSolo: Int? = null
    ): Result<Int>
    suspend fun getRecentResultsForUser(userId: String, limit: Int = 40): Result<List<ChallengeResult>>
    /** Сколько раз пользователь завершил эту викторину с сохранённым результатом (quiz_results). */
    suspend fun countSavedResultsForUserAndQuiz(userId: String, quizId: String): Result<Int>
    /** Все завершённые сессии (соло/дуэль, с записью и без), см. users/.../quiz_play_counts. */
    suspend fun getUserQuizPlayCount(userId: String, quizId: String): Result<Int>
    /* Фиксация завершения сессии прохождения
    Одна и та же sessionId учитывается в счетчике только один раз */
    suspend fun recordUserQuizSessionFinished(
        userId: String,
        quizId: String,
        sessionId: String,
        savedResultToQuizResults: Boolean
    ): Result<Unit>
    suspend fun getPopularQuizzes(limit: Int = 10): Result<List<Quiz>>
    /** Публичные викторины с id `quiz_custom_*` (пользовательский конструктор). */
    suspend fun getPublicCustomQuizzes(limit: Int = 50): Result<List<Quiz>>
}