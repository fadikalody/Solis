package com.solis.tracker.feature.lock.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val isSecureScreenEnabled by appLockManager.isSecureScreenEnabled.collectAsStateWithLifecycle(initialValue = null)

    val activity = LocalActivity.current

    LaunchedEffect(lockState, isSecureScreenEnabled) {
        if (activity != null) {
            val shouldBeSecure = lockState == AppLockState.LOCKED || (isSecureScreenEnabled?.isSecureScreenEnabled == true)
            if (shouldBeSecure) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    if (lockState == AppLockState.UNLOCKED || lockMode == null || isSecureScreenEnabled == null) {
        content()
    } else {
        LockScreen(
            appLockManager = appLockManager,
            pinManager = pinManager,
            biometricHelper = biometricHelper,
            activity = activity as FragmentActivity,
            lockMode = lockMode!!.lockMode,
            isBiometricEnabled = isSecureScreenEnabled!!.isBiometricEnabled
        )
    }
}
