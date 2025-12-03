package com.example.brainracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
            .padding(horizontal = 20.dp, vertical = 10.dp) // Нормальные отступы
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)) // Стандартное закругление
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .shadow(
                    elevation = 8.dp, // Умеренная тень
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .height(60.dp), // Стандартная высота для кнопок/баров
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Пункт Home
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Home",
                    color = if (currentRoute == "home")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleSmall // Стандартный размер
                )
            }

            // Тонкий разделитель
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Пункт Profile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile",
                    color = if (currentRoute == "profile")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 90)
@Composable
fun TextOnlyBottomBarPreviewHome() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TextOnlyBottomBar(
                showBar = true,
                currentRoute = "home"
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 90)
@Composable
fun TextOnlyBottomBarPreviewProfile() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TextOnlyBottomBar(
                showBar = true,
                currentRoute = "profile"
            )
        }
    }
}