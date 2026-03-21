package com.example.brainracer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainracer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }


    val backgroundColor = backgroundLight
    val textColor = onBackgroundLight
    val surfaceColor = surfaceLight
    val surfaceVariantColor = surfaceVariantLight
    val outlineColor = outlineLight
    val primaryColor = primaryLight
    val onPrimaryColor = onPrimaryLight

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                SettingsSectionTitle("Аккаунт", textColor)
            }
            item {
                SettingsItem(Icons.Default.Person, "Личные данные", surfaceColor, textColor, outlineColor, primaryColor)
                SettingsItem(Icons.Default.Lock, "Безопасность", surfaceColor, textColor, outlineColor, primaryColor)
                SettingsItem(Icons.Default.Payment, "Подписка", surfaceColor, textColor, outlineColor, primaryColor)
            }


            item {
                SettingsSectionTitle("Уведомления", textColor)
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    label = "Push-уведомления",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    primaryColor = primaryColor
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Email,
                    label = "Email рассылка",
                    checked = true,
                    onCheckedChange = {},
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    primaryColor = primaryColor
                )
            }

            // Раздел: Приложение
            item {
                SettingsSectionTitle("Приложение", textColor)
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Palette,
                    label = "Темная тема",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it },
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    primaryColor = primaryColor
                )
                SettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    label = "Звуковые эффекты",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    primaryColor = primaryColor
                )
                SettingsItem(Icons.Default.Language, "Язык", surfaceColor, textColor, outlineColor, primaryColor)
                SettingsItem(Icons.Default.Info, "О приложении", surfaceColor, textColor, outlineColor, primaryColor)
            }


            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Выход */ },
                    shape = RoundedCornerShape(12.dp),
                    color = errorContainerLight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = errorLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Выйти",
                            color = errorLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, textColor: Color) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = onSurfaceVariantLight,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    surfaceColor: Color,
    textColor: Color,
    outlineColor: Color,
    primaryColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = outlineColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColor: Color,
    textColor: Color,
    primaryColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = onPrimaryLight,
                    checkedTrackColor = primaryLight,
                    uncheckedThumbColor = surfaceContainerLowestLight,
                    uncheckedTrackColor = outlineLight
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Light")
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenPreviewDark() {
    SettingsScreen()
}