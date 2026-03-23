package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.domain.entities.User
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import com.example.brainracer.ui.utils.FriendRequestUi
import com.example.brainracer.ui.utils.OutgoingRequestUi
import com.example.brainracer.ui.viewmodels.FriendsViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  Главный composable-экран
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = viewModel(),
    onHomeClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "friends"
) {
    // Подписываемся на единое состояние ViewModel.
    // Каждый раз, когда ViewModel обновляет _uiState, Compose автоматически
    // перерисует только те части дерева, которые читают изменившиеся поля.
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var challengeTargetFriend by remember { mutableStateOf<User?>(null) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val tabs = listOf("My friends", "Incoming", "Outgoing")

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
                        text = "Friends",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
            )
        },
        bottomBar = {
            BottomBar(
                showBar = true,
                currentRoute = currentRoute,
                onHomeClick = onHomeClick,
                onFriendsClick = onFriendsClick,
                onProfileClick = onProfileClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Поле поиска ────────────────────────────────────────────────
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
                placeholder = { Text("Find users…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
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
                                contentDescription = "Clear",
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

            // ── Результаты поиска (показываем поверх вкладок) ──────────────
            if (uiState.isSearching && uiState.searchResults.isNotEmpty()) {
                SearchResultsList(
                    results = uiState.searchResults,
                    onSendRequest = { user -> viewModel.sendFriendRequest(user.id) }
                )
            } else {

                // ── Вкладки ────────────────────────────────────────────────
                var selectedTab by remember { mutableIntStateOf(0) }

                // Показываем badge с количеством входящих заявок
                val incomingBadge = uiState.incomingRequests.size

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                // На вкладке «Incoming» показываем счётчик
                                if (index == 1 && incomingBadge > 0) {
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
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        when (selectedTab) {
                            0 -> FriendsTab(
                                friends = filteredFriends,
                                onDelete = { friend -> viewModel.removeFriend(friend.id) },
                                onChallengeFriend = { challengeTargetFriend = it }
                            )
                            1 -> IncomingRequestsTab(
                                requests = filteredIncoming,
                                onAccept = { req ->
                                    viewModel.acceptFriendRequest(req.id, req.senderId)
                                },
                                onDecline = { req ->
                                    viewModel.declineFriendRequest(req.id)
                                }
                            )
                            2 -> OutgoingRequestsTab(
                                requests = filteredOutgoing,
                                onCancel = { req ->
                                    viewModel.cancelOutgoingRequest(req.id)
                                }
                            )
                        }
                    }
                }

                // ── Кнопка «Пригласить» ────────────────────────────────────
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = { /* TODO: поделиться ссылкой */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Invite friends",
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
                            Text("Retry")
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
    onSendRequest: (User) -> Unit
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
                            text = user.rank.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onSendRequest(user) }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add friend",
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
    onChallengeFriend: (User) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyState(message = "No friends yet. Find people using the search above!")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friends, key = { it.id }) { friend ->
            FriendCard(
                friend     = friend,
                onDelete   = { onDelete(friend) },
                onChallenge = { onChallengeFriend(friend) }
            )
        }
    }
}

@Composable
private fun FriendCard(
    friend: User,
    onDelete: () -> Unit,
    onChallenge: () -> Unit
) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = friend.rank.displayName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onChallenge) {
                Icon(
                    imageVector = Icons.Default.Sports,
                    contentDescription = "Challenge friend",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "Remove friend",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
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
    onDecline: (FriendRequestUi) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(message = "No incoming requests")
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
                onDecline = { onDecline(request) }
            )
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequestUi,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Wants to be your friend",
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
                    Text("Accept", fontSize = 13.sp)
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
                    Text("Decline", fontSize = 13.sp)
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
    onCancel: (OutgoingRequestUi) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(message = "No outgoing requests")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            OutgoingRequestCard(
                request = request,
                onCancel = { onCancel(request) }
            )
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: OutgoingRequestUi,
    onCancel: () -> Unit
) {
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
                    text = "Request pending…",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Отозвать запрос
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel request",
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
