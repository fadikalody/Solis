package com.solis.tracker.feature.health.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solis.tracker.R
import com.solis.tracker.feature.health.domain.model.MetricType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSheet by viewModel.showLogSheet.collectAsStateWithLifecycle()
    val selectedMetric by viewModel.selectedMetric.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_health)) })
        },
        floatingActionButton = {
            // Simplified for Phase 4: FAB just opens sheet for the first metric as a fallback if not clicked directly
            val firstMetric = (uiState as? HealthUiState.Success)?.metrics?.firstOrNull()?.metricType
            if (firstMetric != null) {
                FloatingActionButton(onClick = { viewModel.openLogSheet(firstMetric) }) {
                    Icon(Icons.Default.Add, contentDescription = "Log Health")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is HealthUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HealthUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HealthUiState.Success -> {
                    if (state.metrics.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite, 
                                contentDescription = null,
                                modifier = Modifier.padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("No health metrics tracked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = state.metrics,
                                key = { it.metricType.id }
                            ) { item ->
                                MetricCard(
                                    metricWithEntries = item,
                                    onLogClick = { viewModel.openLogSheet(item.metricType) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSheet && selectedMetric != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeLogSheet() },
                sheetState = sheetState
            ) {
                HealthLogEditor(
                    metric = selectedMetric!!,
                    onSave = { valueStr, note ->
                        viewModel.logEntry(selectedMetric!!.id, valueStr, note)
                    },
                    onCancel = { viewModel.closeLogSheet() }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    metricWithEntries: MetricWithRecentEntries,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metric = metricWithEntries.metricType
    val latest = metricWithEntries.recentEntries.maxByOrNull { it.recordedAt }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(metric.name, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onLogClick) {
                    Text("Log")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (latest != null) {
                Text(
                    text = "Latest: ${latest.value} ${metric.unit}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (metric.goalValue != null) {
                    Text(
                        text = "Goal: ${metric.goalValue} ${metric.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                SimpleTrendChart(entries = metricWithEntries.recentEntries)
            } else {
                Text(
                    text = "No entries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HealthLogEditor(
    metric: MetricType,
    onSave: (valueStr: String, note: String) -> Unit,
    onCancel: () -> Unit
) {
    var valueStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Log ${metric.name}", style = MaterialTheme.typography.headlineSmall)
        
        OutlinedTextField(
            value = valueStr,
            onValueChange = { valueStr = it },
            label = { Text("Value (${metric.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = { if (valueStr.isNotBlank()) onSave(valueStr, note) },
                enabled = valueStr.isNotBlank()
            ) {
                Text("Save")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
