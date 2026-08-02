package com.callrecorder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Re-initializes call recording capability after device reboot.
 *
 * BOOT_COMPLETED is received after a full boot.
 * LOCKED_BOOT_COMPLETED is received after an encrypted boot (before unlock).
 *
 * Since call recording relies on a BroadcastReceiver (which works without
 * the app being open), no explicit initialization is needed here — the
 * PhoneStateReceiver will be called when the next call comes in.
 *
 * This BootReceiver exists to perform any deferred cleanup:
 * - Reschedule the auto-delete WorkManager task if it was lost
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        Timber.d("BootReceiver: device booted, rescheduling workers")

        // Reschedule WorkManager tasks (WorkManager normally persists them,
        // but this ensures they're queued even after a factory reset restore)
        // Full worker scheduling will be wired in the DI module.
    }
}
