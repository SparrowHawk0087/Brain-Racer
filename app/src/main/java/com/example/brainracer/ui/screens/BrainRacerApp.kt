package com.example.brainracer.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.brainracer.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// TODO: дописать по мере создания экранов и их переходов
// Навигационные теги экранов
enum class BrainRacerScreen(@StringRes val title: Int) {
    Login(title = R.string.login),
    SignUp(title = R.string.sign_up),
    DashBoard(title = R.string.app_name)
}

// TODO: дописать по мере создания экранов и их переходов
// Экраны приложения и навигация между ними
@Composable
fun BrainRacerApp(navController: NavHostController = rememberNavController())
{
    // TODO: дописать по мере создания экранов и их переходов
    NavHost(navController, startDestination = BrainRacerScreen.Login.name) {
        composable (route = BrainRacerScreen.Login.name) { LoginScreen(navController) }
        composable (route = BrainRacerScreen.SignUp.name) { SignUpScreen(navController) }
        composable (route = BrainRacerScreen.DashBoard.name) { DashBoardScreen(navController) }
    }

}



