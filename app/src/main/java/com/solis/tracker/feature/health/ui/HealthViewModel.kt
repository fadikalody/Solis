package com.solis.tracker.feature.health.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.feature.health.domain.HealthRepository
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class MetricWithRecentEntries(
    val metricType: MetricType,
    val recentEntries: List<HealthEntry>
)

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data class Success(
        val metrics: List<MetricWithRecentEntries>
    ) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _showLogSheet = MutableStateFlow(false)
    val showLogSheet = _showLogSheet.asStateFlow()

    private val _selectedMetric = MutableStateFlow<MetricType?>(null)
    val selectedMetric = _selectedMetric.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedBuiltInMetricsIfNeeded()
            
            combine(
                repository.getAllMetricTypes(),
                repository.getAllEntries()
            ) { metrics, entries ->
                val groupedEntries = entries.groupBy { it.metricTypeId }
                val combined = metrics.map { metric ->
                    MetricWithRecentEntries(
                        metricType = metric,
                        recentEntries = groupedEntries[metric.id]?.take(5) ?: emptyList() // keep recent 5 for overview
                    )
                }
                HealthUiState.Success(combined)
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun openLogSheet(metric: MetricType) {
        _selectedMetric.value = metric
        _showLogSheet.value = true
    }

    fun closeLogSheet() {
        _showLogSheet.value = false
        _selectedMetric.value = null
    }

    fun logEntry(metricId: String, valueStr: String, note: String) {
        viewModelScope.launch {
            val value = valueStr.toDoubleOrNull()
            if (value != null) {
                val entry = HealthEntry(
                    id = UUID.randomUUID().toString(),
                    metricTypeId = metricId,
                    value = value,
                    recordedAt = Instant.now(),
                    note = note
                )
                repository.saveEntry(entry)
                closeLogSheet()
            }
        }
    }
}
