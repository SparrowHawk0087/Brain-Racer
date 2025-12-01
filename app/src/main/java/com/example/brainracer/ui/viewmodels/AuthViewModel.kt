package com.example.brainracer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.domain.entities.User
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel: ViewModel() {
    // Firebase Auth объявление
    // User repository для работы с данными пользователя
    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository = UserRepositoryImpl()
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _user.value = auth.currentUser  // обновляем user
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in", e)
                _error.value = e.message // Показать ошибку пользователю
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                val profile = UserProfileChangeRequest.Builder().setDisplayName(username).build()
                user?.updateProfile(profile)?.await()
               // _user.value = auth.currentUser // обновляем user
                val firebaseUser = authResult.user

                // Создаем профиль пользователя в Firestore
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        nickname = username,
                        createdAt = com.google.firebase.Timestamp.now(),
                        lastLogin = com.google.firebase.Timestamp.now()
                    )

                    userRepository.createUser(user).fold(
                        onSuccess = { _user.value = auth.currentUser },
                        onFailure = { e ->
                            _error.value = "Failed to create user profile: ${e.message}"
                            // Откатываем: удаляем пользователя аутентификации если Firestore не удался
                            firebaseUser.delete().await()
                            _user.value = null
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing up", e)
                _error.value = when (e) {   // Показать ошибку пользователю
                        is FirebaseAuthUserCollisionException -> "Email already in use"
                        is IllegalArgumentException -> "Invalid email or password"
                        else -> e.message ?: "Unknown error"
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
            try {
                auth.currentUser?.delete()?.await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error deleting account", e)
            }
        }
    }

    fun checkAuthStatus() {
        _user.value = auth.currentUser
    }

    fun clearError() {
        _error.value = null
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