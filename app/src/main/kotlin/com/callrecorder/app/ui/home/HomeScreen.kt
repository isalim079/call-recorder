package com.callrecorder.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.app.ui.components.EmptyStateView
import com.callrecorder.app.ui.components.RecordingListItem
import com.callrecorder.core.domain.model.RecordingStatistics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.callrecorder.app.util.AccessibilityUtil
import com.callrecorder.app.util.DialerRoleUtil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Phone

@Composable
fun HomeScreen(
    onRecordingClick: (Long) -> Unit,
    onSeeAllRecordings: () -> Unit,
    onStorageClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    // Define permission list
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    var missingPermissions by remember {
        mutableStateOf(
            requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    var isAccessibilityEnabled by remember {
        mutableStateOf(AccessibilityUtil.isAccessibilityServiceEnabled(context))
    }
    var isDefaultDialer by remember {
        mutableStateOf(DialerRoleUtil.isDefaultDialer(context))
    }

    val dialerRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = DialerRoleUtil.isDefaultDialer(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isAccessibilityEnabled = AccessibilityUtil.isAccessibilityServiceEnabled(context)
        isDefaultDialer = DialerRoleUtil.isDefaultDialer(context)
        missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Mic,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text      = "Call Recorder",
                            style     = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Permission Banner ─────────────────────────────────────────────
        if (missingPermissions.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Permissions Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This app needs Telephone, Call Logs, Contacts, and Microphone permissions to detect and record calls offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                launcher.launch(missingPermissions.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Grant Permissions", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }

        // ── Default Dialer — production VOICE_CALL path ───────────────────
        if (!isDefaultDialer && missingPermissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Set as Default Phone App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Required for real call-stream capture (both sides, full volume) like OEM dialer recorders — without playing a recording announcement to the other party. You can switch back anytime in system settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = DialerRoleUtil.createRequestIntent(context)
                                if (intent != null) dialerRoleLauncher.launch(intent)
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Set Default Phone App")
                        }
                    }
                }
            }
        }

        // ── Accessibility Service Banner ──────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isAccessibilityEnabled && missingPermissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Accessibility Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Android 10+ blocks background call recording. To bypass this and enable auto-recording, you must manually enable the Call Recorder Accessibility Service in your phone settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Open Settings", color = MaterialTheme.colorScheme.onTertiary)
                        }
                    }
                }
            }
        }

        // ── Stat cards ────────────────────────────────────────────────────
        item {
            if (!state.isLoading) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Overview",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label    = "Today",
                            value    = state.todayStats.totalRecordings.toString(),
                            icon     = Icons.Default.PhoneInTalk,
                            color    = MaterialTheme.colorScheme.primary,
                            onClick  = onStatisticsClick,
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label    = "This Week",
                            value    = state.weekStats.totalRecordings.toString(),
                            icon     = Icons.Default.BarChart,
                            color    = MaterialTheme.colorScheme.secondary,
                            onClick  = onStatisticsClick,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label    = "This Month",
                            value    = state.monthStats.totalRecordings.toString(),
                            icon     = Icons.Default.GraphicEq,
                            color    = MaterialTheme.colorScheme.tertiary,
                            onClick  = onStatisticsClick,
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label    = "Total",
                            value    = state.allTimeStats.totalRecordings.toString(),
                            icon     = Icons.Default.Storage,
                            color    = MaterialTheme.colorScheme.error,
                            onClick  = onStatisticsClick,
                        )
                    }
                }
            }
        }

        // ── Storage indicator ─────────────────────────────────────────────
        item {
            val totalBytes = state.totalStorageBytes
            val availableBytes = state.availableStorageBytes
            val total = (totalBytes + availableBytes).coerceAtLeast(1L)
            val usedFraction = totalBytes.toFloat() / total.toFloat()

            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onStorageClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Storage Used",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatBytes(totalBytes),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress             = { usedFraction.coerceIn(0f, 1f) },
                        modifier             = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color                = MaterialTheme.colorScheme.primary,
                        trackColor           = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap            = StrokeCap.Round,
                    )
                }
            }
        }

        // ── Recent recordings ─────────────────────────────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Recordings",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onSeeAllRecordings) {
                    Text("See All", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.recentRecordings.isEmpty() && !state.isLoading) {
            item {
                EmptyStateView(
                    icon     = Icons.Outlined.GraphicEq,
                    title    = "No recordings yet",
                    subtitle = "Recordings will appear here automatically\nafter your first call.",
                )
            }
        } else {
            items(state.recentRecordings, key = { it.id }) { recording ->
                RecordingListItem(
                    recording        = recording,
                    onClick          = { onRecordingClick(recording.id) },
                    onFavoriteToggle = { viewModel.toggleFavorite(recording.id) },
                    onDelete         = { viewModel.deleteRecording(recording.id) },
                    onRename         = { viewModel.showRenameDialog(recording.id, recording.displayName) },
                    onShare          = { },
                    modifier         = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // ── Favorites ─────────────────────────────────────────────────────
        if (state.favoriteRecordings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Favorites",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            items(state.favoriteRecordings, key = { "fav_${it.id}" }) { recording ->
                RecordingListItem(
                    recording        = recording,
                    onClick          = { onRecordingClick(recording.id) },
                    onFavoriteToggle = { viewModel.toggleFavorite(recording.id) },
                    onDelete         = { viewModel.deleteRecording(recording.id) },
                    onRename         = { viewModel.showRenameDialog(recording.id, recording.displayName) },
                    onShare          = { },
                    modifier         = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }

    // ── Rename dialog ────────────────────────────────────────────────────────
    if (state.showRenameDialog) {
        var nameText by remember(state.renameInitialName) { mutableStateOf(state.renameInitialName) }
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            title            = { Text("Rename Recording") },
            text             = {
                OutlinedTextField(
                    value         = nameText,
                    onValueChange = { nameText = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                )
            },
            confirmButton    = {
                TextButton(
                    onClick = { viewModel.renameRecording(nameText) },
                    enabled = nameText.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = viewModel::dismissRenameDialog) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick  = onClick,
        modifier = modifier,
        shape    = MaterialTheme.shapes.extraLarge,
        colors   = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = value,
                style     = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
