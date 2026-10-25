package com.solis.tracker.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.R
import com.solis.tracker.core.security.AppLockManager
import com.solis.tracker.navigation.AppSettingsRoute
import com.solis.tracker.navigation.BackupRestoreRoute
import com.solis.tracker.navigation.LegalRoute
import com.solis.tracker.navigation.LocalNavController
import com.solis.tracker.navigation.ProfileRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel(),
    appLockManager: AppLockManager? = null
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_more)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { navController.navigate(ProfileRoute) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.padding(end = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            val name = userSettings?.fullName
                            Text(
                                if (name.isNullOrBlank()) "Set up your profile" else name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val dob = userSettings?.dateOfBirth
                            if (!dob.isNullOrBlank()) {
                                Text(dob, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                SettingsListItem(
                    title = "App Settings",
                    onClick = { navController.navigate(AppSettingsRoute) }
                )
            }
            
            item {
                SettingsListItem(
                    title = "Backup & Restore",
                    onClick = { navController.navigate(BackupRestoreRoute) }
                )
            }

            item {
                SettingsListItem(
                    title = "Privacy Policy",
                    onClick = { navController.navigate(LegalRoute) }
                )
            }

            item {
                SettingsListItem(
                    title = "Terms & Conditions",
                    onClick = { navController.navigate(LegalRoute) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Solis Life Tracker", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v1.0.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
