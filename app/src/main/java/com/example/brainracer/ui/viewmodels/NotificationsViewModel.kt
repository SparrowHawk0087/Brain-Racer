package com.example.brainracer.ui.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.NotificationRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.AppNotification
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val repository = NotificationRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    init {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Войдите в аккаунт")
            }
        } else {
            viewModelScope.launch {
                try {
                    repository.observeNotificationsForUser(uid).collect { list ->
                        _uiState.update { it.copy(items = list, isLoading = false, errorMessage = null) }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Ошибка загрузки")
                    }
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (repository.markAsRead(notificationId)) {
                is Result.Error -> { }
                is Result.Success -> { }
            }
        }
    }
}
