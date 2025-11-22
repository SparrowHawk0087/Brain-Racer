package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.brainracer.ui.utils.QuizResultsUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.brainracer.ui.utils.DashboardUiState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class QuizResultsViewModel: ViewModel() {
    // QuizResult UI state
    private val _uiState = MutableStateFlow(QuizResultsUiState())
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    // TODO: после добавления UseCases и сущностей
    /**
     * fun onEvent(event: QuizResultsUiEvent) {
     *
     * }
     * */

    //TODO: после добавления UseCases
    private fun loadUserStats()
    {
    }
}
// Класс будет принимать UseCases в конструкторе после их внедрения
class DashboardViewModel(/* ... UseCases ... */) : ViewModel() {

    // Держатель состояния UI
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Заглушка для загрузки всех данных, необходимых для отображения Dashboard.
     */
    private fun loadDashboardData() {
        // TODO: Использовать UseCases
        viewModelScope.launch { // <-- Now correctly resolved
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Имитация загрузки данных
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userName = "fendercm",
                currentLevel = 42,
                availableQuizzesCount = 15,
                newAchievementsCount = 3
            )
        }
    }

    // Заглушка для обработки событий
    // **
    // * fun onEvent(event: DashboardEvent) {
    // * // ...
    // * }
    // **
}

/**
 * Заглушка для событий, которые могут исходить от UI.
 */
sealed class DashboardEvent {
    object NavigateToQuizzes : DashboardEvent()
    object NavigateToChallenges : DashboardEvent()
    object ViewNewAchievements : DashboardEvent()
}