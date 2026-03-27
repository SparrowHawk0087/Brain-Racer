package com.example.brainracer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

fun bottomBarSelectedKey(route: String): String = when {
    route.startsWith("home/")        -> "home"
    route.startsWith("leaderboard/") -> "leaderboard"
    route.startsWith("challenges/")  -> "challenges"
    route == "quizzes"               -> "quizzes"
    route.startsWith("profile/")     -> "profile"
    else                             -> ""
}

@Composable
fun BottomBar(
    showBar: Boolean = true,
    currentRoute: String = "",
    /** Только реальные входящие вызовы (PENDING), не «висящие» записи в notifications. */
    showChallengesIncomingBadge: Boolean = false,
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!showBar) return

    val selected = bottomBarSelectedKey(currentRoute)
    val scheme = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ext.tabBarBackground)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = ext.shadowOnDark
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                label = "домой",
                icon = Icons.Outlined.Home,
                selected = selected == "home",
                activeColor = scheme.primary,
                inactiveColor = ext.tabInactive,
                onClick = onHomeClick,
                modifier = Modifier.weight(1f)
            )
            BottomBarItem(
                label = "лидерборд",
                icon = Icons.Outlined.EmojiEvents,
                selected = selected == "leaderboard",
                activeColor = scheme.primary,
                inactiveColor = ext.tabInactive,
                onClick = onLeaderboardClick,
                modifier = Modifier.weight(1f)
            )
            BottomBarItem(
                label = "вызовы",
                icon = Icons.Outlined.FlashOn,
                selected = selected == "challenges",
                activeColor = scheme.primary,
                inactiveColor = ext.tabInactive,
                onClick = onChallengesClick,
                modifier = Modifier.weight(1f),
                showUnreadDot  = showChallengesIncomingBadge,
                unreadDotColor = ext.difficultyHard
            )
            BottomBarItem(
                label = "викторины",
                icon = Icons.Outlined.MenuBook,
                selected = selected == "quizzes",
                activeColor = scheme.primary,
                inactiveColor = ext.tabInactive,
                onClick = onQuizzesClick,
                modifier = Modifier.weight(1f)
            )
            BottomBarItem(
                label = "профиль",
                icon = Icons.Outlined.Person,
                selected = selected == "profile",
                activeColor = scheme.primary,
                inactiveColor = ext.tabInactive,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showUnreadDot: Boolean = false,
    unreadDotColor: Color = MaterialTheme.colorScheme.error
) {
    val tint by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 220),
        label = "bottomBarTint"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (showUnreadDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(unreadDotColor, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, heightDp = 120, backgroundColor = 0xFF0F0F1A)
@Composable
private fun BottomBarPreviewHome() {
    BrainRacerTheme {
        BottomBar(showBar = true, currentRoute = "home/user1")
    }
}

@Preview(showBackground = true, heightDp = 120, backgroundColor = 0xFF0F0F1A)
@Composable
private fun BottomBarPreviewQuizzes() {
    BrainRacerTheme {
        BottomBar(showBar = true, currentRoute = "quizzes")
    }
}
