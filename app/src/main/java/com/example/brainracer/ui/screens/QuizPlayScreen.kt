package com.example.brainracer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.brainracer.data.storage.StorageConfig
import com.example.brainracer.data.utils.isNetworkLikelyAvailable
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.utils.AppMotionConfig
import com.example.brainracer.ui.utils.QuestionOutcome
import com.example.brainracer.ui.utils.QuizNonScoringReason
import com.example.brainracer.ui.utils.QuizUIState
import com.example.brainracer.ui.viewmodels.QuizViewModel
import kotlinx.coroutines.delay

private const val AUTO_ADVANCE_AFTER_ANSWER_MS  = 1400L
private const val AUTO_ADVANCE_AFTER_TIMEOUT_MS = 900L

// Данные для отображения уровня результата
private data class ResultLevel(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val subtitle: String
)

// Определяет уровень результата по проценту правильных ответов
private fun resultLevel(pct: Int) = when {
    pct >= 86 -> ResultLevel("Легенда!",     Icons.Default.EmojiEvents,      BrainRacerColorTokens.DifficultyExpert,        "Безупречный результат 🏆")
    pct >= 71 -> ResultLevel("А ты крут!",   Icons.Default.Whatshot,         BrainRacerColorTokens.QuizResultEncouragement, "Отличная работа, так держать 🔥")
    pct >= 51 -> ResultLevel("Не плохо",     Icons.Default.ThumbUp,          BrainRacerColorTokens.DetailGreen,             "Хороший результат, ещё немного 👍")
    pct >= 31 -> ResultLevel("На миде",      Icons.Default.SentimentNeutral, BrainRacerColorTokens.StatusOrange,            "Есть куда расти, продолжай 💪")
    else      -> ResultLevel("Попробуй ещё", Icons.Default.Refresh,          BrainRacerColorTokens.Dark.Error,              "Не сдавайся, попробуй снова 😤")
}

// Главный экран прохождения викторины
@Composable
fun QuizPlayScreen(
    quizId: String,
    navController: NavController,
    challengeId: String? = null,
    challengeIntroAlreadyShown: Boolean = false,
    challengeIntroCancelToHome: Boolean = false,
    forceNonScoring: Boolean = false,
    quizViewModel: QuizViewModel = viewModel()
) {
    val uiState by quizViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Загрузка викторины при входе
    LaunchedEffect(quizId, challengeId, forceNonScoring) {
        val online = isNetworkLikelyAvailable(context.applicationContext)
        quizViewModel.loadQuiz(quizId, challengeId, online, forceNonScoring)
    }

    var challengeIntroAcknowledged by rememberSaveable(quizId, challengeId, challengeIntroAlreadyShown) {
        mutableStateOf(challengeIntroAlreadyShown)
    }

    // Маршрутизация по состояниям викторины
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.errorMessage != null ->
            ErrorScreen(uiState.errorMessage!!) { navController.popBackStack() }
        !challengeId.isNullOrBlank() && !challengeIntroAcknowledged -> {
            BackHandler { navController.popBackStack() }
            ChallengeDuelIntroScreen(
                quizTitle           = uiState.quizTitle.ifBlank { "Викторина" },
                totalQuestions      = uiState.totalQuestions,
                onStart             = { challengeIntroAcknowledged = true },
                onCancel            = { navController.popBackStack() },
                cancelButtonLabel   = if (challengeIntroCancelToHome) "На главную" else "Отмена"
            )
        }
        uiState.showResults || uiState.isQuizCompleted -> {
            var showReview by rememberSaveable(quizId, challengeId) { mutableStateOf(false) }
            val challengeMode = !uiState.challengeId.isNullOrBlank()
            BackHandler(enabled = challengeMode && showReview)  { showReview = false }
            BackHandler(enabled = challengeMode && !showReview) { navController.popBackStack() }
            if (showReview) {
                AnswerReviewScreen(uiState = uiState, onBack = { showReview = false })
            } else {
                ResultsScreen(
                    uiState               = uiState,
                    onBack                = { navController.popBackStack() },
                    onRestart             = { quizViewModel.restartQuiz() },
                    onShowReview          = { showReview = true },
                    allowRestart          = uiState.challengeId.isNullOrBlank(),
                    challengeId           = uiState.challengeId,
                    onOpenChallengeSummary = uiState.challengeId?.let { cid ->
                        { navController.navigate("challenge_review/$cid") }
                    }
                )
            }
        }
        uiState.question.isNotEmpty() -> {
            BackHandler { }
            QuestionScreen(
                uiState   = uiState,
                onBack    = null,
                onSelect  = { quizViewModel.selectAnswer(it) },
                onSubmit  = { quizViewModel.submitAnswer() },
                onNext    = { quizViewModel.nextQuestion() },
                onTimeout = { quizViewModel.timeoutQuestion() }
            )
        }
        else -> LoadingScreen()
    }
}

// Интро-экран перед началом дуэли
@Composable
private fun ChallengeDuelIntroScreen(
    quizTitle: String,
    totalQuestions: Int,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    cancelButtonLabel: String = "Отмена"
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.brainracer.R.drawable.cognition),
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text("Вызов", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text(quizTitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, lineHeight = 23.sp)

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Перед стартом", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    IntroBullet("Одна попытка: повторно пройти эту дуэль нельзя.")
                    IntroBullet("Счёт станет виден обоим после того, как оба завершат викторину.")
                    IntroBullet("По завершении дуэль окажется во вкладке «Завершённые».")
                    if (totalQuestions > 0) {
                        Text("Вопросов: $totalQuestions", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Button(
                onClick  = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Начать викторину", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = onCancel) {
                Text(cancelButtonLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

// Пункт списка в интро-экране
@Composable
private fun IntroBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp,
            modifier = Modifier.padding(top = 1.dp))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

// Экран загрузки викторины
@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(
                modifier  = Modifier.size(48.dp),
                color     = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text("Загружаем викторину…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

// Экран ошибки с кнопкой возврата
@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            }
            Text("Ошибка", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onBack, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Назад", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Бейдж режима сессии (офлайн)
@Composable
private fun QuizSessionBadges(uiState: QuizUIState) {
    if (!(!uiState.sessionNetworkAvailable)) return
    SessionModeBadge(
        label          = "Офлайн",
        containerColor = LocalBrainRacerExtendedColors.current.statusOrange.copy(alpha = 0.18f),
        contentColor   = LocalBrainRacerExtendedColors.current.statusOrange
    )
}

// Простой бейдж с текстом
@Composable
private fun SessionModeBadge(label: String, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = containerColor) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

// Круговой индикатор таймера на Canvas
@Composable
private fun CircularTimer(
    timeLeft: Int,
    timeLimit: Int,
    timerColor: Color,
    size: androidx.compose.ui.unit.Dp = 72.dp
) {
    val fraction by animateFloatAsState(
        targetValue   = (timeLeft.toFloat() / timeLimit.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.LinearEasing),
        label         = "timerFraction"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx  = this.size.minDimension * 0.095f
            val halfStroke = strokePx / 2f
            val arcRect   = Size(this.size.width - strokePx, this.size.height - strokePx)
            val arcOffset = Offset(halfStroke, halfStroke)

            drawArc(
                color      = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = arcOffset,
                size       = arcRect,
                style      = Stroke(strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color      = timerColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter  = false,
                topLeft    = arcOffset,
                size       = arcRect,
                style      = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text       = "$timeLeft",
            fontSize   = (size.value * 0.28f).sp,
            fontWeight = FontWeight.ExtraBold,
            color      = timerColor
        )
    }
}

// Сегментированный прогресс-бар вопросов
@Composable
private fun SegmentedProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val primary  = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total.coerceAtMost(30)) { i ->
            val anim by animateFloatAsState(
                targetValue   = if (i <= current) 1f else 0f,
                animationSpec = tween(300, delayMillis = i * 20),
                label         = "seg$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(androidx.compose.ui.graphics.lerp(inactive, primary, anim))
            )
        }
    }
}

// Прогресс-бар с цветовой индикацией результатов
@Composable
private fun QuestionOutcomeProgress(
    outcomes: List<QuestionOutcome>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    if (outcomes.isEmpty()) return
    val green  = LocalBrainRacerExtendedColors.current.detailGreen
    val orange = LocalBrainRacerExtendedColors.current.statusOrange
    val wrong  = MaterialTheme.colorScheme.error
    val base   = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val currentBorder = MaterialTheme.colorScheme.primary
    val currentBorderWidth = 1.5.dp

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        outcomes.forEachIndexed { i, outcome ->
            val color = when (outcome) {
                QuestionOutcome.UNANSWERED -> base
                QuestionOutcome.CORRECT    -> green
                QuestionOutcome.WRONG      -> wrong
                QuestionOutcome.TIMEOUT    -> orange
            }
            val isCurrent = i == currentIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
                    .then(
                        if (isCurrent) Modifier.border(currentBorderWidth, currentBorder, RoundedCornerShape(3.dp))
                        else Modifier
                    )
            )
        }
    }
}

// Отображение изображения вопроса с адаптивной высотой
@Composable
private fun QuestionAttachedImage(
    imageUrl: String,
    isCompactHeight: Boolean,
    isCompactWidth: Boolean,
    topCornerShape: RoundedCornerShape
) {
    val context   = LocalContext.current
    val config    = LocalConfiguration.current
    val surface   = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    // Расчет высоты фрейма под изображение
    val targetH = when {
        isCompactHeight && isCompactWidth -> 168.dp
        isCompactHeight                   -> 188.dp
        isCompactWidth                    -> 220.dp
        else                              -> 260.dp
    }
    val maxH = (config.screenHeightDp * 0.42f).dp.coerceAtLeast(160.dp)
    val frameH = targetH.coerceAtMost(maxH)

    // Анимация загрузки (shimmer)
    val shimmerAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerAnim.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val shimmerBrush = Brush.horizontalGradient(
        colors = listOf(
            surface.copy(alpha = 0.6f),
            surface.copy(alpha = 1.0f),
            surface.copy(alpha = 0.6f),
        ),
        startX = shimmerX * 800f,
        endX   = (shimmerX + 1f) * 800f
    )

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(StorageConfig.resolvePublicUrlForCoil(imageUrl))
            .crossfade(300)
            .build()
    }

    SubcomposeAsyncImage(
        model              = request,
        contentDescription = null,
        contentScale       = ContentScale.Fit,
        modifier           = Modifier
            .fillMaxWidth()
            .height(frameH)
            .clip(topCornerShape),
        loading = {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(shimmerBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Image,
                    contentDescription = null,
                    tint               = onSurface.copy(alpha = 0.25f),
                    modifier           = Modifier.size(32.dp)
                )
            }
        },
        error = {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(surface.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint               = onSurface.copy(alpha = 0.35f),
                        modifier           = Modifier.size(28.dp)
                    )
                    Text(
                        "Не удалось загрузить",
                        fontSize = 11.sp,
                        color    = onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        },
        success = { state ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Размытая подложка для вертикальных фото
                Image(
                    painter            = state.painter,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    alpha              = 0.55f,
                    modifier           = Modifier
                        .fillMaxSize()
                        .blur(28.dp)
                )
                // Градиент для контраста текста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f   to Color.Black.copy(alpha = 0.18f),
                                0.5f to Color.Transparent,
                                1f   to Color.Black.copy(alpha = 0.22f)
                            )
                        )
                )
                // Само изображение
                Image(
                    painter            = state.painter,
                    contentDescription = null,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
    )
}

// Экран вопроса с таймером и вариантами ответов
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionScreen(
    uiState: QuizUIState,
    onBack: (() -> Unit)?,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onTimeout: () -> Unit
) {
    val config         = LocalConfiguration.current
    val isCompactHeight = config.screenHeightDp < 760
    val isCompactWidth  = config.screenWidthDp < 390
    val timeLimit       = uiState.currentQuestionTimeLimit

    // Таймер обратного отсчёта
    var timeLeft by remember(uiState.currentQuestionIndex) { mutableIntStateOf(timeLimit) }

    LaunchedEffect(uiState.currentQuestionIndex, uiState.isAnswerSubmitted) {
        if (uiState.isAnswerSubmitted) return@LaunchedEffect
        timeLeft = timeLimit
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onTimeout()
    }

    // Авто-переход к следующему вопросу
    var autoAdvanceProgress by remember { mutableFloatStateOf(0f) }
    val autoAdvanceDelayMs = if (uiState.isAnswerCorrect == null)
        AUTO_ADVANCE_AFTER_TIMEOUT_MS else AUTO_ADVANCE_AFTER_ANSWER_MS

    LaunchedEffect(uiState.isAnswerSubmitted) {
        if (!uiState.isAnswerSubmitted) { autoAdvanceProgress = 0f; return@LaunchedEffect }
        autoAdvanceProgress = 0f
        val startMs = System.currentTimeMillis()
        while (true) {
            delay(16L)
            val elapsed  = System.currentTimeMillis() - startMs
            val progress = (elapsed.toFloat() / autoAdvanceDelayMs).coerceIn(0f, 1f)
            autoAdvanceProgress = progress
            if (progress >= 1f) break
        }
        onNext()
    }

    // Цвет таймера в зависимости от оставшегося времени
    val timerColor = when {
        timeLeft > timeLimit * 0.5  -> LocalBrainRacerExtendedColors.current.detailGreen
        timeLeft > timeLimit * 0.25 -> LocalBrainRacerExtendedColors.current.statusOrange
        else                        -> MaterialTheme.colorScheme.error
    }

    val hPad = if (isCompactWidth) 14.dp else 18.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = hPad),
        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Шапка: таймер + прогресс + счёт
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularTimer(
                timeLeft   = timeLeft,
                timeLimit  = timeLimit,
                timerColor = timerColor,
                size       = if (isCompactHeight) 60.dp else 70.dp
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Вопрос ${uiState.currentQuestionIndex + 1} / ${uiState.totalQuestions}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Star, null,
                                Modifier.size(13.dp),
                                tint = LocalBrainRacerExtendedColors.current.difficultyExpert)
                            Text("${uiState.score}", fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                QuestionOutcomeProgress(
                    outcomes     = uiState.questionOutcomes.takeIf { it.isNotEmpty() }
                        ?: List(uiState.totalQuestions) { QuestionOutcome.UNANSWERED },
                    currentIndex = uiState.currentQuestionIndex,
                    modifier     = Modifier.fillMaxWidth()
                )

                QuizSessionBadges(uiState)
            }
        }

        // Анимированный блок вопроса и ответов
        AnimatedContent(
            targetState = Triple(
                uiState.question,
                uiState.options,
                uiState.attachedImageUrl.orEmpty()
            ),
            transitionSpec = {
                (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)))
                    .togetherWith(slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
            },
            label    = "questionContent",
            modifier = Modifier.weight(1f)
        ) { (questionText, options, attachedKey) ->
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 7.dp else 9.dp),
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
            ) {
                val topImageCorners = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                // Карточка вопроса
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        val imgUrl = attachedKey.takeUnless { it.isBlank() }
                        if (!imgUrl.isNullOrBlank()) {
                            QuestionAttachedImage(
                                imageUrl         = imgUrl,
                                isCompactHeight  = isCompactHeight,
                                isCompactWidth   = isCompactWidth,
                                topCornerShape   = topImageCorners
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompactWidth) 14.dp else 20.dp,
                                    vertical   = if (isCompactHeight) 14.dp else 18.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = questionText,
                                fontSize   = if (isCompactHeight || isCompactWidth) 15.sp else 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                textAlign  = TextAlign.Center,
                                lineHeight = if (isCompactHeight || isCompactWidth) 22.sp else 25.sp
                            )
                        }
                    }
                }

                // Варианты ответов
                val letters = listOf("A", "B", "C", "D", "E", "F")
                options.forEachIndexed { index, option ->
                    AnswerOption(
                        letter      = letters.getOrElse(index) { "${index + 1}" },
                        text        = option,
                        state       = answerOptionState(uiState, index),
                        isSubmitted = uiState.isAnswerSubmitted,
                        onClick     = { onSelect(index) },
                        compact     = isCompactHeight || isCompactWidth
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        }

        // Зона кнопок действий
        ActionZone(
            uiState             = uiState,
            autoAdvanceProgress = autoAdvanceProgress,
            onSubmit            = onSubmit,
            onNext              = onNext,
            compact             = isCompactHeight || isCompactWidth
        )

        Spacer(Modifier.height(if (isCompactHeight) 8.dp else 14.dp))
    }
}

// Определяет состояние варианта ответа для отображения
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

// Карточка варианта ответа с визуальной индикацией состояния
@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    state: AnswerState,
    isSubmitted: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val green = LocalBrainRacerExtendedColors.current.detailGreen
    val error = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary

    val accentColor = when (state) {
        AnswerState.SELECTED -> primary
        AnswerState.CORRECT  -> green
        AnswerState.WRONG    -> error
        AnswerState.DIMMED   -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        AnswerState.IDLE     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val bgColor = when (state) {
        AnswerState.SELECTED -> primary.copy(alpha = 0.10f)
        AnswerState.CORRECT  -> green.copy(alpha = 0.10f)
        AnswerState.WRONG    -> error.copy(alpha = 0.10f)
        AnswerState.DIMMED   -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        AnswerState.IDLE     -> MaterialTheme.colorScheme.surface
    }
    val textAlpha = if (state == AnswerState.DIMMED) 0.3f else 1f
    val borderWidth = if (state == AnswerState.IDLE || state == AnswerState.DIMMED) 1.dp else 1.5.dp

    val trailingIcon: ImageVector? = when (state) {
        AnswerState.CORRECT -> Icons.Default.CheckCircle
        AnswerState.WRONG   -> Icons.Default.Cancel
        else                -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, accentColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isSubmitted) { onClick() }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Акцентная полоса слева
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Spacer(Modifier.width(if (compact) 11.dp else 13.dp))

        // Буква варианта в круге
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 32.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                fontSize   = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (state == AnswerState.IDLE || state == AnswerState.DIMMED)
                    MaterialTheme.colorScheme.background
                else Color.White
            )
        }

        Spacer(Modifier.width(if (compact) 11.dp else 14.dp))

        Text(
            text       = text,
            modifier   = Modifier
                .weight(1f)
                .padding(vertical = if (compact) 13.dp else 16.dp),
            fontSize   = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            lineHeight = if (compact) 20.sp else 22.sp
        )

        if (trailingIcon != null) {
            Icon(trailingIcon, null,
                tint     = accentColor,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            Spacer(Modifier.width(14.dp))
        } else {
            Spacer(Modifier.width(14.dp))
        }
    }
}

// Зона кнопок: ответ / следующий вопрос
@Composable
private fun ActionZone(
    uiState: QuizUIState,
    autoAdvanceProgress: Float,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean = false
) {
    val vertPad = if (compact) 14.dp else 17.dp
    val fontSize = if (compact) 14.sp else 15.sp

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!uiState.isAnswerSubmitted) {
            // Кнопка отправки ответа
            val enabled = uiState.selectedAnswerIndex != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (enabled) 6.dp else 0.dp,
                        shape     = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                    .clickable(enabled = enabled) { onSubmit() }
                    .padding(vertical = vertPad),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ответить",
                    fontSize   = fontSize,
                    fontWeight = FontWeight.Bold,
                    color      = if (enabled) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            val isLast = uiState.currentQuestionIndex >= uiState.totalQuestions - 1

            // Индикатор авто-перехода
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isLast) "Переход к результатам…" else "Следующий вопрос…",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Нажмите для пропуска",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress   = { autoAdvanceProgress },
                    modifier   = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeCap  = StrokeCap.Round
                )
            }

            // Кнопка перехода / завершения
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onNext() }
                    .padding(vertical = vertPad),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isLast) "Завершить" else "Следующий вопрос",
                        fontSize   = fontSize,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    if (!isLast) {
                        Icon(Icons.Default.ArrowForward, null,
                            Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

// Текст сообщения для не-рейтинговых сессий
private fun nonScoringResultsMessage(reason: QuizNonScoringReason?): String = when (reason) {
    QuizNonScoringReason.PRACTICE_REPLAY ->
        "Повторное прохождение: опыт и статистика не начисляются, результат в облако не сохраняется."
    QuizNonScoringReason.OFFLINE ->
        "Результат не учитывается в рейтинге и статистике и не сохраняется. Нет сети или данные взяты только из кэша устройства."
    QuizNonScoringReason.NOT_SIGNED_IN ->
        "Результат не учитывается в рейтинге и статистике и не сохраняется. Войдите в аккаунт, чтобы сохранять прогресс."
    null ->
        "Результат не учитывается в рейтинге и статистике и не сохраняется."
}

// Баннер с пояснением, почему результат не сохраняется
@Composable
private fun NonScoringResultsBanner(reason: QuizNonScoringReason?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        border   = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            nonScoringResultsMessage(reason),
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize   = 13.sp,
            lineHeight = 18.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Круговой индикатор точности на Canvas
@Composable
private fun AccuracyRing(
    accuracyPct: Int,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 130.dp
) {
    val animated by animateFloatAsState(
        targetValue   = accuracyPct / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label         = "ringAnim"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW  = this.size.minDimension * 0.085f
            val halfS    = strokeW / 2f
            val arcSize  = Size(this.size.width - strokeW, this.size.height - strokeW)
            val arcOff   = Offset(halfS, halfS)
            drawArc(trackColor, -90f, 360f, false, arcOff, arcSize, style = Stroke(strokeW, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * animated, false, arcOff, arcSize, style = Stroke(strokeW, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$accuracyPct%", fontSize = (size.value * 0.24f).sp,
                fontWeight = FontWeight.ExtraBold, color = color)
            Text("точность", fontSize = (size.value * 0.10f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Экран результатов викторины
@Composable
private fun ResultsScreen(
    uiState: QuizUIState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onShowReview: () -> Unit,
    allowRestart: Boolean = true,
    challengeId: String? = null,
    onOpenChallengeSummary: (() -> Unit)? = null
) {
    val expandMotion   = AppMotionConfig.expand
    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 390
    val accuracyPct    = if (uiState.totalQuestions > 0)
        (uiState.correctAnswers * 100) / uiState.totalQuestions else 0
    val level          = resultLevel(accuracyPct)

    val xpProgressAnimated by animateFloatAsState(
        targetValue   = uiState.newLevelProgress,
        animationSpec = tween(1200), label = "xpProgress"
    )

    var showLevelUpBanner by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.leveledUp) {
        if (uiState.leveledUp) { delay(600); showLevelUpBanner = true }
    }

    // Анимация набора очков
    var displayScore by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.score) {
        val target = uiState.score
        val steps  = 30
        repeat(steps) { i ->
            displayScore = (target * (i + 1) / steps)
            delay(20)
        }
        displayScore = target
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isCompactWidth) 16.dp else 22.dp,
                    vertical   = if (isCompactWidth) 20.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isNonScoringSession) NonScoringResultsBanner(uiState.nonScoringReason)

            AnimatedVisibility(
                visible = showLevelUpBanner,
                enter   = fadeIn(tween(280)) + expandVertically(
                    spring(dampingRatio = expandMotion.enterDampingRatio,
                        stiffness   = expandMotion.enterStiffness)
                )
            ) { LevelUpBanner(newLevel = uiState.newLevel) }

            // Заголовок с результатом
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(level.label,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = if (isCompactWidth) 26.sp else 30.sp,
                    color      = level.color,
                    textAlign  = TextAlign.Center)
                Text(level.subtitle,
                    fontSize  = if (isCompactWidth) 13.sp else 14.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }

            // Кольцо точности + счёт
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AccuracyRing(
                    accuracyPct = accuracyPct,
                    color       = level.color,
                    size        = if (isCompactWidth) 110.dp else 130.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "$displayScore",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = if (isCompactWidth) 48.sp else 56.sp,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text("очков", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Карточка статистики
            Card(
                shape     = RoundedCornerShape(22.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStat(
                        "${uiState.correctAnswers}", "правильно",
                        LocalBrainRacerExtendedColors.current.detailGreen,
                        Icons.Default.CheckCircle,
                        isCompactWidth
                    )
                    Box(Modifier.width(1.dp).height(48.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                    ResultStat(
                        "${uiState.incorrectAnswers}", "неверно",
                        MaterialTheme.colorScheme.error,
                        Icons.Default.Cancel,
                        isCompactWidth
                    )
                    Box(Modifier.width(1.dp).height(48.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                    ResultStat(
                        "${uiState.totalQuestions}", "всего",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        Icons.Default.Quiz,
                        isCompactWidth
                    )
                }
            }

            // Карточка с заработанным опытом
            val showXpCard = uiState.xpEarned > 0 ||
                    (uiState.duelXpDeferred && uiState.xpBreakdown != null && !uiState.isNonScoringSession)
            if (showXpCard) {
                XpCard(
                    uiState            = uiState,
                    xpProgressAnimated = xpProgressAnimated,
                    isReferenceOnly    = uiState.isNonScoringSession,
                    duelXpDeferred     = uiState.duelXpDeferred && uiState.xpEarned == 0,
                    compact            = isCompactWidth
                )
            }

            // Кнопки действий
            val btnHeight = if (isCompactWidth) 50.dp else 54.dp
            val btnShape  = RoundedCornerShape(16.dp)

            if (allowRestart) {
                Button(
                    onClick   = onRestart,
                    modifier  = Modifier.fillMaxWidth().height(btnHeight),
                    shape     = btnShape,
                    colors    = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Пройти снова", fontWeight = FontWeight.Bold,
                        fontSize = if (isCompactWidth) 14.sp else 15.sp)
                }
            } else {
                Text(
                    "В режиме вызова повторное прохождение недоступно.",
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            if (!challengeId.isNullOrBlank() && onOpenChallengeSummary != null) {
                Button(
                    onClick   = onOpenChallengeSummary,
                    modifier  = Modifier.fillMaxWidth().height(btnHeight),
                    shape     = btnShape,
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = LocalBrainRacerExtendedColors.current.detailGreen),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Итоги вызова", fontWeight = FontWeight.Bold,
                        fontSize = if (isCompactWidth) 14.sp else 15.sp)
                }
            }

            if (uiState.reviewQuestions.isNotEmpty()) {
                OutlinedButton(
                    onClick  = onShowReview,
                    modifier = Modifier.fillMaxWidth().height(btnHeight),
                    shape    = btnShape,
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = LocalBrainRacerExtendedColors.current.detailGreen),
                    border   = androidx.compose.foundation.BorderStroke(
                        1.5.dp, LocalBrainRacerExtendedColors.current.detailGreen)
                ) {
                    Icon(Icons.Default.Checklist, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Разбор ответов", fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompactWidth) 14.sp else 15.sp)
                }
            }

            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(if (isCompactWidth) 46.dp else 50.dp),
                shape    = btnShape,
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.Home, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("К описанию", fontSize = if (isCompactWidth) 13.sp else 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// Баннер повышения уровня
@Composable
private fun LevelUpBanner(newLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = LocalBrainRacerExtendedColors.current.difficultyExpert.copy(alpha = 0.12f)),
        border   = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape)
                .background(LocalBrainRacerExtendedColors.current.difficultyExpert.copy(0.20f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowUpward, null,
                    tint = LocalBrainRacerExtendedColors.current.difficultyExpert, modifier = Modifier.size(24.dp))
            }
            Column {
                Text("🎉 Новый уровень!", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = LocalBrainRacerExtendedColors.current.difficultyExpert)
                Text("Вы достигли уровня $newLevel. Продолжайте в том же духе!",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}

// Элемент статистики в результатах
@Composable
private fun ResultStat(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector,
    compact: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(if (compact) 34.dp else 40.dp).clip(CircleShape)
            .background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
        }
        Text(value, fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 20.sp else 24.sp, color = color)
        Text(label, fontSize = if (compact) 10.sp else 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Карточка с детализацией заработанного опыта
@Composable
private fun XpCard(
    uiState: QuizUIState,
    xpProgressAnimated: Float,
    isReferenceOnly: Boolean = false,
    duelXpDeferred: Boolean  = false,
    compact: Boolean         = false
) {
    val breakdown    = uiState.xpBreakdown
    val currentLevel = uiState.newLevel

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (compact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(when {
                        isReferenceOnly -> "XP (справочно)"
                        duelXpDeferred  -> "Опыт за дуэль"
                        else            -> "Заработано XP"
                    }, fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 15.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    when {
                        isReferenceOnly -> Text("Не сохраняется в профиль",
                            fontSize = if (compact) 11.sp else 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        duelXpDeferred  -> Text(
                            "В профиль начисляется только победителю после завершения вызова.",
                            fontSize = if (compact) 11.sp else 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Text(if (duelXpDeferred) "—" else "+${uiState.xpEarned} XP",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize   = if (compact) 13.sp else 14.sp,
                        color      = MaterialTheme.colorScheme.primary)
                }
            }

            if (breakdown != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    XpRow("Базовые", breakdown.baseXp, MaterialTheme.colorScheme.onSurfaceVariant, compact)
                    if (breakdown.speedBonusXp > 0)
                        XpRow("Бонус скорости ⚡", breakdown.speedBonusXp, LocalBrainRacerExtendedColors.current.detailGreen, compact)
                    if (breakdown.accuracyBonusXp > 0)
                        XpRow("Бонус точности 🎯", breakdown.accuracyBonusXp, BrainRacerColorTokens.QuizXpBonusAccent, compact)
                    if (!breakdown.difficultyLabel.startsWith("×1.0")) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Множитель сложности", fontSize = if (compact) 12.sp else 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(breakdown.difficultyLabel, fontSize = if (compact) 12.sp else 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = LocalBrainRacerExtendedColors.current.difficultyExpert)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Уровень $currentLevel", fontSize = if (compact) 12.sp else 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (currentLevel < LevelSystem.MAX_LEVEL) "Уровень ${currentLevel + 1}" else "Максимум",
                        fontSize = if (compact) 12.sp else 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress   = { xpProgressAnimated },
                    modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                    strokeCap  = StrokeCap.Round
                )
                Text("Ур. $currentLevel  ·  ${LevelSystem.rankForLevel(currentLevel).displayName}",
                    fontSize  = if (compact) 11.sp else 12.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
            }
        }
    }
}

// Строка детализации опыта
@Composable
private fun XpRow(label: String, value: Int, color: Color, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (compact) 12.sp else 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("+$value XP", fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium, color = color)
    }
}

// Экран разбора ответов после викторины
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerReviewScreen(uiState: QuizUIState, onBack: () -> Unit) {
    val questions      = uiState.reviewQuestions
    val answers        = uiState.reviewAnswers
    val correctCount   = answers.count { it.isCorrect }
    val incorrectCount = answers.count { !it.isCorrect }

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Разбор ответов", color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${questions.size} вопросов  ·  $correctCount правильно  ·  $incorrectCount неверно",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = com.example.brainracer.R.drawable.arrow_back_btn),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(questions) { i, question ->
                ReviewQuestionCard(index = i, question = question, answer = answers.getOrNull(i))
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// Карточка одного вопроса в разборе
@Composable
private fun ReviewQuestionCard(
    index: Int,
    question: com.example.brainracer.domain.entities.Question,
    answer: com.example.brainracer.domain.entities.UserAnswer?
) {
    val isCorrect   = answer?.isCorrect == true
    val isTimeout   = answer == null || answer.selectedAnswerIndex == -1
    val accentColor = when {
        isTimeout -> LocalBrainRacerExtendedColors.current.statusOrange
        isCorrect -> LocalBrainRacerExtendedColors.current.detailGreen
        else      -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when { isTimeout -> "Время вышло"; isCorrect -> "Правильно"; else -> "Неверно" }
    val statusIcon  = when { isTimeout -> Icons.Default.Timer; isCorrect -> Icons.Default.CheckCircle; else -> Icons.Default.Cancel }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                    Text("Вопрос ${index + 1}",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 12.sp, fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(statusIcon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                    if (answer != null && answer.timeSpent > 0) {
                        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${answer.timeSpent}с", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(question.questionText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)

            val letters = listOf("A", "B", "C", "D", "E")
            question.options.forEachIndexed { optIdx, optText ->
                val isUserChoice  = answer?.selectedAnswerIndex == optIdx
                val isRightAnswer = question.correctAnswerIndex == optIdx

                val (bg, border, text, icon) = when {
                    isRightAnswer && isUserChoice ->
                        Quad(LocalBrainRacerExtendedColors.current.detailGreen.copy(.15f),
                            LocalBrainRacerExtendedColors.current.detailGreen,
                            LocalBrainRacerExtendedColors.current.detailGreen,
                            Icons.Default.CheckCircle)
                    isRightAnswer ->
                        Quad(LocalBrainRacerExtendedColors.current.detailGreen.copy(.08f),
                            LocalBrainRacerExtendedColors.current.detailGreen.copy(.6f),
                            LocalBrainRacerExtendedColors.current.detailGreen, null)
                    isUserChoice ->
                        Quad(MaterialTheme.colorScheme.error.copy(.15f),
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error,
                            Icons.Default.Cancel)
                    else ->
                        Quad(MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.outline,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(.4f), null)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(border))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(24.dp).clip(CircleShape).background(border.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center) {
                        Text(letters.getOrElse(optIdx) { "${optIdx + 1}" },
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = border)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(optText, fontSize = 14.sp, color = text,
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                    if (icon != null) {
                        Icon(icon, null, tint = border, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Spacer(Modifier.width(10.dp))
                    }
                }
            }

            val explanation = question.explanation
            if (!explanation.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .5f))
                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lightbulb, null,
                        tint = LocalBrainRacerExtendedColors.current.difficultyExpert,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Text(explanation, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
                }
            }
        }
    }
}

// Вспомогательная структура для группировки 4 значений
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)