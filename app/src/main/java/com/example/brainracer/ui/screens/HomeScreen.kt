package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.R
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    onHomeClick: () -> Unit = {},  // Добавлено
    onProfileClick: () -> Unit = {}, // Добавлено
    currentRoute: String = "home"    // Добавлено
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchActive by remember { mutableStateOf(false) }

    // Отладочный вывод
    LaunchedEffect(uiState.userName) {
        println("DEBUG HomeScreen: userName = '${uiState.userName}', quizzes count = ${uiState.quizzes.size}")
    }

    // Показываем Toast при ошибках
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brain Racer") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout),
                            contentDescription = "Выйти"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Передаём обработчики кликов и текущий маршрут
            BottomBar(
                showBar = true,
                currentRoute = currentRoute,
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Добавляем демо-викторины через ViewModel
                    homeViewModel.addDemoQuizzes()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Добавить демо-викторины"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Приветствие пользователя
            Text(
                text = "Привет, ${uiState.userName.ifBlank { "Гость" }}!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Категории
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categories.forEach { category ->
                    FilterChip(
                        onClick = {
                            homeViewModel.loadQuizzesByCategory(category)
                        },
                        selected = category == uiState.selectedCategory,
                        label = { Text(category) }
                    )
                }
            }


            // Содержимое экрана
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Загружаем викторины...")
                        }
                    }
                }
                uiState.quizzes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Викторины не найдены",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите ➕ чтобы добавить демо-викторины",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { homeViewModel.loadQuizzes() } // Исправлено с loadQuizzesSimple на loadQuizzes
                        ) {
                            Text("Обновить")
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.quizzes) { quiz ->
                            QuizCard(
                                quiz = quiz,
                                onQuizClick = {
                                    /*navController.navigate("quiz/${quiz.id}")*/
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// QuizCard остается без изменений
@Composable
fun QuizCard(
    quiz: QuizItem,
    onQuizClick: () -> Unit
) {
    Card(
        onClick = onQuizClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Заголовок и категория
            Column {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = quiz.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }


            // Информация о викторине
            Column {
                // Количество вопросов и сложность
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${quiz.questionCount} вопросов",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Text(
                        text = when (quiz.difficulty) {
                            "EASY" -> "Легко"
                            "MEDIUM" -> "Средне"
                            "HARD" -> "Сложно"
                            "EXPERT" -> "Эксперт"
                            else -> quiz.difficulty
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (quiz.difficulty) {
                            "EASY" -> Color.Green
                            "MEDIUM" -> Color(0xFFFFA500)
                            "HARD" -> Color.Red
                            "EXPERT" -> Color(0xFF8B00FF)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Рейтинг и количество игр
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Рейтинг
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.kid_star),
                            contentDescription = "Рейтинг",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", quiz.rating),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Количество игр
                    Text(
                        text = "${quiz.playCount} игр",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
