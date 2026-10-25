package com.solis.tracker.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.solis.tracker.R

enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    TODAY(TodayRoute, R.string.nav_today, Icons.Default.DateRange),
    TASKS(TasksRoute, R.string.nav_tasks, Icons.Default.CheckCircle),
    EXPENSES(ExpensesRoute, R.string.nav_expenses, Icons.Default.ShoppingCart),
    HEALTH(HealthRoute, R.string.nav_health, Icons.Default.Favorite),
    MORE(MoreRoute, R.string.nav_more, Icons.Default.Settings)
}
