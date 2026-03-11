package com.example.brainracer.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Модели данных

data class Friend(
    val id: Int,
    val name: String,
    val avatarInitials: String,
    val isOnline: Boolean,
    val avatarColor: Color = Color(0xFF6C63FF)
)

data class FriendRequest(
    val id: Int,
    val name: String,
    val avatarInitials: String,
    val avatarColor: Color = Color(0xFF00BFA5)
)

data class OutgoingRequest(
    val id: Int,
    val name: String,
    val avatarInitials: String,
    val avatarColor: Color = Color(0xFFFF6B6B)
)

// Главный экран

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen() {
    // Состояние
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("My friends", "Incoming", "Outgoing")

    // Тестовые данные
    var friends by remember {
        mutableStateOf(
            listOf(
                Friend(1, "Анна Смирнова", "АС", true, Color(0xFF6C63FF)),
                Friend(2, "Максим Петров", "МП", false, Color(0xFF00BFA5)),
                Friend(3, "Ольга Кузнецова", "ОК", true, Color(0xFFFF6B6B)),
                Friend(4, "Дмитрий Иванов", "ДИ", false, Color(0xFFFFA726)),
                Friend(5, "Екатерина Попова", "ЕП", true, Color(0xFF26C6DA)),
            )
        )
    }

    var incomingRequests by remember {
        mutableStateOf(
            listOf(
                FriendRequest(1, "Алексей Новиков", "АН", Color(0xFF7E57C2)),
                FriendRequest(2, "Мария Козлова", "МК", Color(0xFF26A69A)),
            )
        )
    }

    var outgoingRequests by remember {
        mutableStateOf(
            listOf(
                OutgoingRequest(1, "Сергей Морозов", "СМ", Color(0xFFEF5350)),
                OutgoingRequest(2, "Татьяна Волкова", "ТВ", Color(0xFF42A5F5)),
            )
        )
    }

    // Фильтрация по поиску
    val filteredFriends = friends.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    val filteredIncoming = incomingRequests.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    val filteredOutgoing = outgoingRequests.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Friends",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Find users...") },
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
                        IconButton(onClick = { searchQuery = "" }) {
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

            // Вкладки
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
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
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Контент вкладок
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> FriendsTab(
                        friends = filteredFriends,
                        onCall = { /* TODO: вызов */ },
                        onDelete = { friend ->
                            friends = friends.filter { it.id != friend.id }
                        }
                    )

                    1 -> IncomingRequestsTab(
                        requests = filteredIncoming,
                        onAccept = { req ->
                            friends = friends + Friend(
                                id = req.id + 100,
                                name = req.name,
                                avatarInitials = req.avatarInitials,
                                isOnline = false,
                                avatarColor = req.avatarColor
                            )
                            incomingRequests = incomingRequests.filter { it.id != req.id }
                        },
                        onDecline = { req ->
                            incomingRequests = incomingRequests.filter { it.id != req.id }
                        }
                    )

                    2 -> OutgoingRequestsTab(
                        requests = filteredOutgoing,
                        onCancel = { req ->
                            outgoingRequests = outgoingRequests.filter { it.id != req.id }
                        }
                    )
                }
            }

            // Кнопка "Пригласить друзей"
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { /* TODO: пригласить */ },
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
    }
}

// Вкладка "Мои друзья"

@Composable
fun FriendsTab(
    friends: List<Friend>,
    onCall: (Friend) -> Unit,
    onDelete: (Friend) -> Unit
) {
    if (friends.isEmpty()) {
        EmptyState(message = "Friends doesn't find")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(friends, key = { it.id }) { friend ->
            FriendCard(
                friend = friend,
                onCall = { onCall(friend) },
                onDelete = { onDelete(friend) }
            )
        }
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар
            AvatarCircle(
                initials = friend.avatarInitials,
                color = friend.avatarColor,
                size = 48
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Имя и статус
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (friend.isOnline) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (friend.isOnline) "online" else "offline",
                        fontSize = 12.sp,
                        color = if (friend.isOnline) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                ElevatedButton(onClick = { /* TODO: блокировать пользователя */ },
                    modifier=Modifier.fillMaxWidth()) {
                    Text(text="Block")

                }
            }

            // Кнопка звонка
            IconButton(onClick = onCall) {
                Icon(
                    imageVector = Icons.Default.Rocket,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Кнопка удаления
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// Вкладка "Входящие заявки"

@Composable
fun IncomingRequestsTab(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit
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
fun IncomingRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(
                    initials = request.avatarInitials,
                    color = request.avatarColor,
                    size = 48
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = request.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Would like to make a friendship",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Dismiss", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Вкладка "Исходящие заявки"

@Composable
fun OutgoingRequestsTab(
    requests: List<OutgoingRequest>,
    onCancel: (OutgoingRequest) -> Unit
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
fun OutgoingRequestCard(
    request: OutgoingRequest,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(
                initials = request.avatarInitials,
                color = request.avatarColor,
                size = 48
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "application is pushed",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Deny", fontSize = 12.sp)
            }
        }
    }
}

// Общие компоненты

@Composable
fun AvatarCircle(initials: String, color: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.33f).sp
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
        }
    }
}

// Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FriendsScreenPreview() {
    MaterialTheme {
        FriendsScreen()
    }
}



