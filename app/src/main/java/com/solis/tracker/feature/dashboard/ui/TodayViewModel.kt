package com.solis.tracker.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import com.solis.tracker.core.extension.DashboardContributor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    contributorsSet: Set<@JvmSuppressWildcards DashboardContributor>
) : ViewModel() {
    val contributors: List<DashboardContributor> = contributorsSet.sortedBy { it.order }
}
