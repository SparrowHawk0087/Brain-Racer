package com.example.brainracer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.brainracer.data.utils.QuizDataManager
import com.example.brainracer.data.utils.QuizDataSeeder
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.utils.NavGraph
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // инициализируем

        // Проверяем и добавляем викторины в фоне
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ждем инициализацию Firebase
                delay(2000)

                // Проверяем, есть ли викторины
                val quizzesExist = QuizDataManager.checkIfQuizzesExist()

                if (!quizzesExist) {
                    Log.d("MainActivity", "Викторин не найдено, добавляем демо...")
                    val success = QuizDataManager.addDemoQuizzes()
                    Log.d("MainActivity", "Демо-викторины добавлены: $success")
                } else {
                    Log.d("MainActivity", "Викторины уже существуют")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка инициализации викторин: ${e.message}")
            }
        }
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