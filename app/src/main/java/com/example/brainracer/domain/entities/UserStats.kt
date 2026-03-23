package com.example.brainracer.domain.entities

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
    var quizzesCreated: Int = 0
) {
    // Firestore требует no-arg конструктор для десериализации через toObject()
    constructor() : this(0, 0, 0, 0, 0, 0, 0, 0.0, 0)
}