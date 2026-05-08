package com.example.brainracer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.brainracer.data.local.QuizOfflineCache
import com.example.brainracer.data.preferences.UserPreferencesRepository
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.utils.NavGraph
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() до super.onCreate(): иначе системный SplashScreen
        // не успеет перехватить запуск. Тема активити выставлена в Theme.BrainRacer.Splash,
        // переключение на Theme.BrainRacer выполнит сама библиотека через postSplashScreenTheme.
        val splash = installSplashScreen()

        FirebaseApp.initializeApp(this)
        QuizOfflineCache.init(this)
        super.onCreate(savedInstanceState)

        // Плавный выход системного splash: иконка слегка масштабируется и исчезает,
        // одновременно появляется Compose SplashScreen с тем же фоновым цветом — переход
        // выглядит бесшовным.
        splash.setOnExitAnimationListener { provider: SplashScreenViewProvider ->
            val view: View = provider.iconView
            val fade   = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.85f)
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.85f)
            for (animator in listOf(fade, scaleX, scaleY)) {
                animator.duration = 280L
                animator.interpolator = AnticipateInterpolator(1.2f)
                animator.start()
            }
            fade.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    provider.remove()
                }
            })
        }

        setContent {
            val prefsRepo = remember { UserPreferencesRepository(applicationContext) }
            val darkTheme by prefsRepo.darkTheme.collectAsStateWithLifecycle(initialValue = false)
            BrainRacerTheme(darkTheme = darkTheme) {
                NavGraph()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Нужно для автоматического обновления состояния
    }

    override fun onStop() {
        super.onStop()
        // очистка не требуется, если не добавляли слушатель
    }
}
