package com.callrecorder.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.callrecorder.app.MainActivity
import com.callrecorder.app.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and manages notifications for the call recorder.
 *
 * Responsibilities:
 * - Register notification channels (required Android 8+)
 * - Build the foreground "recording in progress" notification
 * - Post a one-shot "Call recorded" summary notification when a recording is saved
 */
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID_RECORDING  = "call_recording"
        const val CHANNEL_ID_SAVED      = "call_recorded_saved"
        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_SAVED     = 1002
    }

    private val notificationManager: NotificationManager =
        context.getSystemService()!!

    /**
     * Register all notification channels. Safe to call multiple times (idempotent).
     * Must be called before showing any notification.
     */
    fun createNotificationChannels() {
        // ── Ongoing recording foreground channel ──────────────────────────
        val recordingChannel = NotificationChannel(
            CHANNEL_ID_RECORDING,
            context.getString(R.string.notification_channel_recording),
            NotificationManager.IMPORTANCE_LOW,   // Non-intrusive — no sound
        ).apply {
            description = context.getString(R.string.notification_channel_recording_desc)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }

        // ── "Call saved" alert channel ────────────────────────────────────
        val savedChannel = NotificationChannel(
            CHANNEL_ID_SAVED,
            "Call Recorded",
            NotificationManager.IMPORTANCE_DEFAULT,  // Shows in notification shade
        ).apply {
            description = "Shown once after a call recording is successfully saved"
            setShowBadge(true)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(recordingChannel)
        notificationManager.createNotificationChannel(savedChannel)
    }

    /**
     * Build the foreground service notification shown while recording.
     *
     * @param phoneNumber Optional phone number/name for the notification text.
     */
    fun buildRecordingNotification(phoneNumber: String? = null): android.app.Notification {
        val contentText = if (!phoneNumber.isNullOrBlank()) {
            "Recording call with $phoneNumber"
        } else {
            context.getString(R.string.notification_recording_text)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_RECORDING)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)               // Cannot be dismissed by user
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Post a one-shot "Call Recorded" notification shown after a recording is saved.
     *
     * @param displayName   Contact name or phone number.
     * @param durationSecs  Recording duration in seconds.
     */
    fun showRecordingSavedNotification(displayName: String, durationSecs: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID_SAVED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val durationText = formatDuration(durationSecs)
        val contentText  = if (displayName.isNotBlank()) {
            "Saved · $displayName · $durationText"
        } else {
            "Recording saved · $durationText"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SAVED)
            .setContentTitle("✅ Call Recorded")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)            // Dismissed when tapped
            .setShowWhen(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_SAVED, notification)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun formatDuration(secs: Long): String {
        val m = secs / 60
        val s = secs % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
