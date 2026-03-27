package com.example.brainracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeStartScreen(
    challengeId: String,
    navController: NavController
) {
    val challengeRepo = remember { ChallengeRepositoryImpl() }
    val quizRepo = remember { QuizRepositoryImpl() }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var challenge by remember { mutableStateOf<Challenge?>(null) }
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(challengeId) {
        isLoading = true
        error = null
        when (val r = challengeRepo.getChallenge(challengeId)) {
            is Result.Success -> {
                val ch = r.data
                challenge = ch
                when (val qr = quizRepo.getQuiz(ch.quizId)) {
                    is Result.Success -> quiz = qr.data
                    is Result.Error -> error = "Не удалось загрузить викторину"
                }
            }
            is Result.Error -> error = "Вызов не найден"
        }
        isLoading = false
    }

    fun navigateHomeRoot() {
        if (currentUserId.isBlank()) {
            navController.popBackStack()
            return
        }
        navController.navigate("home/$currentUserId") {
            popUpTo("home/$currentUserId") { inclusive = true }
            launchSingleTop = true
        }
    }

    /** Как с главной: интро дуэли в QuizPlay; стек подчищен до home — «Отмена» на интро ведёт на главную. */
    fun navigateToPlayLikeFromHome(qid: String, cid: String) {
        val dest = "quiz_play/$qid?challengeId=$cid&fromNotifFlow=true"
        if (currentUserId.isBlank()) {
            navController.navigate(dest)
            return
        }
        navController.navigate(dest) {
            popUpTo("home/$currentUserId") { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Вызов",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    TextButton(onClick = { navigateHomeRoot() }) {
                        Text("Главная", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            challenge == null || quiz == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val ch = challenge!!
                val qz = quiz!!
                val isParticipant =
                    currentUserId == ch.challengerUserId || currentUserId == ch.challengedUserId
                if (!isParticipant) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Этот вызов не для вашего аккаунта", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                } else {
                    ChallengeStartContent(
                        modifier = Modifier.padding(padding),
                        challenge = ch,
                        quiz = qz,
                        currentUserId = currentUserId,
                        busy = busy,
                        actionError = actionError,
                        onAccept = {
                            scope.launch {
                                busy = true
                                actionError = null
                                when (val r = challengeRepo.acceptChallenge(challengeId)) {
                                    is Result.Success -> navigateToPlayLikeFromHome(qz.id, ch.id)
                                    is Result.Error -> actionError = r.exception.message ?: "Не удалось принять вызов"
                                }
                                busy = false
                            }
                        },
                        onDecline = {
                            scope.launch {
                                busy = true
                                actionError = null
                                when (val r = challengeRepo.declineChallenge(challengeId)) {
                                    is Result.Success -> navController.popBackStack()
                                    is Result.Error -> actionError = r.exception.message ?: "Ошибка"
                                }
                                busy = false
                            }
                        },
                        onStartQuiz = { navigateToPlayLikeFromHome(qz.id, ch.id) },
                        onGoToChallenges = {
                            if (currentUserId.isNotBlank()) {
                                navController.navigate("challenges/$currentUserId")
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeStartContent(
    modifier: Modifier = Modifier,
    challenge: Challenge,
    quiz: Quiz,
    currentUserId: String,
    busy: Boolean,
    actionError: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onStartQuiz: () -> Unit,
    onGoToChallenges: () -> Unit
) {
    val isChallenger = challenge.challengerUserId == currentUserId
    val opponentName =
        if (isChallenger) challenge.challengedNickname else challenge.challengerNickname

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Sports,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )
        Text(
            quiz.title.ifBlank { challenge.quizTitle },
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Соперник: $opponentName",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MetaRow("Описание", quiz.description.ifBlank { "—" })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                MetaRow("Категория", quiz.categoryId.ifBlank { "—" })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                MetaRow("Сложность", quiz.difficulty.labelRu())
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                MetaRow(
                    label = "Время прохождения",
                    value = formatDurationSeconds(quiz.estimatedDurationSeconds()),
                    valueIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                MetaRow("Вопросов", "${quiz.questionCount}")
            }
        }

        StatusBlock(challenge, currentUserId)

        if (!actionError.isNullOrBlank()) {
            Text(
                actionError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        when (challenge.status) {
            ChallengeStatus.PENDING -> when {
                !isChallenger -> {
                    Text(
                        "Примите вызов, чтобы перейти к викторине.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onAccept,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Принять и начать", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Отклонить", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Text(
                        "Ожидайте, пока соперник примет вызов.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onGoToChallenges,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("К списку вызовов", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            ChallengeStatus.ACCEPTED -> when {
                challenge.canPlay(currentUserId) && !challenge.hasUserResult(currentUserId) -> {
                    Button(
                        onClick = onStartQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Начать викторину", fontWeight = FontWeight.SemiBold)
                    }
                }
                challenge.hasUserResult(currentUserId) -> {
                    Text(
                        "Вы уже прошли эту викторину в рамках вызова.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onGoToChallenges,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("К вызовам", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Text(
                        "Вызов недоступен (срок или статус).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            ChallengeStatus.COMPLETED -> {
                CompletedSummary(challenge, currentUserId)
                OutlinedButton(
                    onClick = onGoToChallenges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("К списку вызовов", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                Text(
                    "Этот вызов больше не активен.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onGoToChallenges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("К вызовам", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
    valueIcon: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            valueIcon?.invoke()
            Text(
                value,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StatusBlock(challenge: Challenge, currentUserId: String) {
    val label = when (challenge.status) {
        ChallengeStatus.PENDING -> "Ожидает ответа"
        ChallengeStatus.ACCEPTED -> "Активен"
        ChallengeStatus.COMPLETED -> "Завершён"
        ChallengeStatus.DECLINED -> "Отклонён"
        ChallengeStatus.EXPIRED -> "Истёк"
        ChallengeStatus.CANCELLED -> "Отменён"
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outline)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompletedSummary(challenge: Challenge, currentUserId: String) {
    val my = if (challenge.challengerUserId == currentUserId) {
        challenge.challengerResult
    } else {
        challenge.challengedResult
    }
    val opp = if (challenge.challengerUserId == currentUserId) {
        challenge.challengedResult
    } else {
        challenge.challengerResult
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Итог дуэли", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            if (challenge.isDraw || challenge.winnerId == "draw") {
                Text("Ничья", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            } else if (challenge.winnerId == currentUserId) {
                Text("Победа", color = LocalBrainRacerExtendedColors.current.detailGreen, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            } else if (challenge.winnerId != null) {
                Text("Поражение", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }
            val myScore = my?.score
            val oppScore = opp?.score
            if (myScore != null || oppScore != null) {
                Text(
                    "Ваш счёт: ${myScore ?: "—"} · Соперник: ${oppScore ?: "—"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun QuizDifficulty.labelRu(): String = when (this) {
    QuizDifficulty.EASY -> "Лёгкий"
    QuizDifficulty.MEDIUM -> "Средний"
    QuizDifficulty.HARD -> "Сложный"
    QuizDifficulty.EXPERT -> "Эксперт"
}

/**
 * В Firestore часто лежит totalTime = 0; после десериализации он перезаписывает значение из init.
 * Для UI берём осмысленную оценку по вопросам и лимиту на вопрос.
 */
private fun Quiz.estimatedDurationSeconds(): Int {
    if (totalTime > 0) return totalTime
    val q = questionCount
    val t = timePerQuestion.coerceAtLeast(1)
    return (q * t).coerceAtLeast(0)
}

private fun formatDurationSeconds(totalSec: Int): String {
    if (totalSec <= 0) return "—"
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) {
        String.format(Locale.getDefault(), "%d мин %d с", m, s)
    } else {
        String.format(Locale.getDefault(), "%d с", s)
    }
}
