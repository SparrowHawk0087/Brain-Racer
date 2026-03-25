package com.example.brainracer.data.repositories

import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.AppNotification
import com.example.brainracer.domain.entities.Challenge
import kotlinx.coroutines.flow.Flow

/**
 * In-app уведомления (`notifications` в Firestore).
 *
 * **Правила Firestore (вставить в консоль и при необходимости объединить с остальными):**
 * ```
 * match /notifications/{notificationId} {
 *   allow read: if request.auth != null
 *     && resource.data.recipientUserId == request.auth.uid;
 *   allow update, delete: if request.auth != null
 *     && resource.data.recipientUserId == request.auth.uid;
 *   allow create: if request.auth != null
 *     && request.resource.data.recipientUserId is string
 *     && request.resource.data.type == 'CHALLENGE'
 *     && request.resource.data.actorUserId == request.auth.uid
 *     && request.resource.data.actorUserId != request.resource.data.recipientUserId;
 * }
 * ```
 * Для `GENERAL` / категории / соревнования записи создаёт только Cloud Function или консоль.
 *
 * **Push (FCM):** клиент не может вызвать Admin API. В репозитории задеплоена функция
 * `functions/index.js` → `exports.onNotificationCreatedPush` (триггер `onDocumentCreated` на
 * `notifications/{id}`). Деплой: `cd functions && npm install`, затем из корня
 * `firebase deploy --only functions` (нужен Blaze и привязанный проект: `firebase use`).
 * Поле `users/{uid}.fcmToken` обновляет приложение при входе (см. `HomeViewModel.syncFcmTokenToProfile`).
 *
 * Реализация [NotificationRepositoryImpl] запрашивает документы только по `recipientUserId` и сортирует
 * на клиенте, чтобы не требовать составной индекс.
 */
interface NotificationRepository {

    fun observeNotificationsForUser(userId: String): Flow<List<AppNotification>>

    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Удаляет in-app уведомления о вызове у получателя (после отклонения, отмены или завершения дуэли).
     * Запрос: `recipientUserId` + `challengeId` (может потребоваться составной индекс в Firebase Console).
     */
    suspend fun deleteChallengeNotificationsForRecipient(
        challengeId: String,
        recipientUserId: String
    ): Result<Unit>

    /**
     * [challengerAvatarUrl] — из документа отправителя, для карточки в списке.
     * [quizTotalTimeSeconds] — время прохождения викторины (сек.), для отображения в списке уведомлений.
     */
    suspend fun createChallengeNotification(
        challenge: Challenge,
        challengerAvatarUrl: String? = null,
        quizTotalTimeSeconds: Int? = null
    ): Result<Unit>
}
