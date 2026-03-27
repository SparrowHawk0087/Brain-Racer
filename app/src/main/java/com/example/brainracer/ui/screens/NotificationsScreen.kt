package com.example.brainracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.brainracer.domain.entities.AppNotification
import com.example.brainracer.domain.entities.AppNotificationType
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import com.example.brainracer.ui.viewmodels.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatNotificationQuizDuration(totalSec: Int?): String? {
    if (totalSec == null || totalSec <= 0) return null
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) {
        String.format(Locale.getDefault(), "Время: %d мин %d с", m, s)
    } else {
        String.format(Locale.getDefault(), "Время: %d с", s)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    currentUserId: String,
    vm: NotificationsViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Общие", "Вызовы")

    val hidden = uiState.hiddenChallengeIds
    val generalList = remember(uiState.items) {
        uiState.items.filter { it.type != AppNotificationType.CHALLENGE }
    }
    val challengesList = remember(uiState.items, hidden) {
        uiState.items.filter {
            it.type == AppNotificationType.CHALLENGE &&
                (it.challengeId == null || it.challengeId !in hidden)
        }
    }
    val hasUnreadGeneral = remember(generalList) { generalList.any { !it.read } }
    val hasUnreadChallenges = remember(challengesList) { challengesList.any { !it.read } }
    val listShown = if (tabIndex == 0) generalList else challengesList

    Scaffold(
        containerColor      = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text("Уведомления", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (tabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color    = MaterialTheme.colorScheme.primary,
                            height   = 2.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
            ) {
                tabTitles.forEachIndexed { i, title ->
                    val showUnreadDot = when (i) {
                        0 -> hasUnreadGeneral
                        1 -> hasUnreadChallenges
                        else -> false
                    }
                    Tab(
                        selected = tabIndex == i,
                        onClick  = { tabIndex = i },
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, fontSize = 14.sp)
                                if (showUnreadDot) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(LocalBrainRacerExtendedColors.current.difficultyHard)
                                    )
                                }
                            }
                        },
                        selectedContentColor   = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                uiState.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                uiState.errorMessage != null ->
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                listShown.isEmpty() ->
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (tabIndex == 0) "Пока нет общих уведомлений"
                            else "Нет уведомлений о вызовах",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                else ->
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        items(listShown, key = { it.id }) { n ->
                            NotificationCard(
                                n = n,
                                quizTimeLabel = formatNotificationQuizDuration(n.quizTotalTimeSeconds),
                                onClick = {
                                    vm.markAsRead(n.id)
                                    when (n.type) {
                                        AppNotificationType.CHALLENGE -> {
                                            if (!n.challengeId.isNullOrBlank()) {
                                                navController.navigate("challenge_start/${n.challengeId}")
                                            } else {
                                                navController.navigate("challenges/$currentUserId")
                                            }
                                        }
                                        else -> { }
                                    }
                                }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    n: AppNotification,
    quizTimeLabel: String?,
    onClick: () -> Unit
) {
    val timeStr = remember(n.createdAt) {
        try {
            SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                .format(Date(n.createdAt.seconds * 1000))
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                !n.actorAvatarUrl.isNullOrBlank() ->
                    AsyncImage(
                        model             = n.actorAvatarUrl,
                        contentDescription = null,
                        modifier          = Modifier.size(48.dp).clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentScale      = ContentScale.Crop
                    )
                n.type == AppNotificationType.CHALLENGE ->
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sports, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    }
                else ->
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
            }

            Column(modifier = Modifier.weight(1f)) {
                if (n.title.isNotBlank()) {
                    Text(
                        n.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (!n.actorNickname.isNullOrBlank() && n.type == AppNotificationType.CHALLENGE) {
                    Text(
                        n.actorNickname,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    n.message,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines   = 4,
                    overflow   = TextOverflow.Ellipsis
                )
                if (n.type == AppNotificationType.CHALLENGE && !quizTimeLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            quizTimeLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f))
                    if (!n.read) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(0.2f)) {
                            Text(
                                "Новое",
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
