package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.Quiz

interface QuizRepository {
    suspend fun getQuiz(quizId: String): Result<Quiz>
    suspend fun getQuizzesByCategory(category: String,limit: Int = 20):Result<List<Quiz>>
    suspend fun getQuizzesByUser(userId: String): Result<List<Quiz>>
    suspend fun createQuiz(quiz: Quiz): Result<Unit>
    suspend fun updateQuiz(quiz: Quiz): Result<Unit>
    suspend fun deleteQuiz(quizId: String): Result<Unit>
    suspend fun searchQuizzes(query: String, category: String? = null): Result<List<Quiz>>
    suspend fun recordQuizResult(quizResult: ChallengeResult): Result<Unit>
    suspend fun getPopularQuizzes(limit: Int = 10): Result<List<Quiz>>
}