package com.example.brainracer.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.brainracer.data.local.QuizOfflineCache
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.repositories.UserRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import com.example.brainracer.ui.theme.BrainRacerExtendedColors
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private fun difficultyColor(d: QuizDifficulty, ext: BrainRacerExtendedColors, error: Color): Color = when (d) {
    QuizDifficulty.EASY   -> ext.detailGreen
    QuizDifficulty.MEDIUM -> ext.detailBlue
    QuizDifficulty.HARD   -> ext.detailOrange
    QuizDifficulty.EXPERT -> error
}

private fun difficultyLabel(d: QuizDifficulty): String = when (d) {
    QuizDifficulty.EASY   -> "Лёгкий"
    QuizDifficulty.MEDIUM -> "Средний"
    QuizDifficulty.HARD   -> "Сложный"
    QuizDifficulty.EXPERT -> "Эксперт"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    quizId: String,
    navController: NavController,
    onNavigateToPlay: (quizId: String, practiceReplay: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepositoryImpl() }
    val userRepository = remember { UserRepositoryImpl() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var soloAlreadyCompleted by remember { mutableStateOf(false) }
    var soloCheckDone by remember { mutableStateOf(false) }
    var mySavedPlayCount by remember { mutableIntStateOf(0) }
    var showDeleteQuizDialog by remember { mutableStateOf(false) }
    var isDeletingQuiz by remember { mutableStateOf(false) }

    // Загружаем квиз
    LaunchedEffect(quizId) {
        coroutineScope.launch {
            val result = quizRepository.getQuiz(quizId)
            quiz = result.let {
                when (it) {
                    is com.example.brainracer.data.utils.Result.Success -> {
                        QuizOfflineCache.save(it.data)
                        it.data
                    }
                    else -> null
                }
            }
            isLoading = false
        }
    }

    val latestQuizId by rememberUpdatedState(quizId)
    val latestUserId by rememberUpdatedState(currentUserId)

    LaunchedEffect(quizId, currentUserId) {
        if (currentUserId.isBlank()) {
            soloAlreadyCompleted = false
            soloCheckDone = true
            mySavedPlayCount = 0
            return@LaunchedEffect
        }
        when (val u = userRepository.getUser(currentUserId)) {
            is Result.Success -> {
                soloAlreadyCompleted = quizId in u.data.stats.soloCompletedQuizIds
                soloCheckDone = true
            }
            is Result.Error -> {
                soloAlreadyCompleted = false
                soloCheckDone = true
            }
        }
        when (val c = quizRepository.getUserQuizPlayCount(currentUserId, quizId)) {
            is Result.Success -> mySavedPlayCount = c.data
            is Result.Error -> mySavedPlayCount = 0
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    DisposableEffect(backStackEntry) {
        val lifecycle = backStackEntry?.lifecycle ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            coroutineScope.launch {
                val uid = latestUserId
                val qid = latestQuizId
                if (uid.isBlank()) {
                    mySavedPlayCount = 0
                    return@launch
                }
                when (val c = quizRepository.getUserQuizPlayCount(uid, qid)) {
                    is Result.Success -> mySavedPlayCount = c.data
                    is Result.Error -> Unit
                }
                when (val u = userRepository.getUser(uid)) {
                    is Result.Success -> soloAlreadyCompleted = qid in u.data.stats.soloCompletedQuizIds
                    else -> Unit
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val isQuizOwner = currentUserId.isNotBlank() && quiz?.createdBy == currentUserId

    if (showDeleteQuizDialog && quiz != null) {
        val qDel = quiz!!
        AlertDialog(
            onDismissRequest = { if (!isDeletingQuiz) showDeleteQuizDialog = false },
            title = { Text("Удалить викторину?") },
            text = {
                Text(
                    "«${qDel.title}» будет удалена безвозвратно.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            isDeletingQuiz = true
                            when (val r = quizRepository.deleteQuiz(qDel.id)) {
                                is Result.Success -> {
                                    QuizOfflineCache.remove(qDel.id)
                                    ProfileAfterQuizRefresh.notify(currentUserId)
                                    Toast.makeText(context, "Викторина удалена", Toast.LENGTH_SHORT).show()
                                    showDeleteQuizDialog = false
                                    navController.popBackStack()
                                }
                                is Result.Error -> {
                                    Toast.makeText(
                                        context,
                                        r.exception.message ?: "Не удалось удалить",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            isDeletingQuiz = false
                        }
                    },
                    enabled = !isDeletingQuiz,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeletingQuiz) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Удалить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteQuizDialog = false },
                    enabled = !isDeletingQuiz
                ) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        containerColor = LocalBrainRacerExtendedColors.current.detailBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LocalBrainRacerExtendedColors.current.detailSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = LocalBrainRacerExtendedColors.current.detailTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalBrainRacerExtendedColors.current.detailBackground),
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalBrainRacerExtendedColors.current.detailAccentPurple)
                }
            }
            quiz == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Викторина не найдена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                val q = quiz!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ── Обложка ──────────────────────────────────────────────
                    item {
                        QuizCoverSection(quiz = q)
                    }

                    // ── Информация ────────────────────────────────────────────
                    item {
                        QuizInfoSection(quiz = q)
                    }

                    // ── Описание ──────────────────────────────────────────────
                    item {
                        QuizDescriptionSection(description = q.description)
                    }

                    // ── Кнопки действий ──────────────────────────────────────
                    item {
                        QuizActionsSection(
                            mySavedPlayCount = mySavedPlayCount,
                            signedIn = currentUserId.isNotBlank(),
                            soloAlreadyCompleted = soloCheckDone && soloAlreadyCompleted,
                            isOwner = isQuizOwner,
                            onRequestDelete = { showDeleteQuizDialog = true },
                            onStart = {
                                val replay = soloCheckDone && soloAlreadyCompleted
                                onNavigateToPlay(quizId, replay)
                            },
                            onChallenge = {
                                if (currentUserId.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Войдите в аккаунт, чтобы бросать вызовы",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    navController.navigate(
                                        "friends/$currentUserId?preselectQuizId=${Uri.encode(quizId)}"
                                    )
                                }
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Brain Racer — викторина")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Проверь себя в викторине «${q.title}» в Brain Racer!"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Поделиться викториной")
                                )
                            }
                        )
                    }
                }
            }
        }
    }

}

// ─────────────────────────────────────────────────────────────────────────────
//  Обложка квиза
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizCoverSection(quiz: Quiz) {
    val gradient = Brush.verticalGradient(
        listOf(BrainRacerColorTokens.DetailHeroGradientStart, BrainRacerColorTokens.DetailGreen)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        // Декоративные круги
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-40).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .offset(x = 70.dp, y = 50.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Иконка викторины
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Text(
                    quiz.categoryId,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Блок с названием и метаданными
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizInfoSection(quiz: Quiz) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            quiz.title,
            color = LocalBrainRacerExtendedColors.current.detailTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        )

        if (quiz.id.startsWith("quiz_custom_") && quiz.creatorNickname.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Автор: ${quiz.creatorNickname}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(14.dp))

        // Бейджи
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBadge(
                label = difficultyLabel(quiz.difficulty),
                color = difficultyColor(
                    quiz.difficulty,
                    LocalBrainRacerExtendedColors.current,
                    MaterialTheme.colorScheme.error
                )
            )
            InfoBadge(
                label = "${quiz.questions.size} вопросов",
                color = LocalBrainRacerExtendedColors.current.detailBlue
            )
            InfoBadge(
                label = "${quiz.timePerQuestion}с/вопрос",
                color = LocalBrainRacerExtendedColors.current.detailOrange
            )
        }

        Spacer(Modifier.height(18.dp))

        // Разделитель
        HorizontalDivider(color = LocalBrainRacerExtendedColors.current.detailSurfaceAlt, thickness = 1.dp)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.People,
                value = quiz.stats.timesTaken.toString(),
                label = "всего"
            )
            StatItem(
                icon = Icons.Default.Timer,
                value = "${quiz.questions.size * quiz.timePerQuestion}с",
                label = "время"
            )
        }
    }
}

@Composable
private fun InfoBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = LocalBrainRacerExtendedColors.current.detailAccentPurple, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = LocalBrainRacerExtendedColors.current.detailTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Описание
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizDescriptionSection(description: String) {
    if (description.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Описание",
            color = LocalBrainRacerExtendedColors.current.detailTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LocalBrainRacerExtendedColors.current.detailSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Кнопки действий
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizActionsSection(
    mySavedPlayCount: Int,
    signedIn: Boolean,
    soloAlreadyCompleted: Boolean,
    isOwner: Boolean,
    onRequestDelete: () -> Unit,
    onStart: () -> Unit,
    onChallenge: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (soloAlreadyCompleted) {
            Text(
                "Вы уже прошли эту викторину в соло. «Пройти снова» — для тренировки: опыт и статистика не начисляются. Вызов другу — как обычно.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            if (signedIn) "Сыграно вами раз: $mySavedPlayCount"
            else "Сыграно вами раз: войдите в аккаунт",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        // Начать викторину
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalBrainRacerExtendedColors.current.detailAccentPurple
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (soloAlreadyCompleted) "Пройти снова" else "Начать викторину",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Бросить вызов другу
        OutlinedButton(
            onClick = onChallenge,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LocalBrainRacerExtendedColors.current.detailGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, LocalBrainRacerExtendedColors.current.detailGreen)
        ) {
            Icon(Icons.Default.Sports, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Бросить вызов другу",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // Поделиться
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, LocalBrainRacerExtendedColors.current.detailSurfaceAlt)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Поделиться",
                fontSize = 14.sp
            )
        }

        if (isOwner) {
            OutlinedButton(
                onClick = onRequestDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Удалить викторину", fontSize = 14.sp)
            }
        }
    }
}