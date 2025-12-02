package com.example.brainracer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository = UserRepositoryImpl()
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in", e)
                _error.value = e.message
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                // Обновляем профиль в Firebase Auth
                firebaseUser?.let { user ->
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    user.updateProfile(profile).await()
                }

                // Создаём пользователя в Firestore
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        nickname = username,
                        createdAt = Timestamp.now(),
                        lastLogin = Timestamp.now()
                    )

                    val result = userRepository.createUser(user)
                    if (result is Result.Success) {
                        _user.value = auth.currentUser
                    } else if (result is Result.Error) {
                        _error.value = "Failed to create user profile: ${result.exception.message}"
                        try {
                            firebaseUser.delete().await()
                        } catch (deleteEx: Exception) {
                            Log.e("AuthViewModel", "Failed to delete Firebase user", deleteEx)
                        }
                        _user.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing up", e)
                _error.value = when (e) {
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