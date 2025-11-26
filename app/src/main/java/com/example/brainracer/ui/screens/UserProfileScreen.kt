package com.example.brainracer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.viewmodels.ProfileViewModel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun UserProfileScreen(modifier: Modifier = Modifier) {  // ← ДОБАВИЛИ ПАРАМЕТР modifier
    val viewModel: ProfileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Column(
        modifier = modifier.padding(16.dp)  // ← ИСПОЛЬЗУЕМ modifier
    ) {
        Text(text = "Имя: ${uiState.username}")
        Text(text = "Email: ${uiState.email}")
        Text(text = "Сыграно игр: ${uiState.gamesPlayed}")
        Text(text = "Побед: ${uiState.gamesWon}")
        Text(text = "Процент побед: ${uiState.winRate}%")
        Text(text = "Очков: ${uiState.totalPoints}")
    }
}