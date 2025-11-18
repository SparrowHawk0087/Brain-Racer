package com.example.brainracer.ui.screens

import androidx.annotation.StringRes
import com.example.brainracer.R

// TODO: дописать по мере создания экранов и их переходов
enum class BrainRacerScreen(@StringRes val title: Int) {
    Login(title = R.string.login),
    SignUp(title = R.string.sign_up),
    DashBoard(title = R.string.app_name)
}



