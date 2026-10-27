package com.solis.tracker.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.core.datastore.ThemeMode
import com.solis.tracker.navigation.LocalNavController
import com.solis.tracker.navigation.PinSetupRoute
import com.solis.tracker.navigation.navigateToTopLevel
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsListItem(
                    title = "Security & Lock",
                    subtitle = "Manage PIN and Biometrics",
                    onClick = { navController.navigate(com.solis.tracker.navigation.SecuritySettingsRoute) }
                )
            }
            item {
                SettingsListItem(
                    title = "Theme",
                    subtitle = settings?.themeMode?.name ?: "SYSTEM",
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = "Currency",
                    subtitle = settings?.currencyCode ?: "USD",
                    onClick = { showCurrencyDialog = true }
                )
            }
            item {
                SettingsListItem(
                    title = "Monthly Budget",
                    subtitle = settings?.monthlyBudgetMinor?.let { "$${String.format(Locale.US, "%.2f", it / 100f)}" } ?: "$5000.00",
                    onClick = { showBudgetDialog = true }
                )
            }
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select Theme") },
                text = {
                    Column {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings?.themeMode == mode,
                                    onClick = null
                                )
                                Text(mode.name, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCurrencyDialog) {
            val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD")
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Currency") },
                text = {
                    LazyColumn {
                        items(currencies.size) { index ->
                            val code = currencies[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setCurrencyCode(code)
                                        showCurrencyDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings?.currencyCode == code,
                                    onClick = null
                                )
                                Text(code, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showBudgetDialog) {
            var budgetInput by remember { mutableStateOf(settings?.monthlyBudgetMinor?.let { String.format(Locale.US, "%.2f", it / 100f) } ?: "") }
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Set Monthly Budget") },
                text = {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text("Amount") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val amountMinor = try {
                            BigDecimal(budgetInput).multiply(BigDecimal(100)).toLong()
                        } catch (e: Exception) {
                            0L
                        }
                        if (amountMinor > 0) {
                            viewModel.setMonthlyBudgetMinor(amountMinor)
                        }
                        showBudgetDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
