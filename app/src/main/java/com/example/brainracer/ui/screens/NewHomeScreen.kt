package com.example.brainracer.ui.screens

// ==================== ИМПОРТЫ ====================

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.brainracer.ui.components.BottomBar

// ==================== МОДЕЛИ ДАННЫХ ====================

data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val icon: String
)

data class CourseProgress(
    val id: Int,
    val title: String,
    val progress: Float,
    val totalLessons: Int,
    val completedLessons: Int,
    val icon: String,
    val color: Color
)

data class QuizCardData(
    val id: Int,
    val title: String,
    val category: String,
    val questionCount: Int,
    val rating: Float,
    val players: Int,
    val gradient: List<Color>,
    val isHot: Boolean = false
)

data class ChallengeItem(
    val id: Int,
    val title: String,
    val opponent: String,
    val opponentAvatar: String,
    val result: String,
    val isWin: Boolean,
    val score: String,
    val timeAgo: String
)

data class StatItem(
    val label: String,
    val value: String,
    val icon: String,
    val trend: Float
)

// ==================== ХАРДКОД ДАННЫХ ====================

val sampleBanners = listOf(
    Banner(1, "🏆 Турнир Чемпионов", "Призовой фонд 5000 монет", listOf(Color(0xFF667eea), Color(0xFF764ba2)), "trophy"),
    Banner(2, "⚡ Блиц-неделя", "x2 опыта до воскресенья", listOf(Color(0xFFf093fb), Color(0xFFf5576c)), "bolt"),
    Banner(3, "🎯 Новая категория", "Искусство и Культура", listOf(Color(0xFF4facfe), Color(0xFF00f2fe)), "art")
)

val sampleCourses = listOf(
    CourseProgress(1, "Математика", 0.75f, 20, 15, "calculate", Color(0xFF667eea)),
    CourseProgress(2, "История", 0.45f, 30, 13, "history_edu", Color(0xFFf093fb)),
    CourseProgress(3, "Наука", 0.90f, 15, 14, "science", Color(0xFF4facfe)),
    CourseProgress(4, "География", 0.30f, 25, 7, "public", Color(0xFF43e97b))
)

val sampleQuizzes = listOf(
    QuizCardData(1, "Столицы мира", "География", 15, 4.8f, 1240, listOf(Color(0xFF667eea), Color(0xFF764ba2)), true),
    QuizCardData(2, "Великие учёные", "История", 20, 4.5f, 890, listOf(Color(0xFFf093fb), Color(0xFFf5576c)), false),
    QuizCardData(3, "Формулы физики", "Наука", 12, 4.9f, 2100, listOf(Color(0xFF4facfe), Color(0xFF00f2fe)), true),
    QuizCardData(4, "Литература XIX", "Литература", 18, 4.3f, 560, listOf(Color(0xFF43e97b), Color(0xFF38f9d7)), false),
    QuizCardData(5, "Киновикторина", "Кино", 25, 4.7f, 3200, listOf(Color(0xFFfa709a), Color(0xFFfee140)), true)
)

val sampleChallenges = listOf(
    ChallengeItem(1, "Блиц-раунд", "Алексей К.", "AK", "Победа", true, "15:12", "2 мин назад"),
    ChallengeItem(2, "Марафон", "Мария С.", "MS", "Поражение", false, "18:20", "1 час назад"),
    ChallengeItem(3, "Дуэль", "Дмитрий В.", "DV", "Победа", true, "10:8", "3 часа назад")
)

val userStats = listOf(
    StatItem("Рейтинг", "1,247", "emoji_events", 0.15f),
    StatItem("Победы", "89", "verified", 0.08f),
    StatItem("Серия", "12", "local_fire", 0.25f)
)

// ==================== ОСНОВНОЙ ЭКРАН ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreenNew(
    navController: androidx.navigation.NavController,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "home"
) {
    val userName = "Максим"
    val userLevel = 24
    val userXp = 75

    Scaffold(
        topBar = { HomeTopAppBar(userLevel = userLevel, userXp = userXp) },
        bottomBar = {
            BottomBar(showBar = true, currentRoute = currentRoute, onHomeClick = onHomeClick, onProfileClick = onProfileClick)
        },
        floatingActionButton = { HomeFloatingActionButton() },
        containerColor = Color(0xFF0F0F1A)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFF0F0F1A)),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { UserStatsRow(stats = userStats) }
            item { WelcomeHeader(userName = userName, userLevel = userLevel) }
            item { BannerCarouselWithDots(banners = sampleBanners) }
            item { ContinueLearningBlock(courses = sampleCourses) }
            item { HotQuizzesSection(quizzes = sampleQuizzes.filter { it.isHot }) }
            item { AllQuizzesSection(quizzes = sampleQuizzes) }
            item { RecentChallengesBlock(challenges = sampleChallenges) }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

// ==================== TOP APP BAR С XP ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(userLevel: Int, userXp: Int) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0F0F1A), shadowElevation = 0.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Brain Racer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Уровень $userLevel",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF667eea),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "Поиск", tint = Color.White.copy(alpha = 0.7f))
                    }
                    Box {
                        IconButton(onClick = { }) {
                            Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Уведомления", tint = Color.White.copy(alpha = 0.7f))
                        }
                        Box(
                            modifier = Modifier.size(8.dp).offset(x = 4.dp, y = (-4).dp).background(Color(0xFFf5576c), CircleShape)
                        )
                    }
                }
            }

            // ✅ ИСПРАВЛЕНО: color = Color, не Brush
            LinearProgressIndicator(
                progress = { userXp / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF667eea),
                trackColor = Color(0xFF1A1A2E)
            )
        }
    }
}

// ==================== СТАТИСТИКА ПОЛЬЗОВАТЕЛЯ ====================

@Composable
fun UserStatsRow(stats: List<StatItem>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        stats.forEach { stat ->
            StatCard(stat = stat, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .border(1.dp, Color(0xFF667eea).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = stat.value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = stat.label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1)
        }
    }
}

// ==================== ПРИВЕТСТВЕННЫЙ БЛОК ====================

@Composable
fun WelcomeHeader(userName: String, userLevel: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Добрый день,", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            Text(text = "$userName! 🚀", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFFf093fb))))
                .shadow(elevation = 15.dp, shape = CircleShape, spotColor = Color(0xFF667eea).copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = userName.first().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==================== КАРУСЕЛЬ БАННЕРОВ С ТОЧКАМИ ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarouselWithDots(banners: List<Banner>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 16.dp
        ) { page ->
            BannerCardGradient(banner = banners[page])
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            banners.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Color(0xFF667eea) else Color(0xFF1A1A2E))
                )
            }
        }
    }
}

@Composable
fun BannerCardGradient(banner: Banner) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = banner.gradient, start = Offset(0f, 0f), end = Offset(1000f, 1000f)))
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), spotColor = banner.gradient.first().copy(alpha = 0.4f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
        Box(modifier = Modifier.size(60.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))

        Column(modifier = Modifier.fillMaxHeight().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(text = banner.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = banner.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp), modifier = Modifier.wrapContentWidth()) {
                Text(text = "Участвовать →", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==================== ПРОДОЛЖИТЬ ОБУЧЕНИЕ ====================

@Composable
fun ContinueLearningBlock(courses: List<CourseProgress>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📚 В процессе", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { }) { Text("Все", color = Color(0xFF667eea)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(courses) { course ->
                CourseProgressCardModern(course = course)
            }
        }
    }
}

@Composable
fun CourseProgressCardModern(course: CourseProgress) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2E))
            .border(1.5.dp, course.color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(course.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = course.color, modifier = Modifier.size(20.dp))
                }
                Text(text = course.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ ИСПРАВЛЕНО: color = Color, не Brush
            LinearProgressIndicator(
                progress = { course.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = course.color,
                trackColor = Color(0xFF1A1A2E)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${course.completedLessons}/${course.totalLessons} уроков", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

// ==================== ГОРЯЧИЕ ВИКТОРИНЫ ====================

@Composable
fun HotQuizzesSection(quizzes: List<QuizCardData>) {
    if (quizzes.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🔥", fontSize = 20.sp)
            Text(text = "Горячее сейчас", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(quizzes) { quiz ->
                HotQuizCard(quiz = quiz)
            }
        }
    }
}

@Composable
fun HotQuizCard(quiz: QuizCardData) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = quiz.gradient, start = Offset(0f, 0f), end = Offset(1000f, 1000f)))
            .shadow(elevation = 15.dp, shape = RoundedCornerShape(24.dp), spotColor = quiz.gradient.first().copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFf5576c)).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = "HOT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = quiz.category, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = quiz.rating.toString(), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text(text = "${quiz.questionCount} вопросов", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ==================== ВСЕ ВИКТОРИНЫ ====================

@Composable
fun AllQuizzesSection(quizzes: List<QuizCardData>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📖 Все викторины", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { }) { Text("Смотреть все", color = Color(0xFF667eea)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            quizzes.forEach { quiz ->
                QuizListItem(quiz = quiz)
            }
        }
    }
}

@Composable
fun QuizListItem(quiz: QuizCardData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors = quiz.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = quiz.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = quiz.category, fontSize = 12.sp, color = Color(0xFF667eea))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Text(text = quiz.rating.toString(), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
        }
    }
}

// ==================== ПОСЛЕДНИЕ ВЫЗОВЫ ====================

@Composable
fun RecentChallengesBlock(challenges: List<ChallengeItem>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚔️ Последние вызовы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = { }) { Text("История", color = Color(0xFF667eea)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            challenges.forEach { challenge ->
                ChallengeCardModern(challenge = challenge)
            }
        }
    }
}

@Composable
fun ChallengeCardModern(challenge: ChallengeItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .border(
                1.dp,
                if (challenge.isWin) Color(0xFF43e97b).copy(alpha = 0.3f) else Color(0xFFf5576c).copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFFf093fb)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = challenge.opponentAvatar, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(text = challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(text = "vs ${challenge.opponent}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text(
                    text = challenge.score,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (challenge.isWin) Color(0xFF43e97b) else Color(0xFFf5576c)
                )
                Text(text = challenge.timeAgo, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

// ==================== FLOATING ACTION BUTTON ====================

@Composable
fun HomeFloatingActionButton() {
    // ✅ ИСПРАВЛЕНО: containerColor = Color, не Brush
    ExtendedFloatingActionButton(
        onClick = { },
        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Создать", modifier = Modifier.size(24.dp)) },
        text = { Text("Создать", fontWeight = FontWeight.SemiBold) },
        containerColor = Color(0xFF667eea),
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    )
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, backgroundColor = 0xFF0F0F1A, name = "Домашний экран")
@Composable
fun HomeScreenNewPreview() {
    HomeScreenNew(
        navController = rememberNavController(),
        onHomeClick = {},
        onProfileClick = {},
        currentRoute = "home"
    )
}