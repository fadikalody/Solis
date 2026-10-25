package com.solis.tracker.feature.lock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.security.AppLockManager
import com.solis.tracker.core.security.BiometricHelper
import com.solis.tracker.core.security.PinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    appLockManager: AppLockManager,
    pinManager: PinManager,
    biometricHelper: BiometricHelper,
    activity: FragmentActivity,
    lockMode: LockMode,
    isBiometricEnabled: Boolean
) {
    var pin by remember { mutableStateOf(CharArray(0)) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val isLockedOut = remember { mutableStateOf(pinManager.isLockedOut()) }
    var remainingSeconds by remember { mutableStateOf(pinManager.getRemainingLockoutSeconds()) }

    LaunchedEffect(remainingSeconds, isLockedOut.value) {
        if (isLockedOut.value && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = pinManager.getRemainingLockoutSeconds()
            if (remainingSeconds <= 0) {
                isLockedOut.value = false
                errorMsg = null
            }
        }
    }

    val triggerSystemLock = {
        appLockManager.authInProgress = true
        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Unlock Solis",
            allowDeviceCredentialFallback = true,
            onSuccess = {
                appLockManager.authInProgress = false
                appLockManager.unlock()
            },
            onError = { _, _ ->
                appLockManager.authInProgress = false
            }
        )
    }
    
    val triggerBiometricForPin = {
        appLockManager.authInProgress = true
        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Unlock Solis",
            allowDeviceCredentialFallback = false,
            onSuccess = {
                appLockManager.authInProgress = false
                appLockManager.unlock()
            },
            onError = { _, _ ->
                appLockManager.authInProgress = false
            }
        )
    }

    LaunchedEffect(lockMode, isBiometricEnabled) {
        if (lockMode == LockMode.SYSTEM) {
            triggerSystemLock()
        } else if (lockMode == LockMode.PIN && isBiometricEnabled && biometricHelper.canAuthenticateWithBiometrics()) {
            triggerBiometricForPin()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (lockMode == LockMode.SYSTEM) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Button(onClick = triggerSystemLock) {
                    Text("Unlock")
                }
            }
        } else if (lockMode == LockMode.PIN) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Enter PIN",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(32.dp))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (i in 0 until 4) { // Assuming 4-digit PIN for simplicity
                        val filled = i < pin.size
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLockedOut.value) {
                    Text(
                        text = "Too many attempts. Try again in $remainingSeconds seconds.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp)) // Maintain layout height
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Keypad
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 1..3) {
                                val digit = row * 3 + col
                                KeypadButton(
                                    text = digit.toString(),
                                    enabled = !isLockedOut.value,
                                    onClick = {
                                        if (pin.size < 4) {
                                            val newPin = pin.copyOf(pin.size + 1)
                                            newPin[newPin.size - 1] = digit.toString()[0]
                                            pin = newPin
                                            if (pin.size == 4) {
                                                scope.launch {
                                                    val success = pinManager.verifyPin(pin)
                                                    if (success) {
                                                        appLockManager.unlock()
                                                    } else {
                                                        pin = CharArray(0)
                                                        isLockedOut.value = pinManager.isLockedOut()
                                                        if (isLockedOut.value) {
                                                            remainingSeconds = pinManager.getRemainingLockoutSeconds()
                                                        } else {
                                                            errorMsg = "Incorrect PIN"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            if (isBiometricEnabled && biometricHelper.canAuthenticateWithBiometrics()) {
                                TextButton(onClick = triggerBiometricForPin) {
                                    Text("Bio")
                                }
                            }
                        }
                        KeypadButton(
                            text = "0",
                            enabled = !isLockedOut.value,
                            onClick = {
                                if (pin.size < 4) {
                                    val newPin = pin.copyOf(pin.size + 1)
                                    newPin[newPin.size - 1] = '0'
                                    pin = newPin
                                    if (pin.size == 4) {
                                        scope.launch {
                                            val success = pinManager.verifyPin(pin)
                                            if (success) {
                                                appLockManager.unlock()
                                            } else {
                                                pin = CharArray(0)
                                                isLockedOut.value = pinManager.isLockedOut()
                                                if (isLockedOut.value) {
                                                    remainingSeconds = pinManager.getRemainingLockoutSeconds()
                                                } else {
                                                    errorMsg = "Incorrect PIN"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = {
                                if (pin.isNotEmpty()) {
                                    pin = pin.copyOf(pin.size - 1)
                                    errorMsg = null
                                }
                            }) {
                                Text("Del")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}
