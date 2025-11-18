package com.example.brainracer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun DashBoardScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome to Brain Racer", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.padding(16.dp))

            Button(onClick = {
                Firebase.auth.signOut()
                navController.navigate(BrainRacerScreen.Login.title) {
                    popUpTo(BrainRacerScreen.DashBoard.title) { inclusive = true }
                }
            }) {Text("Logout")}
        }
    }

}