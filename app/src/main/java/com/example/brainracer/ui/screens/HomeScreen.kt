package com.example.brainracer.ui.screens

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
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
import com.example.brainracer.R
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.domain.entities.UserStats
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.components.bottomBarOcclusionBlockClicks
import com.example.brainracer.ui.components.bottomBarOcclusionEffect
import com.example.brainracer.ui.components.bottomBarSafePadding
import com.example.brainracer.ui.components.pressClickable
import com.example.brainracer.ui.components.pressScale
import com.example.brainracer.ui.components.rememberFabVisibilityOnScroll
import com.example.brainracer.ui.utils.HOME_CATEGORY_CUSTOM
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.customAuthorCaption
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel
import java.util.Calendar

private data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val actionLabel: String = "Участвовать →"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "home"
) {
    val uiState       by homeViewModel.uiState.collectAsState()
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ext = LocalBrainRacerExtendedColors.current
    val homeBanners = remember(ext.cardGradients) {
        val g = ext.cardGradients
        listOf(
            Banner(1, "🏆 Турнир Чемпионов", "Призовой фонд 5000 монет", g[0]),
            Banner(2, "⚡ Блиц-неделя", "x2 опыта до воскресенья", g[1]),
            Banner(3, "🎯 Новая категория", "Искусство и Культура", g[2])
        )
    }

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
    val hotQuizzes = remember(uiState.quizzes) {
        uiState.quizzes
            .sortedByDescending { it.playCount }
            .take(3)
    }
    val greetingUi = remember { getGreetingUi() }
    val homeListState = rememberLazyListState()
    val fabVisible = rememberFabVisibilityOnScroll(homeListState)
    val bottomReflexShift by remember {
        derivedStateOf {
            ((homeListState.firstVisibleItemIndex * 4f) +
                    homeListState.firstVisibleItemScrollOffset * 0.012f).coerceIn(0f, 18f)
        }
    }

    if (showChallengeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChallengeSheet = false },
            sheetState       = challengeSheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = MaterialTheme.colorScheme.onSurface
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
                userLevel                  = uiState.userLevel,
                levelProgress              = uiState.levelProgress,
                rankName                   = uiState.rankName,
                unreadNotificationsCount   = uiState.unreadNotificationsCount,
                onSearchClick              = { navController.navigate("search?category=Все&customOnly=false") },
                onNotificationsClick       = {
                    if (uiState.currentUserId.isNotBlank()) {
                        navController.navigate("notifications/${uiState.currentUserId}")
                    } else {
                        Toast.makeText(context, "Войдите в аккаунт", Toast.LENGTH_SHORT).show()
                    }
                },
                onSignOut                  = {
                    authViewModel.signOut()
                    navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                }
            )
        },
        bottomBar = {
            BottomBar(
                showBar                      = true,
                currentRoute                 = currentRoute,
                showChallengesIncomingBadge  = uiState.pendingChallenges.isNotEmpty(),
                onHomeClick                  = onHomeClick,
                onLeaderboardClick           = onLeaderboardClick,
                onChallengesClick            = onChallengesClick,
                onQuizzesClick               = onQuizzesClick,
                onProfileClick               = onProfileClick,
                reflexShift                  = bottomReflexShift
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn(tween(220)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(220)
                ),
                exit = fadeOut(tween(180)) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(180)
                )
            ) {
                Button(
                    onClick = { navController.navigate("quiz_creator") },
                    modifier = Modifier
                        .pressScale()
                        .height(46.dp)
                        .padding(end = 4.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(percent = 30),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 7.dp,
                        hoveredElevation = 6.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_btn),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Создать", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val homeBottomInset = bottomBarSafePadding(
            paddingValues = padding,
            extraBottom = 10.dp,
            minBottom = 152.dp
        )
        val fabReservedBottomInset = 78.dp
        LazyColumn(
            state               = homeListState,
            modifier            = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background),
            contentPadding      = PaddingValues(top = 20.dp, bottom = homeBottomInset + fabReservedBottomInset),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { UserStatRow(uiState.userStats, uiState.userLevel, uiState.levelProgress) }
            item {
                WelcomeRow(
                    userName = uiState.userName.ifBlank { "Гость" },
                    greeting = greetingUi.greeting,
                    emoji = greetingUi.emoji,
                    onClick = onProfileClick
                )
            }
            item { BannerCarousel(homeBanners) }

            if (tabs.isNotEmpty()) {
                item { CategoryTabs(tabs, selectedTabIndex) { selectedTabIndex = it } }
            }

            if (hotQuizzes.isNotEmpty()) {
                item { HotQuizzesSection(hotQuizzes) { id -> navController.navigate("quiz_detail/$id") } }
            }

            item {
                AllQuizzesSection(
                    quizzes         = tabQuizzes,
                    isLoading       = uiState.isLoading,
                    currentCategory = currentCategory,
                    listTitle       = if (currentCategory == HOME_CATEGORY_CUSTOM) "📖 Кастомные" else "📖 Все викторины",
                    emptyMessage    = if (currentCategory == HOME_CATEGORY_CUSTOM) {
                        "Пока нет пользовательских викторин"
                    } else {
                        "В этой категории пока нет викторин"
                    },
                    onQuizClick     = { id -> navController.navigate("quiz_detail/$id") },
                    onCreateQuiz    = { navController.navigate("quiz_creator") },
                    onShowAll       = { cat ->
                        if (cat == HOME_CATEGORY_CUSTOM) {
                            navController.navigate("search?category=Все&customOnly=true")
                        } else {
                            navController.navigate("search?category=$cat&customOnly=false")
                        }
                    }
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
    unreadNotificationsCount: Int,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val ext = LocalBrainRacerExtendedColors.current
    // Анимируем прогресс-бар чтобы он плавно заполнялся при загрузке
    val animatedProgress by animateFloatAsState(
        targetValue   = levelProgress,
        animationSpec = tween(durationMillis = 800),
        label         = "levelProgress"
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    // Показываем уровень И ранг
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ур. $userLevel",
                            fontSize   = 12.sp,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            rankName,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onSearchClick) {
                        Icon(painter = painterResource(id = R.drawable.search_btn), null, tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Box {
                            Icon(painter = painterResource(id = R.drawable.bell_btn), null, tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            if (unreadNotificationsCount > 0) {
                                Box(
                                    Modifier.size(8.dp).align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .background(ext.difficultyHard, CircleShape)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(painter = painterResource(id = R.drawable.logout), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Прогресс-бар с реальным прогрессом уровня
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress  = { animatedProgress },
                    modifier  = Modifier.fillMaxWidth().height(4.dp),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Остальные composable-ы без изменений
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomeRow(userName: String, greeting: String, emoji: String, onClick: () -> Unit) {
    val ext = LocalBrainRacerExtendedColors.current
    val g = ext.cardGradients
    val avatarStops = listOf(g[0][0], g[1][0])
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .pressClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(greeting, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 14.sp)
            Text("$userName! $emoji", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape)
                .background(Brush.linearGradient(avatarStops)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.firstOrNull()?.uppercase() ?: "?",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private data class GreetingUi(
    val greeting: String,
    val emoji: String
)

private fun getGreetingUi(): GreetingUi {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> GreetingUi("Доброе утро,", "🌅")
        in 12..17 -> GreetingUi("Добрый день,", "☀️")
        else -> GreetingUi("Добрый вечер,", "🌙")
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
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor   = MaterialTheme.colorScheme.background,
        contentColor     = MaterialTheme.colorScheme.primary,
        edgePadding      = 20.dp,
        indicator = { positions ->
            if (selectedIndex < positions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    color = MaterialTheme.colorScheme.primary, height = 3.dp
                )
            }
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp) }
    ) {
        tabs.forEachIndexed { i, name ->
            Tab(
                selected               = selectedIndex == i,
                onClick                = { onSelect(i) },
                selectedContentColor   = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    val cardGradients = LocalBrainRacerExtendedColors.current.cardGradients
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🔥", fontSize = 17.sp)
            Text("Популярное", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints {
            val useHorizontalLayout = maxWidth < 360.dp

            if (useHorizontalLayout) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(count = quizzes.size, key = { quizzes[it].id }) { i ->
                        val quiz = quizzes[i]
                        HotQuizCard(
                            quiz = quiz,
                            gradient = cardGradients[i % cardGradients.size],
                            modifier = Modifier.width(162.dp),
                            onQuizClick = onQuizClick
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    quizzes.forEachIndexed { i, quiz ->
                        HotQuizCard(
                            quiz = quiz,
                            gradient = cardGradients[i % cardGradients.size],
                            modifier = Modifier.weight(1f),
                            onQuizClick = onQuizClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotQuizCard(
    quiz: QuizItem,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onQuizClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .then(modifier)
            .height(136.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient, Offset.Zero, Offset(600f, 600f)))
            .pressClickable { onQuizClick(quiz.id) }
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.2f)) {
                Text(
                    "HOT",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column {
                Text(
                    quiz.category,
                    color = Color.White.copy(0.78f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    quiz.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Clip
                )
                Text(
                    "${quiz.questionCount} вопросов",
                    color = Color.White.copy(0.72f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AllQuizzesSection(
    quizzes: List<QuizItem>,
    isLoading: Boolean,
    currentCategory: String,
    listTitle: String,
    emptyMessage: String,
    onQuizClick: (String) -> Unit,
    onCreateQuiz: () -> Unit,
    onShowAll: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .bottomBarOcclusionEffect()
                .bottomBarOcclusionBlockClicks(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(listTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            TextButton(
                onClick = { onShowAll(currentCategory) },
                modifier = Modifier.pressScale(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Смотреть все", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(6.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            quizzes.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Default.Quiz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onCreateQuiz,
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
    val cardGradients = LocalBrainRacerExtendedColors.current.cardGradients
    val gradient = cardGradients[colorIndex % cardGradients.size]
    Box(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .pressClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
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
                Text(
                    quiz.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
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
    val listPreview = listShown.take(5)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .bottomBarOcclusionEffect()
                .bottomBarOcclusionBlockClicks(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "⚔️ Последние вызовы",
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = onNewChallenge,
                modifier = Modifier.pressScale(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cognition),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Вызов", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
        /*Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onViewAll,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Все вызовы", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
    }*/
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .bottomBarOcclusionEffect()
                .bottomBarOcclusionBlockClicks()
        ) {
            TabRow(
                modifier         = Modifier.fillMaxWidth(),
                selectedTabIndex = tabIndex,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { positions ->
                    if (tabIndex < positions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[tabIndex]),
                            color    = MaterialTheme.colorScheme.primary,
                            height   = 2.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
            ) {
                tabLabels.forEachIndexed { i, title ->
                    Tab(
                        selected = tabIndex == i,
                        onClick  = { tabIndex = i },
                        text     = { Text(title, fontSize = 13.sp, maxLines = 1) },
                        selectedContentColor   = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (listPreview.isEmpty()) {
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (tabIndex == 0) {
                        Button(
                            onClick     = onNewChallenge,
                            colors      = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape       = RoundedCornerShape(12.dp)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.cognition), null, Modifier.size(18.dp), Color.White)
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
            listPreview.forEach { ch ->
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
    val cs = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current
    val g = ext.cardGradients
    val rowAvatarGradient = listOf(g[0][0], g[1][0])
    val isChallenger = ch.challengerUserId == currentUserId
    val opponentName = if (isChallenger) ch.challengedNickname else ch.challengerNickname
    val myResult     = if (isChallenger) ch.challengerResult else ch.challengedResult
    val oppResult    = if (isChallenger) ch.challengedResult else ch.challengerResult

    val (statusColor, statusLabel) = when {
        ch.status == ChallengeStatus.PENDING && !isChallenger ->
            ext.statusOrange to "Входящий"
        ch.status == ChallengeStatus.PENDING && isChallenger ->
            ext.statusOrange to "В обработке"
        ch.status == ChallengeStatus.ACCEPTED ->
            cs.tertiary to when {
                ch.hasUserResult(currentUserId) -> "Пройдено"
                else -> "Нужно пройти"
            }
        ch.status == ChallengeStatus.COMPLETED -> {
            val isDraw = ch.isDraw || ch.winnerId == "draw"
            val isWin  = !isDraw && ch.winnerId == currentUserId
            when {
                isDraw -> ext.statusOrange to "Ничья"
                isWin  -> ext.detailGreen to "Победа"
                else   -> cs.error to "Поражение"
            }
        }
        else -> cs.onSurfaceVariant to ch.status.name
    }

    val isFinishedRow = ch.status == ChallengeStatus.COMPLETED
    Box(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .alpha(if (isFinishedRow) 0.82f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, statusColor.copy(0.3f), RoundedCornerShape(16.dp))
            .pressClickable(onClick = onClick)
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
                        .background(Brush.linearGradient(rowAvatarGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(opponentName.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "vs $opponentName",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        ch.quizTitle,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (ch.status == ChallengeStatus.COMPLETED) {
                    Text(
                        "${myResult?.score ?: 0} : ${oppResult?.score ?: 0}",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
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
    val cs = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current
    val color = if (played) ext.detailGreen else cs.onSurfaceVariant
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