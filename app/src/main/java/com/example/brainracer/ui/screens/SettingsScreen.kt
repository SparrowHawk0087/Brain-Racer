package com.example.brainracer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.brainracer.data.preferences.ProfilePrivacyOption
import com.example.brainracer.data.preferences.UserPreferencesRepository
import com.example.brainracer.ui.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val darkEnabled by prefsRepo.darkTheme.collectAsStateWithLifecycle(initialValue = false)
    val notificationsEnabled by prefsRepo.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val privacy by prefsRepo.privacyOption.collectAsStateWithLifecycle(initialValue = ProfilePrivacyOption.EVERYONE)
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = cs.onBackground
                    )
                }
                Text(
                    "Настройки",
                    color = cs.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(cs.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    SectionTitle("Оформление и уведомления")
                    CardBlock {
                        SettingsSwitchRow(
                            icon = Icons.Default.DarkMode,
                            label = "Тёмная тема",
                            checked = darkEnabled,
                            onCheckedChange = { v ->
                                scope.launch { prefsRepo.setDarkTheme(v) }
                            }
                        )
                        HorizontalDivider(color = cs.outline, thickness = 1.dp)
                        SettingsSwitchRow(
                            icon = Icons.Default.Notifications,
                            label = "Уведомления",
                            checked = notificationsEnabled,
                            onCheckedChange = { v ->
                                scope.launch { prefsRepo.setNotificationsEnabled(v) }
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    SectionTitle("Приватность профиля")
                    CardBlock {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Кто видит ваш профиль",
                                color = cs.onSurface,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ProfilePrivacyOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { prefsRepo.setPrivacyOption(option) }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = privacy == option,
                                    onClick = {
                                        scope.launch { prefsRepo.setPrivacyOption(option) }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = cs.primary,
                                        unselectedColor = cs.onSurfaceVariant
                                    )
                                )
                                Text(
                                    option.labelRu,
                                    color = cs.onSurface,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                        border = BorderStroke(1.dp, cs.error.copy(0.65f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = cs.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Выйти", fontWeight = FontWeight.SemiBold, color = cs.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .border(1.dp, cs.primary.copy(0.25f), RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = cs.onSurface, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = cs.primary,
                uncheckedThumbColor = cs.onSurfaceVariant,
                uncheckedTrackColor = cs.outline
            )
        )
    }
}
