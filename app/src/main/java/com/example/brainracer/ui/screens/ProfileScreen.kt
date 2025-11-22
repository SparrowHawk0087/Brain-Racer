package com.example.brainracer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreen(
    /*profileViewModel: ProfileViewModel = viewModel(),*/
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToAuth: () -> Unit
) {
    //TODO: val user = profileViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        //TODO: profileViewModel.fetchUser(authViewModel)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*TODO:user.value?.let {
            Text("Username: ${it.username}"})
            Text("Email: ${it.email}")
            Text("Total Quizzes: ${it.totalQuizzes}")
            Text("Average Score: ${it.averageScore}")
            Text("Achievements: ${it.achievements.joinToString()}")
        }*/

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /*TODO:authViewModel.deleteAccount()*/}) {
            Text("Delete Account")
        }
    }
}