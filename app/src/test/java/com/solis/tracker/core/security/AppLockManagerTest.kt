package com.solis.tracker.core.security

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.solis.tracker.core.datastore.LockMode
import com.solis.tracker.core.datastore.SettingsRepository
import com.solis.tracker.core.datastore.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var pinManager: PinManager
    private lateinit var appLockManager: AppLockManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val userSettingsFlow = MutableStateFlow(UserSettings(lockMode = LockMode.OFF, autoLockTimeoutSeconds = 0))

    private var testTime = 100L

    @Before
    fun setup() {
        settingsRepository = mock()
        pinManager = mock()
        whenever(settingsRepository.userSettings).thenReturn(userSettingsFlow)

        clockProvider = { testTime }

        appLockManager = AppLockManager(settingsRepository, pinManager, testDispatcher)
    }

    @After
    fun tearDown() {
        clockProvider = { android.os.SystemClock.elapsedRealtime() }
    }

    @Test
    fun `initialize sets state to LOCKED if lock mode is not OFF`() = runTest(testDispatcher) {
        userSettingsFlow.value = UserSettings(lockMode = LockMode.PIN)
        appLockManager.initialize(mock())
        
        assertEquals(AppLockState.LOCKED, appLockManager.lockState.value)
    }

    @Test
    fun `initialize sets state to UNLOCKED if lock mode is OFF`() = runTest(testDispatcher) {
        userSettingsFlow.value = UserSettings(lockMode = LockMode.OFF)
        appLockManager.initialize(mock())
        
        assertEquals(AppLockState.UNLOCKED, appLockManager.lockState.value)
    }

    @Test
    fun `unlock changes state to UNLOCKED`() = runTest(testDispatcher) {
        userSettingsFlow.value = UserSettings(lockMode = LockMode.PIN)
        appLockManager.initialize(mock())
        
        appLockManager.unlock()
        assertEquals(AppLockState.UNLOCKED, appLockManager.lockState.value)
    }

    @Test
    fun `lock changes state to LOCKED`() = runTest(testDispatcher) {
        appLockManager.lock()
        assertEquals(AppLockState.LOCKED, appLockManager.lockState.value)
    }

    @Test
    fun `onStart triggers lock if background duration exceeds timeout`() = runTest(testDispatcher) {
        userSettingsFlow.value = UserSettings(lockMode = LockMode.PIN, autoLockTimeoutSeconds = 0)
        appLockManager.initialize(mock())
        appLockManager.unlock()
        
        val lifecycleOwner = mock<LifecycleOwner>()

        testTime = 1000L
        appLockManager.onStop(lifecycleOwner)
        
        testTime = 1000L
        // Simulating immediate return since timeout is 0 seconds
        appLockManager.onStart(lifecycleOwner)
        
        assertEquals(AppLockState.LOCKED, appLockManager.lockState.value)
    }
}
