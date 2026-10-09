package com.example.scrollblock.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.scrollblock.service.ScrollBlockAccessibilityService

object AccessibilityUtils {

    /**
     * Checks whether the ScrollBlock accessibility service is currently enabled and active.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (ScrollBlockAccessibilityService.isServiceRunning.value) {
            return true
        }

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am != null) {
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val myPackage = context.packageName
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo.serviceInfo
                if (serviceInfo.packageName == myPackage &&
                    serviceInfo.name == ScrollBlockAccessibilityService::class.java.name
                ) {
                    return true
                }
            }
        }

        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedComponentName = ComponentName(context, ScrollBlockAccessibilityService::class.java).flattenToString()
            val expectedShortComponentName = ComponentName(context, ScrollBlockAccessibilityService::class.java).flattenToShortString()

            return enabledServicesSetting.contains(expectedComponentName) ||
                enabledServicesSetting.contains(expectedShortComponentName)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Launches the Android Accessibility Settings screen.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Launches the Application Details screen (for Android 13+ Restricted Settings / 3-dot menu).
     */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
