package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.utils.HomeUiState
import com.example.brainracer.ui.utils.QuizItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val quizRepository = QuizRepositoryImpl()

    fun loadQuizzes() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Загружаем популярные викторины
                val popularResult = quizRepository.getPopularQuizzes(limit = 10)

                when (popularResult) {
                    is Result.Success -> {
                        val quizzes = popularResult.data

                        // Конвертируем в QuizItem для UI
                        val quizItems = quizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.category,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name,
                                description = quiz.description,
                                rating = quiz.stats.averageRating,
                                playCount = quiz.stats.timesTaken
                            )
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                quizzes = quizItems,
                                errorMessage = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки викторин: ${popularResult.exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadQuizzesByCategory(category: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val categoryResult = quizRepository.getQuizzesByCategory(category)

                when (categoryResult) {
                    is Result.Success -> {
                        val quizzes = categoryResult.data

                        val quizItems = quizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.category,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name,
                                description = quiz.description,
                                rating = quiz.stats.averageRating,
                                playCount = quiz.stats.timesTaken
                            )
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                quizzes = quizItems,
                                selectedCategory = category
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Ошибка загрузки категории: ${categoryResult.exception.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun searchQuizzes(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            try {
                val searchResult = quizRepository.searchQuizzes(query)

                when (searchResult) {
                    is Result.Success -> {
                        val quizzes = searchResult.data

                        val quizItems = quizzes.map { quiz ->
                            QuizItem(
                                id = quiz.id,
                                title = quiz.title,
                                category = quiz.category,
                                questionCount = quiz.questions.size,
                                difficulty = quiz.difficulty.name,
                                description = quiz.description,
                                rating = quiz.stats.averageRating,
                                playCount = quiz.stats.timesTaken
                            )
                        }

                        _uiState.update {
                            it.copy(
                                searchResults = quizItems,
                                searchQuery = query
                            )
                        }
                    }
                    is Result.Error -> {
                        // Можно не показывать ошибку, если ничего не найдено
                        if (query.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    searchResults = emptyList(),
                                    searchQuery = query
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки поиска
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchResults = emptyList(),
                searchQuery = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
