package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorStatus
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.SensorRed
import com.example.ui.theme.TechCyanPrimary

@Composable
fun StatusBadge(
    status: SensorStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        SensorStatus.ACTIVE -> Pair(SensorGreen, "Active")
        SensorStatus.WAITING -> Pair(TechCyanPrimary, "Waiting")
        SensorStatus.PAUSED -> Pair(SensorAmber, "Paused")
        SensorStatus.UNSUPPORTED -> Pair(Color.Gray, "Unsupported")
        SensorStatus.PERMISSION_REQUIRED -> Pair(SensorRed, "Permission")
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = color
        )
    }
}
