package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.viewmodels.AuthViewModel

//@Preview(showBackground = true, showSystemUi = true)
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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