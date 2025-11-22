package com.example.brainracer.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.brainracer.ui.screens.AuthScreen
import com.example.brainracer.ui.screens.ForgotPasswordScreen
import com.example.brainracer.ui.screens.ProfileScreen
import com.example.brainracer.ui.screens.QuizListScreen
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Composable
fun NavGraph(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    NavHost(navController = navController, startDestination = if (user == null) "auth" else "home/${user!!.uid}") {
        composable("auth") {
            AuthScreen(
                onSignIn = { navController.navigate("home/${authViewModel.user.value?.uid}") },
                onForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onPasswordResetSent = { navController.popBackStack() }
            )
        }
        /*TODO реализовать после создания заглушки домашнегоэкрана: composable(
            "home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            HomeScreen(
                navController = navController,
                userId = backStackEntry.arguments?.getString("userId") ?: ""
            )
        }*/
        composable("quizzes") {
            QuizListScreen(onQuizClick = { quizId ->
                navController.navigate("game/$quizId")
            })
        }

        composable(
            "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProfileScreen(
                onNavigateToAuth = { navController.navigate("auth") }
            )
        }
    }
}