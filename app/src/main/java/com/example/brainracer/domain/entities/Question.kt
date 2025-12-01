package com.example.brainracer.domain.entities

data class Question(
    val id: String = "",
    val questionText: String = "",
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
    val explanation: String? = null,
    val points: Int = 10,
    val timeLimit: Int = 30
)