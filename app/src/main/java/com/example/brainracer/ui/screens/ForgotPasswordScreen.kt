package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.brainracer.ui.viewmodels.AuthViewModel

//@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel = AuthViewModel(),
    onPasswordResetSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email")  },
            isError =  email.isNotBlank() && !isValidEmail(email),
            supportingText = { Text(validateEmailMessage(email)) },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Button(onClick = {
            if (email.isNotBlank() && isValidEmail(email)) {
                authViewModel.sendPasswordResetEmail(email)
                onPasswordResetSent()
            } else {
                Toast.makeText(context, "Enter valid email", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Send Password Reset Email")
        }
    }
}