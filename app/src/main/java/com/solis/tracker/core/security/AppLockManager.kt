package com.solis.tracker.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLockState {
    UNLOCKED,
    LOCKED
}

@Singleton
class AppLockManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager,
    @Dispatcher(AppDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

    private val _lockState = MutableStateFlow(AppLockState.UNLOCKED)
    val lockState: StateFlow<AppLockState> = _lockState.asStateFlow()

    val lockMode = settingsRepository.userSettings
    val isSecureScreenEnabled = settingsRepository.userSettings

    var authInProgress = false
    var externalFlowInProgress = false

    private var appBackgroundedTime: Long = 0L

    fun initialize(processLifecycle: Lifecycle) {
        processLifecycle.addObserver(this)
        
        CoroutineScope(mainDispatcher).launch {
            pinManager.initializeLockoutState()
            
            val settings = settingsRepository.userSettings.first()
            if (settings.lockMode != LockMode.OFF) {
                _lockState.value = AppLockState.LOCKED
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (appBackgroundedTime == 0L) return
        
        val backgroundDuration = clockProvider() - appBackgroundedTime
        appBackgroundedTime = 0L

        CoroutineScope(mainDispatcher).launch {
            val settings = settingsRepository.userSettings.first()
            val timeoutMs = settings.autoLockTimeoutSeconds * 1000L
            
            if (settings.lockMode != LockMode.OFF && !authInProgress && !externalFlowInProgress) {
                if (backgroundDuration >= timeoutMs) {
                    _lockState.value = AppLockState.LOCKED
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!authInProgress && !externalFlowInProgress) {
            appBackgroundedTime = clockProvider()
        }
    }

    fun unlock() {
        _lockState.value = AppLockState.UNLOCKED
    }
    
    fun lock() {
        _lockState.value = AppLockState.LOCKED
    }
}
