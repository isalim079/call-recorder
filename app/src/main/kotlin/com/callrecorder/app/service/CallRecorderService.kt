package com.callrecorder.app.service

import android.app.Service
import android.content.Intent
import android.content.Context
import android.os.PowerManager

import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.callrecorder.app.recorder.AudioRecorderEngine
import com.callrecorder.app.recorder.RecorderError
import com.callrecorder.core.data.contacts.ContactsRepository
import com.callrecorder.core.data.preferences.SettingsRepository
import com.callrecorder.core.data.storage.StorageManager
import com.callrecorder.core.domain.model.AudioQuality
import com.callrecorder.core.domain.model.CallType
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.usecase.InsertRecordingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Foreground service that manages the audio recording phase during an active call.
 *
 * Designed to satisfy strict Android background execution policies:
 * - Started via [ACTION_START_RECORDING] ➔ calls [startForeground] immediately.
 * - Stopped via [ACTION_STOP_RECORDING] ➔ stops recording and saves to database.
 */
@AndroidEntryPoint
class CallRecorderService : Service() {

    companion object {
        const val ACTION_START_RECORDING = "action.START_RECORDING"
        const val ACTION_STOP_RECORDING  = "action.STOP_RECORDING"
        const val EXTRA_PHONE_NUMBER     = "extra.PHONE_NUMBER"
        const val EXTRA_CALL_DIRECTION    = "extra.CALL_DIRECTION"
    }

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var callStateManager: CallStateManager
    @Inject lateinit var recorderEngine: AudioRecorderEngine
    @Inject lateinit var storageManager: StorageManager
    @Inject lateinit var contactsRepository: ContactsRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var insertRecordingUseCase: InsertRecordingUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentFilePath: String? = null
    /**
     * Timestamp set AFTER [recorderEngine.startRecording] succeeds — not before.
     * This eliminates the gap caused by coroutine scheduling + MediaRecorder init
     * (typically 3–10 s on busy devices) that previously caused inflated durations.
     */
    private var recordingStartTime: Long = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallRecorder::RecordingWakeLock")
        Timber.d("CallRecorderService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""

        Timber.d("CallRecorderService action: $action, number: $phoneNumber")

        when (action) {
            ACTION_START_RECORDING -> {
                val directionName = intent.getStringExtra(EXTRA_CALL_DIRECTION) ?: CallStateManager.CallDirection.UNKNOWN.name
                val direction = try {
                    CallStateManager.CallDirection.valueOf(directionName)
                } catch (e: Exception) {
                    CallStateManager.CallDirection.UNKNOWN
                }
                handleStartRecording(phoneNumber, direction)
            }
            ACTION_STOP_RECORDING -> {
                handleStopRecording(phoneNumber)
            }
            else -> {
                // Fallback to prevent background service crashes if started with raw actions
                startForegroundWithFallback(phoneNumber)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleStartRecording(phoneNumber: String, direction: CallStateManager.CallDirection) {
        serviceScope.launch {
            // Check if recording is enabled in settings
            val recordUnknown = settingsRepository.getRecordUnknown().first()
            if (phoneNumber.isBlank() && !recordUnknown) {
                Timber.d("Skipping unknown number recording (disabled in settings)")
                startForegroundWithFallback(phoneNumber)
                cleanup()
                stopSelf()
                return@launch
            }

            // Generate file path
            val filePath = storageManager.createRecordingFilePath(phoneNumber, System.currentTimeMillis())
            currentFilePath = filePath

            // Start foreground service immediately (required before doing any heavy work)
            startForegroundWithFallback(phoneNumber)

            // Start recording. Outgoing: wait for real speech (skip ringback).
            // Incoming: OFFHOOK already means answered.
            val quality = getAudioQuality()
            val waitForAnswer = direction == CallStateManager.CallDirection.OUTGOING
            val result = recorderEngine.startRecording(filePath, quality, waitForAnswer = waitForAnswer)
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Failed to start recording")
                cleanup()
                stopSelf()
            } else {
                // ✅ Set start time AFTER the engine successfully begins recording.
                // This avoids inflating the duration by the coroutine dispatch + MediaRecorder
                // init time (which can be several seconds on slower or busy devices).
                recordingStartTime = System.currentTimeMillis()
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // maximum 2 hours lock
                Timber.d("Recording started: $filePath — WakeLock acquired at $recordingStartTime")
            }
        }
    }

    private fun handleStopRecording(phoneNumber: String) {
        serviceScope.launch {
            val filePath  = currentFilePath
            val startTime = recordingStartTime

            if (!recorderEngine.isRecording || filePath == null) {
                Timber.d("Stop requested but not recording — stopping service")
                startForegroundWithFallback(phoneNumber)
                cleanup()
                stopSelf()
                return@launch
            }

            // stopRecording() returns the duration measured by MediaRecorder itself
            // (System.currentTimeMillis() - recordingStartMs inside the engine).
            // We use that as the authoritative value; fall back to our own timer only
            // if the engine call itself fails.
            val durationResult = recorderEngine.stopRecording()
            val durationMs = durationResult.getOrElse {
                Timber.e(it, "Failed to stop recording properly")
                if (startTime > 0L) System.currentTimeMillis() - startTime else 0L
            }

            // Only save if the file exists and duration > 1 second
            val file = File(filePath)
            if (file.exists() && durationMs > 1_000) {
                saveRecording(
                    filePath    = filePath,
                    phoneNumber = phoneNumber,
                    durationMs  = durationMs,
                    startTime   = startTime,
                    fileSize    = file.length(),
                    direction   = callStateManager.getCurrentDirection(),
                )
            } else {
                Timber.d("Recording too short or file missing — discarding")
                file.delete()
            }

            cleanup()
            stopSelf()
        }
    }

    private fun startForegroundWithFallback(phoneNumber: String) {
        val notification = notificationHelper.buildRecordingNotification(phoneNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_RECORDING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_RECORDING,
                notification
            )
        }
    }

    private suspend fun saveRecording(
        filePath: String,
        phoneNumber: String,
        durationMs: Long,
        startTime: Long,
        fileSize: Long,
        direction: CallStateManager.CallDirection,
    ) {
        try {
            val (contactName, contactPhotoUri) = contactsRepository.resolveContact(phoneNumber)
            val quality = getAudioQuality()

            val recording = Recording(
                phoneNumber     = phoneNumber,
                contactName     = contactName,
                contactPhotoUri = contactPhotoUri,
                callType        = when (direction) {
                    CallStateManager.CallDirection.INCOMING -> CallType.INCOMING
                    CallStateManager.CallDirection.OUTGOING -> CallType.OUTGOING
                    else -> CallType.UNKNOWN
                },
                durationMs      = durationMs,
                timestamp       = startTime,
                filePath        = filePath,
                fileSizeBytes   = fileSize,
                quality         = quality,
            )

            val id = insertRecordingUseCase(recording)
            Timber.d("Recording saved to DB with id=$id")

            // ✅ Fix 4 – Show "Call Recorded" notification to the user
            val displayName = contactName?.takeIf { it.isNotBlank() } ?: phoneNumber
            notificationHelper.showRecordingSavedNotification(
                displayName  = displayName,
                durationSecs = durationMs / 1000L,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to save recording to database")
        }
    }

    private suspend fun getAudioQuality(): AudioQuality {
        return try {
            val qualityName = settingsRepository.getAudioQuality().first()
            AudioQuality.valueOf(qualityName)
        } catch (e: Exception) {
            AudioQuality.MEDIUM
        }
    }

    private fun cleanup() {
        recorderEngine.releaseResources()
        currentFilePath = null
        recordingStartTime = 0L
        callStateManager.reset()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorderEngine.releaseResources()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        serviceScope.cancel()
        Timber.d("CallRecorderService destroyed")
    }
}
