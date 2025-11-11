package com.example.brainracer.domain.entities
import java.time.LocalDateTime

data class Question(
    val id: String,
    val text: String,
    val imageUrl: String?,
    val answers: List<Answer>,
    val timeLimit: Long,
    val points: Int
) {
    fun getCorrectAnswer(): Answer? = answers.find { it.isCorrect }
}