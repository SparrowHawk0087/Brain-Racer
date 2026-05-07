package com.example.brainracer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl.Companion.NICKNAME_TAKEN_ERROR_CODE
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.normalizeNicknameForStorage
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.utils.ModerationResult
import com.example.brainracer.ui.utils.QuizModeration
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
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
                signInWithRetry(email, password)
                _user.value = auth.currentUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in", e)
                _error.value = userFacingAuthMessage(e)
            }
        }
    }

    private suspend fun signInWithRetry(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            if (!isTransientNetworkError(e)) throw e
            Log.w("AuthViewModel", "Transient network error on signIn, retrying once", e)
            delay(SIGN_IN_RETRY_DELAY_MS)
            auth.signInWithEmailAndPassword(email, password).await()
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("Google user is null")
                ensureFirestoreUserForGoogle(firebaseUser)
                _user.value = firebaseUser
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in with Google", e)
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
                when (val nameModeration = QuizModeration.validateUsername(trimmedNick)) {
                    is ModerationResult.Blocked -> {
                        deleteNewAuthUserAndFail(
                            "Никнейм отклонен: [${nameModeration.violation.category.localizedLabel()}] ${nameModeration.violation.reason}"
                        )
                        return@launch
                    }
                    ModerationResult.Allowed -> Unit
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
                    _error.value = if (result.exception.message == NICKNAME_TAKEN_ERROR_CODE) {
                        "Этот никнейм уже занят"
                    } else {
                        "Не удалось создать профиль: ${result.exception.message ?: "ошибка сервера"}"
                    }
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
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _user.value = null
                    return@launch
                }
                val uid = currentUser.uid
                when (val cleanup = userRepository.deleteUserAccountData(uid)) {
                    is Result.Error -> {
                        _deleteAccountError.value =
                            "Не удалось удалить данные профиля: ${cleanup.exception.message ?: "ошибка"}"
                        return@launch
                    }
                    is Result.Success -> Unit
                }
                currentUser.delete().await()
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

    private fun isTransientNetworkError(e: Throwable): Boolean {
        var cur: Throwable? = e
        var depth = 0
        while (cur != null && depth < 8) {
            if (cur is FirebaseNetworkException ||
                cur is SSLException ||
                cur is SocketException ||
                cur is SocketTimeoutException ||
                cur is IOException
            ) return true
            val msg = cur.message?.lowercase().orEmpty()
            if (msg.isNotEmpty() && (
                        "connection reset" in msg ||
                                "broken pipe" in msg ||
                                "read error" in msg ||
                                "i/o error during system call" in msg ||
                                "timed out" in msg ||
                                "timeout" in msg ||
                                "unable to resolve host" in msg ||
                                "failed to connect" in msg
                        )
            ) return true
            cur = cur.cause
            depth++
        }
        return false
    }

    companion object {
        private const val SIGN_IN_RETRY_DELAY_MS = 600L
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

    private suspend fun ensureFirestoreUserForGoogle(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        when (val existing = userRepository.getUser(firebaseUser.uid)) {
            is Result.Success -> {
                val user = existing.data
                val fallbackNormalized = normalizeNicknameForStorage(user.nickname)
                val googlePhoto = firebaseUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
                val mergedAvatar = when {
                    user.avatarUrl?.contains("s3.cloud.ru") == true -> user.avatarUrl
                    googlePhoto != null -> googlePhoto
                    else -> user.avatarUrl
                }
                val updatedUser = user.copy(
                    email = firebaseUser.email ?: user.email,
                    avatarUrl = mergedAvatar,
                    nicknameNormalized = user.nicknameNormalized.ifBlank { fallbackNormalized },
                    lastLogin = Timestamp.now()
                )

                when (val updateResult = userRepository.updateUser(updatedUser)) {
                    is Result.Error -> throw updateResult.exception
                    is Result.Success -> Unit
                }
            }
            is Result.Error -> {
                val isUserMissing = existing.exception.message
                    ?.contains("User not found", ignoreCase = true) == true
                if (!isUserMissing) throw existing.exception

                val fallbackNickname = buildGoogleNickname(firebaseUser)
                val candidates = listOf(
                    fallbackNickname,
                    "${fallbackNickname}_${firebaseUser.uid.take(4)}"
                ).distinct()
                var created = false
                var lastError: Exception? = null
                for (candidate in candidates) {
                    val normalized = normalizeNicknameForStorage(candidate)
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        nickname = candidate,
                        nicknameNormalized = normalized,
                        avatarUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = Timestamp.now(),
                        lastLogin = Timestamp.now()
                    )
                    when (val createResult = userRepository.createUser(user)) {
                        is Result.Success -> {
                            created = true
                            break
                        }
                        is Result.Error -> {
                            lastError = createResult.exception
                            if (createResult.exception.message != NICKNAME_TAKEN_ERROR_CODE) {
                                throw createResult.exception
                            }
                        }
                    }
                }
                if (!created) {
                    throw (lastError ?: IllegalStateException("Не удалось создать профиль Google"))
                }
            }
        }
    }

    private fun buildGoogleNickname(firebaseUser: com.google.firebase.auth.FirebaseUser): String {
        val rawBase = firebaseUser.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore('@').orEmpty()
        val sanitized = rawBase
            .replace("\\s+".toRegex(), "_")
            .replace("[^\\p{L}\\p{N}_-]".toRegex(), "")
            .ifBlank { "Игрок" }
            .take(24)
        return if (sanitized.all { it.isDigit() }) "Игрок_${firebaseUser.uid.take(4)}" else sanitized
    }
}