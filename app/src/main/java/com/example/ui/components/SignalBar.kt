package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.SensorRed
import com.example.ui.theme.TechCyanPrimary

@Composable
fun SignalBar(
    rssiDbm: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val level = when {
        rssiDbm >= -55 -> 4
        rssiDbm >= -68 -> 3
        rssiDbm >= -79 -> 2
        rssiDbm >= -89 -> 1
        else -> 0
    }

    val activeColor = when {
        rssiDbm >= -60 -> SensorGreen
        rssiDbm >= -75 -> TechCyanPrimary
        rssiDbm >= -85 -> SensorAmber
        else -> SensorRed
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 4 vertical bars with increasing heights
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
            val barHeights = listOf(6.dp, 10.dp, 14.dp, 18.dp)
            barHeights.forEachIndexed { index, height ->
                val isFilled = index < level
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height)
                        .background(
                            color = if (isFilled) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (rssiDbm != 0) "$rssiDbm dBm" else "N/A",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = activeColor
            )
        }
    }
}
