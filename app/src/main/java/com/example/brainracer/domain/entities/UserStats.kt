package com.example.brainracer.domain.entities

import com.google.firebase.firestore.PropertyName

data class UserStats(
    @PropertyName("total_quizzes_taken")
    val totalQuizzesTaken: Int = 0,
    @PropertyName("total_questions_answered")
    val totalQuestionsAnswered: Int = 0,
    @PropertyName("correct_answers")
    val correctAnswers: Int = 0,
    @PropertyName("incorrect_answers")
    val incorrectAnswers: Int = 0,
    @PropertyName("average_score")
    val averageScore: Double = 0.0,
    @PropertyName("total_points")
    val totalPoints: Int = 0,
    @PropertyName("current_streak")
    val currentStreak: Int = 0,
    @PropertyName("longest_streak")
    val longestStreak: Int = 0,
    @PropertyName("quizzes_created")
    val quizzesCreated: Int = 0
)