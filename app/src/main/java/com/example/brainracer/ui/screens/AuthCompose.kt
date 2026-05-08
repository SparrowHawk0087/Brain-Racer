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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.example.brainracer.ui.theme.BrainRacerColorTokens
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AuthFieldShape = RoundedCornerShape(12.dp)
val AuthPillShape = RoundedCornerShape(50)

@Composable
fun AuthGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = Brush.horizontalGradient(
        listOf(BrainRacerColorTokens.Accent, BrainRacerColorTokens.AccentSecondary)
    )
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
                color = BrainRacerColorTokens.OnAccent
            )
        } else {
            Text(
                text = text,
                color = BrainRacerColorTokens.OnAccent,
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
    val cs = MaterialTheme.colorScheme
    val fieldBg = cs.surfaceContainerHigh
    val onField = cs.onSurface
    val muted = cs.onSurfaceVariant

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
                color = if (isError) cs.error else muted,
                fontSize = 14.sp
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = muted.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = onField,
            unfocusedTextColor = onField,
            disabledTextColor = onField.copy(alpha = 0.38f),
            errorTextColor = onField,
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            disabledContainerColor = fieldBg.copy(alpha = 0.6f),
            errorContainerColor = fieldBg,
            cursorColor = cs.primary,
            errorCursorColor = cs.error,
            focusedBorderColor = cs.outline,
            unfocusedBorderColor = cs.outline.copy(alpha = 0.55f),
            disabledBorderColor = cs.outline.copy(alpha = 0.35f),
            errorBorderColor = cs.error,
            focusedLabelColor = muted,
            unfocusedLabelColor = muted,
            errorLabelColor = cs.error,
            focusedPlaceholderColor = muted.copy(alpha = 0.65f),
            unfocusedPlaceholderColor = muted.copy(alpha = 0.65f),
            errorPlaceholderColor = muted.copy(alpha = 0.75f),
            focusedSupportingTextColor = muted,
            unfocusedSupportingTextColor = muted,
            errorSupportingTextColor = cs.error
        )
    )
}

@Composable
fun AuthGoogleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(AuthPillShape)
            .background(cs.surfaceContainerHigh)
            .border(1.dp, cs.outline.copy(alpha = 0.65f), AuthPillShape)
            .clickable(enabled = enabled, onClick = onClick),
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
                    .background(cs.onSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = cs.surfaceContainerHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Войти через Google",
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
