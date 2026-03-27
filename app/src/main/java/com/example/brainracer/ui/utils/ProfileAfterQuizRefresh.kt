package com.example.brainracer.ui.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * После успешного [recordQuizResult] профиль нужно перезагрузить с сервера
 * - [events]: если экран профиля уже на экране — подписка обновит сразу
 * - [pendingUserId]: если пользователь ушёл на другой экран — [takePending] на ON_RESUME
 */
object ProfileAfterQuizRefresh {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    @Volatile
    private var pendingUserId: String? = null

    fun notify(userId: String) {
        if (userId.isBlank()) return
        pendingUserId = userId
        _events.tryEmit(userId)
    }

    /** Вернуть true, если для этого [userId] ожидалось обновление после игры (флаг сбрасывается). */
    fun takePending(userId: String): Boolean {
        val p = pendingUserId ?: return false
        if (p != userId) return false
        pendingUserId = null
        return true
    }
}
