package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.components.TextOnlyBottomBar
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    userId: String
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current

    // Если пользователь удален или вышел
    LaunchedEffect(user) {
        if (user == null) {
            Toast.makeText(context, "Session expired or account deleted", Toast.LENGTH_SHORT).show()
            onNavigateToAuth()
        }
    }

    // Весь экран - Column, чтобы бар был внизу
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Основной контент с центрированием
        Box(
            modifier = Modifier
                .fillMaxSize()  // Занимает весь экран
                .weight(1f)     // Но даёт место для бара
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center  // ← Центрирование по вертикали
            ) {
                Text("Profile: $userId")
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    authViewModel.deleteAccount()
                }) {
                    Text("Delete Account")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    authViewModel.signOut()
                }) {
                    Text("Sign Out")
                }
            }
        }

        // НИЖНИЙ БАР
        TextOnlyBottomBar(
            showBar = true,
            currentRoute = "profile"
        )
    }
}