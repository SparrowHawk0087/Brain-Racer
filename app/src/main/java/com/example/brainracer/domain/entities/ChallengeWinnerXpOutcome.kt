package com.example.brainracer.domain.entities

/** Результат однократного начисления XP победителю дуэли. */
data class ChallengeWinnerXpOutcome(
    val winnerId: String,
    val xpAdded: Int
)
