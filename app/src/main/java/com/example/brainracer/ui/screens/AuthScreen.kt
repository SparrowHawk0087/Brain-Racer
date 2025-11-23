package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainracer.R
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = AuthViewModel(),
    onSignIn: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val authResult by authViewModel.authResult.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            isLoading = false
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    LaunchedEffect(authResult) {
        if (authResult?.user != null) {
            Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
            onSignIn() // ← здесь уже user != null
        }
    }


    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Brain Racer",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

         if (!isLogin) {
             OutlinedTextField(
                 value = username,
                 onValueChange = { username = it },
                 label = { Text("Username") },
                 placeholder = { Text("Enter your username") },
                 leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "Username") },
                 isError = username.isBlank() || username.isEmpty() || username.contains(" "),
                 supportingText = {if (username.contains(" ") || username.isEmpty()) Text("Username cannot be empty or contain spaces") },
                 singleLine = true,
                 shape = RoundedCornerShape(16.dp),
                 modifier = Modifier.padding(8.dp)
             )
         }

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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            isError = password.isNotBlank() && !isValidPassword(password),
            supportingText = { Text(validatePasswordMessage(password) ?: "") },
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            trailingIcon = {
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = if (isPasswordVisible) {
                            painterResource(R.drawable.visibility)
                        } else {
                            painterResource(R.drawable.visibility_off)
                        },
                        contentDescription = if (isPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (isLogin) {
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.signIn(email, password)
                    // onSuccess вызывается автоматически через LaunchedEffect
                }
            } else {
                if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.signUp(email, password, username)
                }
            }
        }, modifier = Modifier.width(200.dp)) {
            Text(if (isLogin) "Login" else "Sign Up")
        }

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Go to Sign Up" else "Go to Login")
        }

        if (isLogin) {
            TextButton(onClick = onForgotPassword) {
                Text("Forgot Password?")
            }
        }
    }
}

// Function to send pre-check email
fun validateEmailMessage(email: String): String {
    if (email.isEmpty()) return "Enter your email"

    if (!isValidEmail(email)) {
        return when {
            !email.contains("@") -> "Email must contain @ symbol"
            !email.contains(".") -> "Email must contain a domain (e.g., .com)"
            email.startsWith("@") -> "Email cannot start with @"
            email.endsWith("@") -> "Email cannot end with @"
            email.endsWith(".") -> "Email cannot end with a dot"
            else -> "Invalid email format"
        }
    }
    return "Success"
}

// Function to validate email format
fun isValidEmail(email: String): Boolean {
    val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailPattern.matches(email)
}


// Function to send pre-check password
fun validatePasswordMessage(password: String): String {
    if (password.isEmpty()) return "Enter your password"

    return when {
        password.length < 6 -> "Password must be at least 6 characters"
        password.contains(" ") -> "Password cannot contain spaces"
        !password.any { it.isDigit() } -> "Password must contain at least one digit"
        !password.any { it.isLetter() } -> "Password must contain at least one letter"
        else -> "Success"
    }
}

// Function to validate password format
fun isValidPassword(password: String): Boolean {
    return validatePasswordMessage(password) == "Success"
}