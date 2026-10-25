package com.solis.tracker.core.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface DashboardContributor {
    val order: Int
    @Composable
    fun Card(modifier: Modifier)
}
