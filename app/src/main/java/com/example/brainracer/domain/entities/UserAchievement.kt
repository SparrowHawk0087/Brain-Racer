package com.example.brainracer.domain.entities

import java.time.LocalDateTime

data class UserAchievement(
    val achievementId: String,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedAt: LocalDateTime
) {
    fun checkUnlock(targetValue: Int): Boolean =
        currentValue >= targetValue
}
