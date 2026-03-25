package com.example.brainracer.domain.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class AppNotificationType {
    CHALLENGE,
    GENERAL,
    CATEGORY,
    COMPETITION
}

/**
 * In-app уведомление в коллекции Firestore `notifications`.
 * @see com.example.brainracer.data.repositories.NotificationRepository
 */
data class AppNotification(
    @PropertyName("id")
    val id: String = "",

    @PropertyName("recipientUserId")
    val recipientUserId: String = "",

    @PropertyName("type")
    val type: AppNotificationType = AppNotificationType.GENERAL,

    @PropertyName("title")
    val title: String = "",

    @PropertyName("message")
    val message: String = "",

    @PropertyName("read")
    val read: Boolean = false,

    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),

    @PropertyName("actorUserId")
    val actorUserId: String? = null,

    @PropertyName("actorNickname")
    val actorNickname: String? = null,

    @PropertyName("actorAvatarUrl")
    val actorAvatarUrl: String? = null,

    @PropertyName("challengeId")
    val challengeId: String? = null,

    @PropertyName("quizTitle")
    val quizTitle: String? = null,

    /** Суммарное время на все вопросы, секунды (как у [Quiz.totalTime]). */
    @PropertyName("quizTotalTimeSeconds")
    val quizTotalTimeSeconds: Int? = null
)
