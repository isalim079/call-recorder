package com.callrecorder.app.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.callrecorder.app.service.CallRecorderAccessibilityService

object AccessibilityUtil {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        
        val myServiceName = CallRecorderAccessibilityService::class.java.name
        val myPackageName = context.packageName

        return enabledServices.any { info ->
            info.resolveInfo.serviceInfo.packageName == myPackageName &&
            info.resolveInfo.serviceInfo.name == myServiceName
        }
    }
}
