package com.example.brainracer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.utils.NavGraph
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // инициализируем

        setContent {
            BrainRacerTheme {
                // Передаём auth и currentUser
                NavGraph(auth = auth)
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