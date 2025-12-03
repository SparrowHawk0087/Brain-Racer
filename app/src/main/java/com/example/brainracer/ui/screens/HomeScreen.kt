package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.brainracer.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.data.utils.QuizDataSeeder
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            homeViewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadQuizzes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brain Racer") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout),
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    QuizDataSeeder.seedQuizzes()
                    Toast.makeText(context, "Добавляем викторины...", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Add quizzes"
                )
            }
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Text(
                "Hello, ${uiState.userName}!",
                    modifier = Modifier.padding(8.dp)
            )



            // Категории
            CategoryFilter(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    if (category == "Все") {
                        homeViewModel.loadQuizzes()
                    } else {
                        homeViewModel.loadQuizzesByCategory(category)
                    }
                }
            )

            // Поиск
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { query -> homeViewModel.searchQuizzes(query) },
                onSearch = { searchActive = false },
                active = searchActive,
                onActiveChange = { searchActive = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск викторин...") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { homeViewModel.clearSearch() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = "Clear"
                            )
                        }
                    }
                }
            ) {
                // Результаты поиска при активации
                if (uiState.searchResults.isNotEmpty()) {
                    Column {
                        uiState.searchResults.forEach { quiz ->
                            Text(
                                text = quiz.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("quiz/${quiz.id}")
                                        searchActive = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Список викторин
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            } else if (uiState.errorMessage != null) {
                ErrorMessageInfo(
                    message = uiState.errorMessage!!,
                    onRetry = { homeViewModel.loadQuizzes() }
                )
            } else {
                val quizzesToShow = if (uiState.searchQuery.isNotEmpty() && !searchActive) {
                    uiState.searchResults
                } else {
                    uiState.quizzes
                }

                QuizGrid(
                    quizzes = quizzesToShow,
                    onQuizClick = { quizId ->
                        navController.navigate("quiz/$quizId")
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                onClick = { onCategorySelected(category) },
                selected = category == selectedCategory,
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun ErrorMessageInfo(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun QuizGrid(
    quizzes: List<QuizItem>,
    onQuizClick: (String) -> Unit
) {
    if (quizzes.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Викторины не найдены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Нажмите ➕ чтобы добавить демо-викторины",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quizzes) { quiz ->
                QuizCard(
                    quiz = quiz,
                    onQuizClick = { onQuizClick(quiz.id) }
                )
            }
        }
    }
}

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
                            contentDescription = "Rating",
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
    /*Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {


            uiState.userStats.let { stats ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Quizzes taken: ${stats.totalQuizzesTaken}")
                Text("Total points: ${stats.totalPoints}")
                Text("Current streak: ${stats.currentStreak} days")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate("quizzes")
            }) {
                Text("Start Quiz")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate("profile/$userId")
            }) {
                Text("Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                authViewModel.signOut()
            }) {
                Text("Sign Out")
            }
        }
    }
}*/