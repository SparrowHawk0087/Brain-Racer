package com.example.brainracer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainracer.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch

/**
 * Данный ViewModel: переключает тему через [UserPreferencesRepository] в собственном scope,
 * чтобы клик по `ToggleThemeButton` не зависел от текущего экрана и не отменялся при
 * быстром recomposition.
 */
class ThemeViewModel : ViewModel() {

    fun toggleDarkTheme(prefsRepo: UserPreferencesRepository, currentDark: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkTheme(!currentDark)
        }
    }
}
