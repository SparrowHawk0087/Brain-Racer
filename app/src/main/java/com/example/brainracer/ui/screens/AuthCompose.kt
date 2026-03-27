package com.example.brainracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AuthBgBlack       = Color(0xFF000000)
val AuthFieldBg       = Color(0xFF2D2B45)
val AuthFieldBorder   = Color(0xFF8B82C8)
val AuthTextPrimary   = Color(0xFFFFFFFF)
val AuthPlaceholder   = Color(0xFF9E9EAE)
val AuthLinkForgot    = Color(0xFF8AB4FF)
val AuthGradientStart = Color(0xFF7B61FF)
val AuthGradientEnd   = Color(0xFFB066FE)

val AuthFieldShape = RoundedCornerShape(12.dp)
val AuthPillShape  = RoundedCornerShape(50)

@Composable
fun AuthGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = Brush.horizontalGradient(listOf(AuthGradientStart, AuthGradientEnd))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled || loading) 1f else 0.45f)
            .clip(AuthPillShape)
            .background(brush)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = AuthTextPrimary
            )
        } else {
            Text(
                text = text,
                color = AuthTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun AuthStyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = AuthFieldShape,
        visualTransformation = visualTransformation,
        label = {
            Text(
                text = label,
                color = AuthPlaceholder,
                fontSize = 14.sp
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = AuthPlaceholder.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AuthTextPrimary,
            unfocusedTextColor = AuthTextPrimary,
            cursorColor = AuthTextPrimary,
            focusedBorderColor = AuthFieldBorder,
            unfocusedBorderColor = AuthFieldBorder.copy(alpha = 0.45f),
            errorBorderColor = Color(0xFFFF6B6B),
            focusedContainerColor = AuthFieldBg,
            unfocusedContainerColor = AuthFieldBg,
            errorContainerColor = AuthFieldBg,
            focusedLabelColor = AuthPlaceholder,
            unfocusedLabelColor = AuthPlaceholder,
            errorSupportingTextColor = Color(0xFFFF8A8A),
            focusedSupportingTextColor = AuthPlaceholder,
            unfocusedSupportingTextColor = AuthPlaceholder
        )
    )
}

@Composable
fun AuthGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(AuthPillShape)
            .background(AuthFieldBg)
            .border(1.dp, AuthFieldBorder.copy(alpha = 0.55f), AuthPillShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AuthTextPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = AuthFieldBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Sign in with Google",
                color = AuthTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
