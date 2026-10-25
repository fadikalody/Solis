package com.solis.tracker.core.security

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@VisibleForTesting
var clockProvider: () -> Long = { SystemClock.elapsedRealtime() }

@Singleton
class PinManager @Inject constructor(
    private val keystoreCipher: KeystoreCipher,
    private val securityRepository: SecurityRepository,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) {
    private var lockoutEndTime = 0L

    suspend fun initializeLockoutState() {
        val attempts = securityRepository.failedAttempts.first()
        if (attempts >= 5) {
            applyLockout(attempts)
        }
    }

    suspend fun setPin(pin: CharArray) = withContext(ioDispatcher) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val iterations = 150000
        val hash = hashPin(pin, salt, iterations)
        
        val buffer = ByteBuffer.allocate(4 + 16 + hash.size)
        buffer.putInt(iterations)
        buffer.put(salt)
        buffer.put(hash)
        
        val encrypted = keystoreCipher.encrypt(buffer.array())
        securityRepository.setEncryptedPinData(Base64.getEncoder().encodeToString(encrypted))
        securityRepository.setFailedAttempts(0)
        clearLockoutInRam()
        pin.fill('0')
    }

    suspend fun verifyPin(pin: CharArray): Boolean = withContext(ioDispatcher) {
        if (isLockedOut()) {
            pin.fill('0')
            return@withContext false
        }

        val encryptedString = securityRepository.encryptedPinData.first()
        if (encryptedString == null) {
            pin.fill('0')
            return@withContext false
        }
        
        val encrypted = Base64.getDecoder().decode(encryptedString)
        
        val decrypted = try {
            keystoreCipher.decrypt(encrypted)
        } catch (e: Exception) {
            pin.fill('0')
            return@withContext false
        }
        
        val buffer = ByteBuffer.wrap(decrypted)
        val iterations = buffer.int
        val salt = ByteArray(16)
        buffer.get(salt)
        val storedHash = ByteArray(buffer.remaining())
        buffer.get(storedHash)
        
        val computedHash = hashPin(pin, salt, iterations)
        val isValid = MessageDigest.isEqual(storedHash, computedHash)
        
        if (isValid) {
            securityRepository.setFailedAttempts(0)
            clearLockoutInRam()
        } else {
            val attempts = securityRepository.failedAttempts.first() + 1
            securityRepository.setFailedAttempts(attempts)
            if (attempts >= 5) {
                applyLockout(attempts)
            }
        }
        
        pin.fill('0')
        return@withContext isValid
    }

    suspend fun removePin() = withContext(ioDispatcher) {
        securityRepository.setEncryptedPinData(null)
        securityRepository.setFailedAttempts(0)
        clearLockoutInRam()
    }

    suspend fun hasPin(): Boolean = withContext(ioDispatcher) {
        securityRepository.encryptedPinData.first() != null
    }

    private fun hashPin(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin, salt, iterations, 256)
        val secret = factory.generateSecret(spec)
        val encoded = secret.encoded
        spec.clearPassword()
        return encoded
    }
    
    private fun applyLockout(attempts: Int) {
        val durationMs = when {
            attempts < 5 -> 0L
            attempts == 5 -> 30_000L
            attempts == 6 -> 60_000L
            attempts == 7 -> 300_000L
            else -> 900_000L // 15 mins
        }
        lockoutEndTime = clockProvider() + durationMs
    }

    private fun clearLockoutInRam() {
        lockoutEndTime = 0L
    }

    fun isLockedOut(): Boolean {
        return clockProvider() < lockoutEndTime
    }

    fun getRemainingLockoutSeconds(): Int {
        val remaining = lockoutEndTime - clockProvider()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
}
