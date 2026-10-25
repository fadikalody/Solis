package com.solis.tracker.feature.health.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.core.extension.DashboardContributor
import com.solis.tracker.navigation.HealthRoute
import com.solis.tracker.navigation.LocalNavController
import com.solis.tracker.navigation.navigateToTopLevel
import javax.inject.Inject

class HealthDashboardContributor @Inject constructor() : DashboardContributor {
    override val order: Int = 30

    @Composable
    override fun Card(modifier: Modifier) {
        val viewModel: HealthDashboardViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = LocalNavController.current

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Health (Today)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (state.metrics.isEmpty()) {
                    Text("No health data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.metrics.take(3).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(item.metric.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (item.latestEntry != null) {
                                Text("${item.latestEntry.value} ${item.metric.unit}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("--", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { navController.navigateToTopLevel(HealthRoute) }) {
                    Text("Log Health")
                }
            }
        }
    }
}
