package com.callrecorder.app.ui.phone

import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callrecorder.app.phone.ActiveCallController
import com.callrecorder.app.ui.theme.CallRecorderTheme
import kotlinx.coroutines.delay

/**
 * Full-screen in-call UI (required when app is default dialer + IN_CALL_SERVICE_UI).
 */
class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            CallRecorderTheme {
                val primary by ActiveCallController.primary.collectAsStateWithLifecycle()
                LaunchedEffect(primary) {
                    if (primary == null) {
                        delay(400)
                        if (ActiveCallController.primary.value == null) finish()
                    }
                }
                InCallScreen(
                    snapshot = primary,
                    onAnswer = { ActiveCallController.answer() },
                    onReject = {
                        ActiveCallController.reject()
                        finish()
                    },
                    onHangup = {
                        ActiveCallController.disconnect()
                        finish()
                    },
                    onMute = { ActiveCallController.setMuted(it) },
                    onSpeaker = { ActiveCallController.setSpeaker(it) },
                )
            }
        }
    }
}

@Composable
private fun InCallScreen(
    snapshot: ActiveCallController.CallSnapshot?,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
    onMute: (Boolean) -> Unit,
    onSpeaker: (Boolean) -> Unit,
) {
    val number = snapshot?.number?.ifBlank { "Unknown" } ?: "…"
    val stateLabel = when (snapshot?.state) {
        Call.STATE_RINGING -> if (snapshot.isIncoming) "Incoming call" else "Ringing…"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
        Call.STATE_ACTIVE -> "Connected"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_DISCONNECTED -> "Ended"
        else -> "Call"
    }
    val isRinging = snapshot?.state == Call.STATE_RINGING && snapshot.isIncoming

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1C24), Color(0xFF102A34), Color(0xFF0A1418))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                stateLabel,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                number,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            if (isRinging) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundAction(
                        icon = Icons.Default.CallEnd,
                        label = "Decline",
                        color = Color(0xFFC62828),
                        onClick = onReject,
                    )
                    RoundAction(
                        icon = Icons.Default.Call,
                        label = "Answer",
                        color = Color(0xFF2E7D32),
                        onClick = onAnswer,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundAction(
                        icon = if (snapshot?.isMuted == true) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (snapshot?.isMuted == true) "Unmute" else "Mute",
                        color = Color.White.copy(alpha = 0.15f),
                        onClick = { onMute(!(snapshot?.isMuted ?: false)) },
                    )
                    RoundAction(
                        icon = Icons.Default.VolumeUp,
                        label = "Speaker",
                        color = if (snapshot?.isSpeaker == true) Color(0xFF1EA7AF) else Color.White.copy(alpha = 0.15f),
                        onClick = { onSpeaker(!(snapshot?.isSpeaker ?: false)) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                FilledIconButton(
                    onClick = onHangup,
                    modifier = Modifier.size(76.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Hang up", modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(68.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color,
                contentColor = Color.White,
            ),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
    }
}
