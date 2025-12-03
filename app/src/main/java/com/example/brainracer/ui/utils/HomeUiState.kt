package com.example.brainracer.ui.utils

import com.example.brainracer.domain.entities.UserStats

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "",
    val userStats: UserStats = UserStats(),
    val quizzes: List<QuizItem> = emptyList(),
    val searchResults: List<QuizItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Все",
    val categories: List<String> = listOf(
    "Все",
    "Биология",
    "Кулинария",
    "Наука",
    "Математика",
    "История",
    "География"
)
)
