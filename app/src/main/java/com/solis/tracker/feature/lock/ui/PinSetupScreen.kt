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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.solis.tracker.core.security.PinManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    pinManager: PinManager,
    viewModel: PinSetupViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var pin by remember { mutableStateOf(CharArray(0)) }
    var confirmPin by remember { mutableStateOf(CharArray(0)) }
    var step by remember { mutableStateOf(1) } // 1 = Enter, 2 = Confirm
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (step == 1) "Enter a 4-digit PIN" else "Confirm your PIN",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            val currentPin = if (step == 1) pin else confirmPin
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until 4) {
                    val filled = i < currentPin.size
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

            if (errorMsg != null) {
                Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                                onClick = {
                                    if (step == 1) {
                                        if (pin.size < 4) {
                                            val newPin = pin.copyOf(pin.size + 1)
                                            newPin[newPin.size - 1] = digit.toString()[0]
                                            pin = newPin
                                            if (pin.size == 4) {
                                                step = 2
                                            }
                                        }
                                    } else {
                                        if (confirmPin.size < 4) {
                                            val newPin = confirmPin.copyOf(confirmPin.size + 1)
                                            newPin[newPin.size - 1] = digit.toString()[0]
                                            confirmPin = newPin
                                            if (confirmPin.size == 4) {
                                                if (pin.contentEquals(confirmPin)) {
                                                    scope.launch {
                                                        pinManager.setPin(pin)
                                                        android.util.Log.d("SolisLock", "PinSetupScreen: PIN saved")
                                                        viewModel.onPinSetupComplete()
                                                        android.util.Log.d("SolisLock", "PinSetupScreen: LockMode set, navigating back")
                                                        onComplete()
                                                    }
                                                } else {
                                                    errorMsg = "PINs do not match"
                                                    confirmPin = CharArray(0)
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
                    Box(modifier = Modifier.size(64.dp))
                    KeypadButton(
                        text = "0",
                        onClick = {
                            val digit = "0"
                            if (step == 1) {
                                if (pin.size < 4) {
                                    val newPin = pin.copyOf(pin.size + 1)
                                    newPin[newPin.size - 1] = digit[0]
                                    pin = newPin
                                    if (pin.size == 4) {
                                        step = 2
                                    }
                                }
                            } else {
                                if (confirmPin.size < 4) {
                                    val newPin = confirmPin.copyOf(confirmPin.size + 1)
                                    newPin[newPin.size - 1] = digit[0]
                                    confirmPin = newPin
                                    if (confirmPin.size == 4) {
                                        if (pin.contentEquals(confirmPin)) {
                                            scope.launch {
                                                pinManager.setPin(pin)
                                                android.util.Log.d("SolisLock", "PinSetupScreen: PIN saved")
                                                viewModel.onPinSetupComplete()
                                                android.util.Log.d("SolisLock", "PinSetupScreen: LockMode set, navigating back")
                                                onComplete()
                                            }
                                        } else {
                                            errorMsg = "PINs do not match"
                                            confirmPin = CharArray(0)
                                        }
                                    }
                                }
                            }
                        }
                    )
                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = {
                            if (step == 1 && pin.isNotEmpty()) {
                                pin = pin.copyOf(pin.size - 1)
                                errorMsg = null
                            } else if (step == 2 && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.copyOf(confirmPin.size - 1)
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

@Composable
private fun KeypadButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
