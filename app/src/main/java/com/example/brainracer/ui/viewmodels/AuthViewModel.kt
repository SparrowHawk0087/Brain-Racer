package com.example.brainracer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel: ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val user_ = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = user_
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _authResult = MutableStateFlow<AuthResult?>(null)
    val authResult: StateFlow<AuthResult?> = _authResult

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                user_.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in", e)
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
                user_.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing up", e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        user_.value = auth.currentUser
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
                user_.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error deleting account", e)
            }
        }
    }

    fun checkAuthStatus() {
        user_.value = auth.currentUser
    }

    fun clearError() {
        _error.value = null
    }

    fun reloadUser() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()
                user_.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to reload user", e)
                user_.value = null
            }
        }
    }
}