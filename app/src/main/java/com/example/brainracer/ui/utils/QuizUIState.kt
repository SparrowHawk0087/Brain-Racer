package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.UserAnswer

/** Почему прохождение не идёт в прогресс / облако */
enum class QuizNonScoringReason {
    /** Нет подключения при старте сессии */
    OFFLINE,
    /** Нет вошедшего пользователя */
    NOT_SIGNED_IN
}

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
    val newLevelProgress: Float = 0f,

    // ── Разбор ответов (заполняются после завершения) ─────────────────────
    /** Все вопросы викторины — нужны для экрана разбора */
    val reviewQuestions: List<Question> = emptyList(),
    /** Ответы пользователя в том же порядке, что и вопросы */
    val reviewAnswers: List<UserAnswer> = emptyList(),

    // ── Режим вызова ──────────────────────────────────────────────────────
    /** ID вызова, если викторина проходится в режиме Challenge. null = обычный режим */
    val challengeId: String? = null,
    /** Название викторины (для экрана старта вызова) */
    val quizTitle: String = "",

    /** Сеть была доступна при старте loadQuiz */
    val sessionNetworkAvailable: Boolean = true,

    /** true, если результат не сохраняется и не влияет на статистику */
    val isNonScoringSession: Boolean = false,
    val nonScoringReason: QuizNonScoringReason? = null,

    /**
     * Дуэль: XP в профиль за победу начисляется при завершении вызова (кап/decay).
     * На экране результатов показываем подсказку вместо «+XP».
     */
    val duelXpDeferred: Boolean = false
)

data class XpBreakdown(
    val baseXp: Int,
    val speedBonusXp: Int,
    val accuracyBonusXp: Int,
    val difficultyLabel: String,
    val totalXp: Int
)