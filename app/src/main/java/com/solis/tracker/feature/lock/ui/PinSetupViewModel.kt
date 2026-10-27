package com.solis.tracker.feature.lock.ui

import androidx.lifecycle.ViewModel

import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    val pinManager: PinManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    suspend fun onPinSetupComplete() {
        android.util.Log.d("SolisLock", "PinSetupViewModel: Setting LockMode to PIN")
        settingsRepository.setLockMode(LockMode.PIN)
    }
}
