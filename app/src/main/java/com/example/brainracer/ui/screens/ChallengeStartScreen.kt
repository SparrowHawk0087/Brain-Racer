package com.example.brainracer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.storage.StorageConfig
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.components.pressScale
import com.example.brainracer.ui.theme.BrainRacerExtendedColors
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val ext = LocalBrainRacerExtendedColors.current
    Scaffold(
        containerColor = ext.detailBackground,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ext.detailSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.brainracer.R.drawable.arrow_back_btn),
                                contentDescription = "Назад",
                                tint = ext.detailTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { navigateHomeRoot() }) {
                        Text("Главная", color = ext.detailAccentPurple, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(ext.detailBackground)
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
                CircularProgressIndicator(color = ext.detailAccentPurple)
            }
            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
            challenge == null || quiz == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
    val ext = LocalBrainRacerExtendedColors.current
    val isChallenger = challenge.challengerUserId == currentUserId
    val opponentName =
        if (isChallenger) challenge.challengedNickname else challenge.challengerNickname
    val coverUrl = quiz.imageUrl.trim().takeIf { it.isNotEmpty() }
        ?.let { StorageConfig.resolvePublicUrlForCoil(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            // Дополнительный зазор под TopAppBar (Scaffold уже даёт inset под тулбар)
            .padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Обложка викторины — как на экране описания
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ext.detailAccentPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.example.brainracer.R.drawable.cognition),
                contentDescription = null,
                tint = ext.detailAccentPurple,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            "Вызов",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            color = ext.detailTextPrimary
        )
        Text(
            quiz.title.ifBlank { challenge.quizTitle },
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ext.detailSurface.copy(alpha = 0.85f),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = ext.detailGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Соперник: $opponentName",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChallengeBadge(
                label = quiz.difficulty.labelRu(),
                color = quizDifficultyAccent(quiz.difficulty, ext)
            )
            ChallengeBadge(
                label = "${quiz.questionCount} вопросов",
                color = ext.detailBlue
            )
            ChallengeBadge(
                label = "${quiz.timePerQuestion}с/вопрос",
                color = ext.detailOrange
            )
            val dur = formatDurationSeconds(quiz.estimatedDurationSeconds())
            if (dur != "—") {
                ChallengeBadge(
                    label = dur,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ext.detailSurface),
            elevation = CardDefaults.cardElevation(0.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "О викторине",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = ext.detailTextPrimary
                )
                MetaRow("Описание", quiz.description.ifBlank { "—" }, ext)
                HorizontalDivider(color = ext.detailSurfaceAlt, thickness = 1.dp)
                MetaRow("Категория", quiz.categoryId.ifBlank { "—" }, ext)
            }
        }

        StatusBlock(challenge, ext)

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
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 19.sp
                    )
                    PrimaryChallengeButton(
                        text = "Принять и начать",
                        ext = ext,
                        enabled = !busy,
                        showProgress = busy,
                        onClick = onAccept
                    )
                    SecondaryOutlineChallengeButton(
                        text = "Отклонить",
                        ext = ext,
                        destructive = true,
                        enabled = !busy,
                        onClick = onDecline
                    )
                }
                else -> {
                    Text(
                        "Ожидайте, пока соперник примет вызов.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 20.sp
                    )
                    SecondaryOutlineChallengeButton(
                        text = "К списку вызовов",
                        ext = ext,
                        destructive = false,
                        onClick = onGoToChallenges
                    )
                }
            }
            ChallengeStatus.ACCEPTED -> when {
                challenge.canPlay(currentUserId) && !challenge.hasUserResult(currentUserId) -> {
                    PrimaryChallengeButton(
                        text = "Начать викторину",
                        ext = ext,
                        onClick = onStartQuiz
                    )
                }
                challenge.hasUserResult(currentUserId) -> {
                    Text(
                        "Вы уже прошли эту викторину в рамках вызова.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 20.sp
                    )
                    SecondaryOutlineChallengeButton(
                        text = "К вызовам",
                        ext = ext,
                        destructive = false,
                        onClick = onGoToChallenges
                    )
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
                CompletedSummary(challenge, currentUserId, ext)
                SecondaryOutlineChallengeButton(
                    text = "К списку вызовов",
                    ext = ext,
                    destructive = false,
                    onClick = onGoToChallenges
                )
            }
            else -> {
                Text(
                    "Этот вызов больше не активен.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                SecondaryOutlineChallengeButton(
                    text = "К вызовам",
                    ext = ext,
                    destructive = false,
                    onClick = onGoToChallenges
                )
            }
        }
    }
}

@Composable
private fun ChallengeBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun quizDifficultyAccent(d: QuizDifficulty, ext: BrainRacerExtendedColors): Color = when (d) {
    QuizDifficulty.EASY   -> ext.detailGreen
    QuizDifficulty.MEDIUM -> ext.detailBlue
    QuizDifficulty.HARD   -> ext.detailOrange
    QuizDifficulty.EXPERT -> ext.difficultyExpert
}

@Composable
private fun PrimaryChallengeButton(
    text: String,
    ext: BrainRacerExtendedColors,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !showProgress,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(enabled = enabled && !showProgress)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ext.detailAccentPurple),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SecondaryOutlineChallengeButton(
    text: String,
    ext: BrainRacerExtendedColors,
    destructive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderCol = if (destructive) MaterialTheme.colorScheme.error else ext.detailGreen
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.992f, enabled = enabled)
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (destructive) MaterialTheme.colorScheme.error else ext.detailGreen
        ),
        border = BorderStroke(1.5.dp, borderCol)
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
    ext: BrainRacerExtendedColors,
    valueIcon: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            valueIcon?.invoke()
            Text(
                value,
                fontSize = 15.sp,
                color = ext.detailTextPrimary,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun StatusBlock(challenge: Challenge, ext: BrainRacerExtendedColors) {
    val label = when (challenge.status) {
        ChallengeStatus.PENDING -> "Ожидает ответа"
        ChallengeStatus.ACCEPTED -> "Активен"
        ChallengeStatus.COMPLETED -> "Завершён"
        ChallengeStatus.DECLINED -> "Отклонён"
        ChallengeStatus.EXPIRED -> "Истёк"
        ChallengeStatus.CANCELLED -> "Отменён"
    }
    val (bg, fg) = when (challenge.status) {
        ChallengeStatus.PENDING ->
            ext.statusOrange.copy(alpha = 0.16f) to ext.statusOrange
        ChallengeStatus.ACCEPTED ->
            ext.detailGreen.copy(alpha = 0.14f) to ext.detailGreen
        ChallengeStatus.COMPLETED ->
            ext.detailBlue.copy(alpha = 0.12f) to ext.detailBlue
        else ->
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bg
        ) {
            Text(
                label,
                fontSize = 12.sp,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun CompletedSummary(
    challenge: Challenge,
    currentUserId: String,
    ext: BrainRacerExtendedColors
) {
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ext.detailSurface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Итог дуэли",
                fontWeight = FontWeight.Bold,
                color = ext.detailTextPrimary,
                fontSize = 16.sp
            )
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
