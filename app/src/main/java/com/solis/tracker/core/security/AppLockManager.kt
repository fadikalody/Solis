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
    @Dispatcher(AppDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

    private val _lockState = MutableStateFlow(AppLockState.LOCKED)
    val lockState: StateFlow<AppLockState> = _lockState.asStateFlow()

    val lockMode = settingsRepository.userSettings
    val isSecureScreenEnabled = settingsRepository.userSettings

    var authInProgress = false
    var externalFlowInProgress = false

    private var appBackgroundedTime: Long = 0L

    suspend fun initializeLockState() {
        android.util.Log.d("SolisLock", "AppLockManager: initializeLockState() called")
        kotlinx.coroutines.withContext(ioDispatcher) {
            pinManager.initializeLockoutState()
            val settings = settingsRepository.userSettings.first()
            if (settings.lockMode == LockMode.OFF) {
                android.util.Log.d("SolisLock", "AppLockManager: LockMode is OFF, unlocking app")
                _lockState.value = AppLockState.UNLOCKED
            } else {
                android.util.Log.d("SolisLock", "AppLockManager: LockMode is ${settings.lockMode}, staying locked")
                _lockState.value = AppLockState.LOCKED
            }
        }
    }

    fun initialize(processLifecycle: Lifecycle) {
        processLifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (appBackgroundedTime == 0L) return
        
        val backgroundDuration = clockProvider() - appBackgroundedTime
        appBackgroundedTime = 0L

        CoroutineScope(mainDispatcher).launch {
            val settings = settingsRepository.userSettings.first()
            val timeoutMs = settings.autoLockTimeoutSeconds * 1000L
            
            android.util.Log.d("SolisLock", "AppLockManager onStart: backgroundDuration=$backgroundDuration ms, timeout=$timeoutMs ms")
            if (settings.lockMode != LockMode.OFF && !authInProgress && !externalFlowInProgress) {
                if (backgroundDuration >= timeoutMs) {
                    android.util.Log.d("SolisLock", "AppLockManager onStart: backgroundDuration exceeded timeout, locking app")
                    _lockState.value = AppLockState.LOCKED
                } else {
                    android.util.Log.d("SolisLock", "AppLockManager onStart: within timeout, staying unlocked")
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!authInProgress && !externalFlowInProgress) {
            appBackgroundedTime = clockProvider()
            android.util.Log.d("SolisLock", "AppLockManager onStop: app backgrounded at $appBackgroundedTime")
        } else {
            android.util.Log.d("SolisLock", "AppLockManager onStop: authInProgress or externalFlowInProgress, skipping background timer")
        }
    }

    fun unlock() {
        _lockState.value = AppLockState.UNLOCKED
    }
    
    fun lock() {
        _lockState.value = AppLockState.LOCKED
    }
}
