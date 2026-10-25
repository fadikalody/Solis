package com.solis.tracker.feature.settings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.core.backup.BackupManager
import com.solis.tracker.core.backup.CsvFile
import com.solis.tracker.core.backup.ImportReport
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.datastore.SyncMode
import com.solis.tracker.core.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class Exporting(val message: String) : BackupUiState
    data class Importing(val message: String) : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
    data class DryRunReady(val counts: Map<String, Int>, val uri: Uri, val password: CharArray?) : BackupUiState
}

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val userSettings = settingsRepository.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
    
    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            settingsRepository.setCurrencyCode(code)
        }
    }
    
    fun setMonthlyBudgetMinor(amount: Long) {
        viewModelScope.launch {
            settingsRepository.setMonthlyBudgetMinor(amount)
        }
    }
    
    fun setFullName(name: String) {
        viewModelScope.launch {
            settingsRepository.setFullName(name)
        }
    }

    fun setDateOfBirth(dob: String) {
        viewModelScope.launch {
            settingsRepository.setDateOfBirth(dob)
        }
    }

    fun setSyncMode(mode: SyncMode) {
        viewModelScope.launch {
            settingsRepository.setSyncMode(mode)
        }
    }

    fun exportPlainJson(outputStream: OutputStream) {
        _uiState.value = BackupUiState.Exporting("Exporting JSON...")
        viewModelScope.launch {
            try {
                backupManager.writePlainJson(outputStream)
                _uiState.value = BackupUiState.Success("JSON export successful")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportEncrypted(outputStream: OutputStream, password: CharArray) {
        _uiState.value = BackupUiState.Exporting("Encrypting and exporting...")
        viewModelScope.launch {
            try {
                backupManager.writeEncrypted(outputStream, password)
                _uiState.value = BackupUiState.Success("Encrypted export successful")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportCsv(outputStream: OutputStream) {
        _uiState.value = BackupUiState.Exporting("Exporting CSVs...")
        viewModelScope.launch {
            try {
                val csvs = backupManager.exportCsvs()
                val combined = csvs.joinToString("\n\n") { "--- ${it.filename} ---\n${it.content}" }
                outputStream.use { out ->
                    out.write(combined.toByteArray(Charsets.UTF_8))
                }
                _uiState.value = BackupUiState.Success("CSV export successful")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Export failed")
            }
        }
    }
    
    fun processImportFile(inputStream: InputStream, uri: Uri, password: CharArray?) {
        _uiState.value = BackupUiState.Importing("Analyzing file...")
        viewModelScope.launch {
            try {
                val counts = backupManager.parseDryRun(inputStream, password)
                _uiState.value = BackupUiState.DryRunReady(counts, uri, password)
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Invalid file or wrong password")
            }
        }
    }

    fun executeImport(inputStream: InputStream, password: CharArray?, strategy: ImportStrategy) {
        _uiState.value = BackupUiState.Importing("Importing data...")
        viewModelScope.launch {
            try {
                val report = backupManager.executeImport(inputStream, password, strategy)
                _uiState.value = BackupUiState.Success("Import successful. Added: ${report.added}, Updated: ${report.updated}")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Import failed")
            }
        }
    }
}
