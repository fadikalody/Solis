package com.solis.tracker.navigation

import androidx.compose.ui.graphics.vector.ImageVector

interface FeatureEntry {
    val route: Any
    val labelRes: Int
    val icon: ImageVector
}
