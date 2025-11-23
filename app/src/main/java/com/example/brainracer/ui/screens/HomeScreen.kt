package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    userId: String,
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current

    // Обновляем данные пользователя при входе
    LaunchedEffect(Unit) {
        authViewModel.reloadUser()
    }

    // Если пользователь удалён — перенаправляем
    if (user == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Session expired or account deleted", Toast.LENGTH_SHORT).show()
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome, User ID: $userId")
        Button(onClick = { navController.navigate("quizzes") }) {
            Text("Start Quiz")
        }
    }
}