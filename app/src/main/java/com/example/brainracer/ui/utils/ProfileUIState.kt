package com.example.brainracer.ui.utils

import androidx.compose.runtime.Stable

@Stable
data class ProfileUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val registrationDate: String = "",
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val winRate: Double = 0.0,
    val totalPoints: Int = 0,
    val createdQuizzes: List<QuizItem> = emptyList(),
    val likedQuizzes: List<QuizItem> = emptyList(),
    val currentRank: String = "Новичок",
    val achievementsCount: Int = 0,
    val friendsCount: Int = 0,
    val bio: String = "",
    val interests: List<String> = emptyList()
)

@Stable
data class QuizItem(
    val id: String,
    val title: String,
    val category: String,
    val questionCount: Int,
    val difficulty: String,
)

object ProfileUtils {

    fun calculateWinRate(gamesPlayed: Int, gamesWon: Int): Double {
        return if (gamesPlayed > 0) {
            (gamesWon.toDouble() / gamesPlayed) * 100
        } else {
            0.0
        }
    }

    fun formatRegistrationDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("ru"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            "Дата не указана"
        }
    }

    fun formatLargeNumber(number: Int): String {
        return when {
            number >= 1_000_000 -> "${number / 1_000_000}M"
            number >= 1_000 -> "${number / 1_000}K"
            else -> number.toString()
        }
    }

    fun validateUsername(username: String): ValidationResult {
        return when {
            username.length < 1 -> ValidationResult(
                isValid = false,
                errorMessage = "Имя должно содержать минимум 1 символ"
            )
            username.length > 30 -> ValidationResult(
                isValid = false,
                errorMessage = "Имя не должно превышать 30 символов"
            )
            !username.matches(Regex("^[a-zA-Zа-яА-Я0-9_]+$")) -> ValidationResult(
                isValid = false,
                errorMessage = "Имя может содержать только буквы, цифры и _"
            )
            else -> ValidationResult(isValid = true)
        }
    }

    fun generateAvatarUrl(username: String, size: Int = 100): String {
        return "https://api.dicebear.com/7.x/avataaars/svg?seed=$username&size=$size"
    }

    fun getAvatarPlaceholder(): String {
        return "https://example.com/placeholder-avatar.png"
    }

    fun formatUserStats(gamesPlayed: Int, gamesWon: Int, totalPoints: Int): FormattedStats {
        return FormattedStats(
            gamesPlayedFormatted = formatLargeNumber(gamesPlayed),
            gamesWonFormatted = formatLargeNumber(gamesWon),
            winRateFormatted = calculateWinRate(gamesPlayed, gamesWon).toFormattedPercent(),
            totalPointsFormatted = formatLargeNumber(totalPoints)
        )
    }
}

@Stable
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

@Stable
data class FormattedStats(
    val gamesPlayedFormatted: String,
    val gamesWonFormatted: String,
    val winRateFormatted: String,
    val totalPointsFormatted: String
)

object ProfileConstants {
    const val MAX_USERNAME_LENGTH = 30
    const val MIN_USERNAME_LENGTH = 1
    const val DEFAULT_AVATAR_SIZE = 100
}

fun String.isValidUsername(): Boolean {
    return length in ProfileConstants.MIN_USERNAME_LENGTH..ProfileConstants.MAX_USERNAME_LENGTH &&
            matches(Regex("^[a-zA-Zа-яА-Я0-9_]+$"))
}

fun Double.toFormattedPercent(): String {
    return "%.1f%%".format(this)
}