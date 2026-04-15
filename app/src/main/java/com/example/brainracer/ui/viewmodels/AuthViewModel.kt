package com.example.brainracer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.normalizeNicknameForStorage
import com.example.brainracer.data.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository = UserRepositoryImpl()
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError: StateFlow<String?> = _deleteAccountError.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in", e)
                _error.value = userFacingAuthMessage(e)
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                val trimmedNick = username.trim()
                val normalized = normalizeNicknameForStorage(trimmedNick)

                suspend fun deleteNewAuthUserAndFail(message: String) {
                    _error.value = message
                    try {
                        firebaseUser?.delete()?.await()
                    } catch (deleteEx: Exception) {
                        Log.e("AuthViewModel", "Failed to delete Firebase user after nick check", deleteEx)
                    }
                    _user.value = null
                }

                if (firebaseUser == null) return@launch

                if (normalized.isBlank()) {
                    deleteNewAuthUserAndFail("Введите никнейм")
                    return@launch
                }

                if (trimmedNick.all { it.isDigit() }) {
                    deleteNewAuthUserAndFail("Никнейм не может состоять только из цифр")
                    return@launch
                }

                when (val nickCount = userRepository.countUsersWithNicknameNormalized(normalized)) {
                    is Result.Error -> {
                        deleteNewAuthUserAndFail(
                            "Не удалось проверить никнейм: ${nickCount.exception.message ?: "ошибка"}"
                        )
                        return@launch
                    }
                    is Result.Success -> {
                        if (nickCount.data > 0) {
                            deleteNewAuthUserAndFail("Этот никнейм уже занят")
                            return@launch
                        }
                    }
                }

                // Обновляем профиль в Firebase Auth
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedNick)
                    .build()
                firebaseUser.updateProfile(profile).await()

                // Создаём пользователя в Firestore
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    nickname = trimmedNick,
                    nicknameNormalized = normalized,
                    createdAt = Timestamp.now(),
                    lastLogin = Timestamp.now()
                )

                val result = userRepository.createUser(user)
                if (result is Result.Success) {
                    _user.value = auth.currentUser
                } else if (result is Result.Error) {
                    _error.value =
                        "Не удалось создать профиль: ${result.exception.message ?: "ошибка сервера"}"
                    try {
                        firebaseUser.delete().await()
                    } catch (deleteEx: Exception) {
                        Log.e("AuthViewModel", "Failed to delete Firebase user", deleteEx)
                    }
                    _user.value = null
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing up", e)
                _error.value = when (e) {
                    is FirebaseAuthUserCollisionException -> "Этот email уже зарегистрирован"
                    is IllegalArgumentException -> "Некорректный email или пароль"
                    else -> userFacingAuthMessage(e)
                }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = auth.currentUser
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error sending password reset email", e)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountError.value = null
            try {
                auth.currentUser?.delete()?.await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error deleting account", e)
                _deleteAccountError.value = userFacingDeleteAccountMessage(e)
            }
        }
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }

    private fun userFacingDeleteAccountMessage(e: Exception): String = when (e) {
        is FirebaseAuthRecentLoginRequiredException ->
            "Сессия устарела. Выйдите из аккаунта и войдите снова, затем повторите удаление."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_REQUIRES_RECENT_LOGIN" ->
                "Сессия устарела. Выйдите из аккаунта и войдите снова, затем повторите удаление."
            else -> userFacingAuthMessage(e)
        }
        else -> userFacingAuthMessage(e)
    }

    fun checkAuthStatus() {
        _user.value = auth.currentUser
    }

    fun clearError() {
        _error.value = null
    }

    private fun userFacingAuthMessage(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль"
        is FirebaseAuthInvalidUserException -> "Аккаунт с таким email не найден"
        is FirebaseAuthWeakPasswordException -> "Пароль слишком слабый"
        is FirebaseAuthUserCollisionException -> "Этот email уже зарегистрирован"
        is IllegalArgumentException -> "Некорректные данные"
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_USER_DISABLED" -> "Этот аккаунт отключён"
            "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Попробуйте позже"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Проверьте подключение к интернету"
            "ERROR_INVALID_EMAIL" -> "Некорректный email"
            "ERROR_WRONG_PASSWORD" -> "Неверный пароль"
            "ERROR_USER_NOT_FOUND" -> "Аккаунт с таким email не найден"
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: "Ошибка авторизации (${e.errorCode})"
        }
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Произошла ошибка"
    }

    fun reloadUser() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to reload user", e)
                _user.value = null
            }
        }
    }
}