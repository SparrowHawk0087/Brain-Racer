package com.example.brainracer.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// Простой BottomBar с двумя пунктами
@Composable
fun TextOnlyBottomBar(
    showBar: Boolean = true,
    currentRoute: String = "home" // Чтобы показывать какой пункт активен
) {
    if (!showBar) return

    NavigationBar {
        // Пункт Home (Главная)
        NavigationBarItem(
            icon = {},  // Без иконки
            label = { Text("Home") },
            selected = currentRoute == "home", // Подсвечиваем если активен
            onClick = {} // Пусто - логику добавит другой разработчик
        )

        // Пункт Profile (Профиль)
        NavigationBarItem(
            icon = {},
            label = { Text("Profile") },
            selected = currentRoute == "profile", // Подсвечиваем если активен
            onClick = {} // Пусто
        )
    }
}

// Preview с активным Home
@Preview(showBackground = true, heightDp = 80)
@Composable
fun BottomBarHomePreview() {
    MaterialTheme {
        TextOnlyBottomBar(
            showBar = true,
            currentRoute = "home" // Home активен
        )
    }
}

// Preview с активным Profile
@Preview(showBackground = true, heightDp = 80)
@Composable
fun BottomBarProfilePreview() {
    MaterialTheme {
        TextOnlyBottomBar(
            showBar = true,
            currentRoute = "profile" // Profile активен
        )
    }
}