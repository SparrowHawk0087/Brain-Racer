package com.example.brainracer.domain.entities

/**
 * Вся математика системы уровней.
 *
 * ── Формула XP за викторину ───────────────────────────────────────────────
 *
 *   baseXp         = Σ question.points  для каждого ПРАВИЛЬНОГО ответа
 *
 *   speedBonus     = за каждый правильный ответ:
 *                     timeSpent ≤ timeLimit × 0.33  →  +50% от points
 *                     timeSpent ≤ timeLimit × 0.66  →  +20% от points
 *                     иначе                          →  0
 *
 *   accuracyBonus  = accuracy ≥ 80%  →  +20% от baseXp
 *                    accuracy ≥ 60%  →  +10% от baseXp
 *                    иначе           →  0
 *
 *   diffMultiplier:  EASY=1.0 / MEDIUM=1.2 / HARD=1.5 / EXPERT=2.0
 *
 *   totalXp = round((baseXp + speedBonus + accuracyBonus) × diffMultiplier)
 *
 * ── Пороги уровней ────────────────────────────────────────────────────────
 *
 *   Уровни 1–50.  xpForLevel(n) = 100 × (n-1) × n / 2
 *   Примеры:  1→0  2→100  3→300  5→1000  10→4500  20→19000
 *
 * ── Ранги по уровням ──────────────────────────────────────────────────────
 *
 *   BEGINNER    1–4
 *   EXPLORER    5–9
 *   SCHOLAR     10–19
 *   MASTER      20–34
 *   GRANDMASTER 35+
 */
object LevelSystem {

    const val MAX_LEVEL = 50

    // ── Пороги ────────────────────────────────────────────────────────────

    /** Суммарный XP, нужный чтобы достичь уровня [level] (1-indexed). */
    fun totalXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        val n = (level - 1).coerceAtMost(MAX_LEVEL - 1)
        return 100 * n * (n + 1) / 2
    }

    /** Текущий уровень по суммарному накопленному XP. */
    fun levelFromXp(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        var level = 1
        while (level < MAX_LEVEL && totalXpForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }

    /** XP внутри текущего уровня (от 0 до xpNeededForCurrentLevel). */
    fun xpInCurrentLevel(totalXp: Int): Int {
        val level = levelFromXp(totalXp)
        return totalXp - totalXpForLevel(level)
    }

    /** Сколько XP надо пройти, чтобы выйти на следующий уровень. */
    fun xpNeededForCurrentLevel(totalXp: Int): Int {
        val level = levelFromXp(totalXp)
        if (level >= MAX_LEVEL) return 1          // уже макс
        return totalXpForLevel(level + 1) - totalXpForLevel(level)
    }

    /** Прогресс 0.0–1.0 внутри текущего уровня. */
    fun levelProgress(totalXp: Int): Float {
        val needed = xpNeededForCurrentLevel(totalXp)
        if (needed <= 0) return 1f
        return (xpInCurrentLevel(totalXp).toFloat() / needed).coerceIn(0f, 1f)
    }

    // ── Ранг ──────────────────────────────────────────────────────────────

    fun rankForLevel(level: Int): UserRank = when {
        level >= 35 -> UserRank.GRANDMASTER
        level >= 20 -> UserRank.MASTER
        level >= 10 -> UserRank.SCHOLAR
        level >= 5  -> UserRank.EXPLORER
        else        -> UserRank.BEGINNER
    }

    // ── Расчёт XP за одно прохождение ────────────────────────────────────

    data class QuizXpResult(
        /** Базовые XP = сумма points за правильные ответы */
        val baseXp: Int,
        /** Бонус за скорость ответов */
        val speedBonusXp: Int,
        /** Бонус за высокую точность */
        val accuracyBonusXp: Int,
        /** Множитель сложности (строка для отображения, напр. "×1.5") */
        val difficultyMultiplierLabel: String,
        /** Итоговый XP, который добавляется к totalPoints пользователя */
        val totalXp: Int,
        /** true, если после этого прохождения пользователь перешёл на новый уровень */
        val leveledUp: Boolean = false,
        val newLevel: Int = 1
    )

    /**
     * @param answers    ответы пользователя с timeSpent
     * @param questions  вопросы викторины (нужны points и timeLimit)
     * @param difficulty сложность викторины
     * @param xpBefore   суммарный XP пользователя ДО этого прохождения
     */
    fun calculateQuizXp(
        answers: List<UserAnswer>,
        questions: List<Question>,
        difficulty: QuizDifficulty,
        xpBefore: Int
    ): QuizXpResult {

        var baseXp     = 0
        var speedBonus = 0

        answers.forEachIndexed { i, answer ->
            if (!answer.isCorrect) return@forEachIndexed
            val q      = questions.getOrNull(i) ?: return@forEachIndexed
            val pts    = q.points.coerceAtLeast(1)
            val tLimit = q.timeLimit.coerceAtLeast(1)

            baseXp += pts
            speedBonus += when {
                answer.timeSpent <= tLimit * 0.33 -> (pts * 0.5).toInt()
                answer.timeSpent <= tLimit * 0.66 -> (pts * 0.2).toInt()
                else                              -> 0
            }
        }

        val totalAnswers = questions.size
        val correct      = answers.count { it.isCorrect }
        val accuracy     = if (totalAnswers > 0) correct.toFloat() / totalAnswers else 0f

        val accuracyBonus = when {
            accuracy >= 0.80f -> (baseXp * 0.20).toInt()
            accuracy >= 0.60f -> (baseXp * 0.10).toInt()
            else              -> 0
        }

        val (diffMult, diffLabel) = when (difficulty) {
            QuizDifficulty.EASY   -> 1.0f to "×1.0"
            QuizDifficulty.MEDIUM -> 1.2f to "×1.2"
            QuizDifficulty.HARD   -> 1.5f to "×1.5"
            QuizDifficulty.EXPERT -> 2.0f to "×2.0"
        }

        val totalXp    = ((baseXp + speedBonus + accuracyBonus) * diffMult).toInt()
        val levelAfter = levelFromXp(xpBefore + totalXp)
        val levelBefore = levelFromXp(xpBefore)

        return QuizXpResult(
            baseXp                   = baseXp,
            speedBonusXp             = speedBonus,
            accuracyBonusXp          = accuracyBonus,
            difficultyMultiplierLabel = diffLabel,
            totalXp                  = totalXp,
            leveledUp                = levelAfter > levelBefore,
            newLevel                 = levelAfter
        )
    }
}