package com.example.brainracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    showBar: Boolean = true,
    currentRoute: String = "home",
    onHomeClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},   // <-- НОВЫЙ параметр
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!showBar) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Home ──────────────────────────────────────────────────────────
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = if (currentRoute.startsWith("home"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // ── Friends ───────────────────────────────────────────────────────
            IconButton(
                onClick = onFriendsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "Friends",
                    // Подсветка активна, если маршрут начинается с "friends"
                    tint = if (currentRoute.startsWith("friends"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // ── Profile ───────────────────────────────────────────────────────
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = if (currentRoute.startsWith("profile"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 120)
@Composable
private fun BottomBarPreviewHome() {
    MaterialTheme {
        BottomBar(showBar = true, currentRoute = "home")
    }
}

@Preview(showBackground = true, heightDp = 120)
@Composable
private fun BottomBarPreviewFriends() {
    MaterialTheme {
        BottomBar(showBar = true, currentRoute = "friends")
    }
}

@Preview(showBackground = true, heightDp = 120)
@Composable
private fun BottomBarPreviewProfile() {
    MaterialTheme {
        BottomBar(showBar = true, currentRoute = "profile")
    }
}
