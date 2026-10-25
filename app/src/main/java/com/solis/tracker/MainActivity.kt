package com.solis.tracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.solis.tracker.core.designsystem.theme.SolisTheme
import com.solis.tracker.core.security.AppLockManager
import com.solis.tracker.core.security.BiometricHelper
import com.solis.tracker.core.security.PinManager
import com.solis.tracker.feature.lock.ui.AppLockGate
import com.solis.tracker.navigation.SolisApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var pinManager: PinManager
    @Inject lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        appLockManager.initialize(ProcessLifecycleOwner.get().lifecycle)
        
        enableEdgeToEdge()
        setContent {
            SolisTheme {
                AppLockGate(appLockManager, pinManager, biometricHelper) {
                    SolisApp(appLockManager = appLockManager)
                }
            }
        }
    }
}
