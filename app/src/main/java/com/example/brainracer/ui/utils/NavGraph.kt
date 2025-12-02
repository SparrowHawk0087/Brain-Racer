package com.example.brainracer.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.brainracer.ui.screens.HomeScreen
import com.example.brainracer.ui.screens.ProfileScreen
import com.example.brainracer.ui.screens.QuizListScreen
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = viewModel(),
    onAuthStateChange: (Boolean) -> Unit = {},
    auth: FirebaseAuth
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    LaunchedEffect(user) {
        user?.let { currentUser ->
            val userId = currentUser.uid
            if (userId.isNotBlank()) {
                navController.navigate("home/$userId") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (user != null) "home/${user?.uid}" else "auth"
    ) {
        composable("auth") {
            AuthScreen(
                authViewModel = authViewModel,
                onForgotPassword = {
                    navController.navigate("forgot_password")
                }
            )
        }

        // ОБНОВЛЁННЫЙ ЭКРАН HOME - теперь с двумя ViewModel
        composable(
            "home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            HomeScreen(
                navController = navController,
                userId = userId,
                authViewModel = authViewModel // ← передаём authViewModel
                // homeViewModel создаётся автоматически через viewModel() default параметр
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onPasswordResetSent = {
                    navController.popBackStack()
                }
            )
        }

        composable("quizzes") {
            QuizListScreen(onQuizClick = { quizId ->
                navController.navigate("quiz/$quizId")
            })
        }

        composable(
            "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userId = userId,
                authViewModel = authViewModel
            )
        }
    }
}