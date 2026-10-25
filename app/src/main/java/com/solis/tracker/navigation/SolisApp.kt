package com.solis.tracker.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solis.tracker.core.security.AppLockManager
import com.solis.tracker.feature.dashboard.ui.TodayScreen
import com.solis.tracker.feature.expenses.ui.ExpensesScreen
import com.solis.tracker.feature.health.ui.HealthScreen
import com.solis.tracker.feature.lock.ui.PinSetupScreen
import com.solis.tracker.feature.lock.ui.PinSetupViewModel
import com.solis.tracker.feature.settings.ui.AppSettingsScreen
import com.solis.tracker.feature.settings.ui.BackupRestoreScreen
import com.solis.tracker.feature.settings.ui.LegalScreen
import com.solis.tracker.feature.settings.ui.MoreScreen
import com.solis.tracker.feature.settings.ui.ProfileScreen
import com.solis.tracker.feature.tasks.ui.TasksScreen

fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SolisApp(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        NavigationSuiteScaffold(
            modifier = modifier,
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hasRoute(destination.route::class) == true
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = TodayRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<TodayRoute> {
                    TodayScreen()
                }
                composable<TasksRoute> {
                    TasksScreen()
                }
                composable<ExpensesRoute> {
                    ExpensesScreen()
                }
                composable<HealthRoute> {
                    HealthScreen()
                }
                composable<MoreRoute> {
                    MoreScreen(appLockManager = appLockManager)
                }
                composable<PinSetupRoute> {
                    val pinViewModel: PinSetupViewModel = hiltViewModel()
                    PinSetupScreen(
                        pinManager = pinViewModel.pinManager,
                        onBack = { navController.popBackStack() },
                        onComplete = { navController.popBackStack() }
                    )
                }
                composable<ProfileRoute> {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable<AppSettingsRoute> {
                    AppSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable<BackupRestoreRoute> {
                    BackupRestoreScreen(
                        appLockManager = appLockManager,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<LegalRoute> {
                    LegalScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
