
package com.example.brainracer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.ui.utils.QuizUIState
import com.example.brainracer.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.delay

// ─── Палитра ────────────────────────────────────────────────────────────────
private val QBg      = Color(0xFF0F0F1A)
private val QCard    = Color(0xFF1A1A2E)
private val QBorder  = Color(0xFF2A2A3E)
private val QPurple  = Color(0xFF667EEA)
private val QGreen   = Color(0xFF3ECFA3)
private val QRed     = Color(0xFFEA5C7E)
private val QOrange  = Color(0xFFFFA726)
private val QGold    = Color(0xFFFFD700)
private val QTextPri = Color(0xFFFFFFFF)
private val QTextSec = Color(0xFF8B8AAE)

// Задержка авто-перехода: 1400 мс при ответе, 900 мс при тайм-ауте
private const val AUTO_ADVANCE_AFTER_ANSWER_MS  = 1400L
private const val AUTO_ADVANCE_AFTER_TIMEOUT_MS = 900L

// ─── Уровни результата ───────────────────────────────────────────────────────
private data class ResultLevel(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val subtitle: String
)

private fun resultLevel(pct: Int) = when {
    pct >= 86 -> ResultLevel("Легенда!",     Icons.Default.EmojiEvents,      QGold,             "Безупречный результат 🏆")
    pct >= 71 -> ResultLevel("А ты крут!",   Icons.Default.Whatshot,         Color(0xFFFF7043), "Отличная работа, так держать 🔥")
    pct >= 51 -> ResultLevel("Не плохо",     Icons.Default.ThumbUp,          QGreen,            "Хороший результат, ещё немного 👍")
    pct >= 31 -> ResultLevel("На миде",      Icons.Default.SentimentNeutral, QOrange,           "Есть куда расти, продолжай 💪")
    else      -> ResultLevel("Попробуй ещё", Icons.Default.Refresh,          QRed,              "Не сдавайся, попробуй снова 😤")
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun QuizPlayScreen(
    quizId: String,
    navController: NavController,
    quizViewModel: QuizViewModel = viewModel()
) {
    val uiState by quizViewModel.uiState.collectAsState()

    LaunchedEffect(quizId) { quizViewModel.loadQuiz(quizId) }

    when {
        uiState.isLoading -> LoadingScreen()
        uiState.errorMessage != null ->
            ErrorScreen(uiState.errorMessage!!) { navController.popBackStack() }
        uiState.showResults || uiState.isQuizCompleted -> {
            var showReview by remember { mutableStateOf(false) }
            if (showReview) {
                AnswerReviewScreen(
                    uiState = uiState,
                    onBack  = { showReview = false }
                )
            } else {
                ResultsScreen(
                    uiState      = uiState,
                    onBack       = { navController.popBackStack() },
                    onRestart    = { quizViewModel.restartQuiz() },
                    onShowReview = { showReview = true }
                )
            }
        }        uiState.question.isNotEmpty() ->
        QuestionScreen(
            uiState    = uiState,
            onBack     = { navController.popBackStack() },
            onSelect   = { quizViewModel.selectAnswer(it) },
            onSubmit   = { quizViewModel.submitAnswer() },
            onNext     = { quizViewModel.nextQuestion() },
            onTimeout  = { quizViewModel.timeoutQuestion() }
        )
    }
}

// ── Загрузка ─────────────────────────────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(QBg), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = QPurple)
            Spacer(Modifier.height(16.dp))
            Text("Загружаем викторину…", color = QTextSec, fontSize = 14.sp)
        }
    }
}

// ── Ошибка ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(QBg), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Ошибка", color = QRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, color = QTextSec, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = QPurple)) {
                Text("Назад")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН ВОПРОСА
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionScreen(
    uiState: QuizUIState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onTimeout: () -> Unit
) {
    val timeLimit = uiState.currentQuestionTimeLimit

    // ── Таймер вопроса ────────────────────────────────────────────────────
    // Когда ключ меняется (новый вопрос или ответ засчитан), эффект перезапускается.
    // Если ответ уже засчитан — таймер останавливается.
    // Если время вышло и ответа нет — вызывает onTimeout().
    var timeLeft by remember(uiState.currentQuestionIndex) { mutableIntStateOf(timeLimit) }

    LaunchedEffect(uiState.currentQuestionIndex, uiState.isAnswerSubmitted) {
        if (uiState.isAnswerSubmitted) return@LaunchedEffect  // ответ уже есть, таймер не нужен
        timeLeft = timeLimit
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        // Время вышло — сообщаем ViewModel
        onTimeout()
    }

    // ── Авто-переход после ответа ─────────────────────────────────────────
    // Показывает прогресс-полосу и автоматически переходит к следующему вопросу.
    // Запускается только когда ответ засчитан, отменяется при переходе (isAnswerSubmitted → false).
    var autoAdvanceProgress by remember { mutableFloatStateOf(0f) }
    val autoAdvanceDelayMs = if (uiState.isAnswerCorrect == null)
        AUTO_ADVANCE_AFTER_TIMEOUT_MS   // тайм-аут — быстрее
    else
        AUTO_ADVANCE_AFTER_ANSWER_MS    // обычный ответ — чуть дольше для фидбека

    LaunchedEffect(uiState.isAnswerSubmitted) {
        if (!uiState.isAnswerSubmitted) {
            autoAdvanceProgress = 0f
            return@LaunchedEffect
        }
        // Анимируем полосу заполнения, затем переходим
        autoAdvanceProgress = 0f
        val startMs = System.currentTimeMillis()
        while (true) {
            delay(16L)   // ~60fps
            val elapsed  = System.currentTimeMillis() - startMs
            val progress = (elapsed.toFloat() / autoAdvanceDelayMs).coerceIn(0f, 1f)
            autoAdvanceProgress = progress
            if (progress >= 1f) break
        }
        onNext()
    }

    // ── Прогресс вопросов ─────────────────────────────────────────────────
    val questionProgressAnimated by animateFloatAsState(
        targetValue   = if (uiState.totalQuestions > 0)
            (uiState.currentQuestionIndex + 1f) / uiState.totalQuestions else 0f,
        animationSpec = tween(400),
        label         = "qProgress"
    )

    val timerColor = when {
        timeLeft > timeLimit * 0.5  -> QGreen
        timeLeft > timeLimit * 0.25 -> QOrange
        else                        -> QRed
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor      = QBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Счёт: ${uiState.score}", color = QTextPri,
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null,
                            modifier = Modifier.size(16.dp), tint = QGold)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Прогресс вопросов ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Вопрос ${uiState.currentQuestionIndex + 1} из ${uiState.totalQuestions}",
                        fontSize = 13.sp, color = QTextSec
                    )
                    Text(
                        "${(questionProgressAnimated * 100).toInt()}%",
                        fontSize = 13.sp, color = QPurple, fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress      = { questionProgressAnimated },
                    modifier      = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color         = QPurple,
                    trackColor    = QCard,
                    strokeCap     = StrokeCap.Round
                )
            }

            // ── Таймер ────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = QCard),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(timerColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Timer, null, Modifier.size(17.dp), tint = timerColor)
                    }
                    LinearProgressIndicator(
                        progress      = { (timeLeft.toFloat() / timeLimit).coerceIn(0f, 1f) },
                        modifier      = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color         = timerColor,
                        trackColor    = QBorder,
                        strokeCap     = StrokeCap.Round
                    )
                    Text(
                        "$timeLeft с",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = timerColor
                    )
                }
            }

            // ── Анимированный блок: вопрос + варианты ────────────────────
            // targetState — пара (текст вопроса, варианты ответов).
            // Параметр лямбды (questionText, options) используется напрямую —
            // это гарантирует корректное «замораживание» контента во время анимации
            // уходящего кадра (Compose не подставит свежий uiState в уходящий вопрос).
            AnimatedContent(
                targetState   = uiState.question to uiState.options,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                },
                label = "questionContent",
                modifier = Modifier.weight(1f)
            ) { (questionText, options) ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.fillMaxSize()
                ) {
                    // Текст вопроса
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = QCard),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = questionText,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = QTextPri,
                                textAlign  = TextAlign.Center,
                                lineHeight = 26.sp
                            )
                        }
                    }

                    // Варианты ответов
                    val letters = listOf("A", "B", "C", "D", "E")
                    options.forEachIndexed { index, option ->
                        AnswerOption(
                            letter      = letters.getOrElse(index) { "${index + 1}" },
                            text        = option,
                            state       = answerOptionState(uiState, index),
                            isSubmitted = uiState.isAnswerSubmitted,
                            onClick     = { onSelect(index) }
                        )
                    }
                }
            }

            // ── Зона действий ─────────────────────────────────────────────
            ActionZone(
                uiState              = uiState,
                autoAdvanceProgress  = autoAdvanceProgress,
                onSubmit             = onSubmit,
                onNext               = onNext
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Варианты ответов ──────────────────────────────────────────────────────────

private enum class AnswerState { IDLE, SELECTED, CORRECT, WRONG, DIMMED }

private fun answerOptionState(uiState: QuizUIState, index: Int): AnswerState {
    val isSelected  = uiState.selectedAnswerIndex == index
    val isSubmitted = uiState.isAnswerSubmitted
    return when {
        !isSubmitted && isSelected  -> AnswerState.SELECTED
        !isSubmitted                -> AnswerState.IDLE
        isSubmitted && isSelected && uiState.isAnswerCorrect == true  -> AnswerState.CORRECT
        isSubmitted && isSelected && uiState.isAnswerCorrect == false -> AnswerState.WRONG
        else                        -> AnswerState.DIMMED
    }
}

@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    state: AnswerState,
    isSubmitted: Boolean,
    onClick: () -> Unit
) {
    val bgColor     = when (state) {
        AnswerState.SELECTED -> QPurple.copy(alpha = .20f)
        AnswerState.CORRECT  -> QGreen.copy(alpha = .15f)
        AnswerState.WRONG    -> QRed.copy(alpha = .15f)
        else                 -> QCard
    }
    val borderColor = when (state) {
        AnswerState.SELECTED -> QPurple
        AnswerState.CORRECT  -> QGreen
        AnswerState.WRONG    -> QRed
        else                 -> QBorder
    }
    val textColor   = when (state) {
        AnswerState.SELECTED -> QPurple
        AnswerState.CORRECT  -> QGreen
        AnswerState.WRONG    -> QRed
        AnswerState.DIMMED   -> QTextSec.copy(alpha = .45f)
        AnswerState.IDLE     -> QTextPri
    }

    // Иконка справа для правильного / неверного после подтверждения
    val trailingIcon: ImageVector? = when (state) {
        AnswerState.CORRECT -> Icons.Default.CheckCircle
        AnswerState.WRONG   -> Icons.Default.Cancel
        else                -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSubmitted) { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(borderColor.copy(alpha = .15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = borderColor)
            }
            Text(
                text       = text,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = textColor,
                modifier   = Modifier.weight(1f)
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, null, tint = borderColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Зона действий (кнопка + авто-переход) ────────────────────────────────────

@Composable
private fun ActionZone(
    uiState: QuizUIState,
    autoAdvanceProgress: Float,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!uiState.isAnswerSubmitted) {
            // Кнопка «Ответить»
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
            val isLast = uiState.currentQuestionIndex >= uiState.totalQuestions - 1

            // Полоса авто-перехода — заполняется за AUTO_ADVANCE_AFTER_ANSWER_MS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        if (isLast) "Переход к результатам…" else "Следующий вопрос…",
                        fontSize = 12.sp,
                        color    = QTextSec
                    )
                    Text(
                        "Нажмите для пропуска",
                        fontSize = 12.sp,
                        color    = QPurple
                    )
                }
                LinearProgressIndicator(
                    progress      = { autoAdvanceProgress },
                    modifier      = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color         = QPurple,
                    trackColor    = QBorder,
                    strokeCap     = StrokeCap.Round
                )
            }

            // Кнопка ручного пропуска (кликабельна сразу)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(QPurple)
                    .clickable { onNext() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isLast) "Завершить" else "Следующий вопрос →",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН РЕЗУЛЬТАТОВ (без изменений)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultsScreen(
    uiState: QuizUIState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onShowReview: () -> Unit
) {
    val accuracyPct = if (uiState.totalQuestions > 0)
        (uiState.correctAnswers * 100) / uiState.totalQuestions else 0

    val level = resultLevel(accuracyPct)

    val accuracyAnimated by animateFloatAsState(
        targetValue   = accuracyPct / 100f,
        animationSpec = tween(1000), label = "accuracy"
    )
    val xpProgressAnimated by animateFloatAsState(
        targetValue   = uiState.newLevelProgress,
        animationSpec = tween(1200), label = "xpProgress"
    )

    var showLevelUpBanner by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.leveledUp) {
        if (uiState.leveledUp) {
            delay(600)
            showLevelUpBanner = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(QBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = showLevelUpBanner,
                enter   = fadeIn(tween(400)) + expandVertically(tween(400))
            ) {
                LevelUpBanner(newLevel = uiState.newLevel)
            }

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(level.color.copy(alpha = 0.15f))
                    .border(2.dp, level.color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(level.icon, null, modifier = Modifier.size(54.dp), tint = level.color)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(level.label, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = level.color)
                Spacer(Modifier.height(4.dp))
                Text(level.subtitle, fontSize = 14.sp, color = QTextSec, textAlign = TextAlign.Center)
            }

            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = QCard),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${uiState.score}", fontWeight = FontWeight.Bold,
                            fontSize = 52.sp, color = QPurple)
                        Text("игровых очков", fontSize = 13.sp, color = QTextSec)
                    }

                    HorizontalDivider(color = QBorder)

                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Точность", fontSize = 13.sp, color = QTextSec)
                            Text("$accuracyPct%", fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, color = level.color)
                        }
                        LinearProgressIndicator(
                            progress  = { accuracyAnimated },
                            modifier  = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color      = level.color, trackColor = QBorder, strokeCap = StrokeCap.Round
                        )
                    }

                    HorizontalDivider(color = QBorder)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ResultStat("${uiState.correctAnswers}",  "правильно", QGreen)
                        Box(Modifier.width(1.dp).height(40.dp).background(QBorder))
                        ResultStat("${uiState.incorrectAnswers}", "неверно",  QRed)
                        Box(Modifier.width(1.dp).height(40.dp).background(QBorder))
                        ResultStat("${uiState.totalQuestions}",  "всего",    QTextSec)
                    }
                }
            }

            if (uiState.xpEarned > 0) {
                XpCard(uiState = uiState, xpProgressAnimated = xpProgressAnimated)
            }

            Button(
                onClick  = onRestart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = QPurple)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Пройти снова", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            // Кнопка разбора — показывается только если есть что разбирать
            if (uiState.reviewQuestions.isNotEmpty()) {
                OutlinedButton(
                    onClick  = onShowReview,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = QGreen),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, QGreen)
                ) {
                    Icon(Icons.Default.Checklist, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Разбор ответов", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = QTextSec),
                border   = androidx.compose.foundation.BorderStroke(1.dp, QBorder)
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("На главную", fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Вспомогательные ───────────────────────────────────────────────────────────

@Composable
private fun LevelUpBanner(newLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = QGold.copy(alpha = 0.12f)),
        border   = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(QGold.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowUpward, null, tint = QGold, modifier = Modifier.size(24.dp))
            }
            Column {
                Text("🎉 Новый уровень!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QGold)
                Text(
                    "Вы достигли уровня $newLevel. Продолжайте в том же духе!",
                    fontSize = 13.sp, color = QTextSec, lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun XpCard(uiState: QuizUIState, xpProgressAnimated: Float) {
    val breakdown    = uiState.xpBreakdown
    val currentLevel = uiState.newLevel

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = QCard),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = QPurple, modifier = Modifier.size(20.dp))
                Text("Заработано XP", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QTextPri)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = QPurple.copy(alpha = 0.2f)) {
                    Text("+${uiState.xpEarned} XP",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = QPurple)
                }
            }

            if (breakdown != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    XpRow("Базовые", breakdown.baseXp, QTextSec)
                    if (breakdown.speedBonusXp > 0)
                        XpRow("Бонус скорости ⚡", breakdown.speedBonusXp, QGreen)
                    if (breakdown.accuracyBonusXp > 0)
                        XpRow("Бонус точности 🎯", breakdown.accuracyBonusXp, Color(0xFF4facfe))
                    if (!breakdown.difficultyLabel.startsWith("×1.0")) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Множитель сложности", fontSize = 13.sp, color = QTextSec)
                            Text(breakdown.difficultyLabel, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = QGold)
                        }
                    }
                }
                HorizontalDivider(color = QBorder)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Уровень $currentLevel", fontSize = 13.sp, color = QTextSec)
                    Text(if (currentLevel < LevelSystem.MAX_LEVEL) "Уровень ${currentLevel + 1}"
                    else "Максимум", fontSize = 13.sp, color = QTextSec)
                }
                LinearProgressIndicator(
                    progress  = { xpProgressAnimated },
                    modifier  = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color      = QPurple, trackColor = QBorder, strokeCap = StrokeCap.Round
                )
                Text(
                    "Ур. $currentLevel  ·  ${LevelSystem.rankForLevel(currentLevel).displayName}",
                    fontSize  = 12.sp, color = QTextSec,
                    modifier  = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun XpRow(label: String, value: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = QTextSec)
        Text("+$value XP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun ResultStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 11.sp, color = QTextSec)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ЭКРАН РАЗБОРА ОТВЕТОВ
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerReviewScreen(
    uiState: QuizUIState,
    onBack: () -> Unit
) {
    val questions = uiState.reviewQuestions
    val answers   = uiState.reviewAnswers

    val correctCount   = answers.count { it.isCorrect }
    val incorrectCount = answers.count { !it.isCorrect }

    Scaffold(
        containerColor      = QBg,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Разбор ответов", color = QTextPri,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${questions.size} вопросов  ·  $correctCount правильно  ·  $incorrectCount неверно",
                            color = QTextSec, fontSize = 12.sp)
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(questions) { i, question ->
                val answer = answers.getOrNull(i)
                ReviewQuestionCard(
                    index    = i,
                    question = question,
                    answer   = answer
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ReviewQuestionCard(
    index: Int,
    question: com.example.brainracer.domain.entities.Question,
    answer: com.example.brainracer.domain.entities.UserAnswer?
) {
    val isCorrect     = answer?.isCorrect == true
    val isTimeout     = answer == null || answer.selectedAnswerIndex == -1
    val accentColor   = when {
        isTimeout  -> QOrange
        isCorrect  -> QGreen
        else       -> QRed
    }
    val statusLabel   = when {
        isTimeout -> "Время вышло"
        isCorrect -> "Правильно"
        else      -> "Неверно"
    }
    val statusIcon    = when {
        isTimeout -> Icons.Default.Timer
        isCorrect -> Icons.Default.CheckCircle
        else      -> Icons.Default.Cancel
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = QCard),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ── Заголовок: номер + статус ─────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Номер вопроса
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = QPurple.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Вопрос ${index + 1}",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = QPurple
                    )
                }
                // Статус
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(statusIcon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = accentColor)
                    // Время ответа
                    if (answer != null && answer.timeSpent > 0) {
                        Text("·", fontSize = 12.sp, color = QTextSec)
                        Text("${answer.timeSpent}с", fontSize = 12.sp, color = QTextSec)
                    }
                }
            }

            // ── Текст вопроса ─────────────────────────────────────────────
            Text(
                text       = question.questionText,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = QTextPri,
                lineHeight = 22.sp
            )

            // ── Варианты ответов ──────────────────────────────────────────
            val letters = listOf("A", "B", "C", "D", "E")
            question.options.forEachIndexed { optIdx, optText ->
                val isUserChoice    = answer?.selectedAnswerIndex == optIdx
                val isRightAnswer   = question.correctAnswerIndex == optIdx

                val (bg, border, text, icon) = when {
                    // Правильный вариант — всегда зелёный
                    isRightAnswer && isUserChoice ->
                        Quad(QGreen.copy(.15f), QGreen, QGreen, Icons.Default.CheckCircle)
                    isRightAnswer ->
                        Quad(QGreen.copy(.08f), QGreen.copy(.6f), QGreen, null)
                    // Ответ пользователя, но неверный
                    isUserChoice ->
                        Quad(QRed.copy(.15f), QRed, QRed, Icons.Default.Cancel)
                    // Остальные варианты — приглушённые
                    else ->
                        Quad(QCard, QBorder, QTextSec.copy(.4f), null)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Буква варианта
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                .background(border.copy(alpha = .18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letters.getOrElse(optIdx) { "${optIdx + 1}" },
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = border)
                        }
                        Text(optText, fontSize = 14.sp, color = text, modifier = Modifier.weight(1f))
                        if (icon != null) {
                            Icon(icon, null, tint = border, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Объяснение (если есть) ────────────────────────────────────
            val explanation = question.explanation
            if (!explanation.isNullOrBlank()) {
                HorizontalDivider(color = QBorder.copy(alpha = .5f))
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null,
                        tint = QGold, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Text(
                        text       = explanation,
                        fontSize   = 13.sp,
                        color      = QTextSec,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

/** Вспомогательный data class для деструктурирования четырёх значений стиля варианта */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)