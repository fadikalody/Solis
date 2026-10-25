package com.solis.tracker.feature.lock.ui

import androidx.lifecycle.ViewModel
import com.solis.tracker.core.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    val pinManager: PinManager
) : ViewModel()
