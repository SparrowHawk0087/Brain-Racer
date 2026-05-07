package com.example.brainracer.data.utils

import android.util.Log
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.domain.entities.Quiz

object QuizDataManager {
    private val quizRepository = QuizRepositoryImpl()
    private val quizDataSeeder = QuizDataSeeder

    suspend fun addDemoQuizzes(): Boolean {
        return try {
            Log.d("QuizDataManager", "Начинаем добавление демо-викторин...")

            // Создаем викторины синхронно
            val quizzes = quizDataSeeder.createSampleQuizzes()
            var successCount = 0

            for (quiz in quizzes) {
                try {
                    val result = quizRepository.createQuiz(quiz)
                    result.fold(
                        onSuccess = {
                            successCount++
                            Log.d("QuizDataManager", "✅ Добавлена: ${quiz.title}")
                        },
                        onFailure = { error ->
                            Log.e("QuizDataManager", "❌ Ошибка: ${quiz.title} - ${error.message}")
                        }
                    )
                    // Небольшая пауза между запросами
                    kotlinx.coroutines.delay(100)
                } catch (e: Exception) {
                    Log.e("QuizDataManager", "Исключение: ${e.message}")
                }
            }

            Log.d("QuizDataManager", "Добавлено $successCount из ${quizzes.size} викторин")
            successCount > 0
        } catch (e: Exception) {
            Log.e("QuizDataManager", "Критическая ошибка: ${e.message}")
            false
        }
    }

    suspend fun checkIfQuizzesExist(): Boolean {
        return try {
            // Проверяем первую викторину
            val result = quizRepository.getPopularQuizzes(limit = 1)
            result is com.example.brainracer.data.utils.Result.Success && result.data.isNotEmpty()
        } catch (e: Exception) {
            Log.e("QuizDataManager", "Ошибка проверки: ${e.message}")
            false
        }
    }

    suspend fun getAllQuizzes(): List<Quiz> {
        return try {
            val result = quizRepository.getPopularQuizzes(limit = 50)
            if (result is com.example.brainracer.data.utils.Result.Success) {
                result.data
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("QuizDataManager", "Ошибка загрузки: ${e.message}")
            emptyList()
        }
    }
}