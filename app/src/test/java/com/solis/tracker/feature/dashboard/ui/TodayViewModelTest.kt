package com.solis.tracker.feature.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solis.tracker.core.extension.DashboardContributor
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayViewModelTest {

    @Test
    fun `contributors are sorted by order`() {
        val contributor1 = object : DashboardContributor {
            override val order: Int = 30
            @Composable override fun Card(modifier: Modifier) {}
        }
        val contributor2 = object : DashboardContributor {
            override val order: Int = 10
            @Composable override fun Card(modifier: Modifier) {}
        }
        val contributor3 = object : DashboardContributor {
            override val order: Int = 20
            @Composable override fun Card(modifier: Modifier) {}
        }

        val unsortedSet = setOf(contributor1, contributor2, contributor3)
        val viewModel = TodayViewModel(unsortedSet)

        val sorted = viewModel.contributors
        
        assertEquals(10, sorted[0].order)
        assertEquals(20, sorted[1].order)
        assertEquals(30, sorted[2].order)
    }
}
