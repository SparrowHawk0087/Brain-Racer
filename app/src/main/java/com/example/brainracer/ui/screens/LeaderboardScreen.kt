package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.brainracer.data.repositories.ChallengeRepositoryImpl
import com.example.brainracer.data.repositories.QuizRepositoryImpl
import com.example.brainracer.data.utils.Result
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.LevelSystem
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.components.bottomBarOcclusionBlockClicks
import com.example.brainracer.ui.components.bottomBarOcclusionEffect
import com.example.brainracer.ui.components.bottomBarSafePadding
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.utils.AppMotionConfig
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.toQuizItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private data class LeaderboardRow(
    val rank: Int,
    val userId: String,
    val nickname: String,
    val points: Int,
    val totalGames: Int,
    val accuracyPercent: Int,
    val user: User
)

private data class CardActionHandlers(
    val onChallengeClick: () -> Unit,
    val onAddFriendClick: () -> Unit,
    val onRemoveFriendClick: () -> Unit,
    val onOpenProfileClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    currentUserId: String,
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "leaderboard",
    bottomBarLoggedInUserId: String? = null,
    bottomBarProfileDestinationUserId: String? = null,
    bottomBarShowChallengesIncomingBadge: Boolean = false
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompactWidth = configuration.screenWidthDp < 390
    val scope = rememberCoroutineScope()
    val ext = LocalBrainRacerExtendedColors.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val challengeRepository = remember { ChallengeRepositoryImpl() }
    val quizRepository = remember { QuizRepositoryImpl() }

    var rows by remember { mutableStateOf<List<LeaderboardRow>>(emptyList()) }
    var friendIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedUserId by remember { mutableStateOf<String?>(null) }
    val leaderboardListState = rememberLazyListState()
    val bottomReflexShift by remember {
        derivedStateOf {
            ((leaderboardListState.firstVisibleItemIndex * 4f) +
                    leaderboardListState.firstVisibleItemScrollOffset * 0.012f).coerceIn(0f, 18f)
        }
    }

    var showChallengeSheet by remember { mutableStateOf(false) }
    var challengeTarget by remember { mutableStateOf<User?>(null) }
    var challengeQuizzes by remember { mutableStateOf<List<QuizItem>>(emptyList()) }
    var challengeLoading by remember { mutableStateOf(false) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val challengeButtonBrush = remember {
        Brush.horizontalGradient(
            listOf(BrainRacerColorTokens.Accent, BrainRacerColorTokens.AccentSecondary)
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                if (currentUserId.isNotBlank()) {
                    val meDoc = firestore.collection("users").document(currentUserId).get().await()
                    friendIds = (meDoc.get("friends") as? List<String>).orEmpty().toSet()
                } else {
                    friendIds = emptySet()
                }

                val snap = firestore
                    .collection("users")
                    .orderBy("stats.total_points", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                rows = snap.documents.mapIndexed { index, doc ->
                    val user = (doc.toObject(User::class.java) ?: User()).copy(id = doc.id)
                    val nick = user.nickname.takeIf { it.isNotBlank() } ?: "Игрок"
                    val pts = user.stats.totalPoints
                    val games = user.stats.totalQuizzesTaken
                    val answered = user.stats.totalQuestionsAnswered
                    val accuracy = if (answered > 0) {
                        ((user.stats.correctAnswers.toDouble() / answered) * 100).toInt()
                    } else {
                        0
                    }
                    LeaderboardRow(
                        rank = index + 1,
                        userId = doc.id,
                        nickname = nick,
                        points = pts,
                        totalGames = games,
                        accuracyPercent = accuracy,
                        user = user
                    )
                }
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
                rows = emptyList()
            }
            loading = false
        }
    }

    if (showChallengeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChallengeSheet = false },
            sheetState = challengeSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            ChallengeFriendQuizSheetContent(
                fixedFriend = challengeTarget,
                friends = challengeTarget?.let { listOf(it) } ?: emptyList(),
                quizzes = challengeQuizzes,
                isLoading = challengeLoading,
                onDismiss = { showChallengeSheet = false },
                onSendChallenge = { friendId, quizId, quizTitle ->
                    scope.launch {
                        val challengerId = FirebaseAuth.getInstance().currentUser?.uid
                        if (challengerId.isNullOrBlank()) {
                            Toast.makeText(context, "Нужно войти в аккаунт", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val challenge = Challenge(
                            quizId = quizId,
                            quizTitle = quizTitle,
                            challengerUserId = challengerId,
                            challengedUserId = friendId
                        )
                        when (val result = challengeRepository.createChallenge(challenge)) {
                            is Result.Success -> {
                                Toast.makeText(context, "Вызов отправлен", Toast.LENGTH_SHORT).show()
                                showChallengeSheet = false
                            }
                            is Result.Error -> {
                                Toast.makeText(
                                    context,
                                    result.exception.message ?: "Не удалось отправить вызов",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Лидерборд",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Топ игроков по XP",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
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
                loggedInUserId = bottomBarLoggedInUserId,
                profileDestinationUserId = bottomBarProfileDestinationUserId,
                showChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge,
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
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                else -> {
                    val topThree = rows.take(3)
                    val myRow = rows.firstOrNull { it.userId == currentUserId }
                    val expandedTopRow = topThree.firstOrNull { it.userId == expandedUserId }
                    val leaderboardBottomInset = bottomBarSafePadding(padding)
                    LazyColumn(
                        state = leaderboardListState,
                        contentPadding = PaddingValues(
                            start = if (isCompactWidth) 14.dp else 20.dp,
                            end = if (isCompactWidth) 14.dp else 20.dp,
                            top = 16.dp,
                            bottom = leaderboardBottomInset
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (myRow != null) {
                            item {
                                Text(
                                    "Моя позиция",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                MyPositionCard(row = myRow)
                            }
                        }

                        if (topThree.isNotEmpty()) {
                            item {
                                Text(
                                    "Топ игроки",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                TopThreePodium(
                                    topThree = topThree,
                                    gradientPool = ext.cardGradients,
                                    compact = isCompactWidth,
                                    onSelect = { userId ->
                                        expandedUserId = if (expandedUserId == userId) null else userId
                                    }
                                )
                            }
                        }

                        item {
                            val slideSpring: SpringSpec<IntOffset> = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                            AnimatedContent(
                                targetState = expandedTopRow,
                                transitionSpec = {
                                    val isOpenClose = initialState == null || targetState == null
                                    if (isOpenClose) {
                                        (
                                                slideInVertically(slideSpring) { it / 5 } +
                                                        fadeIn(tween(220)) +
                                                        scaleIn(initialScale = 0.96f, animationSpec = tween(220))
                                                ) togetherWith (
                                                slideOutVertically(tween(200)) { -it / 6 } +
                                                        fadeOut(tween(180)) +
                                                        scaleOut(targetScale = 0.96f, animationSpec = tween(180))
                                                )
                                    } else {
                                        val direction =
                                            if ((targetState?.rank ?: 0) > (initialState?.rank ?: 0)) 1 else -1
                                        (
                                                slideInHorizontally(slideSpring) { fullWidth ->
                                                    direction * fullWidth / 5
                                                } + fadeIn(tween(220))
                                                ) togetherWith (
                                                slideOutHorizontally(slideSpring) { fullWidth ->
                                                    -direction * fullWidth / 5
                                                } + fadeOut(tween(200))
                                                )
                                    }
                                },
                                label = "topPlayerCardAnimation"
                            ) { animatedTopRow ->
                                if (animatedTopRow != null) {
                                    LeaderboardPlayerCard(
                                        row = animatedTopRow,
                                        isSelf = animatedTopRow.userId == currentUserId,
                                        expanded = true,
                                        isFriend = friendIds.contains(animatedTopRow.userId),
                                        canAct = animatedTopRow.userId != currentUserId && currentUserId.isNotBlank(),
                                        gradientPool = ext.cardGradients,
                                        challengeButtonBrush = challengeButtonBrush,
                                        compact = isCompactWidth,
                                        onToggleExpanded = { expandedUserId = null },
                                        actions = rememberCardActionHandlers(
                                            row = animatedTopRow,
                                            currentUserId = currentUserId,
                                            firestore = firestore,
                                            onSetChallengeTarget = { target ->
                                                challengeTarget = target
                                                showChallengeSheet = true
                                            },
                                            onEnsureQuizzes = {
                                                scope.launch {
                                                    if (challengeQuizzes.isEmpty()) {
                                                        challengeLoading = true
                                                        challengeQuizzes = when (val r = quizRepository.getPopularQuizzes(limit = 80)) {
                                                            is Result.Success -> r.data.map { it.toQuizItem() }
                                                            is Result.Error -> emptyList()
                                                        }
                                                        challengeLoading = false
                                                    }
                                                }
                                            },
                                            onAddFriendSuccess = { Toast.makeText(context, "Запрос в друзья отправлен", Toast.LENGTH_SHORT).show() },
                                            onAddFriendFail = { Toast.makeText(context, "Не удалось отправить запрос", Toast.LENGTH_SHORT).show() },
                                            onRemoveFriendSuccess = {
                                                friendIds = friendIds - animatedTopRow.userId
                                                Toast.makeText(context, "Удалено из друзей", Toast.LENGTH_SHORT).show()
                                            },
                                            onRemoveFriendFail = { Toast.makeText(context, "Не удалось удалить из друзей", Toast.LENGTH_SHORT).show() },
                                            onOpenProfile = { uid -> navController.navigate("profile/$uid") },
                                            scope = scope
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                "Остальные",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        itemsIndexed(rows.drop(3), key = { _, r -> r.userId }) { _, row ->
                            LeaderboardPlayerCard(
                                row = row,
                                isSelf = row.userId == currentUserId,
                                expanded = expandedUserId == row.userId,
                                isFriend = friendIds.contains(row.userId),
                                canAct = row.userId != currentUserId && currentUserId.isNotBlank(),
                                gradientPool = ext.cardGradients,
                                challengeButtonBrush = challengeButtonBrush,
                                compact = isCompactWidth,
                                onToggleExpanded = {
                                    expandedUserId = if (expandedUserId == row.userId) null else row.userId
                                },
                                actions = rememberCardActionHandlers(
                                    row = row,
                                    currentUserId = currentUserId,
                                    firestore = firestore,
                                    onSetChallengeTarget = { target ->
                                        challengeTarget = target
                                        showChallengeSheet = true
                                    },
                                    onEnsureQuizzes = {
                                        scope.launch {
                                            if (challengeQuizzes.isEmpty()) {
                                                challengeLoading = true
                                                challengeQuizzes = when (val r = quizRepository.getPopularQuizzes(limit = 80)) {
                                                    is Result.Success -> r.data.map { it.toQuizItem() }
                                                    is Result.Error -> emptyList()
                                                }
                                                challengeLoading = false
                                            }
                                        }
                                    },
                                    onAddFriendSuccess = { Toast.makeText(context, "Запрос в друзья отправлен", Toast.LENGTH_SHORT).show() },
                                    onAddFriendFail = { Toast.makeText(context, "Не удалось отправить запрос", Toast.LENGTH_SHORT).show() },
                                    onRemoveFriendSuccess = {
                                        friendIds = friendIds - row.userId
                                        Toast.makeText(context, "Удалено из друзей", Toast.LENGTH_SHORT).show()
                                    },
                                    onRemoveFriendFail = { Toast.makeText(context, "Не удалось удалить из друзей", Toast.LENGTH_SHORT).show() },
                                    onOpenProfile = { uid -> navController.navigate("profile/$uid") },
                                    scope = scope
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPositionCard(row: LeaderboardRow) {
    val level = LevelSystem.levelFromXp(row.points)
    val rankName = row.user.rank.displayName
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RankPill(rank = row.rank, highlighted = true)
                Column(Modifier.weight(1f)) {
                    Text(
                        row.nickname,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Ур. $level · $rankName",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LeaderStatsChip(modifier = Modifier.weight(1f), value = row.totalGames.toString(), label = "Игр")
                LeaderStatsChip(modifier = Modifier.weight(1f), value = row.points.toString(), label = "XP")
                LeaderStatsChip(modifier = Modifier.weight(1f), value = "${row.accuracyPercent}%", label = "Точность")
            }
        }
    }
}

@Composable
private fun TopThreePodium(
    topThree: List<LeaderboardRow>,
    gradientPool: List<List<Color>>,
    compact: Boolean,
    onSelect: (String) -> Unit
) {
    val second = topThree.getOrNull(1)
    val first = topThree.getOrNull(0)
    val third = topThree.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumSlot(
                row = second,
                gradient = gradientPool[(second.rank - 1).mod(gradientPool.size)],
                compact = compact,
                modifier = Modifier.weight(1f).height(if (compact) 154.dp else 162.dp),
                onClick = { onSelect(second.userId) }
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (first != null) {
            PodiumSlot(
                row = first,
                gradient = gradientPool[(first.rank - 1).mod(gradientPool.size)],
                compact = compact,
                modifier = Modifier.weight(1f).height(if (compact) 176.dp else 194.dp),
                onClick = { onSelect(first.userId) }
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (third != null) {
            PodiumSlot(
                row = third,
                gradient = gradientPool[(third.rank - 1).mod(gradientPool.size)],
                compact = compact,
                modifier = Modifier.weight(1f).height(if (compact) 154.dp else 162.dp),
                onClick = { onSelect(third.userId) }
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSlot(
    row: LeaderboardRow,
    gradient: List<Color>,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pressY = remember { Animatable(0f) }
    val pressAlpha = remember { Animatable(1f) }
    var pulseJob by remember { mutableStateOf<Job?>(null) }
    val tapLiftPx = with(density) { (if (compact) 2.dp else 2.5.dp).toPx() }
    val pressMs = 170
    val releaseAlphaMs = 220
    val pressAlphaTarget = 0.97f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer {
                translationY = pressY.value
                alpha = pressAlpha.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
                pulseJob?.cancel()
                pulseJob = scope.launch {
                    coroutineScope {
                        pressY.snapTo(0f)
                        pressAlpha.snapTo(1f)
                        listOf(
                            async {
                                pressY.animateTo(
                                    -tapLiftPx,
                                    animationSpec = tween(pressMs, easing = FastOutSlowInEasing)
                                )
                            },
                            async {
                                pressAlpha.animateTo(
                                    pressAlphaTarget,
                                    animationSpec = tween(pressMs, easing = FastOutSlowInEasing)
                                )
                            }
                        ).awaitAll()
                        listOf(
                            async {
                                pressY.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 290f)
                                )
                            },
                            async {
                                pressAlpha.animateTo(
                                    1f,
                                    animationSpec = tween(releaseAlphaMs, easing = FastOutSlowInEasing)
                                )
                            }
                        ).awaitAll()
                    }
                }
            }
            .border(
                1.dp,
                if (row.rank == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RankPill(rank = row.rank, highlighted = row.rank == 1)
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.nickname.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 16.sp else 18.sp
                )
            }
            Spacer(Modifier.height(5.dp))
            Spacer(Modifier.weight(1f))
            Text(
                text = row.nickname,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 11.sp else 12.sp,
                    lineHeight = if (compact) 14.sp else 15.sp,
                    textAlign = TextAlign.Center,
                    lineBreak = LineBreak.Paragraph,
                    hyphens = Hyphens.None
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${row.points} XP",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LeaderboardPlayerCard(
    row: LeaderboardRow,
    isSelf: Boolean,
    expanded: Boolean,
    isFriend: Boolean,
    canAct: Boolean,
    gradientPool: List<List<Color>>,
    challengeButtonBrush: Brush,
    compact: Boolean,
    onToggleExpanded: () -> Unit,
    actions: CardActionHandlers
) {
    val expandMotion = AppMotionConfig.expand
    val gradient = gradientPool[(row.rank - 1).mod(gradientPool.size)]
    Column(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleExpanded
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = expandMotion.sizeDampingRatio,
                    stiffness = expandMotion.sizeStiffness
                )
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RankPill(rank = row.rank, highlighted = row.rank <= 3)
            Box(
                modifier = Modifier
                    .size(if (compact) 38.dp else 42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.nickname.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 16.sp else 18.sp
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    row.nickname,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 16.sp else 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${row.points} очков · Ур. ${LevelSystem.levelFromXp(row.points)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compact) 12.sp else 13.sp
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeaderStatsChip(modifier = Modifier.weight(1f), value = row.totalGames.toString(), label = "Игр")
                    LeaderStatsChip(modifier = Modifier.weight(1f), value = row.points.toString(), label = "XP")
                    LeaderStatsChip(modifier = Modifier.weight(1f), value = "${row.accuracyPercent}%", label = "Точность")
                }
                Spacer(Modifier.height(10.dp))
                if (compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientActionButton(
                            text = "Вызов",
                            brush = challengeButtonBrush,
                            enabled = canAct,
                            compact = true,
                            modifier = Modifier.weight(1f),
                            onClick = actions.onChallengeClick
                        )
                        OutlinedButton(
                            onClick = actions.onAddFriendClick,
                            enabled = canAct && !isFriend,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                        ) {
                            Text(
                                text = "Добавить",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = actions.onRemoveFriendClick,
                        enabled = canAct && isFriend,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
                    ) {
                        Text(
                            text = "Удалить",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientActionButton(
                            text = "Вызов",
                            brush = challengeButtonBrush,
                            enabled = canAct,
                            compact = false,
                            modifier = Modifier.weight(1f),
                            onClick = actions.onChallengeClick
                        )
                        OutlinedButton(
                            onClick = actions.onAddFriendClick,
                            enabled = canAct && !isFriend,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                        ) {
                            Text(
                                text = "Добавить",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                        }
                        OutlinedButton(
                            onClick = actions.onRemoveFriendClick,
                            enabled = canAct && isFriend,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                        ) {
                            Text(
                                text = "Удалить",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = actions.onOpenProfileClick,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Открыть профиль игрока") }
            }
        }
    }
}

@Composable
private fun LeaderStatsChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RankPill(rank: Int, highlighted: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            "#$rank",
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    brush: Brush,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(if (compact) 42.dp else 46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 14.sp else 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberCardActionHandlers(
    row: LeaderboardRow,
    currentUserId: String,
    firestore: FirebaseFirestore,
    onSetChallengeTarget: (User) -> Unit,
    onEnsureQuizzes: () -> Unit,
    onAddFriendSuccess: () -> Unit,
    onAddFriendFail: () -> Unit,
    onRemoveFriendSuccess: () -> Unit,
    onRemoveFriendFail: () -> Unit,
    onOpenProfile: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
): CardActionHandlers {
    return remember(row.userId, currentUserId) {
        CardActionHandlers(
            onChallengeClick = {
                onSetChallengeTarget(row.user)
                onEnsureQuizzes()
            },
            onAddFriendClick = {
                scope.launch {
                    val ok = sendFriendRequest(
                        firestore = firestore,
                        currentUserId = currentUserId,
                        receiverId = row.userId
                    )
                    if (ok) onAddFriendSuccess() else onAddFriendFail()
                }
            },
            onRemoveFriendClick = {
                scope.launch {
                    val ok = removeFriend(
                        firestore = firestore,
                        currentUserId = currentUserId,
                        friendId = row.userId
                    )
                    if (ok) onRemoveFriendSuccess() else onRemoveFriendFail()
                }
            },
            onOpenProfileClick = { onOpenProfile(row.userId) }
        )
    }
}

private suspend fun sendFriendRequest(
    firestore: FirebaseFirestore,
    currentUserId: String,
    receiverId: String
): Boolean {
    return try {
        if (currentUserId.isBlank() || receiverId.isBlank() || currentUserId == receiverId) {
            false
        } else {
            val meDoc = firestore.collection("users").document(currentUserId).get().await()
            val alreadyFriends = (meDoc.get("friends") as? List<String>).orEmpty().contains(receiverId)
            if (alreadyFriends) {
                false
            } else {
                val existing = firestore.collection("friend_requests")
                    .whereEqualTo("senderId", currentUserId)
                    .whereEqualTo("receiverId", receiverId)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()
                if (!existing.isEmpty) {
                    false
                } else {
                    val doc = firestore.collection("friend_requests").document()
                    val now = Timestamp.now()
                    doc.set(
                        mapOf(
                            "id" to doc.id,
                            "senderId" to currentUserId,
                            "receiverId" to receiverId,
                            "status" to "PENDING",
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    ).await()
                    true
                }
            }
        }
    } catch (_: Exception) {
        false
    }
}

private suspend fun removeFriend(
    firestore: FirebaseFirestore,
    currentUserId: String,
    friendId: String
): Boolean {
    return try {
        if (currentUserId.isBlank() || friendId.isBlank() || currentUserId == friendId) {
            false
        } else {
            firestore.runTransaction { tr ->
                val meRef = firestore.collection("users").document(currentUserId)
                val frRef = firestore.collection("users").document(friendId)
                tr.update(meRef, "friends", FieldValue.arrayRemove(friendId))
                tr.update(frRef, "friends", FieldValue.arrayRemove(currentUserId))
                null
            }.await()
            true
        }
    } catch (_: Exception) {
        false
    }
}
