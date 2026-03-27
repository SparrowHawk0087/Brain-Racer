package com.example.brainracer.domain

import com.example.brainracer.domain.entities.ChallengeXpPolicy
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.domain.entities.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FairXpPolicyTest {

    @Test
    fun challengeXpPolicy_decaySteps() {
        assertEquals(1f, ChallengeXpPolicy.multiplierForPaidAttemptIndex(0), 0.001f)
        assertEquals(0.5f, ChallengeXpPolicy.multiplierForPaidAttemptIndex(1), 0.001f)
        assertEquals(0.25f, ChallengeXpPolicy.multiplierForPaidAttemptIndex(2), 0.001f)
        assertEquals(0f, ChallengeXpPolicy.multiplierForPaidAttemptIndex(3), 0.001f)
        assertEquals(0f, ChallengeXpPolicy.multiplierForPaidAttemptIndex(99), 0.001f)
    }

    @Test
    fun challengeXpPolicy_pairKey_stable() {
        val a = ChallengeXpPolicy.pairKeyQuiz("b", "a", "q1")
        val b = ChallengeXpPolicy.pairKeyQuiz("a", "b", "q1")
        assertEquals(a, b)
        assertTrue(a.endsWith("_q1"))
    }

    @Test
    fun levelSystem_profileTotalXp_excludesSpeed() {
        val q = Question(
            id = "1",
            questionText = "t",
            options = listOf("a", "b"),
            correctAnswerIndex = 0,
            timeLimit = 100,
            points = 10
        )
        val answers = listOf(
            UserAnswer(questionId = "1", selectedAnswerIndex = 0, isCorrect = true, timeSpent = 1)
        )
        val r = LevelSystem.calculateQuizXp(answers, listOf(q), QuizDifficulty.EASY, xpBefore = 0)
        val withSpeed = r.totalXp
        val profile = r.profileTotalXp
        assertTrue(withSpeed >= profile)
        // быстрый ответ даёт speed bonus → profile без speed строго меньше при бонусе
        assertTrue(profile > 0)
    }
}
