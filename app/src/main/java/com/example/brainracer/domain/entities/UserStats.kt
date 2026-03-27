package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Статистика пользователя

data class UserStats(
    @get:PropertyName("total_quizzes_taken")
    @set:PropertyName("total_quizzes_taken")
    var totalQuizzesTaken: Int = 0,

    @get:PropertyName("total_points")
    @set:PropertyName("total_points")
    var totalPoints: Int = 0,

    @get:PropertyName("current_streak")
    @set:PropertyName("current_streak")
    var currentStreak: Int = 0,

    @get:PropertyName("longest_streak")
    @set:PropertyName("longest_streak")
    var longestStreak: Int = 0,

    @get:PropertyName("correct_answers")
    @set:PropertyName("correct_answers")
    var correctAnswers: Int = 0,

    @get:PropertyName("incorrect_answers")
    @set:PropertyName("incorrect_answers")
    var incorrectAnswers: Int = 0,

    @get:PropertyName("total_questions_answered")
    @set:PropertyName("total_questions_answered")
    var totalQuestionsAnswered: Int = 0,

    @get:PropertyName("average_score")
    @set:PropertyName("average_score")
    var averageScore: Double = 0.0,

    @get:PropertyName("quizzes_created")
    @set:PropertyName("quizzes_created")
    var quizzesCreated: Int = 0,

    /** Викторины, пройденные в соло с зачётом (один раз на quizId). */
    @get:PropertyName("solo_completed_quiz_ids")
    @set:PropertyName("solo_completed_quiz_ids")
    var soloCompletedQuizIds: List<String> = emptyList(),

    /** Начало текущего UTC-дня для сброса дуэльного капа и decay-счётчиков (epoch ms). */
    @get:PropertyName("challenge_xp_day_utc_millis")
    @set:PropertyName("challenge_xp_day_utc_millis")
    var challengeXpDayUtcMillis: Long = 0L,

    /** Сколько XP с дуэлей уже зачтено за текущий UTC-день. */
    @get:PropertyName("challenge_xp_earned_today")
    @set:PropertyName("challenge_xp_earned_today")
    var challengeXpEarnedToday: Int = 0,

    /**
     * Сколько раз сегодня (UTC) по ключу pair+quiz уже было **успешной** выплаты XP победителю.
     * Значения в Firestore могут приходить как Long — при чтении приводим в репозитории при необходимости.
     */
    @get:PropertyName("challenge_pair_paid_today")
    @set:PropertyName("challenge_pair_paid_today")
    var challengePairPaidToday: Map<String, Int> = emptyMap(),

    @get:PropertyName("last_challenge_created_at")
    @set:PropertyName("last_challenge_created_at")
    var lastChallengeCreatedAt: Timestamp? = null
) {
    // Firestore требует no-arg конструктор для десериализации через toObject()
    constructor() : this(
        totalQuizzesTaken = 0,
        totalPoints = 0,
        currentStreak = 0,
        longestStreak = 0,
        correctAnswers = 0,
        incorrectAnswers = 0,
        totalQuestionsAnswered = 0,
        averageScore = 0.0,
        quizzesCreated = 0,
        soloCompletedQuizIds = emptyList(),
        challengeXpDayUtcMillis = 0L,
        challengeXpEarnedToday = 0,
        challengePairPaidToday = emptyMap(),
        lastChallengeCreatedAt = null
    )
}
