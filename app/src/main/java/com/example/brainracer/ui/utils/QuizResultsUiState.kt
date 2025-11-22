package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.Achievement
import com.example.brainracer.domain.entities.UserStats

enum class QuizMark {
    EXCELLENT, // 90-100%
    GREAT,     // 75-89%
    GOOD,      // 60-74%
    AVERAGE,   // 40-59%
    POOR       // 0-39%
}

// TODO: обновить после внедрения UseCases
data class QuizResultsUiState(
    //val quizResult: QuizResult? = null,
    val userStats: UserStats? = null,
    //val levelInfo: LevelInfo? = null,
    val leveledUp: Boolean = false,
    val showAnswerDetails: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val unlockedAchievements: List<Achievement> = emptyList(),
    val showAchievements: Boolean = false,
    val shareContent: String? = null,
    val mark: QuizMark = QuizMark.AVERAGE
    ) {
    // TODO: обновить после внедрения UseCases
    /**
     * val canPlayAgain: Boolean
     *         get() = quizResult != null && !isLoading
     *     val correctPercentage: Int
     *         get() = quizResult?.let {
     *             (it.correctAnswers * 100) / it.totalQuestions
     *         } ?: 0
     * */

}
data class DashboardUiState(
    // 1. Данные пользователя (Пользователь / User UseCase Group)
    val userName: String = "Гость",
    val userStats: UserStats? = null,
    val currentLevel: Int = 1,

    // 2. Сводка по Викторинам и Вызовам (Quizzes / Challenges UseCase Groups)
    val availableQuizzesCount: Int = 0,
    val pendingChallengesCount: Int = 0,

    // 3. Социальные функции / Достижения (Social / Achievements UseCase Groups)
    val newAchievementsCount: Int = 0,
    val notificationsCount: Int = 0,

    // 4. UI-состояние
    val isLoading: Boolean = false,
    val error: String? = null
)
