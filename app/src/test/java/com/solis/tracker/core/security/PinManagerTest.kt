package com.solis.tracker.core.security

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PinManagerTest {

    private lateinit var keystoreCipher: KeystoreCipher
    private lateinit var securityRepository: SecurityRepository
    private lateinit var pinManager: PinManager

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val encryptedDataFlow = MutableStateFlow<String?>(null)
    private val failedAttemptsFlow = MutableStateFlow(0)
    
    private var testTime = 0L

    @Before
    fun setup() {
        keystoreCipher = mock()
        securityRepository = mock()

        clockProvider = { testTime }

        // Fake keystore cipher behavior: just reverse the bytes as a mock encryption
        whenever(keystoreCipher.encrypt(any())).thenAnswer { invocation ->
            val data = invocation.arguments[0] as ByteArray
            data.reversedArray()
        }
        whenever(keystoreCipher.decrypt(any())).thenAnswer { invocation ->
            val data = invocation.arguments[0] as ByteArray
            data.reversedArray()
        }

        whenever(securityRepository.encryptedPinData).thenReturn(encryptedDataFlow)
        whenever(securityRepository.failedAttempts).thenReturn(failedAttemptsFlow)

        pinManager = PinManager(keystoreCipher, securityRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        clockProvider = { android.os.SystemClock.elapsedRealtime() }
    }

    @Test
    fun `setPin saves encrypted data and clears failed attempts`() = runTest(testDispatcher) {
        whenever(securityRepository.setEncryptedPinData(any())).thenAnswer {
            encryptedDataFlow.value = it.arguments[0] as String?
            null
        }
        whenever(securityRepository.setFailedAttempts(any())).thenAnswer {
            failedAttemptsFlow.value = it.arguments[0] as Int
            null
        }

        val pin = charArrayOf('1', '2', '3', '4')
        pinManager.setPin(pin)

        assertTrue(encryptedDataFlow.value != null)
        assertEquals(0, failedAttemptsFlow.value)
    }

    @Test
    fun `verifyPin with correct pin returns true and clears failed attempts`() = runTest(testDispatcher) {
        whenever(securityRepository.setEncryptedPinData(any())).thenAnswer {
            encryptedDataFlow.value = it.arguments[0] as String?
            null
        }
        whenever(securityRepository.setFailedAttempts(any())).thenAnswer {
            failedAttemptsFlow.value = it.arguments[0] as Int
            null
        }

        val pin = charArrayOf('1', '2', '3', '4')
        pinManager.setPin(pin)
        
        // Simulate a prior failure
        failedAttemptsFlow.value = 2

        val result = pinManager.verifyPin(charArrayOf('1', '2', '3', '4'))
        assertTrue(result)
        assertEquals(0, failedAttemptsFlow.value)
    }

    @Test
    fun `verifyPin with incorrect pin returns false and increments failed attempts`() = runTest(testDispatcher) {
        whenever(securityRepository.setEncryptedPinData(any())).thenAnswer {
            encryptedDataFlow.value = it.arguments[0] as String?
            null
        }
        whenever(securityRepository.setFailedAttempts(any())).thenAnswer {
            failedAttemptsFlow.value = it.arguments[0] as Int
            null
        }

        val pin = charArrayOf('1', '2', '3', '4')
        pinManager.setPin(pin)

        val result = pinManager.verifyPin(charArrayOf('0', '0', '0', '0'))
        assertFalse(result)
        assertEquals(1, failedAttemptsFlow.value)
    }

    @Test
    fun `5 failed attempts triggers lockout`() = runTest(testDispatcher) {
        whenever(securityRepository.setEncryptedPinData(any())).thenAnswer {
            encryptedDataFlow.value = it.arguments[0] as String?
            null
        }
        whenever(securityRepository.setFailedAttempts(any())).thenAnswer {
            failedAttemptsFlow.value = it.arguments[0] as Int
            null
        }

        val pin = charArrayOf('1', '2', '3', '4')
        pinManager.setPin(pin)

        for (i in 1..5) {
            pinManager.verifyPin(charArrayOf('0', '0', '0', '0'))
        }

        assertTrue(pinManager.isLockedOut())
        assertTrue(pinManager.getRemainingLockoutSeconds() > 0)
        assertEquals(5, failedAttemptsFlow.value)
        
        // Even with correct PIN, verification should fail while locked out
        val result = pinManager.verifyPin(charArrayOf('1', '2', '3', '4'))
        assertFalse(result)
    }
}
