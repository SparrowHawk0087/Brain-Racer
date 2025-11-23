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
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                // Пользователь вышел или аккаунт удалён
                setContent {
                    BrainRacerTheme {
                        NavGraph()
                    }
                }
            }
        }
    }


override fun onStart() {
    super.onStart()
    auth.addAuthStateListener(authStateListener)
}

override fun onStop() {
    super.onStop()
    auth.removeAuthStateListener(authStateListener)
}
}