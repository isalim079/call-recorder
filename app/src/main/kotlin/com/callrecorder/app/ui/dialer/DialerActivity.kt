package com.callrecorder.app.ui.dialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.callrecorder.app.ui.theme.CallRecorderTheme

/**
 * Minimal dialer UI required for ROLE_DIALER.
 * Default phone app enables Telecom InCallService binding + better VOICE_CALL access.
 */
class DialerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefill = intent?.data?.schemeSpecificPart
            ?: intent?.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            ?: ""

        setContent {
            CallRecorderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DialerScreen(
                        initialNumber = prefill,
                        onCall = { placeCall(it) },
                    )
                }
            }
        }
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        val hasCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        try {
            val tm = getSystemService(TELECOM_SERVICE) as TelecomManager
            val uri = Uri.fromParts("tel", number, null)
            if (hasCall) {
                tm.placeCall(uri, null)
            } else {
                startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot place call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun DialerScreen(
    initialNumber: String,
    onCall: (String) -> Unit,
) {
    var number by remember { mutableStateOf(initialNumber) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Place call", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Default phone app enables full call-audio capture",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = number,
            onValueChange = {
                number = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '*' || ch == '#' }
            },
            label = { Text("Phone number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        DialPad(
            onDigit = { number += it },
            onDelete = {
                if (number.isNotEmpty()) number = number.dropLast(1)
            },
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onCall(number) },
            modifier = Modifier.fillMaxWidth(),
            enabled = number.isNotBlank(),
        ) {
            Text("Call")
        }
    }
}

@Composable
private fun DialPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { d ->
                    Button(onClick = { onDigit(d) }) { Text(d) }
                }
            }
        }
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("Delete")
        }
    }
}
