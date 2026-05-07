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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
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
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import com.example.brainracer.ui.viewmodels.AuthViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xbill.DNS.Lookup
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type
import java.net.InetAddress
import java.net.UnknownHostException

/** Внутренний маркер: email прошёл проверку (не для отображения). */
internal const val AUTH_EMAIL_VALIDATED = "__auth_email_ok__"

private const val PASSWORD_VALIDATED = "__auth_pwd_ok__"

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
    var usernameTouched by rememberSaveable { mutableStateOf(false) }
    var isLogin by remember { mutableStateOf(true) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val appNameBrush = remember {
        Brush.horizontalGradient(
            listOf(
                BrainRacerColorTokens.Accent,
                BrainRacerColorTokens.AccentSecondary
            )
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var emailValidationMessage by remember { mutableStateOf("") }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val isUsernameValid = isValidUsername(username)
    val showUsernameError = !isLogin && usernameTouched && !isUsernameValid

    LaunchedEffect(isLogin) {
        emailValidationMessage = ""
        isCheckingEmail = false
        if (isLogin) {
            usernameTouched = false
        }
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

    val isEmailValid = emailValidationMessage == AUTH_EMAIL_VALIDATED

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
            .background(colorScheme.background)
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
                    painter = painterResource(id = R.drawable.arrow_back_btn),
                    contentDescription = "Назад",
                    tint = colorScheme.onBackground
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
                text = stringResource(R.string.app_name),
                style = TextStyle(brush = appNameBrush),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = if (isLogin) "С возвращением" else "Регистрация",
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = if (isLogin) 30.sp else 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            if (!isLogin) {
                AuthStyledTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameTouched = true
                    },
                    label = "Имя пользователя",
                    placeholder = "Придумайте никнейм",
                    isError = showUsernameError,
                    supportingText = {
                        if (showUsernameError) {
                            when {
                                username.contains(" ") || username.isEmpty() -> {
                                    Text(
                                        "Никнейм не может быть пустым или содержать пробелы",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                username.isNotBlank() && username.all { it.isDigit() } -> {
                                    Text(
                                        "Никнейм не может состоять только из цифр",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

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
                                "Проверка адреса…",
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
                                "Адрес указан корректно",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                    }
                }
            )

            Spacer(Modifier.height(14.dp))

            AuthStyledTextField(
                value = password,
                onValueChange = { password = it },
                label = "Пароль",
                placeholder = "Введите пароль",
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
                            contentDescription = if (isPasswordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingText = {
                    val msg = validatePasswordMessage(password)
                    if (msg.isNotEmpty() && msg != PASSWORD_VALIDATED) {
                        Text(text = msg, fontSize = 12.sp, color = colorScheme.error)
                    }
                }
            )

            if (isLogin) {
                Text(
                    text = "Забыли пароль?",
                    color = colorScheme.primary,
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
                text = if (isLogin) "Войти" else "Зарегистрироваться",
                onClick = {
                    if (isLogin) {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            authViewModel.signIn(email, password)
                        }
                    } else {
                        if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                            usernameTouched = true
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
                    enabled = !isLoading,
                    onClick = {
                        val webClientId = runCatching { context.getString(R.string.default_web_client_id) }
                            .getOrNull()
                            .orEmpty()
                        if (webClientId.isBlank()) {
                            Toast.makeText(
                                context,
                                "Google Sign-In не настроен: отсутствует default_web_client_id",
                                Toast.LENGTH_LONG
                            ).show()
                            return@AuthGoogleButton
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setServerClientId(webClientId)
                                    .setFilterByAuthorizedAccounts(false)
                                    .setAutoSelectEnabled(false)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    context = context,
                                    request = request
                                )
                                val credential = result.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleCredential = GoogleIdTokenCredential
                                        .createFrom(credential.data)
                                    val idToken = googleCredential.idToken
                                    if (idToken.isBlank()) {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Не удалось получить токен Google",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        authViewModel.signInWithGoogle(idToken)
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Получен неподдерживаемый тип учетных данных",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (_: GetCredentialCancellationException) {
                                isLoading = false
                            } catch (_: GoogleIdTokenParsingException) {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Ошибка разбора Google токена",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: GetCredentialException) {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Ошибка Credential Manager: ${e.message ?: "неизвестная ошибка"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Ошибка входа через Google",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (isLogin) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти",
                color = colorScheme.onBackground,
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
            !email.contains("@") -> "В адресе должен быть символ @"
            !email.contains(".") -> "Укажите домен (например, .ru или .com)"
            email.startsWith("@") -> "Адрес не может начинаться с @"
            email.endsWith("@") -> "Адрес не может заканчиваться на @"
            email.endsWith(".") -> "Адрес не может заканчиваться точкой"
            else -> "Некорректный формат email"
        }
    }

    val domain = email.substringAfter('@')
    return if (isDomainExists(domain)) {
        AUTH_EMAIL_VALIDATED
    } else {
        "Домен почты не найден или не принимает письма"
    }
}

private val emailLocalPartCyrillicRegex = Regex("\\p{IsCyrillic}")

/** Убирает кириллицу только в локальной части (до первого @); домен не меняется. */
internal fun sanitizeEmailDisallowCyrillicLocalPart(input: String): String {
    val at = input.indexOf('@')
    if (at < 0) {
        return emailLocalPartCyrillicRegex.replace(input, "")
    }
    val local = emailLocalPartCyrillicRegex.replace(input.substring(0, at), "")
    return local + input.substring(at)
}

fun isValidEmailSyntax(email: String): Boolean {
    val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailPattern.matches(email)
}

fun validatePasswordMessage(password: String): String {
    if (password.isEmpty()) return ""

    return when {
        password.length < 6 -> "Пароль не короче 6 символов"
        password.contains(" ") -> "Пароль не должен содержать пробелы"
        !password.any { it.isDigit() } -> "Добавьте хотя бы одну цифру"
        !password.any { it.isLetter() } -> "Добавьте хотя бы одну букву"
        else -> PASSWORD_VALIDATED
    }
}

fun isValidPassword(password: String): Boolean {
    return validatePasswordMessage(password) == PASSWORD_VALIDATED
}

fun isValidUsername(username: String): Boolean {
    return username.isNotBlank()
            && !username.contains(" ")
            && !username.all { it.isDigit() }
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
