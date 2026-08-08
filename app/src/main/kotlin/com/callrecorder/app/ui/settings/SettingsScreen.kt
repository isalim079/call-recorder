package com.callrecorder.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.app.BuildConfig

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        sectionHeader("Appearance")
        settingItem {
            DropdownSetting(
                icon    = Icons.Default.Brightness6,
                title   = "Theme",
                current = when (state.theme) {
                    "LIGHT" -> "Light"; "DARK" -> "Dark"; else -> "Follow System"
                },
                options = listOf("SYSTEM" to "Follow System", "LIGHT" to "Light", "DARK" to "Dark"),
                onSelect = viewModel::setTheme,
            )
        }
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.Palette,
                title   = "Dynamic Colors",
                subtitle = "Use your wallpaper colors (Android 12+)",
                checked  = state.dynamicColors,
                onCheckedChange = viewModel::setDynamicColors,
            )
        }

        sectionHeader("Recording")
        settingItem {
            DropdownSetting(
                icon    = Icons.Default.GraphicEq,
                title   = "Audio Quality",
                current = when (state.audioQuality) {
                    "LOW" -> "Low (~0.7 MB/min)"
                    "MEDIUM" -> "Medium (~1.2 MB/min)"
                    else -> "High HD (~2 MB/min) — default"
                },
                options = listOf(
                    "LOW" to "Low",
                    "MEDIUM" to "Medium",
                    "HIGH" to "High (HD)",
                ),
                onSelect = viewModel::setAudioQuality,
            )
        }
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.Hearing,
                title   = "Noise Cancellation",
                subtitle = "Remove hiss, ambient noise & level speech after each call (Wiener HD)",
                checked  = state.noiseCancellation,
                onCheckedChange = viewModel::setNoiseCancellation,
            )
        }
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.PhoneCallback,
                title   = "Record All Calls",
                subtitle = "Automatically record every incoming & outgoing call",
                checked  = state.recordEveryone,
                onCheckedChange = viewModel::setRecordEveryone,
            )
        }
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.Phone,
                title   = "Record Unknown Numbers",
                subtitle = "Record calls from numbers not in your contacts",
                checked  = state.recordUnknown,
                onCheckedChange = viewModel::setRecordUnknown,
            )
        }

        sectionHeader("Storage")
        settingItem {
            DropdownSetting(
                icon    = Icons.Default.AutoDelete,
                title   = "Auto-Delete Recordings",
                current = when (state.autoDeleteDays) {
                    0  -> "Never"
                    7  -> "After 7 days"
                    30 -> "After 30 days"
                    90 -> "After 90 days"
                    else -> "After ${state.autoDeleteDays} days"
                },
                options = listOf(0 to "Never", 7 to "After 7 days", 30 to "After 30 days", 90 to "After 90 days"),
                onSelect = { viewModel.setAutoDeleteDays(it) },
            )
        }

        sectionHeader("Security")
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.Lock,
                title   = "App Lock",
                subtitle = "Require PIN or biometric to open the app",
                checked  = state.appLockEnabled,
                onCheckedChange = viewModel::setAppLock,
            )
        }

        sectionHeader("Notifications")
        settingItem {
            SwitchSetting(
                icon    = Icons.Default.Notifications,
                title   = "Recording Notifications",
                subtitle = "Show a notification while a call is being recorded",
                checked  = state.notificationEnabled,
                onCheckedChange = viewModel::setNotification,
            )
        }

        sectionHeader("About")
        settingItem {
            // Version is driven by app.version.name in app.properties — no hardcoding needed.
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Default.Info,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)) {
                    Text(
                        text       = "Version",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text  = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun LazyListScope.sectionHeader(title: String) {
    item {
        Text(
            text     = title,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

private fun LazyListScope.settingItem(content: @Composable () -> Unit) {
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape    = MaterialTheme.shapes.large,
        ) { content() }
    }
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> DropdownSetting(
    icon: ImageVector,
    title: String,
    current: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(current, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { expanded = true }) { Text("Change") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text    = { Text(label) },
                    onClick = { onSelect(value); expanded = false },
                )
            }
        }
    }
}
