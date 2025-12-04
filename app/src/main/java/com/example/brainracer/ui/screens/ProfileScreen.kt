package com.example.brainracer.ui.screens

import android.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    userId: String,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "profile",
    isOwnProfile: Boolean = true // Новый параметр для определения чужой/свой профиль
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Демо-данные для статистики
    val totalQuizzes = remember { 42 }
    val winPercentage = remember { 78 }
    val averageScore = remember { 4.3 }
    val userLevel = remember { 7 }
    val nextLevelProgress = remember { 0.65f }


    // История последних игр
    val recentGames = remember {
        listOf(
            RecentGame("Математика", "Победа", 4.5f, "2 часа назад"),
            RecentGame("История", "Поражение", 3.2f, "1 день назад"),
            RecentGame("Программирование", "Победа", 4.8f, "3 дня назад"),
            RecentGame("География", "Победа", 4.1f, "5 дней назад")
        )
    }

    // Статистика по темам
    val topicStats = remember {
        listOf(
            TopicStat("Математика", 85f, Color(0xFF4CAF50)),
            TopicStat("История", 65f, Color(0xFF2196F3)),
            TopicStat("Программирование", 92f, Color(0xFF9C27B0)),
            TopicStat("География", 78f, Color(0xFFFF9800)),
            TopicStat("Биология", 54f, Color(0xFFE91E63))
        )
    }

    // Достижения
    val achievements = remember {
        listOf(
            Achievement("Новичок", "Пройдите первую викторину", true),
            Achievement("Эксперт", "Пройдите 10 викторин", true),
            Achievement("Непобедимый", "Выиграйте 5 игр подряд", false),
            Achievement("Мастер тем", "Получите 90%+ в 3 темах", true),
            Achievement("Марафонец", "Играйте 7 дней подряд", false)
        )
    }

    // Если пользователь удален или вышел
    LaunchedEffect(user) {
        if (user == null) {
            Toast.makeText(context, "Session expired or account deleted", Toast.LENGTH_SHORT).show()
            onNavigateToAuth()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOwnProfile) "Мой профиль" else "Профиль игрока",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.inverseSurface
                    )
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = {
                            // Навигация на экран редактирования профиля
                            // navController.navigate("editProfile")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать профиль"
                            )
                        }
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
            )
        },
        bottomBar = {
            BottomBar(
                showBar = true,
                currentRoute = currentRoute,
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            if (!isOwnProfile) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // Вызов на дуэль
                        Toast.makeText(context, "Вызов на дуэль отправлен!", Toast.LENGTH_SHORT).show()
                    },
                    icon = {
                        Icon(Icons.Default.Sports, contentDescription = null)
                    },
                    text = { Text("Вызвать на дуэль") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.secondary
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок профиля
            item {
                ProfileHeader(
                    userName = user?.displayName ?: "User $userId",
                    userEmail = user?.email ?: "",
                    userLevel = userLevel,
                    progress = nextLevelProgress,
                    isOwnProfile = isOwnProfile
                )
            }

            // Основная статистика
            item {
                Text(
                    text = "Общая статистика",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }


            item {
                MainStats(
                    totalQuizzes = totalQuizzes,
                    winPercentage = winPercentage,
                    averageScore = averageScore
                )
            }

            // Статистика по темам
            item {
                Text(
                    text = "Статистика по темам",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                TopicStatsSection(topicStats = topicStats)
            }

            // История игр
            item {
                Text(
                    text = "Последние игры",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(recentGames) { game ->
                RecentGameItem(game = game)
            }

            // Достижения
            item {
                Text(
                    text = "Достижения",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                AchievementsSection(achievements = achievements)
            }

            // Действия для своего профиля
            if (isOwnProfile) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Кнопка выхода
                        OutlinedButton(
                            onClick = {
                                authViewModel.signOut()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выйти из аккаунта")
                        }

                        // Кнопка удаления аккаунта
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Удалить аккаунт")
                        }
                    }
                }
            }

            // Отступ внизу
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }


    // Диалог подтверждения удаления аккаунта
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Это действие невозможно отменить. Все ваши данные будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.deleteAccount()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    userEmail: String,
    userLevel: Int,
    progress: Float,
    isOwnProfile: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Аватар",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Имя пользователя
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            // Email
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Уровень и прогресс
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Уровень $userLevel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% до уровня ${userLevel + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Прогресс-бар
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}


@Composable
fun MainStats(
    totalQuizzes: Int,
    winPercentage: Int,
    averageScore: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            title = "Сыграно",
            value = totalQuizzes.toString(),
            icon = Icons.Default.Gamepad,
            color = MaterialTheme.colorScheme.primary
        )

        StatCard(
            title = "Побед",
            value = "$winPercentage%",
            icon = Icons.Default.EmojiEvents,
            color = MaterialTheme.colorScheme.inversePrimary
        )

        StatCard(
            title = "Средний балл",
            value = String.format("%.1f", averageScore),
            icon = Icons.Default.Star,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopicStatsSection(topicStats: List<TopicStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            topicStats.forEach { topic ->
                TopicStatItem(topic = topic)
            }
        }
    }
}

@Composable
fun TopicStatItem(topic: TopicStat) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${topic.score.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = topic.score / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = topic.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}


@Composable
fun RecentGameItem(game: RecentGame) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (game.result) {
                "Победа" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = game.topic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = game.timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Badge(
                    containerColor = when (game.result) {
                        "Победа" -> Color.Green.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.error
                    }
                ) {
                    Text(game.result)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f", game.score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun AchievementsSection(achievements: List<Achievement>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            achievements.forEach { achievement ->
                AchievementItem(achievement = achievement)
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка достижения
        Icon(
            imageVector = if (achievement.achieved) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (achievement.achieved) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Модели данных
data class TopicStat(
    val name: String,
    val score: Float,
    val color: Color
)

data class RecentGame(
    val topic: String,
    val result: String,
    val score: Float,
    val timeAgo: String
)

data class Achievement(
    val title: String,
    val description: String,
    val achieved: Boolean
)

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            onNavigateToAuth = {},
            userId = "user123",
            isOwnProfile = true
        )
    }
}


@Preview(showBackground = true)
@Composable
fun OtherProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            onNavigateToAuth = {},
            userId = "user456",
            isOwnProfile = false
        )
    }
}



/*@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview( ) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))


        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "My stats",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Friend list",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Game history",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                //authViewModel.signOut()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                "Sign Out",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                //authViewModel.deleteAccount()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Unspecified,
                contentColor = Color.Red
            ),
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = "Delete Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(modifier = Modifier.weight(0.02f))

    }

    Text(
        text = "Rank:",
        modifier = Modifier.offset(x = 30.dp, y = 230.dp),
        fontSize = 24.sp,
    )


    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Nick Name", fontSize = 20.sp) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .offset(x = 160.dp, y = 100.dp)
            .fillMaxWidth(0.55f)
            .height(56.dp)
    )

    Text(
        text = "Your email",
        fontSize = 20.sp,
        modifier = Modifier.offset(x = 180.dp, y = 180.dp)
    )


    MyProgressBarScreen()

    GreetingImage()

    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { },
        modifier = Modifier
            .offset(x = 90.dp, y = 180.dp)
            .fillMaxWidth(0.12f)
    ) {
        Text(
            text = "✏️\uFE0F",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}*/