package com.callrecorder.app.ui.statistics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.core.domain.model.RecordingStatistics

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "This Week", "This Month", "All Time")

    val currentStats = when (selectedTab) {
        0    -> state.today
        1    -> state.week
        2    -> state.month
        else -> state.allTime
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding      = 16.dp,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text     = { Text(title) },
                )
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Total recordings
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Total",
                        value    = currentStats.totalRecordings.toString(),
                        icon     = Icons.Default.Timer,
                        tint     = MaterialTheme.colorScheme.primary,
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Favorites",
                        value    = currentStats.favoriteCount.toString(),
                        icon     = Icons.Default.Star,
                        tint     = Color(0xFFFFD700),
                    )
                }
            }

            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Incoming",
                        value    = currentStats.incomingCount.toString(),
                        icon     = Icons.Default.CallReceived,
                        tint     = MaterialTheme.colorScheme.tertiary,
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Outgoing",
                        value    = currentStats.outgoingCount.toString(),
                        icon     = Icons.Default.CallMade,
                        tint     = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Duration stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.extraLarge,
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        DurationRow("Total Duration", formatDuration(currentStats.totalDurationMs))
                        DurationRow("Average Duration", formatDuration(currentStats.avgDurationMs))
                        DurationRow("Longest Recording", formatDuration(currentStats.longestDurationMs))
                    }
                }
            }

            // Storage stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.extraLarge,
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        DurationRow("Total Size", formatBytes(currentStats.totalSizeBytes))
                        if (currentStats.totalRecordings > 0) {
                            DurationRow("Avg Size per Recording",
                                formatBytes(currentStats.totalSizeBytes / currentStats.totalRecordings))
                        }
                    }
                }
            }

            // Call type chart
            if (currentStats.totalRecordings > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.extraLarge,
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Call Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            DonutChart(
                                incoming = currentStats.incomingCount,
                                outgoing = currentStats.outgoingCount,
                                primary  = MaterialTheme.colorScheme.primary,
                                secondary = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape    = MaterialTheme.shapes.extraLarge,
        colors   = CardDefaults.cardColors(
            containerColor = tint.copy(alpha = 0.1f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DurationRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun DonutChart(incoming: Int, outgoing: Int, primary: Color, secondary: Color) {
    val total = (incoming + outgoing).toFloat().coerceAtLeast(1f)
    val incomingAngle = (incoming / total) * 360f
    Box(
        modifier        = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(color = primary, startAngle = -90f, sweepAngle = incomingAngle, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 28.dp.toPx()))
            drawArc(color = secondary, startAngle = -90f + incomingAngle, sweepAngle = 360f - incomingAngle, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 28.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$incoming / $outgoing", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("In / Out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1_000
    val hours  = totalSec / 3_600
    val minutes = (totalSec % 3_600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d h %02d m".format(hours, minutes)
    else "%d m %02d s".format(minutes, seconds)
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.0f MB".format(mb)
        else      -> "$bytes B"
    }
}
