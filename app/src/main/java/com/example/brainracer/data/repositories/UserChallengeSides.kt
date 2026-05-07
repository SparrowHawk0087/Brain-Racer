package com.example.brainracer.data.repositories

import com.example.brainracer.domain.entities.Challenge

/** Снимок вызовов, где пользователь — получатель или отправитель (для real-time и merge на клиенте). */
data class UserChallengeSides(
    val asChallenged: List<Challenge>,
    val asChallenger: List<Challenge>
)
