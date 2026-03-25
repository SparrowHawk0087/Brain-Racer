package com.example.brainracer.data.repositories

import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.AppNotification
import com.example.brainracer.domain.entities.AppNotificationType
import com.example.brainracer.domain.entities.Challenge
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Реализация [NotificationRepository].
 *
 * Push-уведомления на устройство не отправляет — для этого нужна Cloud Function
 * (например триггер `onCreate` на `notifications` или `challenges`), которая читает
 * `users/{recipientId}.fcmToken` и вызывает Admin SDK `messaging.send`.
 */
class NotificationRepositoryImpl : NotificationRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val col = firestore.collection("notifications")

    override fun observeNotificationsForUser(userId: String): Flow<List<AppNotification>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        var registration: ListenerRegistration? = null
        registration = col
            .whereEqualTo("recipientUserId", userId)
            .limit(120)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }?.sortedWith(
                    compareByDescending<AppNotification> { it.createdAt.seconds }
                        .thenByDescending { it.createdAt.nanoseconds }
                ) ?: emptyList()
                trySend(list)
            }
        awaitClose { registration?.remove() }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = try {
        col.document(notificationId).update("read", true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }

    override suspend fun createChallengeNotification(
        challenge: Challenge,
        challengerAvatarUrl: String?,
        quizTotalTimeSeconds: Int?
    ): Result<Unit> = try {
        val docRef = col.document()
        val notification = AppNotification(
            id = docRef.id,
            recipientUserId = challenge.challengedUserId,
            type = AppNotificationType.CHALLENGE,
            title = "Новый вызов",
            message = "${challenge.challengerNickname} бросил вам вызов: «${challenge.quizTitle}»",
            read = false,
            actorUserId = challenge.challengerUserId,
            actorNickname = challenge.challengerNickname,
            actorAvatarUrl = challengerAvatarUrl,
            challengeId = challenge.id,
            quizTitle = challenge.quizTitle,
            quizTotalTimeSeconds = quizTotalTimeSeconds
        )
        docRef.set(notification).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.error(e)
    }
}
