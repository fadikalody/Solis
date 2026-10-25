package com.solis.tracker.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class LockMode {
    OFF, SYSTEM, PIN
}

enum class SyncMode {
    OFFLINE, ONLINE
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val currencyCode: String = "USD",
    val firstDayOfWeek: Int = 1,
    val lockMode: LockMode = LockMode.OFF,
    val autoLockTimeoutSeconds: Int = 0,
    val isSecureScreenEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isHealthSeeded: Boolean = false,
    val isExpensesSeeded: Boolean = false,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val syncMode: SyncMode = SyncMode.OFFLINE,
    val monthlyBudgetMinor: Long = 500000L
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val LOCK_MODE = stringPreferencesKey("lock_mode")
        val AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout")
        val SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HEALTH_SEEDED = booleanPreferencesKey("health_seeded")
        val EXPENSES_SEEDED = booleanPreferencesKey("expenses_seeded")
        val FULL_NAME = stringPreferencesKey("full_name")
        val DATE_OF_BIRTH = stringPreferencesKey("date_of_birth")
        val SYNC_MODE = stringPreferencesKey("sync_mode")
        val MONTHLY_BUDGET_MINOR = longPreferencesKey("monthly_budget_minor")
    }

    val userSettings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "USD",
            firstDayOfWeek = prefs[Keys.FIRST_DAY_OF_WEEK] ?: 1,
            lockMode = prefs[Keys.LOCK_MODE]?.let { runCatching { LockMode.valueOf(it) }.getOrNull() } ?: LockMode.OFF,
            autoLockTimeoutSeconds = prefs[Keys.AUTO_LOCK_TIMEOUT] ?: 0,
            isSecureScreenEnabled = prefs[Keys.SECURE_SCREEN] ?: false,
            isBiometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            isOnboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            isHealthSeeded = prefs[Keys.HEALTH_SEEDED] ?: false,
            isExpensesSeeded = prefs[Keys.EXPENSES_SEEDED] ?: false,
            fullName = prefs[Keys.FULL_NAME] ?: "",
            dateOfBirth = prefs[Keys.DATE_OF_BIRTH] ?: "",
            syncMode = prefs[Keys.SYNC_MODE]?.let { runCatching { SyncMode.valueOf(it) }.getOrNull() } ?: SyncMode.OFFLINE,
            monthlyBudgetMinor = prefs[Keys.MONTHLY_BUDGET_MINOR] ?: 500000L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setLockMode(mode: LockMode) {
        dataStore.edit { it[Keys.LOCK_MODE] = mode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
    
    suspend fun setHealthSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.HEALTH_SEEDED] = seeded }
    }
    
    suspend fun setExpensesSeeded(seeded: Boolean) {
        dataStore.edit { it[Keys.EXPENSES_SEEDED] = seeded }
    }

    suspend fun setFullName(name: String) {
        dataStore.edit { it[Keys.FULL_NAME] = name }
    }

    suspend fun setDateOfBirth(dob: String) {
        dataStore.edit { it[Keys.DATE_OF_BIRTH] = dob }
    }

    suspend fun setSyncMode(mode: SyncMode) {
        dataStore.edit { it[Keys.SYNC_MODE] = mode.name }
    }

    suspend fun setMonthlyBudgetMinor(amount: Long) {
        dataStore.edit { it[Keys.MONTHLY_BUDGET_MINOR] = amount }
    }
}
