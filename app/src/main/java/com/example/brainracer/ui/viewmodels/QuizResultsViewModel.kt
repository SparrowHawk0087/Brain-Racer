package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.brainracer.ui.utils.QuizResultsUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuizResultsViewModel: ViewModel() {
    // QuizResult UI state
    private val _uiState = MutableStateFlow(QuizResultsUiState())
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    // TODO: после добавления UseCases и сущностей
        /**
         * fun onEvent(event: QuizResultsUiEvent) {
         *
         *     }
         * */

    //TODO: после добавления UseCases
    private fun loadUserStats()
    {
    }
}