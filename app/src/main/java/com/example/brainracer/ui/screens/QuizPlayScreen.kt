package com.example.brainracer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.ui.utils.QuizUIState
import com.example.brainracer.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.delay

// ─── Палитра (тёмная тема, совпадает с HomeScreen) ─────────────────────────
private val QBg        = Color(0xFF0F0F1A)
private val QCard      = Color(0xFF1A1A2E)
private val QBorder    = Color(0xFF2A2A3E)
private val QPurple    = Color(0xFF667EEA)
private val QGreen     = Color(0xFF3ECFA3)
private val QRed       = Color(0xFFEA5C7E)
private val QTextPri   = Color(0xFFFFFFFF)
private val QTextSec   = Color(0xFF8B8AAE)

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН ПРОХОЖДЕНИЯ ВИКТОРИНЫ
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayScreen(
    quizId: String,
    navController: NavController,
    quizViewModel: QuizViewModel = viewModel()
) {
    val uiState by quizViewModel.uiState.collectAsState()

    // Загружаем викторину при старте
    LaunchedEffect(quizId) {
        quizViewModel.loadQuiz(quizId)
    }

    when {
        // ── Загрузка ──────────────────────────────────────────────────────
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(QBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = QPurple)
                    Spacer(Modifier.height(16.dp))
                    Text("Загружаем викторину…", color = QTextSec, fontSize = 14.sp)
                }
            }
        }

        // ── Ошибка ────────────────────────────────────────────────────────
        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize().background(QBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("Ошибка загрузки", color = QRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.errorMessage ?: "", color = QTextSec, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = QPurple)
                    ) {
                        Text("Назад")
                    }
                }
            }
        }

        // ── Результаты ────────────────────────────────────────────────────
        uiState.showResults || uiState.isQuizCompleted -> {
            QuizResultsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onRestart = { quizViewModel.restartQuiz() }
            )
        }

        // ── Вопрос ────────────────────────────────────────────────────────
        uiState.question.isNotEmpty() -> {
            QuizQuestionScreen(
                uiState    = uiState,
                onBack     = { navController.popBackStack() },
                onSelect   = { index -> quizViewModel.selectAnswer(index) },
                onSubmit   = { quizViewModel.submitAnswer() },
                onNext     = { quizViewModel.nextQuestion() }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН ВОПРОСА
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizQuestionScreen(
    uiState: QuizUIState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    // Таймер (визуальный, убывает от 1 до 0)
    var timeLeft by remember(uiState.currentQuestionIndex) { mutableIntStateOf(30) }

    LaunchedEffect(uiState.currentQuestionIndex, uiState.isAnswerSubmitted) {
        if (!uiState.isAnswerSubmitted) {
            timeLeft = 30
            while (timeLeft > 0 && !uiState.isAnswerSubmitted) {
                delay(1000)
                timeLeft--
            }
        }
    }

    val progressAnimated by animateFloatAsState(
        targetValue = if (uiState.totalQuestions > 0)
            (uiState.currentQuestionIndex + 1f) / uiState.totalQuestions else 0f,
        animationSpec = tween(500),
        label = "progress"
    )

    val timerColor = when {
        timeLeft > 15 -> QGreen
        timeLeft > 7  -> Color(0xFFFFA726)
        else          -> QRed
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = QBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Счёт: ${uiState.score}",
                        color = QTextPri,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(QCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                tint = QTextPri, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFA000))
                        Text("${uiState.correctAnswers}/${uiState.totalQuestions}",
                            color = QTextPri, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Прогресс-бар вопросов
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Вопрос ${uiState.currentQuestionIndex + 1} из ${uiState.totalQuestions}",
                        fontSize = 13.sp, color = QTextSec
                    )
                    Text(
                        "${((progressAnimated) * 100).toInt()}%",
                        fontSize = 13.sp, color = QPurple, fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { progressAnimated },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color      = QPurple,
                    trackColor = QCard,
                    strokeCap  = StrokeCap.Round
                )
            }

            // Таймер
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = QCard),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(timerColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Timer, null,
                            modifier = Modifier.size(18.dp), tint = timerColor)
                    }
                    LinearProgressIndicator(
                        progress  = { (timeLeft / 30f).coerceIn(0f, 1f) },
                        modifier  = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color      = timerColor,
                        trackColor = QBorder,
                        strokeCap  = StrokeCap.Round
                    )
                    Text("$timeLeft с", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = timerColor)
                }
            }

            // Текст вопроса
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = QCard),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = uiState.question,
                        fontSize  = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color     = QTextPri,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }
            }

            // Варианты ответов
            val letters = listOf("A", "B", "C", "D", "E")
            uiState.options.forEachIndexed { index, option ->
                val isSelected  = uiState.selectedAnswerIndex == index
                val isSubmitted = uiState.isAnswerSubmitted

                val bgColor: Color
                val borderColor: Color
                val textColor: Color

                when {
                    !isSubmitted && isSelected -> {
                        bgColor     = QPurple.copy(alpha = 0.2f)
                        borderColor = QPurple
                        textColor   = QPurple
                    }
                    !isSubmitted -> {
                        bgColor     = QCard
                        borderColor = QBorder
                        textColor   = QTextPri
                    }
                    isSubmitted && uiState.isAnswerCorrect == true && isSelected -> {
                        bgColor     = QGreen.copy(alpha = 0.15f)
                        borderColor = QGreen
                        textColor   = QGreen
                    }
                    isSubmitted && uiState.isAnswerCorrect == false && isSelected -> {
                        bgColor     = QRed.copy(alpha = 0.15f)
                        borderColor = QRed
                        textColor   = QRed
                    }
                    else -> {
                        bgColor     = QCard
                        borderColor = QBorder
                        textColor   = QTextSec.copy(alpha = 0.5f)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSubmitted) {
                            onSelect(index)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letters.getOrElse(index) { "${index + 1}" },
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = borderColor
                            )
                        }
                        Text(option, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Кнопки действий
            if (!uiState.isAnswerSubmitted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.selectedAnswerIndex != null) QPurple else QBorder)
                        .clickable(enabled = uiState.selectedAnswerIndex != null) { onSubmit() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Ответить",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (uiState.selectedAnswerIndex != null) Color.White else QTextSec
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(QPurple)
                        .clickable { onNext() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isLast = uiState.currentQuestionIndex >= uiState.totalQuestions - 1
                    Text(
                        if (isLast) "Завершить" else "Следующий вопрос →",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН РЕЗУЛЬТАТОВ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuizResultsScreen(
    uiState: QuizUIState,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    val accuracyPct = (uiState.accuracy).toInt()

    val resultColor = when {
        accuracyPct >= 80 -> QGreen
        accuracyPct >= 50 -> Color(0xFFFFA726)
        else              -> QRed
    }

    val resultLabel = when {
        accuracyPct >= 90 -> "Отлично!"
        accuracyPct >= 75 -> "Хорошо!"
        accuracyPct >= 50 -> "Неплохо"
        else              -> "Попробуй ещё"
    }

    Box(
        modifier = Modifier.fillMaxSize().background(QBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Иконка результата
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape)
                    .background(resultColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(48.dp), tint = resultColor)
            }

            Text(resultLabel, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = QTextPri)

            // Основные цифры
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = QCard),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "${uiState.score}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 52.sp,
                        color      = QPurple
                    )
                    Text("очков", fontSize = 14.sp, color = QTextSec)

                    HorizontalDivider(color = QBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResultStat("${uiState.correctAnswers}", "правильно", QGreen)
                        ResultStat("${uiState.incorrectAnswers}", "неверно", QRed)
                        ResultStat("$accuracyPct%", "точность", QPurple)
                    }
                }
            }

            // Кнопки
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = QPurple)
            ) {
                Text("Пройти снова", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = QTextSec),
                border   = androidx.compose.foundation.BorderStroke(1.dp, QBorder)
            ) {
                Text("На главную", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ResultStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 12.sp, color = QTextSec)
    }
}