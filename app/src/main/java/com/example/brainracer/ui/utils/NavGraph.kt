package com.example.brainracer.ui.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
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
import com.example.brainracer.ui.screens.SplashScreen
import com.example.brainracer.ui.components.NicknameEnforcementOverlay
import com.example.brainracer.ui.components.bottomBarSelectedKey
import com.example.brainracer.ui.viewmodels.AuthViewModel
import com.example.brainracer.ui.viewmodels.ChallengesIncomingBadgeViewModel
import com.example.brainracer.ui.viewmodels.NicknameEnforcementState
import com.example.brainracer.ui.viewmodels.NicknameEnforcementViewModel

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = viewModel(),
    nicknameEnforcementViewModel: NicknameEnforcementViewModel = viewModel(),
) {
    val navMotion = AppMotionConfig.nav

    fun isTabRoute(route: String?): Boolean {
        if (route.isNullOrBlank()) return false
        return route.startsWith("home/") ||
                route.startsWith("leaderboard/") ||
                route.startsWith("challenges/") ||
                route == "quizzes" ||
                route.startsWith("profile/")
    }

    fun enterFor(initial: NavBackStackEntry, target: NavBackStackEntry): EnterTransition {
        val initialRoute = initial.destination.route
        val targetRoute = target.destination.route
        return if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
            fadeIn(animationSpec = tween(durationMillis = navMotion.tabFadeInMs))
        } else {
            slideInHorizontally(
                initialOffsetX = { width -> (width * navMotion.enterOffsetFraction).toInt() },
                animationSpec = spring(
                    dampingRatio = navMotion.dampingRatio,
                    stiffness = navMotion.stiffness
                )
            ) + fadeIn(animationSpec = tween(durationMillis = navMotion.enterFadeMs))
        }
    }

    fun exitFor(initial: NavBackStackEntry, target: NavBackStackEntry): ExitTransition {
        val initialRoute = initial.destination.route
        val targetRoute = target.destination.route
        return if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
            fadeOut(animationSpec = tween(durationMillis = navMotion.tabFadeOutMs))
        } else {
            slideOutHorizontally(
                targetOffsetX = { width -> -(width * navMotion.exitOffsetFraction).toInt() },
                animationSpec = spring(
                    dampingRatio = navMotion.dampingRatio,
                    stiffness = navMotion.stiffness
                )
            ) + fadeOut(animationSpec = tween(durationMillis = navMotion.exitFadeMs))
        }
    }

    fun popEnterFor(initial: NavBackStackEntry, target: NavBackStackEntry): EnterTransition {
        val initialRoute = initial.destination.route
        val targetRoute = target.destination.route
        return if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
            fadeIn(animationSpec = tween(durationMillis = navMotion.tabFadeInMs))
        } else {
            slideInHorizontally(
                initialOffsetX = { width -> -(width * navMotion.popEnterOffsetFraction).toInt() },
                animationSpec = spring(
                    dampingRatio = navMotion.dampingRatio,
                    stiffness = navMotion.stiffness
                )
            ) + fadeIn(animationSpec = tween(durationMillis = navMotion.popEnterFadeMs))
        }
    }

    fun popExitFor(initial: NavBackStackEntry, target: NavBackStackEntry): ExitTransition {
        val initialRoute = initial.destination.route
        val targetRoute = target.destination.route
        return if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
            fadeOut(animationSpec = tween(durationMillis = navMotion.tabFadeOutMs))
        } else {
            slideOutHorizontally(
                targetOffsetX = { width -> (width * navMotion.popExitOffsetFraction).toInt() },
                animationSpec = spring(
                    dampingRatio = navMotion.dampingRatio,
                    stiffness = navMotion.stiffness
                )
            ) + fadeOut(animationSpec = tween(durationMillis = navMotion.popExitFadeMs))
        }
    }

    val navController = rememberNavController()
    val user by authViewModel.user.collectAsState()
    val enforcementState by nicknameEnforcementViewModel.state.collectAsState()
    val nicknameLockExplanation by nicknameEnforcementViewModel.lockExplanation.collectAsState()
    val nicknameSubmitError by nicknameEnforcementViewModel.submitError.collectAsState()
    val nicknameSaving by nicknameEnforcementViewModel.isSaving.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
    val loggedUid = user?.uid

    // LocalActivity напрямую возвращает текущую Activity (с activity-compose 1.10+),
    // поэтому не нужно кастить LocalContext.current — это и более корректно (хост может быть
    // обёрнут в ContextWrapper), и убирает lint-предупреждение.
    val activity = LocalActivity.current as ComponentActivity
    val challengesBadgeViewModel: ChallengesIncomingBadgeViewModel =
        viewModel(viewModelStoreOwner = activity)
    val bottomBarShowChallengesIncomingBadge by challengesBadgeViewModel.hasIncomingPending.collectAsState()
    LaunchedEffect(loggedUid) {
        challengesBadgeViewModel.bindToUser(loggedUid)
    }
    val profileDestinationUserId = navBackStackEntry?.let { entry ->
        if (entry.destination.route.orEmpty().startsWith("profile/"))
            entry.arguments?.getString("userId")
        else null
    }

    fun bottomTabKey(): String =
        bottomBarSelectedKey(currentRoute, loggedUid, profileDestinationUserId)

    // ── Навигация BottomBar ────────────────────────────────────────────────

    val navigateToHome: () -> Unit = {
        user?.let {
            val route = "home/${it.uid}"
            // route у destination — шаблон (`home/{userId}`), не `home/uid`; иначе каждый тап дублирует navigate.
            if (bottomTabKey() != "home")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToLeaderboard: () -> Unit = {
        user?.let {
            val route = "leaderboard/${it.uid}"
            if (bottomTabKey() != "leaderboard")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToChallenges: () -> Unit = {
        user?.let {
            val route = "challenges/${it.uid}"
            if (bottomTabKey() != "challenges")
                navController.navigate(route) { launchSingleTop = true }
        }
    }

    val navigateToQuizzes: () -> Unit = {
        if (bottomTabKey() != "quizzes")
            navController.navigate("quizzes") { launchSingleTop = true }
    }

    val navigateToProfile: () -> Unit = {
        user?.let {
            val route = "profile/${it.uid}"
            if (bottomTabKey() != "profile")
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

    LaunchedEffect(user?.uid, currentRoute) {
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            nicknameEnforcementViewModel.reset()
        } else {
            nicknameEnforcementViewModel.refreshAsync(uid)
        }
    }

    val nicknameLocked = user != null && enforcementState == NicknameEnforcementState.Locked

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (nicknameLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            NavHost(
                navController    = navController,
                // Стартуем со splash. Splash сам по таймеру вызовет navigate в home/{uid} или auth,
                // в зависимости от текущего состояния FirebaseAuth (доступно синхронно).
                startDestination = "splash",
                enterTransition = { enterFor(initialState, targetState) },
                exitTransition = { exitFor(initialState, targetState) },
                popEnterTransition = { popEnterFor(initialState, targetState) },
                popExitTransition = { popExitFor(initialState, targetState) }
            ) {

                // ── Splash ─────────────────────────────────────────────────────────
                composable("splash") {
                    SplashScreen(
                        onFinished = {
                            val targetRoute = user?.uid?.let { "home/$it" } ?: "auth"
                            navController.navigate(targetRoute) {
                                popUpTo("splash") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

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
                        currentRoute                  = currentRoute,
                        bottomBarLoggedInUserId       = loggedUid,
                        bottomBarProfileDestinationUserId = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge
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
                composable(
                    route = "quiz_creator?editQuizId={editQuizId}",
                    arguments = listOf(
                        navArgument("editQuizId") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { back ->
                    val editQuizId = back.arguments?.getString("editQuizId").orEmpty().ifBlank { null }
                    QuizCreatorScreen(
                        navController = navController,
                        editQuizIdArg = editQuizId
                    )
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
                    val profileUserId = back.arguments?.getString("userId").orEmpty()
                    val preselectQuizId = back.arguments?.getString("preselectQuizId").orEmpty()
                    FriendsScreen(
                        navController               = navController,
                        viewedUserId               = profileUserId,
                        onHomeClick                 = navigateToHome,
                        onLeaderboardClick          = navigateToLeaderboard,
                        onChallengesClick           = navigateToChallenges,
                        onQuizzesClick              = navigateToQuizzes,
                        onProfileClick              = navigateToProfile,
                        currentRoute                       = currentRoute,
                        bottomBarLoggedInUserId            = loggedUid,
                        bottomBarProfileDestinationUserId    = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge,
                        preselectChallengeQuizIdArg        = preselectQuizId.ifBlank { null }
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
                        currentRoute                  = currentRoute,
                        bottomBarLoggedInUserId       = loggedUid,
                        bottomBarProfileDestinationUserId = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge
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
                        currentRoute                  = currentRoute,
                        bottomBarLoggedInUserId       = loggedUid,
                        bottomBarProfileDestinationUserId = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge
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
                        currentRoute                  = currentRoute,
                        bottomBarLoggedInUserId       = loggedUid,
                        bottomBarProfileDestinationUserId = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge
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
                        currentRoute                       = currentRoute,
                        bottomBarLoggedInUserId            = loggedUid,
                        bottomBarProfileDestinationUserId    = profileDestinationUserId,
                        bottomBarShowChallengesIncomingBadge = bottomBarShowChallengesIncomingBadge,
                        isOwnProfile         = isOwnProfile
                    )
                }
            }
        }

        NicknameEnforcementOverlay(
            visible = nicknameLocked,
            explanationText = nicknameLockExplanation,
            submitError = nicknameSubmitError,
            isSaving = nicknameSaving,
            onClearError = { nicknameEnforcementViewModel.clearSubmitError() },
            onSubmit = { nick -> nicknameEnforcementViewModel.submitNewNickname(nick) { } }
        )
    }
}