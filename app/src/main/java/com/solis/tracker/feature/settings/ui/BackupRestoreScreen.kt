package com.solis.tracker.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.core.security.AppLockManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    appLockManager: AppLockManager?,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is BackupUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as BackupUiState.Success).message)
                viewModel.resetState()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as BackupUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(true) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        appLockManager?.externalFlowInProgress = false
        uri?.let { 
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportPlainJson(stream)
            }
        }
    }
    
    val exportEncryptedLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        appLockManager?.externalFlowInProgress = false
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportEncrypted(stream, passwordInput.toCharArray())
            }
        }
    }
    
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        appLockManager?.externalFlowInProgress = false
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                viewModel.exportCsv(stream)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        appLockManager?.externalFlowInProgress = false
        uri?.let {
            val name = it.lastPathSegment ?: ""
            if (name.endsWith(".ltbk")) {
                pendingImportUri = it
                isExporting = false
                showPasswordDialog = true
            } else {
                context.contentResolver.openInputStream(it)?.let { stream ->
                    viewModel.processImportFile(stream, it, null)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Keep your data safe by exporting it to a file. Encrypted backups (.ltbk) require a password to restore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = { 
                        isExporting = true
                        showPasswordDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Encrypted Backup (.ltbk)")
                }
                
                Button(
                    onClick = { 
                        appLockManager?.externalFlowInProgress = true
                        exportJsonLauncher.launch("solis_backup_${System.currentTimeMillis()}.json") 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Plain JSON (.json)")
                }
                
                Button(
                    onClick = { 
                        appLockManager?.externalFlowInProgress = true
                        exportCsvLauncher.launch("solis_export_${System.currentTimeMillis()}.csv") 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export CSV data (.csv)")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        appLockManager?.externalFlowInProgress = true
                        importLauncher.launch(arrayOf("*/*")) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Backup")
                }
                
                if (uiState is BackupUiState.Importing || uiState is BackupUiState.Exporting) {
                    Text("Processing...", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text(if (isExporting) "Set Backup Password" else "Enter Backup Password") },
                text = {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("Password") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPasswordDialog = false
                        if (isExporting) {
                            appLockManager?.externalFlowInProgress = true
                            exportEncryptedLauncher.launch("solis_backup_${System.currentTimeMillis()}.ltbk")
                        } else {
                            pendingImportUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)?.let { stream ->
                                    viewModel.processImportFile(stream, uri, passwordInput.toCharArray())
                                }
                            }
                        }
                        passwordInput = ""
                    }) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showPasswordDialog = false 
                        passwordInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (uiState is BackupUiState.DryRunReady) {
            val state = uiState as BackupUiState.DryRunReady
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                title = { Text("Confirm Import") },
                text = {
                    Column {
                        Text("File contains:")
                        state.counts.forEach { (module, count) ->
                            Text("- $module: $count items")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            context.contentResolver.openInputStream(state.uri)?.let { stream ->
                                viewModel.executeImport(stream, state.password, ImportStrategy.MERGE)
                            }
                        }
                    }) {
                        Text("Merge")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch {
                            context.contentResolver.openInputStream(state.uri)?.let { stream ->
                                viewModel.executeImport(stream, state.password, ImportStrategy.REPLACE)
                            }
                        }
                    }) {
                        Text("Replace All")
                    }
                }
            )
        }
    }
}
