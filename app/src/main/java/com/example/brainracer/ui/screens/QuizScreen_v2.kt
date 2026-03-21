package com.example.brainracer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainracer.ui.theme.onBackgroundLight
import com.example.brainracer.ui.theme.onSurfaceVariantLight
import com.example.brainracer.ui.theme.primaryLight
import com.example.brainracer.ui.theme.surfaceContainerLight
import com.example.brainracer.ui.theme.surfaceContainerLowLight
import com.example.brainracer.ui.theme.surfaceLight
import kotlinx.coroutines.delay


data class QuizQuestionNew(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreenNew(
    topic: String = "Математика",
    onBack: () -> Unit = {}
) {
    val questions = remember {
        listOf(
            QuizQuestionNew(
                "Сколько будет 7 × 8?",
                listOf("54", "56", "64", "48"),
                1
            ),
            QuizQuestionNew(
                "Чему равно π (пи) приблизительно?",
                listOf("2.14", "3.14", "4.14", "1.14"),
                1
            ),
            QuizQuestionNew(
                "Квадратный корень из 144?",
                listOf("11", "14", "12", "13"),
                2
            ),
            QuizQuestionNew(
                "Сколько градусов в прямом угле?",
                listOf("45°", "180°", "90°", "60°"),
                2
            ),
            QuizQuestionNew(
                "Чему равно 2 в степени 10?",
                listOf("512", "1024", "2048", "256"),
                1
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(20) }
    var isFinished by remember { mutableStateOf(false) }

    val totalQuestions = questions.size
    val currentQuestion = questions[currentIndex]

    val progressAnimated by animateFloatAsState(
        targetValue = (currentIndex + 1f) / totalQuestions,
        animationSpec = tween(500),
        label = "progress"
    )

    val timerProgress by animateFloatAsState(
        targetValue = timeLeft / 20f,
        animationSpec = tween(300),
        label = "timer"
    )

    LaunchedEffect(currentIndex, isFinished) {
        if (isFinished) return@LaunchedEffect
        timeLeft = 20
        while (timeLeft > 0 && selectedOption == null) {
            delay(1000)
            timeLeft--
        }
        if (selectedOption == null && !isFinished) {
            if (currentIndex < totalQuestions - 1) {
                currentIndex++
                selectedOption = null
            } else {
                isFinished = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = surfaceContainerLowLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = onBackgroundLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = onSurfaceVariantLight)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFA000))
                        Text(
                            "$score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = onBackgroundLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceContainerLowLight)
            )
        }
    ) { padding ->
        if (isFinished) {
            QuizResultSectionNew(
                score = score,
                total = totalQuestions,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                QuizProgressBarNew(
                    current = currentIndex + 1,
                    total = totalQuestions,
                    progress = progressAnimated
                )

                QuizTimerNew(timeLeft = timeLeft, timerProgress = timerProgress)

                QuizQuestionCardNew(question = currentQuestion.question)

                QuizOptionsNew(
                    options = currentQuestion.options,
                    selectedOption = selectedOption,
                    correctIndex = currentQuestion.correctIndex,
                    onOptionSelected = { index ->
                        if (selectedOption == null) {
                            selectedOption = index
                            if (index == currentQuestion.correctIndex) {
                                score += 100 + (timeLeft * 5)
                            }
                        }
                    }
                )

                if (selectedOption != null) {
                    NextButtonNew(
                        isLast = currentIndex == totalQuestions - 1,
                        onClick = {
                            if (currentIndex < totalQuestions - 1) {
                                currentIndex++
                                selectedOption = null
                            } else {
                                isFinished = true
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun QuizProgressBarNew(current: Int, total: Int, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Вопрос $current из $total", fontSize = 13.sp, color = onSurfaceVariantLight)
            Text("${(progress * 100).toInt()}%", fontSize = 13.sp, color = primaryLight, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = primaryLight,
            trackColor = surfaceContainerLight,
            strokeCap = StrokeCap.Round
        )
    }
}


@Composable
fun QuizTimerNew(timeLeft: Int, timerProgress: Float) {
    val timerColor = when {
        timeLeft > 10 -> Color(0xFF388E3C)
        timeLeft > 5  -> Color(0xFFFF8F00)
        else          -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(timerColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, null, Modifier.size(18.dp), tint = timerColor)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                LinearProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = timerColor,
                    trackColor = surfaceContainerLight,
                    strokeCap = StrokeCap.Round
                )
            }
            Text(
                "$timeLeft с",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = timerColor
            )
        }
    }
}


@Composable
fun QuizQuestionCardNew(question: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = onBackgroundLight,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}


@Composable
fun QuizOptionsNew(
    options: List<String>,
    selectedOption: Int?,
    correctIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, option ->
            val bgColor: Color
            val borderColor: Color
            val textColor: Color

            when {
                selectedOption == null -> {
                    bgColor = surfaceLight
                    borderColor = surfaceContainerLight
                    textColor = onBackgroundLight
                }
                index == correctIndex -> {
                    bgColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
                    borderColor = Color(0xFF4CAF50)
                    textColor = Color(0xFF2E7D32)
                }
                index == selectedOption -> {
                    bgColor = Color(0xFFE57373).copy(alpha = 0.12f)
                    borderColor = Color(0xFFE57373)
                    textColor = Color(0xFFC62828)
                }
                else -> {
                    bgColor = surfaceLight
                    borderColor = surfaceContainerLight
                    textColor = onSurfaceVariantLight.copy(alpha = 0.5f)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = selectedOption == null) { onOptionSelected(index) }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = listOf("A", "B", "C", "D")[index],
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedOption == null) onSurfaceVariantLight else borderColor
                        )
                    }
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}


@Composable
fun NextButtonNew(isLast: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(primaryLight)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLast) "Завершить" else "Следующий вопрос →",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}


@Composable
fun QuizResultSectionNew(score: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(primaryLight.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, null, Modifier.size(48.dp), tint = primaryLight)
        }

        Spacer(Modifier.height(24.dp))

        Text("Викторина завершена!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = onBackgroundLight)

        Spacer(Modifier.height(8.dp))

        Text("Ваши очки", fontSize = 14.sp, color = onSurfaceVariantLight)

        Text(
            "$score",
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = primaryLight
        )

        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceLight),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$total", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onBackgroundLight)
                    Text("Вопросов", fontSize = 12.sp, color = onSurfaceVariantLight)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${score / if (total > 0) total else 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = primaryLight
                    )
                    Text("Очков/вопрос", fontSize = 12.sp, color = onSurfaceVariantLight)
                }
            }
        }
    }
}


@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun QuizScreenNewPreview() {
    MaterialTheme {
        QuizScreenNew(topic = "Математика")
    }
}