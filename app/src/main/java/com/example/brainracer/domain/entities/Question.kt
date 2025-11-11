package com.example.brainracer.domain.entities

// Сущность вопроса
data class Question(
    val id: String,
    val text: String,
    val imageUrl: String?,
    val answers: List<UserAnswer>,
    val timeLimit: Long,
    val points: Int
) {
    fun getCorrectAnswer(): UserAnswer? = answers.find { it.isCorrect }
}