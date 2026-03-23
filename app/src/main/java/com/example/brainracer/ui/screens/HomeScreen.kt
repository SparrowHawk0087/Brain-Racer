package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.UserStats
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel

// ─── Цвета ──────────────────────────────────────────────────────────────────
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

private data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val actionLabel: String = "Участвовать →"
)

private val homeBanners = listOf(
    Banner(1, "🏆 Турнир Чемпионов", "Призовой фонд 5000 монет",
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))),
    Banner(2, "⚡ Блиц-неделя", "x2 опыта до воскресенья",
        listOf(Color(0xFFf093fb), Color(0xFFf5576c))),
    Banner(3, "🎯 Новая категория", "Искусство и Культура",
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)))
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
    val uiState       by homeViewModel.uiState.collectAsState()
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Обновляем статистику при каждом возвращении на экран ─────────────
    // Это нужно, чтобы уровень/XP обновились после прохождения викторины
    // без полной перезагрузки страницы.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.refreshUserStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tabs = remember(uiState.categories) { uiState.categories.filter { it != "Все" } }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentCategory = tabs.getOrNull(selectedTabIndex) ?: "Все"

    LaunchedEffect(selectedTabIndex, tabs) {
        if (tabs.isNotEmpty()) homeViewModel.loadQuizzesByCategory(currentCategory)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            homeViewModel.clearError()
        }
    }

    var showChallengeSheet by remember { mutableStateOf(false) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.challengeSentMessage) {
        uiState.challengeSentMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            homeViewModel.consumeChallengeSentMessage()
            showChallengeSheet = false
        }
    }

    LaunchedEffect(showChallengeSheet) {
        if (showChallengeSheet) homeViewModel.loadChallengePickerData()
    }

    val tabQuizzes = uiState.quizzes.take(5)

    if (showChallengeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChallengeSheet = false },
            sheetState       = challengeSheetState,
            containerColor   = BgCard,
            contentColor     = TextPri
        ) {
            ChallengeFriendQuizSheetContent(
                fixedFriend   = null,
                friends       = uiState.friendsForChallenge,
                quizzes       = uiState.challengePickerQuizzes,
                isLoading     = uiState.challengePickerLoading,
                onDismiss     = { showChallengeSheet = false },
                onSendChallenge = { fid, qid, title ->
                    homeViewModel.sendChallengeToFriend(fid, qid, title)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userLevel     = uiState.userLevel,
                levelProgress = uiState.levelProgress,
                rankName      = uiState.rankName,
                onSearchClick = { navController.navigate("search") },
                onSignOut     = {
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
                onClick        = { navController.navigate("quiz_creator") },
                icon           = { Icon(Icons.Default.Create, null) },
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
            item { UserStatRow(uiState.userStats, uiState.userLevel, uiState.levelProgress) }
            item { WelcomeRow(uiState.userName.ifBlank { "Гость" }) }
            item { BannerCarousel(homeBanners) }

            if (tabs.isNotEmpty()) {
                item { CategoryTabs(tabs, selectedTabIndex) { selectedTabIndex = it } }
            }

            val hot = tabQuizzes.take(2)
            if (hot.isNotEmpty()) {
                item { HotQuizzesSection(hot) { id -> navController.navigate("quiz_detail/$id") } }
            }

            item {
                AllQuizzesSection(
                    quizzes         = tabQuizzes,
                    isLoading       = uiState.isLoading,
                    currentCategory = currentCategory,
                    onQuizClick     = { id -> navController.navigate("quiz_detail/$id") },
                    onCreateQuiz    = { navController.navigate("quiz_creator") },
                    onShowAll       = { cat -> navController.navigate("search?category=$cat") }
                )
            }

            item {
                RecentChallengesSection(
                    activeChallenges   = uiState.homeActiveChallenges,
                    finishedChallenges = uiState.homeFinishedChallenges,
                    currentUserId      = uiState.currentUserId,
                    onViewAll          = { navController.navigate("challenges/${uiState.currentUserId}") },
                    onOpenChallenge    = { ch ->
                        navigateFromHomeToChallenge(navController, ch, uiState.currentUserId)
                    },
                    onNewChallenge     = { showChallengeSheet = true }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TOP BAR — исправленный
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HomeTopBar(
    userLevel: Int,
    levelProgress: Float,       // 0.0–1.0 реальный прогресс внутри уровня
    rankName: String,
    onSearchClick: () -> Unit,
    onSignOut: () -> Unit
) {
    // Анимируем прогресс-бар чтобы он плавно заполнялся при загрузке
    val animatedProgress by animateFloatAsState(
        targetValue   = levelProgress,
        animationSpec = tween(durationMillis = 800),
        label         = "levelProgress"
    )

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
                    Text(
                        "Brain Racer",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp,
                        color      = TextPri
                    )
                    // Показываем уровень И ранг
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ур. $userLevel",
                            fontSize   = 12.sp,
                            color      = AccentPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("·", fontSize = 12.sp, color = TextSec)
                        Text(
                            rankName,
                            fontSize = 12.sp,
                            color    = TextSec
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onSearchClick) {
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

            // Прогресс-бар с реальным прогрессом уровня
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress  = { animatedProgress },
                    modifier  = Modifier.fillMaxWidth().height(4.dp),
                    color      = AccentPurple,
                    trackColor = BgCard
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  STAT ROW — обновлённый
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun UserStatRow(
    stats: UserStats?,
    userLevel: Int,
    levelProgress: Float
) {
    val winPct = if ((stats?.totalQuestionsAnswered ?: 0) > 0)
        "${stats!!.correctAnswers * 100 / stats.totalQuestionsAnswered}%"
    else "—"

    // Три карточки: сыграно / XP (totalPoints) / точность
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Triple((stats?.totalQuizzesTaken ?: 0).toString(), "игр",     false),
            Triple((stats?.totalPoints ?: 0).toString(),        "XP",      false),
            Triple(winPct,                                       "точность", false)
        ).forEach { (value, label, _) ->
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

// ══════════════════════════════════════════════════════════════════════════════
//  Остальные composable-ы без изменений
// ══════════════════════════════════════════════════════════════════════════════

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
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
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
                modifier = Modifier.width(320.dp).height(150.dp)
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
                            color      = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    color = AccentPurple, height = 3.dp
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
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.2f)) {
                            Text("HOT",
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color      = Color.White, fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text(quiz.category, color = Color.White.copy(0.8f), fontSize = 11.sp)
                            Text(quiz.title, color = Color.White, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${quiz.questionCount} вопросов", color = Color.White.copy(0.72f),
                                fontSize = 11.sp)
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
    currentCategory: String,
    onQuizClick: (String) -> Unit,
    onCreateQuiz: () -> Unit,
    onShowAll: (String) -> Unit
) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("📖 Все викторины", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPri)
            TextButton(onClick = { onShowAll(currentCategory) }) {
                Text("Смотреть все", color = AccentPurple, fontSize = 13.sp)
            }
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
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Default.Quiz, null, tint = TextSec, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("В этой категории пока нет викторин", color = TextSec, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onCreateQuiz,
                        colors  = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape   = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Create, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Создать викторину", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
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
        Row(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(quiz.category, fontSize = 12.sp, color = AccentPurple)
                    Text("·", color = TextSec)
                    Text("${quiz.questionCount} вопр.", fontSize = 12.sp, color = TextSec)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSec, modifier = Modifier.size(20.dp))
        }
    }
}

private fun navigateFromHomeToChallenge(
    navController: NavController,
    ch: Challenge,
    currentUserId: String
) {
    if (currentUserId.isBlank()) return
    when {
        ch.status == ChallengeStatus.PENDING ->
            navController.navigate("challenges/$currentUserId")
        ch.status == ChallengeStatus.ACCEPTED &&
                ch.canPlay(currentUserId) &&
                !ch.hasUserResult(currentUserId) ->
            navController.navigate("quiz_play/${ch.quizId}?challengeId=${ch.id}")
        else ->
            navController.navigate("challenge_review/${ch.id}")
    }
}

@Composable
private fun RecentChallengesSection(
    activeChallenges: List<Challenge>,
    finishedChallenges: List<Challenge>,
    currentUserId: String,
    onViewAll: () -> Unit,
    onOpenChallenge: (Challenge) -> Unit,
    onNewChallenge: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("Активные", "Завершённые")
    val listShown = if (tabIndex == 0) activeChallenges else finishedChallenges

    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("⚔️ Последние вызовы", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPri)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNewChallenge) {
                    Icon(
                        Icons.Default.Sports,
                        contentDescription = null,
                        tint       = AccentPurple,
                        modifier   = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Вызов", color = AccentPurple, fontSize = 13.sp)
                }
                TextButton(onClick = onViewAll) {
                    Text("Все вызовы", color = AccentPurple, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor   = BgDeep,
            contentColor     = AccentPurple,
            indicator = { positions ->
                if (tabIndex < positions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[tabIndex]),
                        color    = AccentPurple,
                        height   = 2.dp
                    )
                }
            },
            divider = { HorizontalDivider(color = BgBorder) }
        ) {
            tabLabels.forEachIndexed { i, title ->
                Tab(
                    selected = tabIndex == i,
                    onClick  = { tabIndex = i },
                    text     = { Text(title, fontSize = 13.sp) },
                    selectedContentColor   = AccentPurple,
                    unselectedContentColor = TextSec
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (listShown.isEmpty()) {
            val (title, subtitle) = if (tabIndex == 0) {
                "Нет активных вызовов" to "Примите вызов или бросьте вызов другу во вкладке друзей"
            } else {
                "Пока нет завершённых" to "Завершённые дуэли появятся здесь"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BgBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, color = TextPri, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(subtitle, color = TextSec, fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (tabIndex == 0) {
                        Button(
                            onClick     = onNewChallenge,
                            colors      = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape       = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sports, null, Modifier.size(18.dp), Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Бросить вызов", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
            return
        }

        Column(
            modifier            = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listShown.forEach { ch ->
                HomeChallengeRow(
                    ch            = ch,
                    currentUserId = currentUserId,
                    onClick       = { onOpenChallenge(ch) }
                )
            }
        }
    }
}

@Composable
private fun HomeChallengeRow(
    ch: Challenge,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isChallenger = ch.challengerUserId == currentUserId
    val opponentName = if (isChallenger) ch.challengedNickname else ch.challengerNickname
    val myResult     = if (isChallenger) ch.challengerResult else ch.challengedResult
    val oppResult    = if (isChallenger) ch.challengedResult else ch.challengerResult

    val (statusColor, statusLabel) = when {
        ch.status == ChallengeStatus.PENDING && !isChallenger ->
            Color(0xFFFFA726) to "Входящий"
        ch.status == ChallengeStatus.PENDING && isChallenger ->
            Color(0xFFFFA726) to "В обработке"
        ch.status == ChallengeStatus.ACCEPTED ->
            Color(0xFF4facfe) to when {
                ch.hasUserResult(currentUserId) -> "Пройдено"
                else -> "Нужно пройти"
            }
        ch.status == ChallengeStatus.COMPLETED -> {
            val isDraw = ch.isDraw || ch.winnerId == "draw"
            val isWin  = !isDraw && ch.winnerId == currentUserId
            when {
                isDraw -> Color(0xFFFFA726) to "Ничья"
                isWin  -> Color(0xFF3ECFA3) to "Победа"
                else   -> Color(0xFFEA5C7E) to "Поражение"
            }
        }
        else -> Color(0xFF8B8AAE) to ch.status.name
    }

    val isFinishedRow = ch.status == ChallengeStatus.COMPLETED
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isFinishedRow) 0.82f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, statusColor.copy(0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF667EEA), Color(0xFFf093fb)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(opponentName.take(2).uppercase(),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("vs $opponentName", fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp, color = TextPri,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(ch.quizTitle, fontSize = 11.sp, color = TextSec,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (ch.status == ChallengeStatus.COMPLETED) {
                    Text(
                        "${myResult?.score ?: 0} : ${oppResult?.score ?: 0}",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPri
                    )
                }
                if (ch.status == ChallengeStatus.ACCEPTED) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HomeProgressDot(played = myResult != null, label = "Вы")
                        HomeProgressDot(played = oppResult != null, label = opponentName.split(" ").first())
                    }
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(.15f)) {
                    Text(
                        statusLabel,
                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProgressDot(played: Boolean, label: String) {
    val color = if (played) Color(0xFF3ECFA3) else Color(0xFF8B8AAE)
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (played) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint     = color,
            modifier = Modifier.size(11.dp)
        )
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}