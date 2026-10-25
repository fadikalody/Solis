package com.solis.tracker.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val ENCRYPTED_PIN_DATA = stringPreferencesKey("encrypted_pin_data")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    }

    val encryptedPinData: Flow<String?> = dataStore.data.map { it[Keys.ENCRYPTED_PIN_DATA] }
    val failedAttempts: Flow<Int> = dataStore.data.map { it[Keys.FAILED_ATTEMPTS] ?: 0 }

    suspend fun setEncryptedPinData(data: String?) {
        dataStore.edit {
            if (data == null) it.remove(Keys.ENCRYPTED_PIN_DATA)
            else it[Keys.ENCRYPTED_PIN_DATA] = data
        }
    }

    suspend fun setFailedAttempts(attempts: Int) {
        dataStore.edit { it[Keys.FAILED_ATTEMPTS] = attempts }
    }
}
