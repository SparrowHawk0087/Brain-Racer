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
import com.google.firebase.auth.FirebaseAuth

// ─── Палитра (та же что в ChallengesScreen) ──────────────────────────────────
private val RBg      = Color(0xFF0F0F1A)
private val RCard    = Color(0xFF1A1A2E)
private val RBorder  = Color(0xFF2A2A3E)
private val RPurple  = Color(0xFF667EEA)
private val RGreen   = Color(0xFF3ECFA3)
private val RRed     = Color(0xFFEA5C7E)
private val ROrange  = Color(0xFFFFA726)
private val RGold    = Color(0xFFFFD700)
private val RTextPri = Color(0xFFFFFFFF)
private val RTextSec = Color(0xFF8B8AAE)

// ══════════════════════════════════════════════════════════════════════════════

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
        containerColor      = RBg,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text("Разбор раунда", color = RTextPri,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(RCard),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                tint = RTextPri, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RBg)
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = RPurple)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(error!!, color = RRed, textAlign = TextAlign.Center,
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
                "Вызов принят" to RTextSec
            myResult != null && opponentResult == null ->
                "Ожидаем соперника" to ROrange
            myResult == null && opponentResult != null ->
                "Ваш ход" to RPurple
            else -> "В процессе" to RTextSec
        }
        isDraw -> "Ничья" to ROrange
        isWin  -> "Победа" to RGreen
        else   -> "Поражение" to RRed
    }

    val myScoreDisplay     = myResult?.score
    val oppScoreDisplay    = opponentResult?.score
    val hideMyScore        = !finished && myResult == null
    val hideOpponentScore  = !finished && opponentResult == null

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = RCard),
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
                Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = RTextSec)
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
        Text(name, fontSize = 13.sp, color = RTextSec)
        Text(
            if (hideScore) "—" else "${score ?: 0}",
            fontWeight = FontWeight.Bold,
            fontSize   = 32.sp,
            color      = when {
                hideScore -> RTextSec
                isWinner  -> RGreen
                else      -> RTextPri
            }
        )
        if (isWinner) {
            Icon(Icons.Default.EmojiEvents, null, tint = RGold, modifier = Modifier.size(18.dp))
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
        colors    = CardDefaults.cardColors(containerColor = RCard),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Номер + текст вопроса
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(6.dp), color = RPurple.copy(.15f)) {
                    Text("${index + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RPurple)
                }
                Text(question.questionText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = RTextPri, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }

            // Варианты ответов
            val letters = listOf("A", "B", "C", "D", "E")
            question.options.forEachIndexed { optIdx, optText ->
                val isCorrect      = question.correctAnswerIndex == optIdx
                val iMyChoice      = myAnswer?.selectedAnswerIndex == optIdx
                val isOpponentChoice = opponentAnswer?.selectedAnswerIndex == optIdx

                // Определяем цвет строки
                val rowColor = when {
                    isCorrect -> RGreen
                    iMyChoice || isOpponentChoice -> RRed
                    else -> RBorder
                }
                val rowBg = when {
                    isCorrect -> RGreen.copy(.08f)
                    iMyChoice || isOpponentChoice -> RRed.copy(.08f)
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
                    Text(optText, fontSize = 13.sp, color = RTextPri,
                        modifier = Modifier.weight(1f))

                    // Иконки: кто выбрал этот вариант
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (iMyChoice) {
                            AnswerTag(myLabel, if (myAnswer?.isCorrect == true) RGreen else RRed)
                        }
                        if (isOpponentChoice) {
                            AnswerTag(opponentLabel, if (opponentAnswer?.isCorrect == true) RGreen else RRed)
                        }
                        if (isCorrect) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = RGreen, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            // Время ответа обоих игроков
            HorizontalDivider(color = RBorder.copy(.5f))
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
                        tint = RGold, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text(question.explanation!!, fontSize = 12.sp, color = RTextSec, lineHeight = 17.sp)
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
        true  -> RGreen
        false -> RRed
        null  -> RTextSec
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = RTextSec)
        Text(
            if (timeSpent != null && timeSpent >= 0) "${timeSpent}с" else "—",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color
        )
    }
}


