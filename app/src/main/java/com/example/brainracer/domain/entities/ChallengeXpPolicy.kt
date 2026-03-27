package com.example.brainracer.domain.entities

/**
 * Балансируемые константы антигринда дуэлей (см. план Fair XP).
 */
object ChallengeXpPolicy {

    /** Максимум XP с дуэлей за календарные сутки UTC. */
    const val DAILY_CHALLENGE_XP_CAP = 800

    /** Пауза между созданием вызовов одним пользователем (секунды). */
    const val CHALLENGE_CREATE_COOLDOWN_SECONDS = 120L

    /**
     * Множитель к [sessionProfileXp] для N-й успешной выплаты XP за день
     * по паре (соперник + квиз). N = 0 → первая выплата.
     */
    private val DECAY_STEPS = floatArrayOf(1f, 0.5f, 0.25f, 0f)

    fun multiplierForPaidAttemptIndex(alreadyPaidCount: Int): Float {
        val idx = alreadyPaidCount.coerceIn(0, DECAY_STEPS.lastIndex)
        return DECAY_STEPS[idx]
    }

    /** Канонический ключ пары пользователей и викторины для счётчика decay. */
    fun pairKeyQuiz(userIdA: String, userIdB: String, quizId: String): String {
        val (a, b) = if (userIdA <= userIdB) userIdA to userIdB else userIdB to userIdA
        return "${a}_${b}_$quizId"
    }

    fun utcDayStartMillis(): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
