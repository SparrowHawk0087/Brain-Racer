package com.example.brainracer.domain.entities

import java.util.Date
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class ChallengeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    COMPLETED,
    EXPIRED,
    CANCELLED
}

data class Challenge(
    @PropertyName("id")
    val id: String = "",

    @PropertyName("quizId")
    val quizId: String = "",

    @PropertyName("quizTitle")
    val quizTitle: String = "",

    @PropertyName("challengerUserId")
    val challengerUserId: String = "",

    @PropertyName("challengerNickname")
    val challengerNickname: String = "",

    @PropertyName("challengedUserId")
    val challengedUserId: String = "",

    @PropertyName("challengedNickname")
    val challengedNickname: String = "",

    @PropertyName("status")
    val status: ChallengeStatus = ChallengeStatus.PENDING,

    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),

    @PropertyName("expiresAt")
    val expiresAt: Timestamp = Timestamp(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)),

    @PropertyName("completedAt")
    val completedAt: Timestamp? = null,

    @PropertyName("challengerResult")
    val challengerResult: ChallengeResult? = null,

    @PropertyName("challengedResult")
    val challengedResult: ChallengeResult? = null,

    // Поле winnerId остаётся — оно хранит победителя, записанного в Firestore.
    // Конфликт устранён переименованием метода ниже.
    @PropertyName("winnerId")
    val winnerId: String? = null,

    @PropertyName("isDraw")
    val isDraw: Boolean = false
) {
    // Переименовано: getWinnerId() → resolveWinnerId().
    // Старое имя getWinnerId() совпадало с JVM-геттером свойства winnerId,
    // что и вызывало "Platform declaration clash".
    fun resolveWinnerId(): String? {
        return if (status == ChallengeStatus.COMPLETED) {
            when {
                isDraw -> null
                winnerId != null -> winnerId
                else -> {
                    val challengerScore = challengerResult?.score ?: 0
                    val challengedScore = challengedResult?.score ?: 0
                    when {
                        challengerScore > challengedScore -> challengerUserId
                        challengedScore > challengerScore -> challengedUserId
                        else -> null
                    }
                }
            }
        } else null
    }

    // Может ли пользователь пройти викторину
    fun canPlay(userId: String): Boolean {
        return status == ChallengeStatus.ACCEPTED &&
                (userId == challengerUserId || userId == challengedUserId) &&
                expiresAt.toDate().after(Date())
    }

    // Есть ли уже результат у пользователя
    fun hasUserResult(userId: String): Boolean {
        return when (userId) {
            challengerUserId -> challengerResult != null
            challengedUserId -> challengedResult != null
            else -> false
        }
    }
}