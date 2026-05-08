package com.example.brainracer.ui.utils

import androidx.compose.runtime.Stable
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.UserStats
import java.text.SimpleDateFormat
import java.util.*

@Stable
data class PassedQuizUi(
    val quizId: String,
    val title: String,
    val category: String,
    val accuracyPercent: Int,
    val pointsEarned: Int,
    val completedAtEpochMs: Long
)

@Stable
data class TopicStatUi(
    val categoryName: String,
    /** 0f–100f */
    val percent: Float,
    val paletteIndex: Int
)

@Stable
data class AchievementUi(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

/** Бейджи целей хранятся в `User.interests` как список id. */
object ProfileGoalBadges {
    const val MAX_SELECTED = 5

    data class Option(val id: String, val label: String)

    val all: List<Option> = listOf(
        Option("accuracy", "Точность"),
        Option("speed", "Скорость"),
        Option("streak", "Серии побед"),
        Option("creator", "Создаю викторины"),
        Option("marathon", "Марафон игр"),
        Option("history", "История"),
        Option("science", "Наука"),
        Option("sport", "Спорт"),
        Option("geo", "География"),
        Option("movies", "Кино и музыка"),
        Option("math", "Математика")
    )

    private val byId: Map<String, String> = all.associate { it.id to it.label }

    fun labelFor(id: String): String? = byId[id]

    fun normalizeBadgeIds(ids: List<String>): List<String> =
        ids.distinct().filter { byId.containsKey(it) }.take(MAX_SELECTED)
}

@Stable
data class ProfileUIState(
    val isLoading: Boolean = true,
    val isUploadingAvatar: Boolean = false,
    val isSavingProfile: Boolean = false,
    val errorMessage: String? = null,
    /** Ошибка запроса quiz_results (индекс, сеть); не блокирует остальной профиль. */
    val quizHistoryLoadError: String? = null,
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val registrationDate: String = "",
    val userLevel: Int = 1,
    val levelProgress: Float = 0f,
    val rankName: String = "Новичок",
    val userStats: UserStats? = null,
    val createdQuizzes: List<QuizItem> = emptyList(),
    val likedQuizzes: List<QuizItem> = emptyList(),
    val passedAttempts: List<PassedQuizUi> = emptyList(),
    val topicStats: List<TopicStatUi> = emptyList(),
    val achievements: List<AchievementUi> = emptyList(),
    val currentRank: String = "Новичок",
    val achievementsCount: Int = 0,
    val friendsCount: Int = 0,
    val bio: String = "",
    val interests: List<String> = emptyList(),
    // id викторины, для которой идёт удаление (свой профиль)
    val deletingQuizId: String? = null
)

@Stable
data class QuizItem(
    val id: String,
    val title: String,
    val category: String,
    val questionCount: Int,
    val difficulty: String,
    val description: String,
    val rating: Double,
    val playCount: Int,
    val authorNickname: String = "",
)

/** UI-строка «Автор: …» только для кастомных викторин с сохранённым ником. */
fun QuizItem.customAuthorCaption(): String? =
    if (id.startsWith("quiz_custom_") && authorNickname.isNotBlank()) "Автор: $authorNickname" else null

fun Quiz.toQuizItem(): QuizItem = QuizItem(
    id = id,
    title = title,
    category = categoryId,
    questionCount = questions.size,
    difficulty = difficulty.name,
    description = description,
    rating = stats.averageRating,
    playCount = stats.timesTaken,
    authorNickname = creatorNickname
)

object ProfileAchievements {

    fun compute(
        stats: UserStats?,
        topicStats: List<TopicStatUi>,
        createdQuizCount: Int
    ): List<AchievementUi> {
        val played = stats?.totalQuizzesTaken ?: 0
        val longest = stats?.longestStreak ?: 0
        val current = stats?.currentStreak ?: 0
        val masterTopics = topicStats.count { it.percent >= 90f }

        return listOf(
            AchievementUi(
                id = "first_quiz",
                title = "Новичок",
                description = "Пройдите первую викторину",
                unlocked = played >= 1
            ),
            AchievementUi(
                id = "ten_quizzes",
                title = "Эксперт",
                description = "Пройдите 10 викторин",
                unlocked = played >= 10
            ),
            AchievementUi(
                id = "streak_5",
                title = "Непобедимый",
                description = "Серия из 5 успешных игр",
                unlocked = current >= 5 || longest >= 5
            ),
            AchievementUi(
                id = "topic_master",
                title = "Мастер тем",
                description = "90%+ точности в 3+ темах",
                unlocked = masterTopics >= 3
            ),
            AchievementUi(
                id = "week_streak",
                title = "Марафонец",
                description = "Серия из 7 игр",
                unlocked = longest >= 7 || current >= 7
            ),
            AchievementUi(
                id = "creator",
                title = "Автор",
                description = "Создайте хотя бы одну викторину",
                unlocked = createdQuizCount >= 1 || (stats?.quizzesCreated ?: 0) >= 1
            )
        )
    }
}

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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
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
            winRateFormatted = calculateWinRate(gamesPlayed, gamesWon).toString(),
            totalPointsFormatted = formatLargeNumber(totalPoints)
        )
    }

    fun formatPassedDate(epochMs: Long): String {
        if (epochMs <= 0L) return "—"
        return try {
            val fmt = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
            fmt.format(Date(epochMs))
        } catch (_: Exception) {
            "—"
        }
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
