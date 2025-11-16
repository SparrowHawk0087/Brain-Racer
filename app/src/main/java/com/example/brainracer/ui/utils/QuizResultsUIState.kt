package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.Achievement
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.domain.entities.UserStats

enum class QuizMark {
    EXCELLENT, // 90-100%
    GREAT,     // 75-89%
    GOOD,      // 60-74%
    AVERAGE,   // 40-59%
    POOR       // 0-39%
}

// TODO: обновить после внедрения UseCases
data class QuizResultsUIState(
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
