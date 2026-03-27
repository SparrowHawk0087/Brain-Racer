package com.example.brainracer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.brainracer.domain.entities.User
import com.example.brainracer.domain.entities.UserRank
import com.example.brainracer.domain.entities.UserStats
import com.example.brainracer.ui.components.BottomBar
import com.example.brainracer.ui.components.ChallengeFriendQuizSheetContent
import coil.compose.AsyncImage
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.ui.utils.AchievementUi
import com.example.brainracer.ui.utils.PassedQuizUi
import com.example.brainracer.ui.utils.ProfileGoalBadges
import com.example.brainracer.ui.utils.ProfileUtils
import com.example.brainracer.ui.utils.QuizItem
import com.example.brainracer.ui.utils.customAuthorCaption
import com.example.brainracer.ui.utils.TopicStatUi
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.FriendsViewModel
import com.example.brainracer.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel(),
    userId: String,
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    currentRoute: String = "profile",
    isOwnProfile: Boolean = true
) {
    val user by authViewModel.user.collectAsState()
    val uiState by profileViewModel.uiState.collectAsState()
    val friendsUiState by friendsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quizPendingDelete by remember { mutableStateOf<QuizItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var createdHistoryExpanded by remember { mutableStateOf(true) }
    var passedHistoryExpanded by remember { mutableStateOf(true) }
    var showChallengeSheet by remember { mutableStateOf(false) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabs = listOf("Созданное", "Пройденное", "Достижения")

    LaunchedEffect(userId, isOwnProfile) {
        if (isOwnProfile) ProfileAfterQuizRefresh.takePending(userId)
        profileViewModel.loadUserProfile(userId, forceRefresh = true)
    }

    LaunchedEffect(userId, isOwnProfile) {
        if (!isOwnProfile) return@LaunchedEffect
        ProfileAfterQuizRefresh.events.collectLatest { uid ->
            if (uid == userId) {
                profileViewModel.invalidateProfileCache()
                profileViewModel.loadUserProfile(userId, forceRefresh = true)
            }
        }
    }

    DisposableEffect(lifecycleOwner, userId, isOwnProfile) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val forceAfterQuiz = isOwnProfile && ProfileAfterQuizRefresh.takePending(userId)
                if (forceAfterQuiz) profileViewModel.invalidateProfileCache()
                profileViewModel.loadUserProfile(userId, forceRefresh = forceAfterQuiz)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(user) {
        if (user == null && isOwnProfile) {
            Toast.makeText(context, "Сессия завершена или аккаунт удалён", Toast.LENGTH_SHORT).show()
            onNavigateToAuth()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            profileViewModel.clearError()
        }
    }

    LaunchedEffect(uiState.quizHistoryLoadError) {
        uiState.quizHistoryLoadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            profileViewModel.clearQuizHistoryError()
        }
    }

    LaunchedEffect(friendsUiState.challengeSentMessage) {
        friendsUiState.challengeSentMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            friendsViewModel.consumeChallengeSentMessage()
            showChallengeSheet = false
        }
    }

    var showProfileEditSheet by remember { mutableStateOf(false) }
    val profileEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editDraftBio by remember { mutableStateOf("") }
    var editSelectedBadges by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(showProfileEditSheet) {
        if (showProfileEditSheet) {
            editDraftBio = uiState.bio
            editSelectedBadges = uiState.interests.toSet()
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileViewModel.uploadAvatar(context, userId, it) }
    }

    if (showProfileEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileEditSheet = false },
            sheetState = profileEditSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Редактирование профиля",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                Text("О себе", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = editDraftBio,
                    onValueChange = { if (it.length <= 400) editDraftBio = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    placeholder = { Text("Расскажите о себе", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) }
                )
                Text(
                    "${editDraftBio.length}/400",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(Modifier.height(16.dp))
                Text("Бейджи целей (до ${ProfileGoalBadges.MAX_SELECTED})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileGoalBadges.all.forEach { opt ->
                        val sel = opt.id in editSelectedBadges
                        FilterChip(
                            selected = sel,
                            onClick = {
                                editSelectedBadges = when {
                                    sel -> editSelectedBadges - opt.id
                                    editSelectedBadges.size >= ProfileGoalBadges.MAX_SELECTED -> editSelectedBadges
                                    else -> editSelectedBadges + opt.id
                                }
                            },
                            label = { Text(opt.label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.35f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.outline.copy(0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showProfileEditSheet = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        enabled = !uiState.isSavingProfile
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            profileViewModel.saveBioAndGoalBadges(
                                userId,
                                editDraftBio,
                                editSelectedBadges.toList()
                            ) { ok -> if (ok) showProfileEditSheet = false }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !uiState.isSavingProfile
                    ) {
                        if (uiState.isSavingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Сохранить", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showChallengeSheet && !isOwnProfile) {
        val rankFromProfile =
            UserRank.entries.find { it.displayName == uiState.rankName } ?: UserRank.BEGINNER
        val challengedUser = User(
            id = userId,
            nickname = uiState.username.ifBlank { "Игрок" },
            email = uiState.email,
            rank = rankFromProfile
        )
        ModalBottomSheet(
            onDismissRequest = { showChallengeSheet = false },
            sheetState = challengeSheetState
        ) {
            LaunchedEffect(Unit) { friendsViewModel.loadChallengePickerQuizzes() }
            ChallengeFriendQuizSheetContent(
                fixedFriend = challengedUser,
                friends = emptyList(),
                quizzes = friendsUiState.challengePickerQuizzes,
                isLoading = friendsUiState.challengePickerLoading,
                onDismiss = { showChallengeSheet = false },
                onSendChallenge = { fid, qid, title ->
                    friendsViewModel.sendChallenge(fid, qid, title)
                }
            )
        }
    }

    quizPendingDelete?.let { pending ->
        val deletingThis = uiState.deletingQuizId == pending.id
        AlertDialog(
            onDismissRequest = { if (!deletingThis) quizPendingDelete = null },
            title = { Text("Удалить викторину?") },
            text = {
                Text(
                    "«${pending.title}» будет удалена безвозвратно.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.deleteCreatedQuiz(pending.id, userId) { ok, err ->
                            quizPendingDelete = null
                            if (ok) {
                                Toast.makeText(context, "Викторина удалена", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    err ?: "Не удалось удалить",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !deletingThis,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (deletingThis) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Удалить")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { quizPendingDelete = null },
                    enabled = !deletingThis
                ) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.background,
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
        },
        floatingActionButton = {
            if (!isOwnProfile) {
                ExtendedFloatingActionButton(
                    onClick = { showChallengeSheet = true },
                    shape = RoundedCornerShape(24.dp),
                    icon = { Icon(Icons.Default.Sports, contentDescription = null) },
                    text = { Text("Вызвать на дуэль") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.username.isBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ProfileScreenTopBar(
                        title = if (isOwnProfile) "Мой профиль" else "Профиль игрока",
                        showEdit = isOwnProfile,
                        onEditClick = { showProfileEditSheet = true },
                        showSettings = isOwnProfile,
                        onSettingsClick = { navController.navigate("settings") },
                        onFriendsClick = if (isOwnProfile) {
                            { navController.navigate("friends/$userId") }
                        } else null
                    )
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    ProfileMainCard(
                        userName = uiState.username.ifBlank { user?.displayName ?: "Игрок" },
                        userEmail = uiState.email.ifBlank { user?.email ?: "" },
                        userLevel = uiState.userLevel,
                        levelProgress = uiState.levelProgress,
                        rankName = uiState.rankName,
                        avatarUrl = uiState.avatarUrl,
                        isUploadingAvatar = uiState.isUploadingAvatar,
                        onAvatarClick = if (isOwnProfile) {
                            { avatarPicker.launch("image/*") }
                        } else null
                    )
                }

                item {
                    Spacer(Modifier.height(14.dp))
                    ProfileBioBadgesSection(
                        bio = uiState.bio,
                        badgeIds = uiState.interests
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    ProfileUserStatRow(stats = uiState.userStats)
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    ProfileTopicStatsCard(topicStats = uiState.topicStats)
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 20.dp,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                                )
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (uiState.createdQuizzes.isEmpty()) {
                            item {
                                EmptyTabHint("Вы ещё не создали викторин")
                            }
                        } else {
                            item {
                                val n = uiState.createdQuizzes.size
                                ProfileCollapsibleHistoryHeader(
                                    title = "Созданные викторины",
                                    subtitle = "$n ${createdQuizCountWord(n)}",
                                    expanded = createdHistoryExpanded,
                                    onToggle = { createdHistoryExpanded = !createdHistoryExpanded }
                                )
                            }
                            if (createdHistoryExpanded) {
                                itemsIndexed(
                                    items = uiState.createdQuizzes,
                                    key = { _, q -> q.id }
                                ) { index, quiz ->
                                    CreatedQuizRow(
                                        quiz = quiz,
                                        colorIndex = index,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                        onClick = { navController.navigate("quiz_detail/${quiz.id}") },
                                        showDeleteButton = isOwnProfile,
                                        isDeleting = uiState.deletingQuizId == quiz.id,
                                        onDeleteClick = { quizPendingDelete = quiz }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (uiState.passedAttempts.isEmpty()) {
                            item {
                                EmptyTabHint("Пока нет завершённых прохождений")
                            }
                        } else {
                            item {
                                val n = uiState.passedAttempts.size
                                ProfileCollapsibleHistoryHeader(
                                    title = "История прохождений",
                                    subtitle = "$n ${passAttemptCountWord(n)}",
                                    expanded = passedHistoryExpanded,
                                    onToggle = { passedHistoryExpanded = !passedHistoryExpanded }
                                )
                            }
                            if (passedHistoryExpanded) {
                                items(
                                    items = uiState.passedAttempts,
                                    key = { "${it.quizId}_${it.completedAtEpochMs}" }
                                ) { attempt ->
                                    PassedQuizRow(
                                        attempt = attempt,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                        onClick = { navController.navigate("quiz_detail/${attempt.quizId}") }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        items(
                            items = uiState.achievements,
                            key = { it.id }
                        ) { achievement ->
                            ProfileAchievementRow(
                                achievement = achievement,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isOwnProfile) {
                    item { Spacer(Modifier.height(24.dp)) }
                    item {
                        OutlinedButton(
                            onClick = {
                                authViewModel.signOut()
                                onNavigateToAuth()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text("Выйти", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("Удалить аккаунт", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Удалить аккаунт?",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Все данные, прогресс и достижения будут удалены навсегда.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
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
                    Text("Отмена", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
private fun ProfileScreenTopBar(
    title: String,
    showEdit: Boolean,
    onEditClick: () -> Unit,
    showSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onFriendsClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        if (onFriendsClick != null) {
            IconButton(
                onClick = onFriendsClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = "Друзья",
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                )
            }
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        if (showSettings || showEdit) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSettings) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                        )
                    }
                }
                if (showEdit) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль", tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileBioBadgesSection(
    bio: String,
    badgeIds: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("О себе", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = bio.trim().ifBlank { "Пользователь пока ничего не написал." },
            color = if (bio.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(0.92f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        if (badgeIds.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Цели", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badgeIds.forEach { id ->
                    val label = ProfileGoalBadges.labelFor(id) ?: id
                    SuggestionChip(
                        onClick = {},
                        label = { Text(label, fontSize = 12.sp) },
                        enabled = false,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.2f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = false,
                            borderColor = MaterialTheme.colorScheme.primary.copy(0.4f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMainCard(
    userName: String,
    userEmail: String,
    userLevel: Int,
    levelProgress: Float,
    rankName: String,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onAvatarClick: (() -> Unit)?
) {
    val animatedProgress by animateFloatAsState(
        targetValue = levelProgress,
        animationSpec = tween(600),
        label = "lvl"
    )
    val ext = LocalBrainRacerExtendedColors.current
    val g = ext.cardGradients
    val avatarStops = listOf(g[0][0], g[1][0])
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.35f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .then(
                    if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick)
                    else Modifier
                )
                .background(Brush.linearGradient(avatarStops)),
            contentAlignment = Alignment.Center
        ) {
            val url = avatarUrl?.takeIf { it.isNotBlank() }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (isUploadingAvatar) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(userName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(userEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            rankName,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Уровень $userLevel",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ProfileUserStatRow(stats: UserStats?) {
    val accPct = if ((stats?.totalQuestionsAnswered ?: 0) > 0) {
        val tq = stats!!.totalQuestionsAnswered
        "${stats.correctAnswers.toLong() * 100L / tq}%"
    } else "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            (stats?.totalQuizzesTaken ?: 0).toString() to "игр",
            (stats?.totalPoints ?: 0).toString() to "рейтинг",
            accPct to "точность"
        ).forEach { (value, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
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

@Composable
private fun ProfileTopicStatsCard(topicStats: List<TopicStatUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            "Статистика по темам",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        if (topicStats.isEmpty()) {
            Text(
                "Пройдите викторины, чтобы увидеть статистику по темам",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            topicStats.forEach { stat ->
                ProfileTopicRow(stat = stat)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileTopicRow(stat: TopicStatUi) {
    val topicBarColors = LocalBrainRacerExtendedColors.current.topicBarColors
    val barColor = topicBarColors[stat.paletteIndex % topicBarColors.size]
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stat.categoryName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(
                "${stat.percent.toInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (stat.percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.outline,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyTabHint(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp)
    )
}

@Composable
private fun ProfileCollapsibleHistoryHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Свернуть список" else "Развернуть список",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun createdQuizCountWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod100 in 11..14 -> "викторин"
        mod10 == 1 -> "викторина"
        mod10 in 2..4 -> "викторины"
        else -> "викторин"
    }
}

private fun passAttemptCountWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod100 in 11..14 -> "прохождений"
        mod10 == 1 -> "прохождение"
        mod10 in 2..4 -> "прохождения"
        else -> "прохождений"
    }
}

@Composable
private fun CreatedQuizRow(
    quiz: QuizItem,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    isDeleting: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    val cardGradients = LocalBrainRacerExtendedColors.current.cardGradients
    val gradient = cardGradients[colorIndex % cardGradients.size]
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(gradient, Offset.Zero, Offset(400f, 400f))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Quiz, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        quiz.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            if (showDeleteButton) {
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Удалить викторину",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassedQuizRow(
    attempt: PassedQuizUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(attempt.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(attempt.category, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Точность: ${attempt.accuracyPercent}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                "+${attempt.pointsEarned} XP",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            ProfileUtils.formatPassedDate(attempt.completedAtEpochMs),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ProfileAchievementRow(
    achievement: AchievementUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (achievement.unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = if (achievement.unlocked) LocalBrainRacerExtendedColors.current.detailGreen
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                achievement.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                achievement.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    BrainRacerTheme {
        ProfileScreen(
            navController = rememberNavController(),
            onNavigateToAuth = {},
            userId = "user123",
            isOwnProfile = true
        )
    }
}
