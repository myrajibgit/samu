package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceHardwareStats
import com.example.ui.theme.AccentMint
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import java.util.Locale

@Composable
fun HardwareMonitorBadge(
    stats: DeviceHardwareStats,
    modifier: Modifier = Modifier
) {
    val ramStatusColor = when {
        stats.ramUsedPercent > 0.85f -> AccentRose
        stats.ramUsedPercent > 0.70f -> AccentAmber
        else -> AccentMint
    }

    Row(
        modifier = modifier
            .testTag("hardware_monitor_badge")
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // RAM indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ramStatusColor)
            )
            Text(
                text = String.format(Locale.US, "RAM: %.1f/%.1f GB", stats.availableRamGb, stats.totalRamGb),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Divider dot
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )

        // Storage indicator
        Text(
            text = String.format(Locale.US, "Disk: %.1f GB free", stats.freeStorageGb),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
