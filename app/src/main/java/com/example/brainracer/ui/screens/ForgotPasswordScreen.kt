package com.example.brainracer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainracer.ui.theme.BrainRacerTheme
import com.example.brainracer.ui.theme.ButtonShapeLarge
import com.example.brainracer.ui.theme.Shapes
import com.example.brainracer.ui.viewmodels.AuthViewModel

@Preview( showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel = viewModel(),
    onPasswordResetSent: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    Column(
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
            Text(
                text = "Forgot Password",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(8.dp),
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = "Enter your email to reset your password",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp),
            )

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

            Spacer(modifier = Modifier.padding(8.dp))

            Button(onClick = {
                if (email.isNotBlank() && isValidEmail(email)) {
                    authViewModel.sendPasswordResetEmail(email)
                    onPasswordResetSent()
                } else {
                    Toast.makeText(context, "Enter valid email", Toast.LENGTH_SHORT).show()
                }
            },
                shape = ButtonShapeLarge,
                modifier = Modifier.fillMaxWidth()
                ) {
                Text(
                    "Reset Email",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Button(onClick = { onNavigateBack() },
                shape = ButtonShapeLarge,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onTertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@Preview
@Composable
fun ForgotPasswordScreenPreview() {
    BrainRacerTheme(
        darkTheme = false
    ) {
        ForgotPasswordScreen()
    }
}

@Preview
@Composable
fun ForgotPasswordScreenPreviewDark() {
    BrainRacerTheme(
        darkTheme = true
    ) {
        ForgotPasswordScreen()
    }
}