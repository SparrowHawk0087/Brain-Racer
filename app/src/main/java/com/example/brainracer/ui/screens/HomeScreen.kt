package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.brainracer.ui.components.TextOnlyBottomBar
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    userId: String,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            homeViewModel.clearError()
        }
    }

    // Весь экран - Column, чтобы бар был внизу
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Основной контент занимает всё доступное пространство
        Box(
            modifier = Modifier
                .weight(1f)  // Заставляет занимать всё пространство кроме бара
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Welcome, ${uiState.userName}!")

                    uiState.userStats?.let { stats ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Quizzes taken: ${stats.totalQuizzesTaken}")
                        Text("Total points: ${stats.totalPoints}")
                        Text("Current streak: ${stats.currentStreak} days")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        navController.navigate("quizzes")
                    }) {
                        Text("Start Quiz")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        navController.navigate("profile/$userId")
                    }) {
                        Text("Profile")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        authViewModel.signOut()
                    }) {
                        Text("Sign Out")
                    }
                }
            }
        }

        // НИЖНИЙ БАР - Home подсвечен так как это HomeScreen
        TextOnlyBottomBar(
            showBar = true,
            currentRoute = "home"  // ← ВОТ ТУТ Home будет подсвечен
        )
    }
}