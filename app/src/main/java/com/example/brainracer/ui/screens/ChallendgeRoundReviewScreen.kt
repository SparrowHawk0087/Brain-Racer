package com.example.brainracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.brainracer.domain.entities.Question
import com.example.brainracer.domain.entities.UserAnswer
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeRoundReviewScreen(
    challengeId: String,
    navController: NavController
) {
    val challengeRepo = remember { ChallengeRepositoryImpl() }
    val quizRepo      = remember { QuizRepositoryImpl() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var challenge  by remember { mutableStateOf<Challenge?>(null) }
    var questions  by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(challengeId) {
        when (val r = challengeRepo.getChallenge(challengeId)) {
            is Result.Success -> {
                val ch = r.data
                challenge = ch
                when (val qr = quizRepo.getQuiz(ch.quizId)) {
                    is Result.Success -> questions = qr.data.questions
                    is Result.Error   -> error = "Не удалось загрузить вопросы"
                }
            }
            is Result.Error -> error = "Вызов не найден"
        }
        isLoading = false
    }

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text("Разбор раунда", color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp))
            }
            challenge != null -> {
                val ch = challenge!!
                val isChallenger    = ch.challengerUserId == currentUserId
                val myResult        = if (isChallenger) ch.challengerResult else ch.challengedResult
                val opponentResult  = if (isChallenger) ch.challengedResult else ch.challengerResult
                val opponentName    = if (isChallenger) ch.challengedNickname else ch.challengerNickname

                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Итоговый счёт
                    item {
                        ScoreHeader(
                            challenge     = ch,
                            opponentName  = opponentName,
                            currentUserId = currentUserId
                        )
                    }

                    // Вопросы
                    itemsIndexed(questions) { i, question ->
                        val myAnswer = myResult?.answers?.find { it.questionId == question.id }
                            ?: myResult?.answers?.getOrNull(i)
                        val opponentAnswer = opponentResult?.answers?.find { it.questionId == question.id }
                            ?: opponentResult?.answers?.getOrNull(i)
                        QuestionComparisonCard(
                            index          = i,
                            question       = question,
                            myAnswer       = myAnswer,
                            opponentAnswer = opponentAnswer,
                            myLabel        = "Вы",
                            opponentLabel  = opponentName.split(" ").first()
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Итоговый счёт ─────────────────────────────────────────────────────────────

@Composable
private fun ScoreHeader(
    challenge: Challenge,
    opponentName: String,
    currentUserId: String
) {
    val myResult = if (challenge.challengerUserId == currentUserId) {
        challenge.challengerResult
    } else {
        challenge.challengedResult
    }
    val opponentResult = if (challenge.challengerUserId == currentUserId) {
        challenge.challengedResult
    } else {
        challenge.challengerResult
    }

    val finished = challenge.status == ChallengeStatus.COMPLETED
    val isDraw   = challenge.isDraw || challenge.winnerId == "draw"
    val isWin    = finished && !isDraw && challenge.winnerId == currentUserId

    val (resultLabel, resultColor) = when {
        !finished -> when {
            myResult == null && opponentResult == null ->
                "Вызов принят" to MaterialTheme.colorScheme.onSurfaceVariant
            myResult != null && opponentResult == null ->
                "Ожидаем соперника" to LocalBrainRacerExtendedColors.current.statusOrange
            myResult == null && opponentResult != null ->
                "Ваш ход" to MaterialTheme.colorScheme.primary
            else -> "В процессе" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        isDraw -> "Ничья" to LocalBrainRacerExtendedColors.current.statusOrange
        isWin  -> "Победа" to LocalBrainRacerExtendedColors.current.detailGreen
        else   -> "Поражение" to MaterialTheme.colorScheme.error
    }

    val myScoreDisplay     = myResult?.score
    val oppScoreDisplay    = opponentResult?.score
    val hideMyScore        = !finished && myResult == null
    val hideOpponentScore  = !finished && opponentResult == null

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Результат
            Surface(shape = RoundedCornerShape(10.dp), color = resultColor.copy(.15f)) {
                Text(resultLabel,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = resultColor)
            }

            // Счёт
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                PlayerScoreColumn(
                    name      = "Вы",
                    score     = myScoreDisplay,
                    isWinner  = isWin,
                    hideScore = hideMyScore
                )
                Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val oppWins = finished && !isDraw && !isWin
                PlayerScoreColumn(
                    name      = opponentName.split(" ").first(),
                    score     = oppScoreDisplay,
                    isWinner  = oppWins,
                    hideScore = hideOpponentScore
                )
            }
        }
    }
}

@Composable
private fun PlayerScoreColumn(
    name: String,
    score: Int?,
    isWinner: Boolean,
    hideScore: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (hideScore) "—" else "${score ?: 0}",
            fontWeight = FontWeight.Bold,
            fontSize   = 32.sp,
            color      = when {
                hideScore -> MaterialTheme.colorScheme.onSurfaceVariant
                isWinner  -> LocalBrainRacerExtendedColors.current.detailGreen
                else      -> MaterialTheme.colorScheme.onSurface
            }
        )
        if (isWinner) {
            Icon(Icons.Default.EmojiEvents, null, tint = LocalBrainRacerExtendedColors.current.difficultyExpert, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Карточка сравнения вопроса ────────────────────────────────────────────────

@Composable
private fun QuestionComparisonCard(
    index: Int,
    question: Question,
    myAnswer: UserAnswer?,
    opponentAnswer: UserAnswer?,
    myLabel: String,
    opponentLabel: String
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Номер + текст вопроса
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(.15f)) {
                    Text("${index + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(question.questionText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }

            // Варианты ответов
            val letters = listOf("A", "B", "C", "D", "E")
            question.options.forEachIndexed { optIdx, optText ->
                val isCorrect      = question.correctAnswerIndex == optIdx
                val iMyChoice      = myAnswer?.selectedAnswerIndex == optIdx
                val isOpponentChoice = opponentAnswer?.selectedAnswerIndex == optIdx

                // Определяем цвет строки
                val rowColor = when {
                    isCorrect -> LocalBrainRacerExtendedColors.current.detailGreen
                    iMyChoice || isOpponentChoice -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                val rowBg = when {
                    isCorrect -> LocalBrainRacerExtendedColors.current.detailGreen.copy(.08f)
                    iMyChoice || isOpponentChoice -> MaterialTheme.colorScheme.error.copy(.08f)
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(rowBg)
                        .border(1.dp, rowColor.copy(.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Буква
                    Text(letters.getOrElse(optIdx) { "$optIdx" },
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = rowColor,
                        modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)

                    // Текст варианта
                    Text(optText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))

                    // Иконки: кто выбрал этот вариант
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (iMyChoice) {
                            AnswerTag(myLabel, if (myAnswer?.isCorrect == true) LocalBrainRacerExtendedColors.current.detailGreen else MaterialTheme.colorScheme.error)
                        }
                        if (isOpponentChoice) {
                            AnswerTag(opponentLabel, if (opponentAnswer?.isCorrect == true) LocalBrainRacerExtendedColors.current.detailGreen else MaterialTheme.colorScheme.error)
                        }
                        if (isCorrect) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = LocalBrainRacerExtendedColors.current.detailGreen, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            // Время ответа обоих игроков
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(.5f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeChip(myLabel, myAnswer?.timeSpent, myAnswer?.isCorrect)
                TimeChip(opponentLabel, opponentAnswer?.timeSpent, opponentAnswer?.isCorrect)
            }

            // Объяснение
            if (!question.explanation.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Lightbulb, null,
                        tint = LocalBrainRacerExtendedColors.current.difficultyExpert, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text(question.explanation!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun AnswerTag(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun TimeChip(label: String, timeSpent: Int?, isCorrect: Boolean?) {
    val color = when (isCorrect) {
        true  -> LocalBrainRacerExtendedColors.current.detailGreen
        false -> MaterialTheme.colorScheme.error
        null  -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (timeSpent != null && timeSpent >= 0) "${timeSpent}с" else "—",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color
        )
    }
}


