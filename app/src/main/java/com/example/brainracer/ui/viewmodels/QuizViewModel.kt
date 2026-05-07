package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.local.QuizOfflineCache
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.data.utils.fold
import com.example.brainracer.data.utils.isNetworkLikelyAvailable
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.UserAnswer
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.ui.utils.QuizNonScoringReason
import com.example.brainracer.ui.utils.QuizUIState
import com.example.brainracer.ui.utils.XpBreakdown
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUIState())
    val uiState: StateFlow<QuizUIState> = _uiState.asStateFlow()

    private val userRepository = UserRepositoryImpl()
    private val quizRepository = QuizRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private var currentQuiz: Quiz? = null
    private val userAnswers = mutableListOf<UserAnswer>()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private var totalTimeSpent = 0
    private var questionStartTime = 0L
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var finalizationInFlight: Boolean = false
    private val finalizedSessionIds = mutableSetOf<String>()

    private var networkAvailableAtStart = true
    private var forceNonScoringSession = false

    private fun sessionCountsTowardProgress(): Boolean {
        if (forceNonScoringSession) return false
        val hasUser = currentUserId != null
        if (!hasUser) return false
        if (!networkAvailableAtStart) return false
        return true
    }

    private fun nonScoringReasonForSession(): QuizNonScoringReason? {
        if (forceNonScoringSession) return QuizNonScoringReason.PRACTICE_REPLAY
        if (sessionCountsTowardProgress()) return null
        if (currentUserId == null) return QuizNonScoringReason.NOT_SIGNED_IN
        if (!networkAvailableAtStart) return QuizNonScoringReason.OFFLINE
        return QuizNonScoringReason.NOT_SIGNED_IN
    }

    // ── Загрузка викторины ────────────────────────────────────────────────

    fun loadQuiz(
        quizId: String,
        challengeId: String? = null,
        networkAvailableAtStart: Boolean = true,
        forceNonScoring: Boolean = false
    ) {
        val prior = _uiState.value
        val challengeMatches = prior.challengeId == challengeId ||
                (prior.challengeId.isNullOrBlank() && challengeId.isNullOrBlank())
        if (currentQuiz?.id == quizId && challengeMatches &&
            prior.sessionNetworkAvailable == networkAvailableAtStart &&
            forceNonScoringSession == forceNonScoring &&
            (prior.showResults || prior.isQuizCompleted || prior.question.isNotEmpty())
        ) {
            return
        }

        forceNonScoringSession = forceNonScoring
        this.networkAvailableAtStart = networkAvailableAtStart

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            quizRepository.getQuiz(quizId).fold(
                onSuccess = { quiz ->
                    QuizOfflineCache.save(quiz)
                    onQuizLoaded(quiz, challengeId, networkAvailableAtStart)
                },
                onFailure = { error ->
                    val cached = QuizOfflineCache.load(quizId)
                    if (cached != null) {
                        onQuizLoaded(cached, challengeId, networkAvailableAtStart)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Ошибка загрузки: ${error.message}")
                        }
                    }
                }
            )
        }
    }

    private fun onQuizLoaded(
        quiz: Quiz,
        challengeId: String?,
        networkAvailableAtStart: Boolean
    ) {
        this.networkAvailableAtStart = networkAvailableAtStart

        currentQuiz = quiz
        currentSessionId = UUID.randomUUID().toString()
        finalizationInFlight = false
        userAnswers.clear()
        totalTimeSpent = 0

        val counting = sessionCountsTowardProgress()
        val reason = nonScoringReasonForSession()

        if (quiz.questions.isNotEmpty()) {
            questionStartTime = System.currentTimeMillis()
            val first = quiz.questions[0]
            _uiState.value = QuizUIState(
                isLoading = false,
                question = first.questionText,
                options = first.options,
                totalQuestions = quiz.questions.size,
                currentQuestionTimeLimit = first.timeLimit.coerceAtLeast(5),
                attachedImageUrl = first.imageUrl ?: first.gifUrl,
                challengeId = challengeId,
                quizTitle = quiz.title,
                sessionNetworkAvailable = networkAvailableAtStart,
                isNonScoringSession = !counting,
                nonScoringReason = reason
            )
        } else {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "В викторине нет вопросов")
            }
        }
    }

    // ── Выбор ответа ──────────────────────────────────────────────────────

    fun selectAnswer(answerIndex: Int) {
        if (_uiState.value.isAnswerSubmitted) return
        _uiState.update { it.copy(selectedAnswerIndex = answerIndex) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val quiz = currentQuiz ?: return
        val selected = state.selectedAnswerIndex ?: run {
            _uiState.update { it.copy(errorMessage = "Выберите ответ") }
            return
        }
        if (state.isAnswerSubmitted) return

        recordAnswer(
            quiz = quiz,
            state = state,
            selectedIdx = selected,
            usedFullTime = false
        )
    }

    fun timeoutQuestion() {
        val state = _uiState.value
        val quiz = currentQuiz ?: return
        if (state.isAnswerSubmitted) return

        recordAnswer(
            quiz = quiz,
            state = state,
            selectedIdx = -1,
            usedFullTime = true
        )
    }

    private fun recordAnswer(
        quiz: Quiz,
        state: QuizUIState,
        selectedIdx: Int,
        usedFullTime: Boolean
    ) {
        val qIndex = state.currentQuestionIndex
        val question = quiz.questions.getOrNull(qIndex) ?: return

        val timeSpent = if (usedFullTime) {
            question.timeLimit
        } else {
            ((System.currentTimeMillis() - questionStartTime) / 1000).toInt()
        }
        totalTimeSpent += timeSpent

        val isCorrect = selectedIdx != -1 && selectedIdx == question.correctAnswerIndex

        userAnswers.add(
            UserAnswer(
                questionId = question.id,
                selectedAnswerIndex = selectedIdx,
                isCorrect = isCorrect,
                timeSpent = timeSpent
            )
        )

        _uiState.update {
            it.copy(
                isAnswerSubmitted = true,
                isAnswerCorrect = isCorrect,
                score = it.score + if (isCorrect) question.points else 0,
                correctAnswers = it.correctAnswers + if (isCorrect) 1 else 0,
                incorrectAnswers = it.incorrectAnswers + if (!isCorrect) 1 else 0
            )
        }

        if (qIndex >= quiz.questions.size - 1) {
            saveQuizResults()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswerSubmitted) return

        val quiz = currentQuiz ?: return
        val nextIdx = state.currentQuestionIndex + 1

        if (nextIdx >= quiz.questions.size) {
            _uiState.update { it.copy(isQuizCompleted = true, showResults = true) }
            return
        }

        val next = quiz.questions[nextIdx]
        questionStartTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                currentQuestionIndex = nextIdx,
                selectedAnswerIndex = null,
                isAnswerSubmitted = false,
                isAnswerCorrect = null,
                errorMessage = null,
                question = next.questionText,
                options = next.options,
                currentQuestionTimeLimit = next.timeLimit.coerceAtLeast(5),
                attachedImageUrl = next.imageUrl ?: next.gifUrl
            )
        }
    }

    private fun saveQuizResults() {
        val sessionId = currentSessionId
        if (sessionId in finalizedSessionIds || finalizationInFlight) return
        finalizationInFlight = true
        viewModelScope.launch {
            val quiz = currentQuiz
            if (quiz == null) {
                finalizationInFlight = false
                return@launch
            }
            val userId = currentUserId
            val xpBefore = userId?.let { uid ->
                when (val r = userRepository.getUser(uid)) {
                    is Result.Success -> r.data.stats.totalPoints
                    is Result.Error -> 0
                }
            } ?: 0

            val persist = sessionCountsTowardProgress()
            try {
                finishWithXp(
                    quiz = quiz,
                    xpBefore = xpBefore,
                    persistResults = persist,
                    userId = userId,
                    sessionId = sessionId
                )
                finalizedSessionIds += sessionId
            } finally {
                finalizationInFlight = false
            }
        }
    }

    private suspend fun finishWithXp(
        quiz: Quiz,
        xpBefore: Int,
        persistResults: Boolean,
        userId: String? = null,
        sessionId: String
    ) {
        val totalQ = quiz.questions.size
        val correct = userAnswers.count { it.isCorrect }
        val accuracy = if (totalQ > 0) correct.toDouble() / totalQ * 100 else 0.0

        val xpResult = LevelSystem.calculateQuizXp(
            answers = userAnswers,
            questions = quiz.questions,
            difficulty = quiz.difficulty,
            xpBefore = xpBefore
        )

        val breakdown = XpBreakdown(
            baseXp = xpResult.baseXp,
            speedBonusXp = xpResult.speedBonusXp,
            accuracyBonusXp = xpResult.accuracyBonusXp,
            difficultyLabel = "${xpResult.difficultyMultiplierLabel} (${quiz.difficulty.name})",
            totalXp = xpResult.totalXp
        )

        val cid = _uiState.value.challengeId
        val isChallenge = !cid.isNullOrBlank()

        if (!persistResults || userId == null) {
            _uiState.update {
                it.copy(
                    showResults = true,
                    isQuizCompleted = true,
                    accuracy = accuracy,
                    xpEarned = 0,
                    xpBreakdown = breakdown,
                    leveledUp = false,
                    newLevel = LevelSystem.levelFromXp(xpBefore),
                    newLevelProgress = LevelSystem.levelProgress(xpBefore),
                    reviewQuestions = quiz.questions,
                    reviewAnswers = userAnswers.toList(),
                    isNonScoringSession = true,
                    nonScoringReason = it.nonScoringReason,
                    duelXpDeferred = isChallenge
                )
            }
            if (userId != null) {
                quizRepository.recordUserQuizSessionFinished(
                    userId,
                    quiz.id,
                    sessionId,
                    savedResultToQuizResults = false
                )
            }
            return
        }

        val avgTime = if (totalQ > 0) totalTimeSpent.toDouble() / totalQ else 0.0
        val nickname = when (val r = userRepository.getUser(userId)) {
            is Result.Success -> r.data.nickname
            is Result.Error -> "Игрок"
        }

        val quizResult = com.example.brainracer.domain.entities.ChallengeResult(
            quizId = quiz.id,
            userId = userId,
            userNickname = nickname,
            score = _uiState.value.score,
            totalQuestions = totalQ,
            correctAnswers = correct,
            incorrectAnswers = totalQ - correct,
            timeSpent = totalTimeSpent,
            averageTimePerQuestion = avgTime,
            answers = userAnswers.toList(),
            pointsEarned = 0,
            challengeId = cid
        )

        when (val rec = quizRepository.recordQuizResult(quizResult, xpResult.profileTotalXp)) {
            is Result.Error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "Не удалось завершить сохранение: ${rec.exception.message}"
                    )
                }
            is Result.Success -> {
                val soloAwarded = rec.data
                var xpAfterDisplay = xpBefore
                if (!isChallenge) {
                    xpAfterDisplay = xpBefore + soloAwarded
                } else {
                    when (val refreshed = userRepository.getUser(userId)) {
                        is Result.Success -> xpAfterDisplay = refreshed.data.stats.totalPoints
                        is Result.Error -> Unit
                    }
                }

                val showLevelUp = LevelSystem.levelFromXp(xpAfterDisplay) > LevelSystem.levelFromXp(xpBefore)

                _uiState.update {
                    it.copy(
                        showResults = true,
                        isQuizCompleted = true,
                        accuracy = accuracy,
                        xpEarned = soloAwarded,
                        xpBreakdown = breakdown,
                        leveledUp = showLevelUp,
                        newLevel = LevelSystem.levelFromXp(xpAfterDisplay),
                        newLevelProgress = LevelSystem.levelProgress(xpAfterDisplay),
                        reviewQuestions = quiz.questions,
                        reviewAnswers = userAnswers.toList(),
                        isNonScoringSession = false,
                        nonScoringReason = null,
                        duelXpDeferred = isChallenge && soloAwarded == 0
                    )
                }
                quizRepository.recordUserQuizSessionFinished(
                    userId,
                    quiz.id,
                    sessionId,
                    savedResultToQuizResults = true
                )
                ProfileAfterQuizRefresh.notify(userId)
            }
        }
    }

    fun restartQuiz() {
        if (!_uiState.value.challengeId.isNullOrBlank()) return
        val id = currentQuiz?.id ?: return
        val cid = _uiState.value.challengeId
        val ctx = QuizOfflineCache.applicationContextOrNull()
        val online = ctx != null && isNetworkLikelyAvailable(ctx)
        userAnswers.clear()
        totalTimeSpent = 0
        _uiState.value = QuizUIState()
        loadQuiz(id, cid, online, forceNonScoringSession)
    }

    fun closeResults() {
        _uiState.update { it.copy(showResults = false) }
    }

    fun attachImage(imageUrl: String) {
        _uiState.update { it.copy(attachedImageUrl = imageUrl) }
    }
}
