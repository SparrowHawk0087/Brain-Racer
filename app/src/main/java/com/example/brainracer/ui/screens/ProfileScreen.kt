package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onFriendsClick: () -> Unit = {}, // ← новый параметр
    onProfileClick: () -> Unit = {},
    currentRoute: String = "profile",
    isOwnProfile: Boolean = true
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

    // Если пользователь удалён или вышел — перенаправляем на Auth
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
                            // TODO: навигация на экран редактирования профиля
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
                onFriendsClick = onFriendsClick, // ← пробрасываем в BottomBar
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            if (!isOwnProfile) {
                ExtendedFloatingActionButton(
                    onClick = {
                        Toast.makeText(context, "Вызов на дуэль отправлен!", Toast.LENGTH_SHORT).show()
                    },
                    icon = { Icon(Icons.Default.Sports, contentDescription = null) },
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
                    nextLevelProgress = nextLevelProgress
                )
            }

            // Краткая статистика
            item {
                StatsRow(
                    totalQuizzes = totalQuizzes,
                    winPercentage = winPercentage,
                    averageScore = averageScore
                )
            }

            // Статистика по темам
            item { TopicStatsSection(topicStats = topicStats) }

            // История игр
            item { RecentGamesSection(recentGames = recentGames) }

            // Достижения
            item { AchievementsSection(achievements = achievements) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Вспомогательные composable-ы и модели
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileHeader(
    userName: String,
    userEmail: String,
    userLevel: Int,
    nextLevelProgress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватар-заглушка
            Surface(
                modifier = Modifier.size(80.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userName.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Уровень $userLevel", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = nextLevelProgress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun StatsRow(totalQuizzes: Int, winPercentage: Int, averageScore: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), label = "Игр", value = "$totalQuizzes")
        StatCard(modifier = Modifier.weight(1f), label = "Побед", value = "$winPercentage%")
        StatCard(modifier = Modifier.weight(1f), label = "Рейтинг", value = "$averageScore")
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TopicStatsSection(topicStats: List<TopicStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Статистика по темам", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            topicStats.forEach { stat ->
                TopicStatItem(stat = stat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TopicStatItem(stat: TopicStat) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stat.name, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${stat.score.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = stat.score / 100f,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = stat.color,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun RecentGamesSection(recentGames: List<RecentGame>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Последние игры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            recentGames.forEach { game ->
                RecentGameItem(game = game)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RecentGameItem(game: RecentGame) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (game.result == "Победа") Icons.Default.EmojiEvents else Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (game.result == "Победа") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = game.topic, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = game.timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = game.result,
            style = MaterialTheme.typography.labelMedium,
            color = if (game.result == "Победа") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AchievementsSection(achievements: List<Achievement>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Достижения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            achievements.forEach { achievement ->
                AchievementItem(achievement = achievement)
                Spacer(modifier = Modifier.height(8.dp))
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
        Icon(
            imageVector = if (achievement.achieved) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (achievement.achieved) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = achievement.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = achievement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Модели данных
// ─────────────────────────────────────────────────────────────────────────────

data class TopicStat(val name: String, val score: Float, val color: Color)
data class RecentGame(val topic: String, val result: String, val score: Float, val timeAgo: String)
data class Achievement(val title: String, val description: String, val achieved: Boolean)

// ─────────────────────────────────────────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(onNavigateToAuth = {}, userId = "user123", isOwnProfile = true)
    }
}

@Preview(showBackground = true)
@Composable
fun OtherProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(onNavigateToAuth = {}, userId = "user456", isOwnProfile = false)
    }
}
