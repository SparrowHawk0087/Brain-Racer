package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.UserStats

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "",
    val userStats: UserStats = UserStats(),
    val quizzes: List<QuizItem> = emptyList(),
    val searchResults: List<QuizItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Все",
    val categories: List<String> = listOf(
        "Все", "География", "История", "Математика",
        "Фильмы и музыка", "Наука", "Спорт"
    ),

    // ── Уровень (вычисляется через LevelSystem на основе totalPoints) ─────
    /** Текущий уровень пользователя (1–50) */
    val userLevel: Int = 1,
    /** Прогресс внутри текущего уровня 0.0–1.0 */
    val levelProgress: Float = 0f,
    /** Название ранга (Beginner / Explorer / …) */
    val rankName: String = "Новичок"
)