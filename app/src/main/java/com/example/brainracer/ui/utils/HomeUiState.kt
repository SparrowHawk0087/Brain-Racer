package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.UserStats

/** Вкладка категорий на главной: пользовательские викторины (`quiz_custom_*`). */
const val HOME_CATEGORY_CUSTOM = "Кастомные"

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
        "Фильмы и музыка", "Наука", "Спорт", HOME_CATEGORY_CUSTOM
    ),

    // ── Уровень ───────────────────────────────────────────────────────────
    val userLevel: Int = 1,
    val levelProgress: Float = 0f,
    val rankName: String = "Новичок",

    // ── Вызовы для HomeScreen ─────────────────────────────────────────────
    /** Входящие ожидающие вызовы — показываем badge */
    val pendingChallenges: List<Challenge> = emptyList(),
    /** Активные вызовы: входящие на решение, исходящие в ожидании, принятые в процессе */
    val homeActiveChallenges: List<Challenge> = emptyList(),
    /** Завершённые вызовы для вкладки «Завершённые» */
    val homeFinishedChallenges: List<Challenge> = emptyList(),
    /** ID текущего пользователя — нужен для определения роли в вызове */
    val currentUserId: String = "",

    // ── Новый вызов с главной ─────────────────────────────────────────────
    val friendsForChallenge: List<User> = emptyList(),
    val challengePickerQuizzes: List<QuizItem> = emptyList(),
    val challengePickerLoading: Boolean = false,
    val challengeSentMessage: String? = null,

    /** Непрочитанные in-app уведомления (коллекция notifications). */
    val unreadNotificationsCount: Int = 0
)