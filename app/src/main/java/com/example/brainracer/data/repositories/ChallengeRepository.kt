package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeResult
import com.example.brainracer.data.utils.Result
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    // Создание нового вызова
    suspend fun createChallenge(challenge: Challenge): Result<String>

    // Получить все вызовы для пользователя (входящие + исходящие)
    suspend fun getChallengesForUser(userId: String): Result<List<Challenge>>

    // Получить только входящие вызовы (ожидают ответа)
    suspend fun getIncomingChallenges(userId: String): Result<List<Challenge>>

    // Получить только исходящие вызовы
    suspend fun getOutgoingChallenges(userId: String): Result<List<Challenge>>

    // Получить активные вызовы (ACCEPTED, можно проходить)
    suspend fun getActiveChallenges(userId: String): Result<List<Challenge>>

    // Получить историю завершённых вызовов
    suspend fun getCompletedChallenges(userId: String, limit: Int = 20): Result<List<Challenge>>

    // Получить конкретный вызов по ID
    suspend fun getChallenge(challengeId: String): Result<Challenge>

    // Принять вызов
    suspend fun acceptChallenge(challengeId: String): Result<Unit>

    // Отклонить вызов
    suspend fun declineChallenge(challengeId: String): Result<Unit>

    // Отменить вызов (для отправителя)
    suspend fun cancelChallenge(challengeId: String): Result<Unit>

    // Отправить результат прохождения викторины в вызове
    suspend fun submitChallengeResult(
        challengeId: String,
        userId: String,
        result: ChallengeResult
    ): Result<Unit>

    // Проверить и обновить истёкшие вызовы
    suspend fun checkExpiredChallenges(): Result<Unit>

    /**
     * Подписка на изменения: два snapshot-listener (`challengedUserId`, `challengerUserId`) без `orderBy` —
     * не требуются составные индексы, соперник видит вызов сразу после записи.
     *
     * В Firebase Console для `challenges` нужны правила, чтобы получатель мог **читать** документ, например:
     * `allow read: if request.auth != null && (resource.data.challengerUserId == request.auth.uid || resource.data.challengedUserId == request.auth.uid);`
     * и `allow create` при `request.resource.data.challengerUserId == request.auth.uid`.
     */
    fun observeUserChallengeSides(userId: String): Flow<UserChallengeSides>

    /** Одноразовая загрузка тех же данных (pull-to-refresh и т.п.). */
    suspend fun fetchUserChallengeSides(userId: String): Result<UserChallengeSides>
}