package com.solis.tracker.feature.health.ui

import app.cash.turbine.test
import com.solis.tracker.feature.health.domain.HealthRepository
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import com.solis.tracker.feature.health.domain.model.ValueKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {
    private lateinit var repository: HealthRepository
    private lateinit var viewModel: HealthViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val metricsFlow = MutableStateFlow<List<MetricType>>(emptyList())
    private val entriesFlow = MutableStateFlow<List<HealthEntry>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()

        whenever(repository.getAllMetricTypes()).thenReturn(metricsFlow)
        whenever(repository.getAllEntries()).thenReturn(entriesFlow)

        viewModel = HealthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logEntry saves successfully when value is valid`() = runTest {
        viewModel.logEntry("metric1", "70.5", "Test note")

        val captor = argumentCaptor<HealthEntry>()
        verify(repository).saveEntry(captor.capture())

        val savedEntry = captor.firstValue
        assertEquals("metric1", savedEntry.metricTypeId)
        assertEquals(70.5, savedEntry.value, 0.001)
        assertEquals("Test note", savedEntry.note)
    }

    @Test
    fun `logEntry ignores invalid value`() = runTest {
        viewModel.logEntry("metric1", "invalid", "Test note")
        
        verify(repository, org.mockito.kotlin.never()).saveEntry(any())
    }

    @Test
    fun `uiState combines metrics and entries successfully`() = runTest {
        val metric = MetricType("m1", "weight", "Weight", "kg", ValueKind.NUMBER, true, null)
        val entry1 = HealthEntry("e1", "m1", 70.0, Instant.now(), "")
        val entry2 = HealthEntry("e2", "m2", 2000.0, Instant.now(), "") // Different metric

        metricsFlow.value = listOf(metric)
        entriesFlow.value = listOf(entry1, entry2)

        viewModel.uiState.test {
            // It might take multiple emissions to settle
            val finalState = expectMostRecentItem()
            
            assertTrue(finalState is HealthUiState.Success)
            val success = finalState as HealthUiState.Success
            
            assertEquals(1, success.metrics.size)
            assertEquals("m1", success.metrics[0].metricType.id)
            
            // Only entry1 should be in recentEntries for m1
            assertEquals(1, success.metrics[0].recentEntries.size)
            assertEquals("e1", success.metrics[0].recentEntries[0].id)
        }
    }
}
