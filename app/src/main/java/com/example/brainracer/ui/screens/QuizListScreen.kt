package com.example.brainracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Quiz
import com.example.brainracer.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.bottomBarOcclusionBlockClicks
import com.example.brainracer.ui.components.bottomBarOcclusionEffect
import com.example.brainracer.ui.components.bottomBarSafePadding
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.customAuthorCaption
import com.example.brainracer.ui.utils.toQuizItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    navController: NavController,
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "quizzes"
) {
    val repo = remember { QuizRepositoryImpl() }
    var quizzes by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val quizListState = rememberLazyListState()
    val bottomReflexShift by remember {
        derivedStateOf {
            ((quizListState.firstVisibleItemIndex * 4f) +
                    quizListState.firstVisibleItemScrollOffset * 0.012f).coerceIn(0f, 18f)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            when (val r = repo.getPopularQuizzes(limit = 80)) {
                is Result.Success -> {
                    quizzes = r.data.map { it.toQuizItem() }
                    error = null
                }
                is Result.Error -> {
                    error = r.exception.message ?: "Ошибка загрузки"
                    quizzes = emptyList()
                }
            }
            loading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Викторины",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("search?category=Все&customOnly=false") }) {
                        Icon(painter = painterResource(id = R.drawable.search_btn), contentDescription = "Поиск", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomBar(
                showBar = true,
                currentRoute = currentRoute,
                onHomeClick = onHomeClick,
                onLeaderboardClick = onLeaderboardClick,
                onChallengesClick = onChallengesClick,
                onQuizzesClick = onQuizzesClick,
                onProfileClick = onProfileClick,
                reflexShift = bottomReflexShift
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
                    }
                }
                quizzes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Пока нет викторин", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
                else -> {
                    val quizListBottomInset = bottomBarSafePadding(padding)
                    LazyColumn(
                        state = quizListState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = quizListBottomInset),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(quizzes, key = { _, q -> q.id }) { index, quiz ->
                            QuizListRowCard(
                                quiz = quiz,
                                colorIndex = index,
                                onClick = { navController.navigate("quiz_detail/${quiz.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizListRowCard(quiz: QuizItem, colorIndex: Int, onClick: () -> Unit) {
    val cardGradients = LocalBrainRacerExtendedColors.current.cardGradients
    val gradient = cardGradients[colorIndex % cardGradients.size]
    Box(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(gradient, Offset.Zero, Offset(400f, 400f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quiz.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(quiz.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${quiz.questionCount} вопр.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                quiz.customAuthorCaption()?.let { cap ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        cap,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
