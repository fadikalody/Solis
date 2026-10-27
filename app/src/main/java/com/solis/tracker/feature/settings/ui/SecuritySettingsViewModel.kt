package com.solis.tracker.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.datastore.UserSettings
import com.solis.tracker.core.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager
) : ViewModel() {

    val userSettings: StateFlow<UserSettings?> = settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setLockMode(mode: LockMode) {
        viewModelScope.launch {
            if (mode == LockMode.OFF) {
                pinManager.removePin()
            }
            settingsRepository.setLockMode(mode)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

}
