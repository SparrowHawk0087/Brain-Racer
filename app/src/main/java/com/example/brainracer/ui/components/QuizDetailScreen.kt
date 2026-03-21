package com.example.brainracer.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.domain.entities.Quiz
import com.example.brainracer.domain.entities.QuizDifficulty
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// ─── Палитра (совпадает с HomeScreen) ──────────────────────────────────────
private val DbBg       = Color(0xFF12111A)
private val DbCard     = Color(0xFF1E1D2B)
private val DbCardAlt  = Color(0xFF25243A)
private val DbPurple   = Color(0xFF7C6FCD)
private val DbBlue     = Color(0xFF4F9CF9)
private val DbGreen    = Color(0xFF3ECFA3)
private val DbOrange   = Color(0xFFF97B3E)
private val DbTextPri  = Color(0xFFF0EFFF)
private val DbTextSec  = Color(0xFF8B8AAE)

private fun difficultyColor(d: QuizDifficulty): Color = when (d) {
    QuizDifficulty.EASY   -> Color(0xFF3ECFA3)
    QuizDifficulty.MEDIUM -> Color(0xFF4F9CF9)
    QuizDifficulty.HARD   -> Color(0xFFF97B3E)
    QuizDifficulty.EXPERT -> Color(0xFFEA5C7E)
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
    onStartQuiz: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepositoryImpl() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showFriendDialog by remember { mutableStateOf(false) }

    // Загружаем квиз
    LaunchedEffect(quizId) {
        coroutineScope.launch {
            val result = quizRepository.getQuiz(quizId)
            quiz = result.let {
                when (it) {
                    is com.example.brainracer.data.utils.Result.Success -> it.data
                    else -> null
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        containerColor = DbBg,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DbCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = DbTextPri,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DbBg),
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DbPurple)
                }
            }
            quiz == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Викторина не найдена", color = DbTextSec)
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
                            quizTitle = q.title,
                            onStart = { onStartQuiz(quizId) },
                            onChallenge = { showFriendDialog = true },
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

    // ── Диалог выбора друга для вызова ───────────────────────────────────────
    if (showFriendDialog) {
        AlertDialog(
            onDismissRequest = { showFriendDialog = false },
            containerColor = DbCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Бросить вызов",
                    color = DbTextPri,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Выберите друга, которому хотите бросить вызов в викторине «${quiz?.title}».\n\nФункция доступна на экране «Друзья» → выберите друга → «Вызвать».",
                    color = DbTextSec,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFriendDialog = false
                    navController.navigate("friends/$currentUserId")
                }) {
                    Text("Перейти к друзьям", color = DbPurple, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFriendDialog = false }) {
                    Text("Отмена", color = DbTextSec)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Обложка квиза
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizCoverSection(quiz: Quiz) {
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF6C63FF), Color(0xFF3ECFA3))
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
            color = DbTextPri,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        )

        Spacer(Modifier.height(14.dp))

        // Бейджи
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBadge(
                label = difficultyLabel(quiz.difficulty),
                color = difficultyColor(quiz.difficulty)
            )
            InfoBadge(
                label = "${quiz.questions.size} вопросов",
                color = DbBlue
            )
            InfoBadge(
                label = "${quiz.timePerQuestion}с/вопрос",
                color = DbOrange
            )
        }

        Spacer(Modifier.height(18.dp))

        // Разделитель
        HorizontalDivider(color = DbCardAlt, thickness = 1.dp)

        Spacer(Modifier.height(16.dp))

        // Мини-стата в строку
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.People,
                value = quiz.stats.timesTaken.toString(),
                label = "сыграно"
            )
            StatItem(
                icon = Icons.Default.Star,
                value = "%.1f".format(quiz.stats.averageRating),
                label = "рейтинг"
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
        Icon(icon, contentDescription = null, tint = DbPurple, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = DbTextPri, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = DbTextSec, fontSize = 11.sp)
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
            color = DbTextPri,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DbCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                description,
                color = DbTextSec,
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
    quizTitle: String,
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
        // Начать викторину
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DbPurple
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Начать викторину",
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
                contentColor = DbGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DbGreen)
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
                contentColor = DbTextSec
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, DbCardAlt)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Поделиться",
                fontSize = 14.sp
            )
        }
    }
}