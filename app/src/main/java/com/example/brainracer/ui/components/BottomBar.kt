package com.example.brainracer.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun TextOnlyBottomBar(
    showBar: Boolean = true,
    currentRoute: String = "home"
) {
    if (!showBar) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant) // Фон островка
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка Home прямо на фоне островка
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = if (currentRoute == "home")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )

            // Иконка Profile прямо на фоне островка
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = if (currentRoute == "profile")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 120)
@Composable
fun TextOnlyBottomBarPreviewHome() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TextOnlyBottomBar(
                showBar = true,
                currentRoute = "home"
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 120)
@Composable
fun TextOnlyBottomBarPreviewProfile() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TextOnlyBottomBar(
                showBar = true,
                currentRoute = "profile"
            )
        }
    }
}