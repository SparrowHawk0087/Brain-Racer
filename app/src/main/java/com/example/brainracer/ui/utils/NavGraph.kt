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
import com.example.brainracer.ui.screens.ChallengeRoundReviewScreen
import com.example.brainracer.ui.screens.ChallengesScreen
import com.example.brainracer.ui.screens.ForgotPasswordScreen
import com.example.brainracer.ui.screens.FriendsScreen
import com.example.brainracer.ui.screens.HomeScreen
import com.example.brainracer.ui.screens.ProfileScreen
import com.example.brainracer.ui.components.QuizDetailScreen
import com.example.brainracer.ui.screens.QuizListScreen
import com.example.brainracer.ui.screens.QuizCreatorScreen
import com.example.brainracer.ui.screens.QuizPlayScreen
import com.example.brainracer.ui.screens.SearchScreen
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // ── Навигация BottomBar ────────────────────────────────────────────────

    val navigateToHome: () -> Unit = {
        user?.let {
            val route = "home/${it.uid}"
            if (currentRoute != route)
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToFriends: () -> Unit = {
        user?.let {
            val route = "friends/${it.uid}"
            if (currentRoute != route)
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToProfile: () -> Unit = {
        user?.let {
            val route = "profile/${it.uid}"
            if (currentRoute != route)
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    // После входа — на home
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
        navController    = navController,
        startDestination = if (user != null) "home/${user?.uid}" else "auth"
    ) {

        // ── Auth ───────────────────────────────────────────────────────────
        composable("auth") {
            AuthScreen(
                authViewModel    = authViewModel,
                onForgotPassword = { navController.navigate("forgot_password") }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                authViewModel      = authViewModel,
                onPasswordResetSent = { navController.popBackStack() },
                onNavigateBack     = { navController.popBackStack() }
            )
        }

        // ── Home ───────────────────────────────────────────────────────────
        composable(
            "home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            HomeScreen(
                navController  = navController,
                authViewModel  = authViewModel,
                onHomeClick    = navigateToHome,
                onFriendsClick = navigateToFriends,
                onProfileClick = navigateToProfile,
                currentRoute   = currentRoute
            )
        }

        // ── Quiz Detail ────────────────────────────────────────────────────
        composable(
            "quiz_detail/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { back ->
            val quizId = back.arguments?.getString("quizId") ?: ""
            QuizDetailScreen(
                quizId       = quizId,
                navController = navController,
                onStartQuiz  = { id -> navController.navigate("quiz_play/$id") }
            )
        }

        // ── Quiz Play (обычный режим) ──────────────────────────────────────
        composable(
            "quiz_play/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { back ->
            val quizId = back.arguments?.getString("quizId") ?: ""
            QuizPlayScreen(quizId = quizId, navController = navController)
        }

        // ── Quiz Play (режим вызова) ───────────────────────────────────────
        // Маршрут: quiz_play/{quizId}?challengeId={challengeId}
        composable(
            route     = "quiz_play/{quizId}?challengeId={challengeId}",
            arguments = listOf(
                navArgument("quizId")      { type = NavType.StringType },
                navArgument("challengeId") {
                    type             = NavType.StringType
                    nullable         = true
                    defaultValue     = null
                }
            )
        ) { back ->
            val quizId      = back.arguments?.getString("quizId") ?: ""
            val challengeId = back.arguments?.getString("challengeId")
            QuizPlayScreen(
                quizId      = quizId,
                challengeId = challengeId,
                navController = navController
            )
        }

        // ── Search ────────────────────────────────────────────────────────
        composable("search") {
            SearchScreen(navController = navController)
        }
        composable(
            "search?category={category}",
            arguments = listOf(navArgument("category") {
                type         = NavType.StringType
                defaultValue = "Все"
            })
        ) { back ->
            val category = back.arguments?.getString("category") ?: "Все"
            SearchScreen(navController = navController, initialCategory = category)
        }

        // ── Quiz Creator ───────────────────────────────────────────────────
        composable("quiz_creator") {
            QuizCreatorScreen(navController = navController)
        }

        // ── Friends ────────────────────────────────────────────────────────
        composable(
            "friends/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            FriendsScreen(
                onHomeClick    = navigateToHome,
                onFriendsClick = navigateToFriends,
                onProfileClick = navigateToProfile,
                currentRoute   = currentRoute
            )
        }

        // ── Challenges (вкладки: входящие / активные / история) ───────────
        composable(
            "challenges/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            ChallengesScreen(
                navController  = navController,
                currentUserId  = userId,
                onHomeClick    = navigateToHome,
                onFriendsClick = navigateToFriends,
                onProfileClick = navigateToProfile,
                currentRoute   = currentRoute
            )
        }

        // ── Challenge Round Review ─────────────────────────────────────────
        composable(
            "challenge_review/{challengeId}",
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { back ->
            val challengeId = back.arguments?.getString("challengeId") ?: ""
            ChallengeRoundReviewScreen(
                challengeId   = challengeId,
                navController = navController
            )
        }

        // ── Quizzes list ───────────────────────────────────────────────────
        composable("quizzes") {
            QuizListScreen(onQuizClick = { quizId ->
                navController.navigate("quiz_detail/$quizId")
            })
        }

        // ── Profile ────────────────────────────────────────────────────────
        composable(
            "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId       = back.arguments?.getString("userId") ?: ""
            val isOwnProfile = userId == (user?.uid ?: "")

            ProfileScreen(
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userId         = userId,
                authViewModel  = authViewModel,
                onHomeClick    = navigateToHome,
                onFriendsClick = navigateToFriends,
                onProfileClick = navigateToProfile,
                currentRoute   = currentRoute,
                isOwnProfile   = isOwnProfile
            )
        }
    }
}