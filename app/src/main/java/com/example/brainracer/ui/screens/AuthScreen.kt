package com.example.brainracer.ui.screens

import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.R
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.ButtonShapeLogin
import com.example.brainracer.ui.theme.Shapes
import com.example.brainracer.ui.viewmodels.AuthViewModel


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(),
    onForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    //val user by authViewModel.user.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // Обработка ошибок
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
            isLoading = false
        }
    }

    Column (
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLogin) {
                Text(
                    text = "Welcome back",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                Text(
                    text = "Create account",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            if (!isLogin) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(
                        text = "Username",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    ) },
                    placeholder = { Text(
                        text = "Enter your username",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall
                    ) },
                    leadingIcon = { Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Username",
                        tint = MaterialTheme.colorScheme.onPrimary
                        ) },
                    isError = username.isBlank() || username.isEmpty() || username.contains(" "),
                    supportingText = {
                        if (username.contains(" ") || username.isEmpty())
                            Text(
                                text = "Username cannot be empty or contain spaces",
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.labelSmall
                                )
                    },
                    singleLine = true,
                    shape = Shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        errorTextColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(
                    text = "Email",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge
                    ) },
                placeholder = { Text(
                    text = "Enter your email",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall
                ) },
                singleLine = true,
                leadingIcon = { Icon(
                    Icons.Default.Email,
                    contentDescription = "Email",
                    tint = MaterialTheme.colorScheme.onPrimary
                    ) },
                isError = email.isNotBlank() && !isValidEmail(email),
                supportingText = { Text(
                    text = validateEmailMessage(email),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall
                    ) },
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    errorTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(
                    text = "Password",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyLarge

                ) },
                placeholder = { Text(
                    text = "Enter your password",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall
                ) },
                isError = password.isNotBlank() && !isValidPassword(password),
                supportingText = { Text(
                    text = validatePasswordMessage(password),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall
                    ) },
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                leadingIcon = { Icon(
                    Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = MaterialTheme.colorScheme.onPrimary
                    ) },
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
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    errorTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (isLogin) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        "Forgot Password?",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLogin) {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            authViewModel.signIn(email, password)
                        }
                    } else {
                        if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            authViewModel.signUp(email, password, username)
                        }
                    }
                },
                enabled = !isLoading,
                shape = ButtonShapeLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLogin) "Login" else "Register",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                        )
                }
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin) "Don't have an account? Sign up" else "Already have an account? Log in",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall
                    )
            }
        }
    }
}

// Function to send pre-check email
fun validateEmailMessage(email: String): String {
    if (email.isEmpty()) return ""

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
    if (password.isEmpty()) return ""

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

@Preview
@Composable
fun AuthScreenPreview() {
    BrainRacerTheme(
            darkTheme = false
    ) {
        AuthScreen()
    }
}

@Preview
@Composable
fun AuthScreenPreviewDark() {
    BrainRacerTheme(
        darkTheme = true
    ) {
        AuthScreen()
    }
}