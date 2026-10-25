package com.solis.tracker.feature.expenses.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.solis.tracker.navigation.ExpensesRoute
import com.solis.tracker.navigation.LocalNavController
import com.solis.tracker.navigation.navigateToTopLevel
import java.util.Locale
import javax.inject.Inject

class ExpensesDashboardContributor @Inject constructor() : DashboardContributor {
    override val order: Int = 20

    @Composable
    override fun Card(modifier: Modifier) {
        val viewModel: ExpensesDashboardViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = LocalNavController.current

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Expenses",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Today's Spend: $${String.format(Locale.US, "%.2f", state.todaySpend / 100f)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val progress = (state.monthlySpend.toFloat() / state.monthlyBudget.toFloat()).coerceIn(0f, 1f)
                
                Text(
                    "Monthly: $${String.format(Locale.US, "%.2f", state.monthlySpend / 100f)} / $${String.format(Locale.US, "%.2f", state.monthlyBudget / 100f)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { navController.navigateToTopLevel(ExpensesRoute) }) {
                    Text("Go to Expenses")
                }
            }
        }
    }
}
