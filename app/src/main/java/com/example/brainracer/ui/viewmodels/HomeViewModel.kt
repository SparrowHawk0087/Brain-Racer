package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userName: String = "",
    val userStats: UserStats? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // Заглушка данных как в других ViewModel
            _uiState.value = HomeUiState(
                isLoading = false,
                userName = "Brain Racer",
                userStats = UserStats(
                    totalQuizzesTaken = 15,
                    totalQuestionsAnswered = 150,
                    correctAnswers = 120,
                    incorrectAnswers = 30,
                    averageScore = 80.0,
                    totalPoints = 1250,
                    currentStreak = 5,
                    longestStreak = 10,
                    quizzesCreated = 3
                )
            )
        }
    }

    fun refreshData() {
        loadHomeData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

// Нужно импортировать или объявить UserStats
data class UserStats(
    val totalQuizzesTaken: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val averageScore: Double = 0.0,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val quizzesCreated: Int = 0
)
