package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.normalizeNicknameForStorage
import com.example.brainracer.ui.screens.isValidUsername
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class NicknameEnforcementState {
    Idle,
    Checking,
    Ok,
    Locked,
}

class NicknameEnforcementViewModel : ViewModel() {

    private val userRepository = UserRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(NicknameEnforcementState.Idle)
    val state: StateFlow<NicknameEnforcementState> = _state.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _lockExplanation = MutableStateFlow<String?>(null)
    val lockExplanation: StateFlow<String?> = _lockExplanation.asStateFlow()

    fun reset() {
        _state.value = NicknameEnforcementState.Idle
        _submitError.value = null
        _isSaving.value = false
        _lockExplanation.value = null
    }

    fun refreshAsync(userId: String) {
        viewModelScope.launch { refresh(userId) }
    }

    suspend fun refresh(userId: String) {
        if (userId.isBlank()) {
            reset()
            return
        }
        _state.value = NicknameEnforcementState.Checking
        when (val u = userRepository.getUser(userId)) {
            is Result.Error -> {
                _lockExplanation.value = null
                _state.value = NicknameEnforcementState.Ok
            }
            is Result.Success -> {
                val user = u.data
                val effective = user.effectiveNicknameNormalized
                if (user.nicknameNormalized.isBlank() && user.nickname.isNotBlank()) {
                    userRepository.mergeNicknameNormalized(userId, effective)
                }
                if (user.nickname.isBlank() || effective.isBlank()) {
                    _lockExplanation.value =
                        "Никнейм не указан. Введите новый никнейм, чтобы пользоваться приложением."
                    _state.value = NicknameEnforcementState.Locked
                    return
                }
                if (!isValidUsername(user.nickname)) {
                    _lockExplanation.value =
                        "Текущий никнейм не соответствует правилам: без пробелов, не только цифры. Введите новый никнейм."
                    _state.value = NicknameEnforcementState.Locked
                    return
                }
                when (val c = userRepository.countUsersWithNicknameNormalized(effective)) {
                    is Result.Error -> {
                        _lockExplanation.value = null
                        _state.value = NicknameEnforcementState.Ok
                    }
                    is Result.Success -> {
                        if (c.data > 1) {
                            _lockExplanation.value =
                                "Ваш никнейм совпадает с другим аккаунтом. Введите другой никнейм, чтобы пользоваться приложением."
                            _state.value = NicknameEnforcementState.Locked
                        } else {
                            _lockExplanation.value = null
                            _state.value = NicknameEnforcementState.Ok
                        }
                    }
                }
            }
        }
    }

    fun submitNewNickname(raw: String, onFinished: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            onFinished(false)
            return
        }
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            _submitError.value = "Введите никнейм"
            onFinished(false)
            return
        }
        if (!isValidUsername(trimmed)) {
            _submitError.value = "Никнейм не должен быть пустым или содержать пробелы"
            onFinished(false)
            return
        }
        val normalized = normalizeNicknameForStorage(trimmed)
        viewModelScope.launch {
            _isSaving.value = true
            _submitError.value = null
            when (val cnt = userRepository.countUsersWithNicknameNormalized(normalized, userId)) {
                is Result.Error -> {
                    _submitError.value = cnt.exception.message
                    _isSaving.value = false
                    onFinished(false)
                    return@launch
                }
                is Result.Success -> {
                    if (cnt.data > 0) {
                        _submitError.value = "Этот никнейм уже занят"
                        _isSaving.value = false
                        onFinished(false)
                        return@launch
                    }
                }
            }
            when (val gu = userRepository.getUser(userId)) {
                is Result.Error -> {
                    _submitError.value = gu.exception.message
                    _isSaving.value = false
                    onFinished(false)
                }
                is Result.Success -> {
                    val upd = gu.data.copy(
                        nickname = trimmed,
                        nicknameNormalized = normalized
                    )
                    when (val up = userRepository.updateUser(upd)) {
                        is Result.Error -> {
                            _submitError.value = up.exception.message
                            _isSaving.value = false
                            onFinished(false)
                        }
                        is Result.Success -> {
                            try {
                                auth.currentUser?.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(trimmed)
                                        .build()
                                )?.await()
                            } catch (_: Exception) {
                            }
                            refresh(userId)
                            _isSaving.value = false
                            onFinished(true)
                        }
                    }
                }
            }
        }
    }

    fun clearSubmitError() {
        _submitError.value = null
    }
}
