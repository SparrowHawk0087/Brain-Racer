package com.example.brainracer.ui.screens

import android.net.Uri
import com.example.brainracer.R
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.brainracer.ui.components.bottomBarOcclusionBlockClicks
import com.example.brainracer.ui.components.bottomBarOcclusionEffect
import com.example.brainracer.ui.components.bottomBarSafePadding
import com.example.brainracer.ui.components.pressClickable
import com.example.brainracer.ui.components.pressScale
import com.example.brainracer.ui.components.rememberFabVisibilityOnScroll
import coil.compose.AsyncImage
import com.example.brainracer.ui.utils.ProfileAfterQuizRefresh
import com.example.brainracer.ui.utils.AppMotionConfig
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
    val expandMotion = AppMotionConfig.expand
    val user by authViewModel.user.collectAsState()
    val deleteAccountError by authViewModel.deleteAccountError.collectAsState()
    val uiState by profileViewModel.uiState.collectAsState()
    val friendsUiState by friendsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quizPendingDelete by remember { mutableStateOf<QuizItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedAchievementId by rememberSaveable { mutableStateOf<String?>(null) }
    var createdHistoryExpanded by remember { mutableStateOf(true) }
    var passedHistoryExpanded by remember { mutableStateOf(true) }
    var showChallengeSheet by remember { mutableStateOf(false) }
    val challengeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profileListState = rememberLazyListState()
    val challengeFabVisible = rememberFabVisibilityOnScroll(profileListState)
    val bottomReflexShift by remember {
        derivedStateOf {
            ((profileListState.firstVisibleItemIndex * 4f) +
                    profileListState.firstVisibleItemScrollOffset * 0.012f).coerceIn(0f, 18f)
        }
    }
    val tabs = listOf("Созданное", "Пройденное", "Достижения")
    val isFriend = remember(friendsUiState.friends, userId) {
        friendsUiState.friends.any { it.id == userId }
    }

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

    LaunchedEffect(deleteAccountError) {
        deleteAccountError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearDeleteAccountError()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            profileViewModel.clearError()
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
    var editDraftUsername by remember { mutableStateOf("") }
    var editDraftBio by remember { mutableStateOf("") }
    var editSelectedBadges by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(showProfileEditSheet) {
        if (showProfileEditSheet) {
            editDraftUsername = uiState.username
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
                Text("Имя пользователя", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = editDraftUsername,
                    onValueChange = { if (it.length <= 30) editDraftUsername = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    placeholder = { Text("Введите никнейм", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)) }
                )
                Text(
                    "${editDraftUsername.length}/30",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(Modifier.height(14.dp))
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
                            val usernameValidation = ProfileUtils.validateUsername(editDraftUsername.trim())
                            if (!usernameValidation.isValid) {
                                Toast.makeText(
                                    context,
                                    usernameValidation.errorMessage ?: "Некорректный никнейм",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val saveBioAndBadges = {
                                    profileViewModel.saveBioAndGoalBadges(
                                        userId,
                                        editDraftBio,
                                        editSelectedBadges.toList()
                                    ) { ok -> if (ok) showProfileEditSheet = false }
                                }
                                if (editDraftUsername.trim() != uiState.username.trim()) {
                                    profileViewModel.updateUsername(userId, editDraftUsername.trim()) { nameSaved ->
                                        if (nameSaved) saveBioAndBadges()
                                    }
                                } else {
                                    saveBioAndBadges()
                                }
                            }
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
        topBar = {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ProfileScreenTopBar(
                    title = if (isOwnProfile) "Мой профиль" else "Профиль игрока",
                    onBackToFriendsClick = if (!isOwnProfile && !user?.uid.isNullOrBlank()) {
                        {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate("friends/${user!!.uid}")
                            }
                        }
                    } else null,
                    showEdit = isOwnProfile,
                    onEditClick = { showProfileEditSheet = true },
                    showSettings = isOwnProfile,
                    onSettingsClick = { navController.navigate("settings") },
                    showAddFriend = !isOwnProfile && !isFriend,
                    onAddFriendClick = { friendsViewModel.sendFriendRequest(userId) },
                    addFriendEnabled = true,
                    showRemoveFriend = !isOwnProfile && isFriend,
                    onRemoveFriendClick = { friendsViewModel.removeFriend(userId) },
                    removeFriendEnabled = true
                )
            }
        },
        bottomBar = {
            BottomBar(
                showBar              = true,
                currentRoute         = currentRoute,
                onHomeClick          = onHomeClick,
                onLeaderboardClick   = onLeaderboardClick,
                onChallengesClick    = onChallengesClick,
                onQuizzesClick       = onQuizzesClick,
                onProfileClick       = onProfileClick,
                reflexShift          = bottomReflexShift
            )
        },
        floatingActionButton = {
            if (!isOwnProfile) {
                AnimatedVisibility(
                    visible = challengeFabVisible,
                    enter = fadeIn(tween(220)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(220)
                    ),
                    exit = fadeOut(tween(180)) + slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(180)
                    )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showChallengeSheet = true },
                        modifier = Modifier.pressScale(),
                        shape = RoundedCornerShape(12.dp),
                        icon = { Icon(painter = painterResource(id = R.drawable.cognition), contentDescription = null) },
                        text = { Text("Вызов") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        val profileBottomInset = bottomBarSafePadding(paddingValues)
        if (uiState.isLoading && uiState.username.isBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                state = profileListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = profileBottomInset)
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                    ProfileMainCard(
                        userName = uiState.username.ifBlank { user?.displayName ?: "Игрок" },
                        userEmail = if (isOwnProfile) {
                            uiState.email.ifBlank { user?.email ?: "" }
                        } else {
                            ""
                        },
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
                    Spacer(Modifier.height(14.dp))
                    ProfileFriendsSummaryCard(
                        friendsCount = uiState.friendsCount,
                        onOpenFriends = { navController.navigate("friends/$userId") }
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    ProfileUserStatRow(stats = uiState.userStats)
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    ProfileTopicStatsCard(
                        topicStats = uiState.topicStats,
                        isLoading = uiState.isLoading,
                        historyLoadError = uiState.quizHistoryLoadError,
                        onRetry = {
                            profileViewModel.invalidateProfileCache()
                            profileViewModel.loadUserProfile(userId, forceRefresh = true)
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bottomBarOcclusionEffect()
                            .bottomBarOcclusionBlockClicks()
                    ) {
                        ScrollableTabRow(
                            modifier = Modifier.fillMaxWidth(),
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
                }

                when (selectedTab) {
                    0 -> {
                        if (uiState.createdQuizzes.isEmpty()) {
                            item {
                                if (isOwnProfile) EmptyTabHint("Вы ещё не создали викторин")
                                else EmptyTabHint("Игрок пока ничего не создавал")
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
                            itemsIndexed(
                                items = uiState.createdQuizzes,
                                key = { _, q -> q.id }
                            ) { index, quiz ->
                                AnimatedVisibility(
                                    visible = createdHistoryExpanded,
                                    enter = fadeIn() + expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = expandMotion.enterDampingRatio,
                                            stiffness = expandMotion.enterStiffness
                                        )
                                    ),
                                    exit = fadeOut() + shrinkVertically(
                                        animationSpec = spring(
                                            dampingRatio = expandMotion.exitDampingRatio,
                                            stiffness = expandMotion.exitStiffness
                                        )
                                    )
                                ) {
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
                            items(
                                items = uiState.passedAttempts,
                                key = { "${it.quizId}_${it.completedAtEpochMs}" }
                            ) { attempt ->
                                AnimatedVisibility(
                                    visible = passedHistoryExpanded,
                                    enter = fadeIn() + expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = expandMotion.enterDampingRatio,
                                            stiffness = expandMotion.enterStiffness
                                        )
                                    ),
                                    exit = fadeOut() + shrinkVertically(
                                        animationSpec = spring(
                                            dampingRatio = expandMotion.exitDampingRatio,
                                            stiffness = expandMotion.exitStiffness
                                        )
                                    )
                                ) {
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
                                expanded = expandedAchievementId == achievement.id,
                                onToggleExpanded = {
                                    expandedAchievementId =
                                        if (expandedAchievementId == achievement.id) null else achievement.id
                                },
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
                                .bottomBarOcclusionEffect()
                                .bottomBarOcclusionBlockClicks()
                                .pressScale()
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
                                .bottomBarOcclusionEffect()
                                .bottomBarOcclusionBlockClicks()
                                .pressScale(pressedScale = 0.992f)
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
    onBackToFriendsClick: (() -> Unit)? = null,
    showEdit: Boolean,
    onEditClick: () -> Unit,
    showSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
    showAddFriend: Boolean = false,
    onAddFriendClick: () -> Unit = {},
    addFriendEnabled: Boolean = true,
    showRemoveFriend: Boolean = false,
    onRemoveFriendClick: () -> Unit = {},
    removeFriendEnabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(88.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBackToFriendsClick != null) {
                        IconButton(onClick = onBackToFriendsClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.brainracer.R.drawable.arrow_back_btn),
                                contentDescription = "К списку друзей",
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showSettings || showEdit || showAddFriend || showRemoveFriend) {
                Row(
                    modifier = Modifier.widthIn(min = 88.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAddFriend) {
                        IconButton(onClick = onAddFriendClick, enabled = addFriendEnabled, modifier = Modifier.size(36.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.person_add),
                                contentDescription = "Добавить в друзья",
                                tint = if (addFriendEnabled) {
                                    MaterialTheme.colorScheme.onSurface.copy(0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(0.35f)
                                }
                            )
                        }
                    }
                    if (showRemoveFriend) {
                        IconButton(onClick = onRemoveFriendClick, enabled = removeFriendEnabled, modifier = Modifier.size(36.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.person_remove),
                                contentDescription = "Удалить из друзей",
                                tint = if (removeFriendEnabled) {
                                    MaterialTheme.colorScheme.error.copy(0.9f)
                                } else {
                                    MaterialTheme.colorScheme.error.copy(0.35f)
                                }
                            )
                        }
                    }
                    if (showSettings) {
                        IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.settings),
                                contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.85f)
                            )
                        }
                    }
                    if (showEdit) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                            Icon(painter = painterResource(id = R.drawable.change_info_btn), contentDescription = "Редактировать профиль", tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                        }
                    }
                }
            } else {
                Spacer(Modifier.width(88.dp))
            }
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp)
        )
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
private fun ProfileFriendsSummaryCard(
    friendsCount: Int,
    onOpenFriends: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .pressClickable(onClick = onOpenFriends)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.diversity),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Друзья", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("$friendsCount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    if (onAvatarClick != null) Modifier.pressClickable(onClick = onAvatarClick)
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
        if (userEmail.isNotBlank()) {
            Text(userEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
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
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
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
private fun ProfileTopicStatsCard(
    topicStats: List<TopicStatUi>,
    isLoading: Boolean,
    historyLoadError: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
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
        if (isLoading && topicStats.isEmpty() && historyLoadError == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Загружаем статистику по темам...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else if (historyLoadError != null && topicStats.isEmpty()) {
            Text(
                "Не удалось загрузить статистику по темам.\n$historyLoadError",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Повторить")
            }
        } else if (topicStats.isEmpty()) {
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
            if (historyLoadError != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Часть данных недоступна: $historyLoadError",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
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
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
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
    val expandMotion = AppMotionConfig.expand
    val shape = RoundedCornerShape(14.dp)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = spring(
            dampingRatio = expandMotion.iconDampingRatio,
            stiffness = expandMotion.iconStiffness
        ),
        label = "historyArrowRotation"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .pressClickable(onClick = onToggle)
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
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { rotationZ = arrowRotation }
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
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
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
                    .pressClickable(onClick = onClick),
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
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .pressClickable(onClick = onClick)
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
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expandMotion = AppMotionConfig.expand
    Column(
        modifier = modifier
            .bottomBarOcclusionEffect()
            .bottomBarOcclusionBlockClicks()
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .pressClickable(onClick = onToggleExpanded)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = expandMotion.sizeDampingRatio,
                    stiffness = expandMotion.sizeStiffness
                )
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.lock_btn),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (achievement.unlocked) LocalBrainRacerExtendedColors.current.detailGreen
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
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
                    if (achievement.unlocked) "Достижение открыто" else "Достижение заблокировано",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) 90f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 300f
                ),
                label = "achievementChevronRotation"
            )
            Icon(
                painter = painterResource(id = R.drawable.chevron_forward),
                contentDescription = if (expanded) "Свернуть достижение" else "Развернуть достижение",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = "За что дается: ${achievement.description}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
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
