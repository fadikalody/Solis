package com.solis.tracker.core.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCryptoException(message: String) : Exception(message)

object BackupCrypto {
    private val MAGIC_BYTES = "LTBK".toByteArray(Charsets.UTF_8)
    private const val FORMAT_VERSION = 1
    private const val ITERATIONS = 210000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKey(password, salt, ITERATIONS)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(data)

        val buffer = ByteBuffer.allocate(MAGIC_BYTES.size + 4 + 4 + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        buffer.put(MAGIC_BYTES)
        buffer.putInt(FORMAT_VERSION)
        buffer.putInt(ITERATIONS)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(ciphertext)

        return buffer.array()
    }

    fun decrypt(encryptedData: ByteArray, password: CharArray): ByteArray {
        try {
            val buffer = ByteBuffer.wrap(encryptedData)
            
            val magic = ByteArray(MAGIC_BYTES.size)
            buffer.get(magic)
            if (!magic.contentEquals(MAGIC_BYTES)) {
                throw BackupCryptoException("Invalid or corrupted file format")
            }

            val version = buffer.int
            if (version != FORMAT_VERSION) {
                throw BackupCryptoException("Unsupported backup version: $version")
            }

            val iterations = buffer.int
            val salt = ByteArray(SALT_LENGTH)
            buffer.get(salt)
            
            val iv = ByteArray(IV_LENGTH)
            buffer.get(iv)
            
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val secretKey = deriveKey(password, salt, iterations)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return cipher.doFinal(ciphertext)
        } catch (e: BackupCryptoException) {
            throw e
        } catch (e: Exception) {
            throw BackupCryptoException("Wrong password or corrupted file")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keySpec = PBEKeySpec(password, salt, iterations, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        keySpec.clearPassword()
        return secretKey
    }
}
