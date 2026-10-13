package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.theme.TechCyanContainer

@Composable
fun SparklineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = TechCyanPrimary,
    fillColor: Color = TechCyanContainer.copy(alpha = 0.25f),
    strokeWidth: Float = 4f
) {
    if (values.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Collecting waveform data...",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val minVal = remember(values) { values.minOrNull() ?: 0f }
    val maxVal = remember(values) { values.maxOrNull() ?: 1f }
    val range = remember(minVal, maxVal) {
        val diff = maxVal - minVal
        if (diff == 0f) 1f else diff
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        values.forEachIndexed { index, value ->
            val normY = (value - minVal) / range
            val x = index * stepX
            val y = height - (normY * (height - 8f)) - 4f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw translucent gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent)
            )
        )

        // Draw neon stroke
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw glow point at latest sample
        val lastNormY = (values.last() - minVal) / range
        val lastX = width
        val lastY = height - (lastNormY * (height - 8f)) - 4f
        drawCircle(
            color = lineColor,
            radius = strokeWidth * 1.5f,
            center = Offset(lastX, lastY)
        )
    }
}
