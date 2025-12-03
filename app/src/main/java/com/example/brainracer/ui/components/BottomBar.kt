package com.example.brainracer.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// Самый простой BottomBar без иконок
@Composable
fun TextOnlyBottomBar(
    showBar: Boolean = true
) {
    if (!showBar) return

    NavigationBar {
        NavigationBarItem(
            icon = {},  // Без иконки
            label = { Text("Главная") },
            selected = true,
            onClick = {}
        )

        NavigationBarItem(
            icon = {},
            label = { Text("Поиск") },
            selected = false,
            onClick = {}
        )

        NavigationBarItem(
            icon = {},
            label = { Text("Профиль") },
            selected = false,
            onClick = {}
        )
    }
}

// Preview
@Preview(showBackground = true, heightDp = 80)
@Composable
fun TextOnlyBottomBarPreview() {
    MaterialTheme {
        TextOnlyBottomBar(showBar = true)
    }
}