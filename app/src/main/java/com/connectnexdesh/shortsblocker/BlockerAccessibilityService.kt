package com.connectnexdesh.shortsblocker

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlockerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingScan: Runnable? = null
    private lateinit var overlayController: OverlayController

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayController = OverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()

        if (packageName == null || !DetectionRules.isSupportedPackage(packageName)) {
            if (::overlayController.isInitialized) overlayController.clear()
            return
        }

        scheduleScan(packageName)
    }

    private fun scheduleScan(expectedPackage: String) {
        pendingScan?.let(handler::removeCallbacks)
        val task = Runnable { scan(expectedPackage) }
        pendingScan = task
        handler.postDelayed(task, 180L)
    }

    private fun scan(expectedPackage: String) {
        if (!::overlayController.isInitialized) return

        val prefs = getSharedPreferences(BlockerPreferences.FILE_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(BlockerPreferences.MASTER, true) ||
            !isPackageEnabled(expectedPackage, prefs)
        ) {
            overlayController.clear()
            return
        }

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            overlayController.clear()
            return
        }

        val actualPackage = root.packageName?.toString()
        if (actualPackage != expectedPackage) {
            overlayController.clear()
            return
        }

        val metrics = resources.displayMetrics
        val targets = DetectionRules.findTargets(
            root = root,
            packageName = expectedPackage,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            density = metrics.density
        )
        overlayController.update(targets)
    }

    private fun isPackageEnabled(
        packageName: String,
        prefs: android.content.SharedPreferences
    ): Boolean {
        return when (packageName) {
            "com.google.android.youtube" ->
                prefs.getBoolean(BlockerPreferences.YOUTUBE, true)
            "com.instagram.android" ->
                prefs.getBoolean(BlockerPreferences.INSTAGRAM, true)
            "com.facebook.katana" ->
                prefs.getBoolean(BlockerPreferences.FACEBOOK, true)
            else -> false
        }
    }

    override fun onInterrupt() {
        pendingScan?.let(handler::removeCallbacks)
        if (::overlayController.isInitialized) overlayController.clear()
    }

    override fun onDestroy() {
        pendingScan?.let(handler::removeCallbacks)
        if (::overlayController.isInitialized) overlayController.clear()
        super.onDestroy()
    }
}
