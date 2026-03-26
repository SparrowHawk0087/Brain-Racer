package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.domain.entities.Challenge
import com.example.brainracer.domain.entities.ChallengeStatus
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.viewmodels.ChallengeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChallengesScreen(
    navController: NavController,
    currentUserId: String,
    vm: ChallengeViewModel = viewModel(),
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "challenges"
) {
    val uiState     by vm.uiState.collectAsState()
    val context     = LocalContext.current
    val pagerState  = rememberPagerState { 3 }
    val scope       = rememberCoroutineScope()
    val tabTitles   = listOf("Входящие", "Активные", "Завершённые")

    var showChallengeSheet by remember { mutableStateOf(false) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.challengeSentMessage) {
        uiState.challengeSentMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeChallengeSentMessage()
            showChallengeSheet = false
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    LaunchedEffect(showChallengeSheet) {
        if (showChallengeSheet) vm.loadChallengePickerData()
    }

    // Badge counts
    val incomingCount = uiState.incomingChallenges.size
    val activeCount   = uiState.activeChallenges.size +
            uiState.outgoingChallenges.count { it.status == ChallengeStatus.PENDING }

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
                    vm.sendChallengeFromPicker(fid, qid, title)
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Вызовы", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                },
                actions = {
                    TextButton(onClick = { showChallengeSheet = true }) {
                        Icon(
                            Icons.Default.Sports,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Вызов", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            BottomBar(
                showBar              = true,
                currentRoute         = currentRoute,
                onHomeClick          = onHomeClick,
                onLeaderboardClick   = onLeaderboardClick,
                onChallengesClick    = onChallengesClick,
                onQuizzesClick       = onQuizzesClick,
                onProfileClick       = onProfileClick
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Вкладки ───────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { positions ->
                    if (pagerState.currentPage < positions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(positions[pagerState.currentPage])
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            color    = MaterialTheme.colorScheme.primary, height = 3.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
            ) {
                tabTitles.forEachIndexed { i, title ->
                    val badge = when (i) {
                        0 -> incomingCount
                        1 -> activeCount
                        else -> 0
                    }
                    Tab(
                        selected               = pagerState.currentPage == i,
                        onClick                = { scope.launch { pagerState.animateScrollToPage(i) } },
                        selectedContentColor   = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(title, fontSize = 13.sp,
                                fontWeight = if (pagerState.currentPage == i) FontWeight.SemiBold
                                else FontWeight.Normal)
                            if (badge > 0) {
                                Badge { Text("$badge") }
                            }
                        }
                    }
                }
            }

            // ── Контент страниц ───────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when {
                    uiState.isLoading -> CenteredLoader()
                    page == 0 -> IncomingTab(
                        challenges    = uiState.incomingChallenges,
                        currentUserId = currentUserId,
                        onAccept      = { vm.acceptChallenge(it.id) },
                        onDecline     = { vm.declineChallenge(it.id) }
                    )
                    page == 1 -> {
                        val outgoingPending = uiState.outgoingChallenges
                            .filter { it.status == ChallengeStatus.PENDING }
                        val mergedActive = (uiState.activeChallenges + outgoingPending)
                            .distinctBy { it.id }
                            .sortedWith(
                                compareByDescending<Challenge> { it.createdAt.seconds }
                                    .thenByDescending { it.createdAt.nanoseconds }
                            )
                        ActiveTab(
                            challenges    = mergedActive,
                            currentUserId = currentUserId,
                            onCancelPending = { vm.cancelChallenge(it.id) },
                            onPlay          = { challenge ->
                                navController.navigate(
                                    "quiz_play/${challenge.quizId}?challengeId=${challenge.id}"
                                )
                            },
                            onViewRound     = { challenge ->
                                navController.navigate("challenge_review/${challenge.id}")
                            }
                        )
                    }
                    else -> HistoryTab(
                        completed     = uiState.completedChallenges,
                        currentUserId = currentUserId,
                        onViewRound   = { challenge ->
                            navController.navigate("challenge_review/${challenge.id}")
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: ВХОДЯЩИЕ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun IncomingTab(
    challenges: List<Challenge>,
    currentUserId: String,
    onAccept: (Challenge) -> Unit,
    onDecline: (Challenge) -> Unit
) {
    if (challenges.isEmpty()) {
        EmptyState("Нет входящих вызовов", "Когда друг бросит вызов — он появится здесь")
        return
    }
    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(challenges, key = { it.id }) { ch ->
            IncomingChallengeCard(
                challenge = ch,
                onAccept  = { onAccept(ch) },
                onDecline = { onDecline(ch) }
            )
        }
    }
}

@Composable
private fun IncomingChallengeCard(
    challenge: Challenge,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarCircle(challenge.challengerNickname.take(2).uppercase(), MaterialTheme.colorScheme.primary, 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.challengerNickname, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("бросает вам вызов", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = LocalBrainRacerExtendedColors.current.statusOrange.copy(.15f)) {
                    Text("Новый", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LocalBrainRacerExtendedColors.current.statusOrange)
                }
            }

            // Викторина
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Quiz, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(challenge.quizTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }

            // Срок истечения
            Text(
                "Истекает: ${formatTimestamp(challenge.expiresAt.seconds * 1000)}",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Кнопки
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalBrainRacerExtendedColors.current.detailGreen)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Принять", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Отклонить", fontSize = 13.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: АКТИВНЫЕ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveTab(
    challenges: List<Challenge>,
    currentUserId: String,
    onCancelPending: (Challenge) -> Unit,
    onPlay: (Challenge) -> Unit,
    onViewRound: (Challenge) -> Unit
) {
    if (challenges.isEmpty()) {
        EmptyState("Нет активных вызовов", "Примите входящий вызов или бросьте вызов другу")
        return
    }
    val pendingOutgoing = challenges.filter {
        it.status == ChallengeStatus.PENDING && it.challengerUserId == currentUserId
    }.sortedWith(
        compareByDescending<Challenge> { it.createdAt.seconds }
            .thenByDescending { it.createdAt.nanoseconds }
    )
    val acceptedInPlay = challenges.filter { it.status == ChallengeStatus.ACCEPTED }
        .sortedWith(
            compareByDescending<Challenge> { it.createdAt.seconds }
                .thenByDescending { it.createdAt.nanoseconds }
        )

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pendingOutgoing.isNotEmpty()) {
            item {
                ActiveSectionHeader(
                    title    = "В обработке",
                    subtitle = "Ожидание принятия или отклонения вызова соперником"
                )
            }
            items(pendingOutgoing, key = { it.id }) { ch ->
                OutgoingPendingCard(challenge = ch, onCancel = { onCancelPending(ch) })
            }
        }
        if (acceptedInPlay.isNotEmpty()) {
            item {
                ActiveSectionHeader(
                    title    = "Принятые дуэли",
                    subtitle = "Можно проходить викторину, пока не истёк срок"
                )
            }
            items(acceptedInPlay, key = { it.id }) { ch ->
                ActiveChallengeCard(
                    challenge     = ch,
                    currentUserId = currentUserId,
                    onPlay        = { onPlay(ch) },
                    onViewRound   = { onViewRound(ch) }
                )
            }
        }
    }
}

@Composable
private fun ActiveSectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
    }
}

@Composable
private fun ActiveChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    onPlay: () -> Unit,
    onViewRound: () -> Unit
) {
    val isChallenger   = challenge.challengerUserId == currentUserId
    val opponentName   = if (isChallenger) challenge.challengedNickname else challenge.challengerNickname
    val myResult       = if (isChallenger) challenge.challengerResult else challenge.challengedResult
    val opponentResult = if (isChallenger) challenge.challengedResult else challenge.challengerResult
    val iAlreadyPlayed = myResult != null
    val bothPlayed     = myResult != null && opponentResult != null

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Соперник
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarCircle(opponentName.take(2).uppercase(), LocalBrainRacerExtendedColors.current.detailGreen, 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text("vs $opponentName", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(challenge.quizTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Статусы игроков (счёт скрыт до завершения обоих)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerStatusChip(
                    label  = "Вы",
                    played = iAlreadyPlayed,
                    score  = if (bothPlayed) myResult?.score else null
                )
                PlayerStatusChip(
                    label  = opponentName.split(" ").first(),
                    played = opponentResult != null,
                    score  = if (bothPlayed) opponentResult?.score else null
                )
            }

            // Кнопки
            if (!iAlreadyPlayed) {
                Button(
                    onClick  = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Пройти викторину", fontWeight = FontWeight.SemiBold)
                }
            } else if (bothPlayed) {
                OutlinedButton(
                    onClick  = onViewRound,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Analytics, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Просмотр раунда")
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.outline
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(14.dp),
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ждём хода соперника…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatusChip(label: String, played: Boolean, score: Int?) {
    Surface(
        shape  = RoundedCornerShape(8.dp),
        color  = if (played) LocalBrainRacerExtendedColors.current.detailGreen.copy(.12f) else MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (played) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (played) LocalBrainRacerExtendedColors.current.detailGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (score != null) "$label: $score" else label,
                fontSize = 12.sp,
                color    = if (played) LocalBrainRacerExtendedColors.current.detailGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ВКЛАДКА: ИСТОРИЯ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryTab(
    completed: List<Challenge>,
    currentUserId: String,
    onViewRound: (Challenge) -> Unit
) {
    if (completed.isEmpty()) {
        EmptyState("Нет завершённых вызовов", "Завершённые дуэли появятся здесь")
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(completed, key = { it.id }) { ch ->
            CompletedChallengeCard(
                challenge     = ch,
                currentUserId = currentUserId,
                onViewRound   = { onViewRound(ch) }
            )
        }
    }
}

@Composable
private fun OutgoingPendingCard(challenge: Challenge, onCancel: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarCircle(challenge.challengedNickname.take(2).uppercase(), LocalBrainRacerExtendedColors.current.statusOrange, 42)
            Column(modifier = Modifier.weight(1f)) {
                Text("→ ${challenge.challengedNickname}", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(challenge.quizTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("В обработке · ждём соперника", fontSize = 11.sp, color = LocalBrainRacerExtendedColors.current.statusOrange)
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CompletedChallengeCard(
    challenge: Challenge,
    currentUserId: String,
    onViewRound: () -> Unit
) {
    val isChallenger   = challenge.challengerUserId == currentUserId
    val opponentName   = if (isChallenger) challenge.challengedNickname else challenge.challengerNickname
    val myScore        = if (isChallenger) challenge.challengerResult?.score ?: 0
    else challenge.challengedResult?.score ?: 0
    val opponentScore  = if (isChallenger) challenge.challengedResult?.score ?: 0
    else challenge.challengerResult?.score ?: 0

    val isDraw = challenge.isDraw || challenge.winnerId == "draw"
    val isWin  = !isDraw && challenge.winnerId == currentUserId
    val (resultLabel, resultColor) = when {
        isDraw -> "Ничья" to LocalBrainRacerExtendedColors.current.statusOrange
        isWin  -> "Победа" to LocalBrainRacerExtendedColors.current.detailGreen
        else   -> "Поражение" to MaterialTheme.colorScheme.error
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onViewRound() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarCircle(opponentName.take(2).uppercase(), MaterialTheme.colorScheme.primary, 42)
            Column(modifier = Modifier.weight(1f)) {
                Text("vs $opponentName", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(challenge.quizTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatTimestamp(challenge.completedAt?.seconds?.times(1000) ?: 0L),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$myScore : $opponentScore", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Surface(shape = RoundedCornerShape(6.dp), color = resultColor.copy(.15f)) {
                    Text(resultLabel,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize   = 11.sp, fontWeight = FontWeight.SemiBold, color = resultColor)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Общие компоненты ──────────────────────────────────────────────────────────

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), Alignment.Center) {
                Icon(Icons.Default.Sports, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AvatarCircle(initials: String, color: Color, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color.copy(.2f))
            .border(1.dp, color.copy(.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = color, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    return SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(millis))
}

