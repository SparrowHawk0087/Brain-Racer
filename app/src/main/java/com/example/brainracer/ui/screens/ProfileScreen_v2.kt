package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.theme.onBackgroundLight
import com.example.brainracer.ui.theme.onSurfaceVariantLight
import com.example.brainracer.ui.theme.primaryContainerLight
import com.example.brainracer.ui.theme.primaryLight
import com.example.brainracer.ui.theme.surfaceContainerHighLight
import com.example.brainracer.ui.theme.surfaceContainerLight
import com.example.brainracer.ui.theme.surfaceContainerLowLight
import com.example.brainracer.ui.theme.surfaceLight
import com.example.brainracer.ui.viewmodels.AuthViewModel


data class ProfileTopicStat(
    val name: String,
    val score: Float,
    val color: Color,
    val icon: ImageVector
)

data class ProfileRecentGame(
    val topic: String,
    val result: String,
    val score: Float,
    val timeAgo: String
)

data class ProfileAchievement(
    val title: String,
    val description: String,
    val achieved: Boolean,
    val icon: ImageVector
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNew(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    userId: String,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "profile",
    isOwnProfile: Boolean = true
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val userLevel = 7
    val nextLevelProgress = 0.65f
    val currentStreak = 5
    val totalQuizzes = 42
    val winPercentage = 78
    val averageScore = 4.3

    val recentGames = remember {
        listOf(
            ProfileRecentGame("Математика", "Победа", 4.5f, "2 часа назад"),
            ProfileRecentGame("История", "Поражение", 3.2f, "1 день назад"),
            ProfileRecentGame("Программирование", "Победа", 4.8f, "3 дня назад"),
            ProfileRecentGame("География", "Победа", 4.1f, "5 дней назад"),
            ProfileRecentGame("Биология", "Поражение", 2.9f, "1 неделя назад")
        )
    }

    val topicStats = remember {
        listOf(
            ProfileTopicStat("Математика", 85f, Color(0xFF4CAF50), Icons.Default.TrendingUp),
            ProfileTopicStat("История", 65f, Color(0xFF2196F3), Icons.Default.Person),
            ProfileTopicStat("Программирование", 92f, Color(0xFF9C27B0), Icons.Default.Code),
            ProfileTopicStat("География", 78f, Color(0xFFFF9800), Icons.Default.Public),
            ProfileTopicStat("Биология", 54f, Color(0xFFE91E63), Icons.Default.Science)
        )
    }

    val achievements = remember {
        listOf(
            ProfileAchievement("Новичок", "Первая викторина пройдена", true, Icons.Default.Star),
            ProfileAchievement("Эксперт", "10 викторин пройдено", true, Icons.Default.EmojiEvents),
            ProfileAchievement("Непобедимый", "5 побед подряд", false, Icons.Default.LocalFireDepartment),
            ProfileAchievement("Мастер тем", "90%+ в 3 темах", true, Icons.Default.TrendingUp),
            ProfileAchievement("Марафонец", "7 дней подряд", false, Icons.Default.Sports)
        )
    }

    LaunchedEffect(user) {
        if (user == null && isOwnProfile) {
            Toast.makeText(context, "Сессия истекла", Toast.LENGTH_SHORT).show()
            onNavigateToAuth()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = surfaceContainerLowLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOwnProfile) "Мой профиль" else "Профиль игрока",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = onBackgroundLight
                    )
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = {
                            Toast.makeText(context, "Поделиться", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = onSurfaceVariantLight)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = onSurfaceVariantLight)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceContainerLowLight,
                    scrolledContainerColor = surfaceContainerHighLight
                ),
                scrollBehavior = scrollBehavior
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
                    onClick = { Toast.makeText(context, "Вызов отправлен!", Toast.LENGTH_SHORT).show() },
                    icon = { Icon(Icons.Default.Sports, contentDescription = null) },
                    text = { Text("Вызвать") },
                    containerColor = primaryLight,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileHeaderSection(
                    userName = user?.displayName ?: "Игрок $userId",
                    userEmail = user?.email ?: "",
                    userLevel = userLevel,
                    progress = nextLevelProgress,
                    currentStreak = currentStreak
                )
            }

            item {
                StatsRow(
                    totalQuizzes = totalQuizzes,
                    winPercentage = winPercentage,
                    averageScore = averageScore
                )
            }

            item {
                SectionHeader("Статистика по темам")
                TopicsCard(topicStats)
            }

            item {
                SectionHeader("Последние игры")
            }

            items(recentGames) { game ->
                GameRow(game)
            }

            item {
                SectionHeader("Достижения")
                AchievementsCard(achievements)
            }

            if (isOwnProfile) {
                item {
                    ActionsSection(
                        onSignOut = { authViewModel.signOut() },
                        onDeleteClick = { showDeleteDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = surfaceLight,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Удалить аккаунт?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Все данные, прогресс и достижения будут удалены навсегда.",
                    color = onSurfaceVariantLight
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.deleteAccount()
                    showDeleteDialog = false
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = onBackgroundLight)
                }
            }
        )
    }
}


@Composable
fun ProfileHeaderSection(
    userName: String,
    userEmail: String,
    userLevel: Int,
    progress: Float,
    currentStreak: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(primaryContainerLight.copy(alpha = 0.25f))
                    .border(3.dp, primaryLight.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(44.dp), tint = primaryLight)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(userName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onBackgroundLight)
                if (userEmail.isNotEmpty()) {
                    Text(userEmail, fontSize = 13.sp, color = onSurfaceVariantLight)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = primaryLight.copy(alpha = 0.10f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, Modifier.size(15.dp), tint = primaryLight)
                        Text("Уровень $userLevel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = primaryLight)
                    }
                }
                if (currentStreak > 0) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFA726).copy(alpha = 0.10f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(15.dp), tint = Color(0xFFFFA726))
                            Text("$currentStreak дней", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("До уровня ${userLevel + 1}", fontSize = 12.sp, color = onSurfaceVariantLight)
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = primaryLight, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = primaryLight,
                    trackColor = surfaceContainerLight,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}


@Composable
fun StatsRow(totalQuizzes: Int, winPercentage: Int, averageScore: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SimpleStatCard("Сыграно", "$totalQuizzes", Icons.Default.Gamepad, Color(0xFF5C6BC0), Modifier.weight(1f))
        SimpleStatCard("Побед", "$winPercentage%", Icons.Default.EmojiEvents, primaryLight, Modifier.weight(1f))
        SimpleStatCard("Рейтинг", String.format("%.1f", averageScore), Icons.Default.Star, Color(0xFFFF8F00), Modifier.weight(1f))
    }
}

@Composable
fun SimpleStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(17.dp), tint = color)
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = onBackgroundLight)
            Text(title, fontSize = 11.sp, color = onSurfaceVariantLight)
        }
    }
}


@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = onSurfaceVariantLight,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp)
    )
}


@Composable
fun TopicsCard(topics: List<ProfileTopicStat>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            topics.forEachIndexed { index, topic ->
                val animated by animateFloatAsState(
                    targetValue = topic.score / 100f,
                    animationSpec = tween(600, delayMillis = index * 80),
                    label = "t$index"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(topic.icon, null, Modifier.size(15.dp), tint = topic.color)
                            Text(topic.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onBackgroundLight)
                        }
                        Text("${topic.score.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = topic.color)
                    }
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = topic.color,
                        trackColor = surfaceContainerLight,
                        strokeCap = StrokeCap.Round
                    )
                }
                if (index < topics.lastIndex) {
                    HorizontalDivider(color = surfaceContainerLight, thickness = 0.5.dp)
                }
            }
        }
    }
}


@Composable
fun GameRow(game: ProfileRecentGame) {
    val isWin = game.result == "Победа"
    val accentColor = if (isWin) Color(0xFF388E3C) else Color(0xFFC62828)
    val bgColor = if (isWin) Color(0xFF4CAF50).copy(alpha = 0.07f) else Color(0xFFE57373).copy(alpha = 0.07f)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(game.topic, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = onBackgroundLight)
                Text(game.timeAgo, fontSize = 12.sp, color = onSurfaceVariantLight)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFA000))
                    Text(String.format("%.1f", game.score), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = onBackgroundLight)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.13f)) {
                    Text(
                        text = game.result,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}


@Composable
fun AchievementsCard(achievements: List<ProfileAchievement>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            achievements.forEachIndexed { index, it ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (it.achieved) primaryLight.copy(alpha = 0.11f) else surfaceContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(it.icon, null, Modifier.size(18.dp),
                            tint = if (it.achieved) primaryLight else onSurfaceVariantLight.copy(alpha = 0.3f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            it.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (it.achieved) onBackgroundLight else onSurfaceVariantLight.copy(alpha = 0.45f)
                        )
                        Text(it.description, fontSize = 12.sp, color = onSurfaceVariantLight)
                    }
                    Icon(
                        if (it.achieved) Icons.Default.CheckCircle else Icons.Default.Lock,
                        null,
                        Modifier.size(18.dp),
                        tint = if (it.achieved) Color(0xFF4CAF50) else onSurfaceVariantLight.copy(alpha = 0.22f)
                    )
                }
                if (index < achievements.lastIndex) {
                    HorizontalDivider(color = surfaceContainerLight, thickness = 0.5.dp)
                }
            }
        }
    }
}


@Composable
fun ActionsSection(onSignOut: () -> Unit, onDeleteClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = onSurfaceVariantLight)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("Выйти из аккаунта", fontWeight = FontWeight.Medium)
        }
        TextButton(
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.65f))
        ) {
            Icon(Icons.Default.Delete, null, Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("Удалить аккаунт", fontWeight = FontWeight.Medium)
        }
    }
}


@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun ProfileScreenPreviewNew() {
    MaterialTheme {
        ProfileScreenNew(onNavigateToAuth = {}, userId = "user123", isOwnProfile = true)
    }
}

@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun OtherProfileScreenPreviewNew() {
    MaterialTheme {
        ProfileScreenNew(onNavigateToAuth = {}, userId = "user456", isOwnProfile = false)
    }
}