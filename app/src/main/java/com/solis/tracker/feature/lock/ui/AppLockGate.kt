package com.solis.tracker.feature.lock.ui


import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.core.security.AppLockManager
import com.solis.tracker.core.security.AppLockState
import com.solis.tracker.core.security.BiometricHelper
import com.solis.tracker.core.security.PinManager

@Composable
fun AppLockGate(
    appLockManager: AppLockManager,
    pinManager: PinManager,
    biometricHelper: BiometricHelper,
    content: @Composable () -> Unit
) {
    val lockState by appLockManager.lockState.collectAsStateWithLifecycle()
    val lockMode by appLockManager.lockMode.collectAsStateWithLifecycle(initialValue = null)

    val activity = LocalActivity.current

    // Don't wait for lockMode to load if we are already explicitly UNLOCKED
    if (lockState == AppLockState.UNLOCKED) {
        content()
        return
    }

    // If we are LOCKED, wait for the actual lockMode before showing LockScreen
    if (lockMode == null) {
        return
    }

    LockScreen(
        appLockManager = appLockManager,
        pinManager = pinManager,
        biometricHelper = biometricHelper,
        activity = activity as FragmentActivity,
        lockMode = lockMode!!.lockMode,
        isBiometricEnabled = lockMode!!.isBiometricEnabled
    )
}
