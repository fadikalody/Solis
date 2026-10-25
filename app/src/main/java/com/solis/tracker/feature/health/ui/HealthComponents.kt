package com.solis.tracker.feature.health.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.solis.tracker.feature.health.domain.model.HealthEntry

@Composable
fun SimpleTrendChart(entries: List<HealthEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return

    val color = MaterialTheme.colorScheme.primary
    val values = entries.sortedBy { it.recordedAt }.map { it.value.toFloat() }
    val maxVal = values.maxOrNull() ?: 1f
    val minVal = values.minOrNull() ?: 0f
    
    val range = maxVal - minVal
    val normalizedRange = if (range == 0f) 1f else range

    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val width = size.width
        val height = size.height
        val stepX = if (values.size > 1) width / (values.size - 1) else width

        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minVal) / normalizedRange * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
        
        if (values.size > 1) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
