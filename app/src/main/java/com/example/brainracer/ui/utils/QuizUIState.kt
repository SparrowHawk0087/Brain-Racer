package com.example.brainracer.ui.utils

/**
 * UI-состояние экрана прохождения викторины.
 */
data class QuizUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // ── Текущий вопрос ────────────────────────────────────────────────────
    val question: String = "",
    val options: List<String> = emptyList(),
    val selectedAnswerIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val isAnswerCorrect: Boolean? = null,
    val attachedImageUrl: String? = null,

    /** Лимит времени на текущий вопрос (в секундах), берётся из Question.timeLimit */
    val currentQuestionTimeLimit: Int = 30,

    // ── Счётчики ──────────────────────────────────────────────────────────
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val accuracy: Double = 0.0,

    // ── Управление экранами ───────────────────────────────────────────────
    val isQuizCompleted: Boolean = false,
    val showResults: Boolean = false,

    // ── XP / уровни (заполняются после завершения) ────────────────────────
    val xpEarned: Int = 0,
    val xpBreakdown: XpBreakdown? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int = 1,
    val newLevelProgress: Float = 0f
)

data class XpBreakdown(
    val baseXp: Int,
    val speedBonusXp: Int,
    val accuracyBonusXp: Int,
    val difficultyLabel: String,
    val totalXp: Int
)