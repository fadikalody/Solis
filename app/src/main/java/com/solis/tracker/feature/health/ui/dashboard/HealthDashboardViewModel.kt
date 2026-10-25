package com.solis.tracker.feature.health.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solis.tracker.feature.health.domain.HealthRepository
import com.solis.tracker.feature.health.domain.model.HealthEntry
import com.solis.tracker.feature.health.domain.model.MetricType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class TodayHealthMetric(
    val metric: MetricType,
    val latestEntry: HealthEntry?
)

data class HealthDashboardState(
    val metrics: List<TodayHealthMetric> = emptyList()
)

@HiltViewModel
class HealthDashboardViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val now = Instant.now()
    private val startOfDay = ZonedDateTime.ofInstant(now, ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS).toInstant()

    val state: StateFlow<HealthDashboardState> = combine(
        repository.getAllMetricTypes(),
        repository.getAllEntries()
    ) { metrics, entries ->
        // Filter entries for today
        val todayEntries = entries.filter { it.recordedAt.isAfter(startOfDay) || it.recordedAt == startOfDay }
        val grouped = todayEntries.groupBy { it.metricTypeId }
        
        val dashboardMetrics = metrics.map { metric ->
            TodayHealthMetric(
                metric = metric,
                latestEntry = grouped[metric.id]?.maxByOrNull { it.recordedAt }
            )
        }.filter { it.metric.isBuiltIn || it.latestEntry != null }
        
        HealthDashboardState(metrics = dashboardMetrics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HealthDashboardState()
    )
}
