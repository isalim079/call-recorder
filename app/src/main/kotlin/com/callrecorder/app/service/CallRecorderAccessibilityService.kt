package com.callrecorder.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import timber.log.Timber

/**
 * A dummy AccessibilityService that enables call recording on Android 10+.
 * 
 * Android 10 (API 29) introduced aggressive background microphone restrictions.
 * Standard apps are completely blocked from recording the microphone during a call.
 * 
 * However, the Android system grants elevated privileges (bypassing the microphone lock)
 * to apps that have an active Accessibility Service and use the 
 * MediaRecorder.AudioSource.VOICE_RECOGNITION audio source.
 * 
 * This service does not interact with the UI or read screen contents. Its sole purpose
 * is to be enabled by the user to grant the app the necessary privileges.
 */
class CallRecorderAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("CallRecorderAccessibilityService: Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: We do not need to process UI events.
        // We only need the service to be active.
    }

    override fun onInterrupt() {
        Timber.d("CallRecorderAccessibilityService: Interrupted")
    }
}
