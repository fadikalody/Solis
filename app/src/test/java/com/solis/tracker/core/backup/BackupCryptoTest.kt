package com.solis.tracker.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `encrypt and decrypt round trip works`() {
        val originalData = "test_payload_123".toByteArray()
        val password = "secure_password".toCharArray()

        val encrypted = BackupCrypto.encrypt(originalData, password)
        val decrypted = BackupCrypto.decrypt(encrypted, password)

        assertArrayEquals(originalData, decrypted)
    }

    @Test(expected = BackupCryptoException::class)
    fun `decrypt fails with wrong password`() {
        val originalData = "test_payload".toByteArray()
        val password = "secure_password".toCharArray()
        val wrongPassword = "wrong_password".toCharArray()

        val encrypted = BackupCrypto.encrypt(originalData, password)
        
        // This should throw BackupCryptoException
        BackupCrypto.decrypt(encrypted, wrongPassword)
    }

    @Test(expected = BackupCryptoException::class)
    fun `decrypt fails with corrupted payload`() {
        val originalData = "test_payload".toByteArray()
        val password = "secure_password".toCharArray()

        val encrypted = BackupCrypto.encrypt(originalData, password)
        
        // Corrupt the ciphertext
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()
        
        BackupCrypto.decrypt(encrypted, password)
    }

    @Test(expected = BackupCryptoException::class)
    fun `decrypt fails with missing magic bytes`() {
        val originalData = "test_payload".toByteArray()
        val password = "secure_password".toCharArray()

        val encrypted = BackupCrypto.encrypt(originalData, password)
        
        // Corrupt magic bytes
        encrypted[0] = 0x00
        
        BackupCrypto.decrypt(encrypted, password)
    }
}
