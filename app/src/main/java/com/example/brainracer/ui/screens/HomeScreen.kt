package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.domain.entities.UserStats
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel

private data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val actionLabel: String = "Участвовать →"
)

private data class ChallengePreview(
    val id: Int,
    val title: String,
    val opponent: String,
    val avatarInitials: String,
    val isWin: Boolean,
    val score: String,
    val timeAgo: String
)

private val homeBanners = listOf(
    Banner(1, "🏆 Турнир Чемпионов", "Призовой фонд 5000 монет",
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))),
    Banner(2, "⚡ Блиц-неделя", "x2 опыта до воскресенья",
        listOf(Color(0xFFf093fb), Color(0xFFf5576c))),
    Banner(3, "🎯 Новая категория", "Искусство и Культура",
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)))
)

private val sampleChallenges = listOf(
    ChallengePreview(1, "Дуэль", "Алексей К.", "AK", true,  "15:12", "2 мин назад"),
    ChallengePreview(2, "Блиц",  "Мария С.",   "MS", false, "18:20", "1 час назад"),
    ChallengePreview(3, "Дуэль", "Дмитрий В.", "DV", true,  "10:8",  "3 часа назад")
)

private val BgDeep       = Color(0xFF0F0F1A)
private val BgCard       = Color(0xFF1A1A2E)
private val BgBorder     = Color(0xFF2A2A3E)
private val AccentPurple = Color(0xFF667EEA)
private val TextPri      = Color(0xFFFFFFFF)
private val TextSec      = Color(0xFF8B8AAE)

private val cardGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
    listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
    listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
    listOf(Color(0xFFfa709a), Color(0xFFfee140)),
    listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb)),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    onHomeClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "home"
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val tabs = remember(uiState.categories) { uiState.categories.filter { it != "Все" } }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Один LaunchedEffect — обновляет фильтр в ViewModel при смене вкладки
    LaunchedEffect(selectedTabIndex, tabs) {
        if (tabs.isNotEmpty())
            homeViewModel.loadQuizzesByCategory(tabs.getOrNull(selectedTabIndex) ?: "Все")
    }

    // Одно объявление — uiState.quizzes уже отфильтрован ViewModel-ом
    val tabQuizzes = uiState.quizzes.take(5)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            homeViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userLevel = uiState.userStats?.totalQuizzesTaken ?: 0,
                userXp    = (uiState.userStats?.totalPoints ?: 0) % 100,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                }
            )
        },
        bottomBar = {
            BottomBar(
                showBar        = true,
                currentRoute   = currentRoute,
                onHomeClick    = onHomeClick,
                onFriendsClick = onFriendsClick,
                onProfileClick = onProfileClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { homeViewModel.addDemoQuizzes() },
                icon           = { Icon(Icons.Default.Add, null) },
                text           = { Text("Создать", fontWeight = FontWeight.SemiBold) },
                containerColor = AccentPurple,
                contentColor   = Color.White,
                elevation      = FloatingActionButtonDefaults.elevation(8.dp, 12.dp)
            )
        },
        containerColor = BgDeep
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).background(BgDeep),
            contentPadding      = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { UserStatRow(uiState.userStats) }
            item { WelcomeRow(uiState.userName.ifBlank { "Гость" }) }
            item { BannerCarousel(homeBanners) }

            if (tabs.isNotEmpty()) {
                item { CategoryTabs(tabs, selectedTabIndex) { selectedTabIndex = it } }
            }

            val hot = tabQuizzes.take(2)
            if (hot.isNotEmpty()) {
                item {
                    HotQuizzesSection(hot) { id -> navController.navigate("quiz_detail/$id") }
                }
            }

            item {
                AllQuizzesSection(
                    quizzes     = tabQuizzes,
                    isLoading   = uiState.isLoading,
                    onQuizClick = { id -> navController.navigate("quiz_detail/$id") },
                    onAddDemo   = { homeViewModel.addDemoQuizzes() }
                )
            }

            item { RecentChallengesSection(sampleChallenges) }
        }
    }
}

@Composable
private fun HomeTopBar(userLevel: Int, userXp: Int, onSignOut: () -> Unit) {
    Surface(color = BgDeep) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Brain Racer", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPri)
                    Text("Уровень $userLevel", fontSize = 12.sp, color = AccentPurple, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, null, tint = TextPri.copy(0.7f))
                    }
                    IconButton(onClick = {}) {
                        Box {
                            Icon(Icons.Outlined.Notifications, null, tint = TextPri.copy(0.7f))
                            Box(
                                Modifier.size(8.dp).align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .background(Color(0xFFf5576c), CircleShape)
                            )
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, null, tint = TextSec)
                    }
                }
            }
            LinearProgressIndicator(
                progress  = { (userXp / 100f).coerceIn(0f, 1f) },
                modifier  = Modifier.fillMaxWidth().height(4.dp),
                color      = AccentPurple,
                trackColor = BgCard
            )
        }
    }
}

@Composable
private fun UserStatRow(stats: UserStats?) {
    val winPct = if ((stats?.totalQuestionsAnswered ?: 0) > 0)
        "${stats!!.correctAnswers * 100 / stats.totalQuestionsAnswered}%"
    else "—"

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            (stats?.totalQuizzesTaken ?: 0).toString() to "игр",
            (stats?.totalPoints ?: 0).toString()       to "рейтинг",
            winPct                                      to "побед"
        ).forEach { (value, label) ->
            Box(
                modifier = Modifier.weight(1f).height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, AccentPurple.copy(0.25f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPri)
                    Text(label, fontSize = 11.sp, color = TextSec)
                }
            }
        }
    }
}

@Composable
private fun WelcomeRow(userName: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Добрый день,", color = TextPri.copy(0.6f), fontSize = 14.sp)
            Text("$userName! 🚀", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextPri)
        }
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFFf093fb)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.firstOrNull()?.uppercase() ?: "?",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerCarousel(banners: List<Banner>) {
    val pagerState = rememberPagerState { banners.size }

    Column {
        HorizontalPager(
            state          = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing    = 16.dp
        ) { page ->
            val b = banners[page]
            Box(
                modifier = Modifier
                    .width(320.dp).height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(b.gradient, Offset.Zero, Offset(1000f, 1000f))),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.size(110.dp).align(Alignment.TopEnd).offset(x = 28.dp, y = (-28).dp)
                    .clip(CircleShape).background(Color.White.copy(0.08f)))
                Box(Modifier.size(55.dp).align(Alignment.BottomStart).offset(x = (-18).dp, y = 18.dp)
                    .clip(CircleShape).background(Color.White.copy(0.08f)))
                Column(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(b.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.height(5.dp))
                    Text(b.subtitle, fontSize = 13.sp, color = Color.White.copy(0.85f))
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.22f)) {
                        Text(b.actionLabel,
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            banners.forEachIndexed { i, _ ->
                val selected = pagerState.currentPage == i
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .size(if (selected) 22.dp else 7.dp, 7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) AccentPurple else BgCard)
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor   = BgDeep,
        contentColor     = AccentPurple,
        edgePadding      = 20.dp,
        indicator = { positions ->
            if (selectedIndex < positions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    color    = AccentPurple,
                    height   = 3.dp
                )
            }
        },
        divider = { HorizontalDivider(color = BgBorder, thickness = 1.dp) }
    ) {
        tabs.forEachIndexed { i, name ->
            Tab(
                selected               = selectedIndex == i,
                onClick                = { onSelect(i) },
                selectedContentColor   = AccentPurple,
                unselectedContentColor = TextSec
            ) {
                Text(
                    name,
                    fontWeight = if (selectedIndex == i) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize   = 13.sp,
                    modifier   = Modifier.padding(horizontal = 4.dp, vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun HotQuizzesSection(quizzes: List<QuizItem>, onQuizClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🔥", fontSize = 17.sp)
            Text("Популярное", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPri)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            quizzes.forEachIndexed { i, quiz ->
                val gradient = cardGradients[i % cardGradients.size]
                Box(
                    modifier = Modifier.weight(1f).height(118.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(gradient, Offset.Zero, Offset(600f, 600f)))
                        .clickable { onQuizClick(quiz.id) }
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.2f)) {
                            Text("HOT",
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color      = Color.White,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text(quiz.category, color = Color.White.copy(0.8f), fontSize = 11.sp)
                            Text(quiz.title, color = Color.White, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${quiz.questionCount} вопросов", color = Color.White.copy(0.72f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllQuizzesSection(
    quizzes: List<QuizItem>,
    isLoading: Boolean,
    onQuizClick: (String) -> Unit,
    onAddDemo: () -> Unit
) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("📖 Все викторины", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPri)
            TextButton(onClick = {}) { Text("Смотреть все", color = AccentPurple, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(6.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPurple)
                }
            }
            quizzes.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Викторин нет в этой категории", color = TextSec, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onAddDemo) {
                        Text("Добавить демо-викторины", color = AccentPurple)
                    }
                }
            }
            else -> {
                Column(
                    modifier            = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quizzes.forEachIndexed { i, quiz ->
                        QuizRowCard(quiz = quiz, colorIndex = i, onClick = { onQuizClick(quiz.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizRowCard(quiz: QuizItem, colorIndex: Int, onClick: () -> Unit) {
    val gradient = cardGradients[colorIndex % cardGradients.size]
    Box(
        modifier = Modifier.fillMaxWidth().height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(gradient, Offset.Zero, Offset(400f, 400f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = TextPri, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(quiz.category, fontSize = 12.sp, color = AccentPurple)
                    Text("·", color = TextSec)
                    Text("${quiz.questionCount} вопр.", fontSize = 12.sp, color = TextSec)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSec, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RecentChallengesSection(challenges: List<ChallengePreview>) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("⚔️ Последние вызовы", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPri)
            TextButton(onClick = {}) { Text("История", color = AccentPurple, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            challenges.forEach { ch ->
                val winColor = if (ch.isWin) Color(0xFF43e97b) else Color(0xFFf5576c)
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, winColor.copy(0.28f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFFf093fb)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ch.avatarInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text(ch.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPri)
                                Text("vs ${ch.opponent}", fontSize = 12.sp, color = TextSec)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(ch.score, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = winColor)
                            Text(ch.timeAgo, fontSize = 11.sp, color = TextSec)
                        }
                    }
                }
            }
        }
    }
}