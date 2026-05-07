package com.example.brainracer.ui.screens

import android.content.Intent
import android.widget.Toast
import com.example.brainracer.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.components.bottomBarOcclusionBlockClicks
import com.example.brainracer.ui.components.bottomBarOcclusionEffect
import com.example.brainracer.ui.components.bottomBarSafePadding
import com.example.brainracer.ui.components.pressClickable
import com.example.brainracer.ui.utils.FriendRequestUi
import com.example.brainracer.ui.utils.OutgoingRequestUi
import com.example.brainracer.ui.viewmodels.FriendsViewModel
// ─────────────────────────────────────────────────────────────────────────────
//  Главный composable-экран
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = viewModel(),
    viewedUserId: String = "",
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "friends",
    bottomBarLoggedInUserId: String? = null,
    bottomBarProfileDestinationUserId: String? = null,
    bottomBarShowChallengesIncomingBadge: Boolean = false,
    /** Из превью викторины: после загрузки названия — нажатие на карточку друга шлёт вызов */
    preselectChallengeQuizIdArg: String? = null
) {
    // Подписываемся на единое состояние ViewModel.
    // Каждый раз, когда ViewModel обновляет _uiState, Compose автоматически
    // перерисует только те части дерева, которые читают изменившиеся поля.
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val myUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val targetUserId = viewedUserId.ifBlank { myUserId }
    val isOwnFriendsList = targetUserId == myUserId

    var challengeTargetFriend by remember { mutableStateOf<User?>(null) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchBarVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preselectChallengeQuizIdArg) {
        viewModel.setPreselectedChallengeQuiz(preselectChallengeQuizIdArg)
    }

    LaunchedEffect(uiState.challengeSentMessage) {
        uiState.challengeSentMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeChallengeSentMessage()
            challengeTargetFriend = null
        }
    }

    // Локальное состояние только для текста поля поиска —
    // так мы избегаем лишних emit-ов в ViewModel при каждом нажатии клавиши.
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }

    val tabs = if (isOwnFriendsList) listOf("Мои друзья", "Входящие", "Исходящие") else listOf("Друзья")

    LaunchedEffect(targetUserId) {
        viewModel.loadForUser(targetUserId)
    }

    // Фильтрация на стороне UI (ViewModel уже возвращает полные списки).
    val filteredFriends = uiState.friends.filter {
        it.nickname.contains(searchQuery, ignoreCase = true)
    }
    val filteredIncoming = uiState.incomingRequests.filter {
        it.senderName.contains(searchQuery, ignoreCase = true)
    }
    val filteredOutgoing = uiState.outgoingRequests.filter {
        it.receiverName.contains(searchQuery, ignoreCase = true)
    }

    if (challengeTargetFriend != null) {
        ModalBottomSheet(
            onDismissRequest = { challengeTargetFriend = null },
            sheetState       = challengeSheetState
        ) {
            val f = challengeTargetFriend!!
            LaunchedEffect(f.id) { viewModel.loadChallengePickerQuizzes() }
            ChallengeFriendQuizSheetContent(
                fixedFriend   = f,
                friends       = emptyList(),
                quizzes       = uiState.challengePickerQuizzes,
                isLoading     = uiState.challengePickerLoading,
                onDismiss     = { challengeTargetFriend = null },
                onSendChallenge = { fid, qid, title ->
                    viewModel.sendChallenge(fid, qid, title)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Друзья",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate("profile/$targetUserId")
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_back_btn),
                                contentDescription = "Назад к профилю",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = {
                        searchBarVisible = !searchBarVisible
                        if (!searchBarVisible) {
                            searchQuery = ""
                            viewModel.searchUsers("")
                        }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.person_search),
                                contentDescription = "Показать поиск",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomBar(
                showBar                  = true,
                currentRoute             = currentRoute,
                loggedInUserId           = bottomBarLoggedInUserId,
                profileDestinationUserId = bottomBarProfileDestinationUserId,
                showChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge,
                onHomeClick              = onHomeClick,
                onLeaderboardClick       = onLeaderboardClick,
                onChallengesClick        = onChallengesClick,
                onQuizzesClick           = onQuizzesClick,
                onProfileClick           = onProfileClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = bottomBarSafePadding(paddingValues, extraBottom = 8.dp)
                )
        ) {
            val preId = uiState.preselectChallengeQuizId
            if (preId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = when {
                            uiState.preselectChallengeQuizLoading ->
                                "Загрузка викторины… Нажмите на друга, чтобы бросить вызов."
                            else ->
                                "Нажмите на друга — вызов в «${uiState.preselectChallengeQuizTitle ?: "…"}»"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ── Поле поиска ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = searchBarVisible || searchQuery.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        // Передаём запрос в ViewModel, чтобы она обновила
                        // searchResults (поиск по Firestore выполняется там).
                        viewModel.searchUsers(query)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Найти пользователей…") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search_btn),
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.searchUsers("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // ── Результаты поиска (показываем поверх вкладок) ──────────────
            if (uiState.isSearching && uiState.searchResults.isNotEmpty()) {
                SearchResultsList(
                    results = uiState.searchResults,
                    onSendRequest = { user -> viewModel.sendFriendRequest(user.id) },
                    onOpenProfile = { user -> navController.navigate("profile/${user.id}") }
                )
            } else {

                // ── Вкладки ────────────────────────────────────────────────
                var selectedTab by remember(targetUserId) { mutableIntStateOf(0) }

                // Показываем badge с количеством входящих заявок
                val incomingBadge = uiState.incomingRequests.size

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .padding(horizontal = 14.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                // На вкладке «Входящие» показываем счётчик
                                if (isOwnFriendsList && index == 1 && incomingBadge > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge { Text("$incomingBadge") }
                                    }
                                } else {
                                    Text(title, fontSize = 13.sp)
                                }
                            }
                        )
                    }
                }

                // ── Контент вкладок ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        when (selectedTab) {
                            0 -> FriendsTab(
                                friends = filteredFriends,
                                onDelete = { friend -> viewModel.removeFriend(friend.id) },
                                onChallengeFriend = { if (isOwnFriendsList) challengeTargetFriend = it },
                                onOpenProfile = { friend ->
                                    navController.navigate("profile/${friend.id}")
                                },
                                quickChallengeFromQuiz = preId != null,
                                quickChallengeReady = preId != null &&
                                        !uiState.preselectChallengeQuizLoading &&
                                        !uiState.preselectChallengeQuizTitle.isNullOrBlank(),
                                onQuickChallenge = { friend ->
                                    val qid = uiState.preselectChallengeQuizId
                                    val title = uiState.preselectChallengeQuizTitle
                                    if (qid != null && title != null) {
                                        viewModel.sendChallenge(friend.id, qid, title)
                                    }
                                }
                            )
                            1 -> if (isOwnFriendsList) IncomingRequestsTab(
                                requests = filteredIncoming,
                                onAccept = { req ->
                                    viewModel.acceptFriendRequest(req.id, req.senderId)
                                },
                                onDecline = { req ->
                                    viewModel.declineFriendRequest(req.id)
                                },
                                onOpenProfile = { senderId ->
                                    navController.navigate("profile/$senderId")
                                }
                            )
                            2 -> if (isOwnFriendsList) OutgoingRequestsTab(
                                requests = filteredOutgoing,
                                onCancel = { req ->
                                    viewModel.cancelOutgoingRequest(req.id)
                                },
                                onOpenProfile = { receiverId ->
                                    navController.navigate("profile/$receiverId")
                                }
                            )
                        }
                    }
                }

                // ── Кнопка «Пригласить» ────────────────────────────────────
                if (isOwnFriendsList) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Brain Racer")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Присоединяйся ко мне в Brain Racer — викторины и дуэли с друзьями!\n" +
                                            "Приложение: com.example.brainracer"
                                )
                            }
                            try {
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Пригласить друзей")
                                )
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "Не удалось открыть окно отправки",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .shadow(14.dp, RoundedCornerShape(14.dp))
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 14.dp,
                            hoveredElevation = 12.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.person_add),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Пригласить друзей",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Сообщение об ошибке ────────────────────────────────────────
            uiState.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.loadFriends() }) {
                            Text("Повторить")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Результаты поиска новых пользователей
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsList(
    results: List<User>,
    onSendRequest: (User) -> Unit,
    onOpenProfile: (User) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { it.id }) { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватар-инициалы из никнейма
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenProfile(user) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(
                            initials = user.nickname.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.secondary,
                            size = 48
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.nickname,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user.rank.displayName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onSendRequest(user) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.person_add),
                            contentDescription = "Добавить в друзья",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Вкладка «My friends»
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FriendsTab(
    friends: List<User>,
    onDelete: (User) -> Unit,
    onChallengeFriend: (User) -> Unit,
    onOpenProfile: (User) -> Unit,
    quickChallengeFromQuiz: Boolean = false,
    quickChallengeReady: Boolean = false,
    onQuickChallenge: (User) -> Unit = {}
) {
    if (friends.isEmpty()) {
        EmptyState(message = "Пока нет друзей. Найдите людей через поиск выше.")
        return
    }
    val quickMode = quickChallengeFromQuiz
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!quickMode) {
            item {
                FriendRankingCard(friends = friends)
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Все друзья",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
        }
        items(friends, key = { it.id }) { friend ->
            FriendCard(
                friend = friend,
                onDelete = { onDelete(friend) },
                onChallenge = { onChallengeFriend(friend) },
                onOpenProfile = { onOpenProfile(friend) },
                quickChallengeMode = quickMode,
                quickChallengeReady = quickChallengeReady,
                onQuickChallenge = { onQuickChallenge(friend) }
            )
        }
    }
}

@Composable
private fun FriendCard(
    friend: User,
    onDelete: () -> Unit,
    onChallenge: () -> Unit,
    onOpenProfile: () -> Unit,
    quickChallengeMode: Boolean = false,
    quickChallengeReady: Boolean = false,
    onQuickChallenge: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val mainClick = when {
                quickChallengeMode && quickChallengeReady -> onQuickChallenge
                quickChallengeMode -> ({ })
                else -> onOpenProfile
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pressClickable(onClick = mainClick, enabled = !quickChallengeMode || quickChallengeReady),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarCircle(
                    initials = friend.nickname.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.tertiary,
                    size = 48
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.nickname,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            quickChallengeMode && quickChallengeReady ->
                                "Нажмите, чтобы бросить вызов"
                            quickChallengeMode ->
                                "Подождите…"
                            else ->
                                "${friend.rank.displayName} · ${friend.stats.totalPoints} очков"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!quickChallengeMode) {
                val duelShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(duelShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .pressClickable(onClick = onChallenge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cognition),
                        contentDescription = "Вызвать на дуэль",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                    .pressClickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.person_remove),
                    contentDescription = "Удалить из друзей",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.95f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FriendRankingCard(
    friends: List<User>
) {
    val sorted = remember(friends) {
        friends.sortedByDescending { it.stats.totalPoints }.take(3)
    }
    val maxPoints = remember(sorted) {
        sorted.maxOfOrNull { it.stats.totalPoints }?.coerceAtLeast(1) ?: 1
    }
    if (sorted.isEmpty()) return

    val extBarColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Рейтинг друзей",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        sorted.forEachIndexed { index, user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        else -> "🥉"
                    },
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(8.dp))
                AvatarCircle(
                    initials = user.nickname.take(2).uppercase(),
                    color = extBarColors[index % extBarColors.size].copy(alpha = 0.9f),
                    size = 34
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.nickname,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${user.stats.totalPoints} очков",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((user.stats.totalPoints.toFloat() / maxPoints).coerceIn(0.08f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(extBarColors[index % extBarColors.size])
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Вкладка «Incoming»
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IncomingRequestsTab(
    requests: List<FriendRequestUi>,
    onAccept: (FriendRequestUi) -> Unit,
    onDecline: (FriendRequestUi) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(message = "Нет входящих заявок")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            IncomingRequestCard(
                request = request,
                onAccept = { onAccept(request) },
                onDecline = { onDecline(request) },
                onOpenProfile = { onOpenProfile(request.senderId) }
            )
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequestUi,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClickable(onClick = onOpenProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarCircle(
                    initials = request.senderName.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.secondary,
                    size = 48
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.senderName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Хочет добавиться в друзья",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Принять
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Принять", fontSize = 13.sp)
                }
                // Отклонить
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отклонить", fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Вкладка «Outgoing»
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OutgoingRequestsTab(
    requests: List<OutgoingRequestUi>,
    onCancel: (OutgoingRequestUi) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(message = "Нет исходящих заявок")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            OutgoingRequestCard(
                request = request,
                onCancel = { onCancel(request) },
                onOpenProfile = { onOpenProfile(request.receiverId) }
            )
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: OutgoingRequestUi,
    onCancel: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pressClickable(onClick = onOpenProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarCircle(
                    initials = request.receiverName.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    size = 48
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.receiverName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Заявка отправлена…",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Отозвать запрос
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Отозвать заявку",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Общие вспомогательные composable-ы
// ─────────────────────────────────────────────────────────────────────────────

/** Цветной круг с инициалами — заменяет реальный аватар, пока нет фото. */
@Composable
private fun AvatarCircle(
    initials: String,
    color: Color,
    size: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 3).sp
        )
    }
}

/** Заглушка, когда список пустой. */
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}