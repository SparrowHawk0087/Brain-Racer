package com.example.brainracer.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

    // Получаем текущий маршрут из back stack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
        ?: if (user != null) "home/${user?.uid}" else "auth"

    // Функции навигации для BottomBar - УПРОЩЕННЫЕ версии
    val navigateToHome: () -> Unit = {
        user?.let {
            val homeRoute = "home/${it.uid}"
            // Проверяем, не находимся ли уже на домашнем экране
            if (currentRoute != homeRoute) {
                println("DEBUG NavGraph: Navigating to $homeRoute from $currentRoute")
                navController.navigate(homeRoute) {
                    // Простая навигация без popUpTo
                    launchSingleTop = true
                }
            } else {
                println("DEBUG NavGraph: Already on home screen")
            }
        }
    }

    val navigateToProfile: () -> Unit = {
        user?.let {
            val profileRoute = "profile/${it.uid}"
            // Проверяем, не находимся ли уже на профиле
            if (currentRoute != profileRoute) {
                println("DEBUG NavGraph: Navigating to $profileRoute from $currentRoute")
                navController.navigate(profileRoute) {
                    // Простая навигация без popUpTo
                    launchSingleTop = true
                }
            } else {
                println("DEBUG NavGraph: Already on profile screen")
            }
        }
    }

    LaunchedEffect(user) {
        user?.let { currentUser ->
            val userId = currentUser.uid
            if (userId.isNotBlank() && currentRoute == "auth") {
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

        composable(
            "home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel,
                onHomeClick = navigateToHome,
                onProfileClick = navigateToProfile,
                currentRoute = currentRoute
            )
        }


        composable("forgot_password") {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onPasswordResetSent = {
                    navController.popBackStack()
                },
                onNavigateBack = {
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
            val currentUserId = user?.uid ?: ""
            val isOwnProfile = userId == currentUserId

            ProfileScreen(
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userId = userId,
                authViewModel = authViewModel,
                onHomeClick = navigateToHome,
                onProfileClick = navigateToProfile,
                currentRoute = currentRoute,
                isOwnProfile = isOwnProfile
            )
        }
    }
}