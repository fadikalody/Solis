package com.solis.tracker.feature.settings.backup

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.core.backup.CsvFile
import com.solis.tracker.core.backup.ImportReport
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.datastore.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class SettingsBackupContributor @Inject constructor(
    private val settingsRepository: SettingsRepository
) : BackupContributor {
    override val key: String = "settings"
    override val schemaVersion: Int = 1

    override suspend fun export(): JsonElement {
        val settings = settingsRepository.userSettings.first()
        return buildJsonObject {
            put("themeMode", settings.themeMode.name)
            put("useDynamicColor", settings.useDynamicColor)
            put("currencyCode", settings.currencyCode)
            put("firstDayOfWeek", settings.firstDayOfWeek)
            put("lockMode", settings.lockMode.name)
            put("autoLockTimeoutSeconds", settings.autoLockTimeoutSeconds)
            put("isSecureScreenEnabled", settings.isSecureScreenEnabled)
            put("isBiometricEnabled", settings.isBiometricEnabled)
            put("isOnboardingCompleted", settings.isOnboardingCompleted)
            put("isHealthSeeded", settings.isHealthSeeded)
        }
    }

    override suspend fun import(data: JsonElement, strategy: ImportStrategy): ImportReport {
        val obj = data.jsonObject
        
        obj["themeMode"]?.jsonPrimitive?.content?.let {
            runCatching { ThemeMode.valueOf(it) }.onSuccess { settingsRepository.setThemeMode(it) }
        }
        
        obj["useDynamicColor"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            settingsRepository.setUseDynamicColor(it)
        }
        
        obj["currencyCode"]?.jsonPrimitive?.content?.let {
            settingsRepository.setCurrencyCode(it)
        }
        
        // Skip lockMode, lock states, biometric settings during import for security reasons
        // Wait, if it's REPLACE we might overwrite everything. But lock state depends on Keystore PIN, 
        // which we do not backup. So we must NOT restore lockMode=PIN because the PIN hash won't be there.
        // It's safest to skip lock-related settings or explicitly set them to OFF.
        
        return ImportReport(updated = 1)
    }

    override suspend fun exportCsv(): List<CsvFile> {
        return emptyList()
    }
}
