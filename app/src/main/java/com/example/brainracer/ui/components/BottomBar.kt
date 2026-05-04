import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.LocalBrainRacerExtendedColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.brainracer.R

fun bottomBarSelectedKey(route: String): String = when {
    route.startsWith("home/")        -> "home"
    route.startsWith("leaderboard/") -> "leaderboard"
    route.startsWith("challenges/")  -> "challenges"
    route == "quizzes"               -> "quizzes"
    route.startsWith("profile/")     -> "profile"
    else                             -> ""
}

enum class BottomBarGlassIntensity {
    Medium,
    Strong
}

@Composable
fun BottomBar(
    showBar: Boolean = true,
    currentRoute: String = "",
    showChallengesIncomingBadge: Boolean = false,
    onHomeClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onQuizzesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    reflexShift: Float = 0f,
    glassIntensity: BottomBarGlassIntensity = BottomBarGlassIntensity.Strong,
    modifier: Modifier = Modifier
) {
    if (!showBar) return

    val selected = bottomBarSelectedKey(currentRoute)
    val scheme = MaterialTheme.colorScheme
    val ext = LocalBrainRacerExtendedColors.current
    val isStrongGlass = glassIntensity == BottomBarGlassIntensity.Strong
    val clampedReflexShift = reflexShift.coerceIn(-18f, 18f)
    val glassShape = RoundedCornerShape(30.dp)
    val glassBackground = remember(ext.tabBarBackground, glassIntensity) {
        Brush.verticalGradient(
            colors = listOf(
                ext.tabBarBackground.copy(alpha = if (isStrongGlass) 0.86f else 0.76f),
                ext.tabBarBackground.copy(alpha = if (isStrongGlass) 0.66f else 0.58f)
            )
        )
    }
    val isLightTheme = scheme.surface.luminance() > 0.5f
    val selectedContentColor = if (isLightTheme) scheme.primary else Color.White.copy(alpha = 0.98f)
    val reflectionSweep = remember(glassIntensity, clampedReflexShift, isLightTheme) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = if (isStrongGlass) 0.11f else 0.08f),
                0.24f to Color.White.copy(alpha = if (isStrongGlass) 0.07f else 0.05f),
                0.52f to Color.Transparent,
                0.78f to Color.White.copy(alpha = if (isStrongGlass) 0.05f else 0.03f),
                1.00f to Color.Transparent
            ),
            start = Offset(-160f + clampedReflexShift, -80f),
            end = Offset(860f + clampedReflexShift, 360f)
        )
    }
    val reflectionDepth = remember(glassIntensity, isLightTheme) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = if (isStrongGlass) 0.08f else 0.05f),
                0.45f to Color.Transparent,
                1f to Color.White.copy(alpha = if (isStrongGlass) 0.04f else 0.025f)
            )
        )
    }
    val shadowElevation = if (isStrongGlass) 18.dp else 13.dp
    val shadowAlpha = if (isStrongGlass) 0.70f else 0.48f
    val borderAlpha = if (isStrongGlass) 0.26f else 0.19f
    val inactiveAlpha = if (isStrongGlass) 1f else 0.95f
    val inactiveColor = if (isLightTheme) scheme.onSurface.copy(alpha = 0.64f) else ext.tabInactive.copy(alpha = inactiveAlpha)
    val pillColor = if (isLightTheme) scheme.primary.copy(alpha = if (isStrongGlass) 0.14f else 0.10f) else Color.White.copy(alpha = if (isStrongGlass) 0.22f else 0.16f)
    val pillBorderColor = if (isLightTheme) scheme.primary.copy(alpha = if (isStrongGlass) 0.34f else 0.24f) else Color.White.copy(alpha = if (isStrongGlass) 0.34f else 0.24f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElevation,
                    shape = glassShape,
                    spotColor = ext.shadowOnDark.copy(alpha = shadowAlpha)
                )
                .clip(glassShape)
                .background(glassBackground)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = borderAlpha),
                    shape = glassShape
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(glassShape)
                    .background(reflectionSweep)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(glassShape)
                    .background(reflectionDepth)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarItem(
                    label = "домой",
                    iconRes = R.drawable.home,
                    selected = selected == "home",
                    activeColor = selectedContentColor,
                    inactiveColor = inactiveColor,
                    pillColor = pillColor,
                    pillBorderColor = pillBorderColor,
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f)
                )
                BottomBarItem(
                    label = "лидерборд",
                    iconRes = R.drawable.trophy,
                    selected = selected == "leaderboard",
                    activeColor = selectedContentColor,
                    inactiveColor = inactiveColor,
                    pillColor = pillColor,
                    pillBorderColor = pillBorderColor,
                    onClick = onLeaderboardClick,
                    modifier = Modifier.weight(1f)
                )
                BottomBarItem(
                    label = "вызовы",
                    iconRes = R.drawable.cognition,
                    selected = selected == "challenges",
                    activeColor = selectedContentColor,
                    inactiveColor = inactiveColor,
                    pillColor = pillColor,
                    pillBorderColor = pillBorderColor,
                    onClick = onChallengesClick,
                    modifier = Modifier.weight(1f),
                    showUnreadDot = showChallengesIncomingBadge,
                    unreadDotColor = ext.difficultyHard
                )
                BottomBarItem(
                    label = "викторины",
                    iconRes = R.drawable.library_books,
                    selected = selected == "quizzes",
                    activeColor = selectedContentColor,
                    inactiveColor = inactiveColor,
                    pillColor = pillColor,
                    pillBorderColor = pillBorderColor,
                    onClick = onQuizzesClick,
                    modifier = Modifier.weight(1f)
                )
                BottomBarItem(
                    label = "профиль",
                    iconRes = R.drawable.sentiment_calm,
                    selected = selected == "profile",
                    activeColor = selectedContentColor,
                    inactiveColor = inactiveColor,
                    pillColor = pillColor,
                    pillBorderColor = pillBorderColor,
                    onClick = onProfileClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    pillColor: Color,
    pillBorderColor: Color,
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
    val selectedPillColor by animateColorAsState(
        targetValue = if (selected) pillColor else Color.Transparent,
        animationSpec = tween(durationMillis = 260),
        label = "bottomBarPill"
    )
    val selectedPillBorderColor by animateColorAsState(
        targetValue = if (selected) pillBorderColor else Color.Transparent,
        animationSpec = tween(durationMillis = 260),
        label = "bottomBarPillBorder"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(selectedPillColor)
            .border(1.dp, selectedPillBorderColor, RoundedCornerShape(18.dp))
            .pressClickable(
                interactionSource = interactionSource,
                indication = null,
                pressedScale = 0.965f,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                }
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
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
