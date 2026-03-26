package com.example.brainracer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ProfilePrivacyOption(val labelRu: String) {
    EVERYONE("Профиль виден всем"),
    FRIENDS_ONLY("Только друзьям"),
    MINIMAL("Минимум данных в списках")
}

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.userPrefsDataStore

    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS] ?: true
    }

    val privacyOption: Flow<ProfilePrivacyOption> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_PRIVACY] ?: ProfilePrivacyOption.EVERYONE.name
        ProfilePrivacyOption.entries.find { it.name == raw } ?: ProfilePrivacyOption.EVERYONE
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setPrivacyOption(option: ProfilePrivacyOption) {
        dataStore.edit { it[KEY_PRIVACY] = option.name }
    }

    private companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_PRIVACY = stringPreferencesKey("privacy_level")
    }
}
