package com.example.brainracer.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.R
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.xbill.DNS.Lookup
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type
import java.net.InetAddress
import java.net.UnknownHostException

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
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    var emailValidationMessage by remember { mutableStateOf("") }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val isUsernameValid = isValidUsername(username)

    LaunchedEffect(isLogin) {
        emailValidationMessage = ""
        isCheckingEmail = false
    }

    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            emailValidationMessage = ""
            isCheckingEmail = true
            delay(500)
            emailValidationMessage = validateEmailMessageSuspend(email)
            isCheckingEmail = false
        } else {
            emailValidationMessage = ""
            isCheckingEmail = false
        }
    }

    val isEmailValid = emailValidationMessage == "Success"

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuthBgBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (!isLogin) {
            IconButton(
                onClick = { isLogin = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 4.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AuthTextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLogin) "Welcome back" else "Create Account",
                color = AuthTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (isLogin) 30.sp else 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            if (!isLogin) {
                AuthStyledTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = "Enter your username",
                    isError = username.contains(" ") || username.isEmpty(),
                    supportingText = {
                        if (username.contains(" ") || username.isEmpty()) {
                            Text(
                                "Username cannot be empty or contain spaces",
                                fontSize = 12.sp,
                                color = AuthPlaceholder
                            )
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

            AuthStyledTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "Enter your email",
                isError = email.isNotBlank() && !isEmailValid && emailValidationMessage.isNotEmpty(),
                trailingIcon = if (isCheckingEmail) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AuthGradientEnd
                        )
                    }
                } else null,
                supportingText = {
                    if (emailValidationMessage.isNotEmpty()) {
                        Text(
                            text = emailValidationMessage,
                            fontSize = 12.sp,
                            color = if (isEmailValid) AuthPlaceholder else BrainRacerColorTokens.InputValidationError
                        )
                    }
                }
            )

            Spacer(Modifier.height(14.dp))

            AuthStyledTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Enter your password",
                isError = password.isNotBlank() && !isValidPassword(password),
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = if (isPasswordVisible) {
                                painterResource(R.drawable.visibility)
                            } else {
                                painterResource(R.drawable.visibility_off)
                            },
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = AuthPlaceholder
                        )
                    }
                },
                supportingText = {
                    val msg = validatePasswordMessage(password)
                    if (msg.isNotEmpty() && msg != "Success") {
                        Text(text = msg, fontSize = 12.sp, color = BrainRacerColorTokens.InputValidationError)
                    }
                }
            )

            if (isLogin) {
                Text(
                    text = "Forgot Password?",
                    color = AuthLinkForgot,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { onForgotPassword() }
                )
            }

            Spacer(Modifier.height(22.dp))

            AuthGradientButton(
                text = if (isLogin) "Login" else "Register",
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
                enabled = when {
                    isLogin -> email.isNotBlank() && password.isNotBlank() && isEmailValid && isValidPassword(password)
                    else -> isUsernameValid && email.isNotBlank() && password.isNotBlank() && isEmailValid && isValidPassword(password)
                },
                loading = isLoading
            )

            if (isLogin) {
                Spacer(Modifier.height(14.dp))
                AuthGoogleButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Google sign-in will be available in a future update",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (isLogin) "Don't have an account? Sign up" else "Already have an account? Log in",
                color = AuthTextPrimary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isLogin = !isLogin }
                    .padding(vertical = 8.dp)
            )

        }
    }
}

suspend fun isDomainExists(domain: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val resolver = SimpleResolver("8.8.8.8")
        resolver.setTimeout(5)

        fun checkWithResolver(type: Int): Boolean {
            val lookup = Lookup(domain, type)
            lookup.setResolver(resolver)
            val records = lookup.run()
            return records != null && records.isNotEmpty()
        }

        if (checkWithResolver(Type.MX)) {
            Log.d("DNS_CHECK", "Domain $domain has MX records")
            return@withContext true
        }
        if (checkWithResolver(Type.A)) {
            Log.d("DNS_CHECK", "Domain $domain has A records")
            return@withContext true
        }
        if (checkWithResolver(Type.AAAA)) {
            Log.d("DNS_CHECK", "Domain $domain has AAAA records")
            return@withContext true
        }

        try {
            InetAddress.getByName(domain)
            Log.d("DNS_CHECK", "Domain $domain resolved via InetAddress")
            return@withContext true
        } catch (e: UnknownHostException) {
            Log.d("DNS_CHECK", "Domain $domain not found via InetAddress")
            return@withContext false
        }
    } catch (e: TextParseException) {
        Log.e("DNS_CHECK", "TextParseException for domain $domain", e)
        false
    } catch (e: Exception) {
        Log.e("DNS_CHECK", "Unexpected error checking domain $domain", e)
        true
    }
}

suspend fun validateEmailMessageSuspend(email: String): String {
    if (email.isEmpty()) return ""

    if (!isValidEmailSyntax(email)) {
        return when {
            !email.contains("@") -> "Email must contain @ symbol"
            !email.contains(".") -> "Email must contain a domain (e.g., .com)"
            email.startsWith("@") -> "Email cannot start with @"
            email.endsWith("@") -> "Email cannot end with @"
            email.endsWith(".") -> "Email cannot end with a dot"
            else -> "Invalid email format"
        }
    }

    val domain = email.substringAfter('@')
    return if (isDomainExists(domain)) {
        "Success"
    } else {
        "Email domain does not exist or cannot receive mail"
    }
}

fun isValidEmailSyntax(email: String): Boolean {
    val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailPattern.matches(email)
}

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

fun isValidPassword(password: String): Boolean {
    return validatePasswordMessage(password) == "Success"
}

fun isValidUsername(username: String): Boolean {
    return username.isNotBlank() && !username.contains(" ")
}

@Preview
@Composable
fun AuthScreenPreview() {
    BrainRacerTheme(darkTheme = false) {
        AuthScreen()
    }
}

@Preview
@Composable
fun AuthScreenPreviewDark() {
    BrainRacerTheme(darkTheme = true) {
        AuthScreen()
    }
}
