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
import com.example.brainracer.ui.screens.ChallengeStartScreen
import com.example.brainracer.ui.screens.ChallengesScreen
import com.example.brainracer.ui.screens.ForgotPasswordScreen
import com.example.brainracer.ui.screens.FriendsScreen
import com.example.brainracer.ui.screens.HomeScreen
import com.example.brainracer.ui.screens.LeaderboardScreen
import com.example.brainracer.ui.screens.NotificationsScreen
import com.example.brainracer.ui.screens.ProfileScreen
import com.example.brainracer.ui.components.QuizDetailScreen
import com.example.brainracer.ui.screens.QuizListScreen
import com.example.brainracer.ui.screens.QuizCreatorScreen
import com.example.brainracer.ui.screens.QuizPlayScreen
import com.example.brainracer.ui.screens.SearchScreen
import com.example.brainracer.ui.screens.SettingsScreen
import com.example.brainracer.ui.components.bottomBarSelectedKey
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // ── Навигация BottomBar ────────────────────────────────────────────────

    val navigateToHome: () -> Unit = {
        user?.let {
            val route = "home/${it.uid}"
            // route у destination — шаблон (`home/{userId}`), не `home/uid`; иначе каждый тап дублирует navigate.
            if (bottomBarSelectedKey(currentRoute) != "home")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToLeaderboard: () -> Unit = {
        user?.let {
            val route = "leaderboard/${it.uid}"
            if (bottomBarSelectedKey(currentRoute) != "leaderboard")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToChallenges: () -> Unit = {
        user?.let {
            val route = "challenges/${it.uid}"
            if (bottomBarSelectedKey(currentRoute) != "challenges")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToQuizzes: () -> Unit = {
        if (bottomBarSelectedKey(currentRoute) != "quizzes")
            navController.navigate("quizzes") { launchSingleTop = true }
    }

    val navigateToProfile: () -> Unit = {
        user?.let {
            val route = "profile/${it.uid}"
            if (bottomBarSelectedKey(currentRoute) != "profile")
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
                navController        = navController,
                authViewModel        = authViewModel,
                onHomeClick          = navigateToHome,
                onLeaderboardClick   = navigateToLeaderboard,
                onChallengesClick    = navigateToChallenges,
                onQuizzesClick       = navigateToQuizzes,
                onProfileClick       = navigateToProfile,
                currentRoute         = currentRoute
            )
        }

        composable(
            "notifications/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val uid = back.arguments?.getString("userId") ?: ""
            NotificationsScreen(
                navController   = navController,
                currentUserId   = uid
            )
        }

        composable(
            "challenge_start/{challengeId}",
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { back ->
            val cid = back.arguments?.getString("challengeId") ?: ""
            ChallengeStartScreen(challengeId = cid, navController = navController)
        }

        // ── Quiz Detail ────────────────────────────────────────────────────
        composable(
            "quiz_detail/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) { back ->
            val quizId = back.arguments?.getString("quizId") ?: ""
            QuizDetailScreen(
                quizId = quizId,
                navController = navController,
                onNavigateToPlay = { id, practiceReplay ->
                    val q = if (practiceReplay) "?nonScoring=true" else ""
                    navController.navigate("quiz_play/$id$q")
                }
            )
        }

        // ── Quiz Play (соло + вызов) ─────────────────────────────────────────
        // fromNotifFlow: сценарий «уведомление → старт вызова» — интро как с главной, «Отмена» = на главную
        composable(
            route = "quiz_play/{quizId}?challengeId={challengeId}&introShown={introShown}&fromNotifFlow={fromNotifFlow}&nonScoring={nonScoring}",
            arguments = listOf(
                navArgument("quizId") { type = NavType.StringType },
                navArgument("challengeId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("introShown") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("fromNotifFlow") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("nonScoring") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { back ->
            val quizId = back.arguments?.getString("quizId") ?: ""
            val challengeId = back.arguments?.getString("challengeId").orEmpty().ifBlank { null }
            val introShown = back.arguments?.getBoolean("introShown") ?: false
            val fromNotif = back.arguments?.getBoolean("fromNotifFlow") ?: false
            val nonScoring = back.arguments?.getBoolean("nonScoring") ?: false
            QuizPlayScreen(
                quizId = quizId,
                challengeId = challengeId,
                challengeIntroAlreadyShown = introShown,
                challengeIntroCancelToHome = fromNotif,
                forceNonScoring = nonScoring,
                navController = navController
            )
        }

        // ── Search (category + режим только кастомных викторин) ─────────────
        composable(
            route = "search?category={category}&customOnly={customOnly}",
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = "Все"
                },
                navArgument("customOnly") {
                    type = NavType.StringType
                    defaultValue = "false"
                }
            )
        ) { back ->
            val category = back.arguments?.getString("category") ?: "Все"
            val customOnly = back.arguments?.getString("customOnly") == "true"
            SearchScreen(
                navController = navController,
                initialCategory = category,
                initialCustomOnly = customOnly
            )
        }

        // ── Quiz Creator ───────────────────────────────────────────────────
        composable("quiz_creator") {
            QuizCreatorScreen(navController = navController)
        }

        // ── Friends ────────────────────────────────────────────────────────
        composable(
            route = "friends/{userId}?preselectQuizId={preselectQuizId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("preselectQuizId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { back ->
            val preselectQuizId = back.arguments?.getString("preselectQuizId").orEmpty()
            FriendsScreen(
                navController               = navController,
                onHomeClick                 = navigateToHome,
                onLeaderboardClick          = navigateToLeaderboard,
                onChallengesClick           = navigateToChallenges,
                onQuizzesClick              = navigateToQuizzes,
                onProfileClick              = navigateToProfile,
                currentRoute                = currentRoute,
                preselectChallengeQuizIdArg = preselectQuizId.ifBlank { null }
            )
        }

        // ── Challenges (вкладки: входящие / активные / история) ───────────
        composable(
            "challenges/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            ChallengesScreen(
                navController        = navController,
                currentUserId        = userId,
                onHomeClick          = navigateToHome,
                onLeaderboardClick   = navigateToLeaderboard,
                onChallengesClick    = navigateToChallenges,
                onQuizzesClick       = navigateToQuizzes,
                onProfileClick       = navigateToProfile,
                currentRoute         = currentRoute
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

        // ── Leaderboard ────────────────────────────────────────────────────
        composable(
            "leaderboard/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            LeaderboardScreen(
                navController        = navController,
                currentUserId        = userId,
                onHomeClick          = navigateToHome,
                onLeaderboardClick   = navigateToLeaderboard,
                onChallengesClick    = navigateToChallenges,
                onQuizzesClick       = navigateToQuizzes,
                onProfileClick       = navigateToProfile,
                currentRoute         = currentRoute
            )
        }

        // ── Quizzes list ───────────────────────────────────────────────────
        composable("quizzes") {
            QuizListScreen(
                navController        = navController,
                onHomeClick          = navigateToHome,
                onLeaderboardClick   = navigateToLeaderboard,
                onChallengesClick    = navigateToChallenges,
                onQuizzesClick       = navigateToQuizzes,
                onProfileClick       = navigateToProfile,
                currentRoute         = currentRoute
            )
        }

        composable("settings") {
            SettingsScreen(
                navController   = navController,
                authViewModel   = authViewModel
            )
        }

        // ── Profile ────────────────────────────────────────────────────────
        composable(
            "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId       = back.arguments?.getString("userId") ?: ""
            val isOwnProfile = userId == (user?.uid ?: "")

            ProfileScreen(
                navController        = navController,
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userId               = userId,
                authViewModel        = authViewModel,
                onHomeClick          = navigateToHome,
                onLeaderboardClick   = navigateToLeaderboard,
                onChallengesClick    = navigateToChallenges,
                onQuizzesClick       = navigateToQuizzes,
                onProfileClick       = navigateToProfile,
                currentRoute         = currentRoute,
                isOwnProfile         = isOwnProfile
            )
        }
    }
}