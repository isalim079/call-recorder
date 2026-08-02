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
 * - Register the recording notification channel (required Android 8+)
 * - Build a minimal, non-intrusive foreground service notification
 * - Update notification content when call info is available
 */
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID_RECORDING = "call_recording"
        const val NOTIFICATION_ID_RECORDING = 1001
    }

    private val notificationManager: NotificationManager =
        context.getSystemService()!!

    /**
     * Register the notification channel. Safe to call multiple times (idempotent).
     * Must be called before showing any notification.
     */
    fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_ID_RECORDING,
            context.getString(R.string.notification_channel_recording),
            NotificationManager.IMPORTANCE_LOW,   // Non-intrusive — no sound
        ).apply {
            description = context.getString(R.string.notification_channel_recording_desc)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
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
}
