package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel = viewModel(),
    onPasswordResetSent: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var emailValidationMessage by remember { mutableStateOf("") }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

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

    val isEmailValid = emailValidationMessage == AUTH_EMAIL_VALIDATED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 4.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = colorScheme.onBackground
            )
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
                text = "Восстановление пароля",
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Укажите email — мы отправим ссылку для сброса пароля",
                color = colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            AuthStyledTextField(
                value = email,
                onValueChange = { email = sanitizeEmailDisallowCyrillicLocalPart(it) },
                label = "Электронная почта",
                placeholder = "Введите email",
                isError = email.isNotBlank() && !isEmailValid && emailValidationMessage.isNotEmpty(),
                trailingIcon = if (isCheckingEmail) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary
                        )
                    }
                } else null,
                supportingText = {
                    when {
                        isCheckingEmail ->
                            Text(
                                text = "Проверка адреса…",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        emailValidationMessage.isNotEmpty() && !isEmailValid ->
                            Text(
                                text = emailValidationMessage,
                                fontSize = 12.sp,
                                color = colorScheme.error
                            )
                        isEmailValid ->
                            Text(
                                text = "Адрес указан корректно",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            AuthGradientButton(
                text = "Отправить ссылку",
                onClick = {
                    if (isEmailValid) {
                        authViewModel.sendPasswordResetEmail(email)
                        onPasswordResetSent()
                    } else {
                        Toast.makeText(context, "Введите корректный email", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isEmailValid,
                loading = false
            )
        }
    }
}

@Preview
@Composable
fun ForgotPasswordScreenPreview() {
    BrainRacerTheme(darkTheme = false) {
        ForgotPasswordScreen()
    }
}

@Preview
@Composable
fun ForgotPasswordScreenPreviewDark() {
    BrainRacerTheme(darkTheme = true) {
        ForgotPasswordScreen()
    }
}
