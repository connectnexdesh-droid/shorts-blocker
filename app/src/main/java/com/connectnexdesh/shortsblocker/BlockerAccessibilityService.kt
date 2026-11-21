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
        // Do not clear masks just because Android reports a null/System UI event.
        // Inspect the actual active window instead; the event package can be misleading.
        scheduleScan()
    }

    private fun scheduleScan() {
        if (pendingScan != null) return
        val task = Runnable {
            pendingScan = null
            scanActiveWindow()
        }
        pendingScan = task
        handler.postDelayed(task, 120L)
    }

    private fun scanActiveWindow() {
        if (!::overlayController.isInitialized) return

        val prefs = getSharedPreferences(BlockerPreferences.FILE_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(BlockerPreferences.MASTER, true)) {
            overlayController.clear()
            saveDiagnostics("Blocker disabled", 0, emptyList(), emptyList(), 0)
            return
        }

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            overlayController.clear()
            saveDiagnostics("No active accessibility window", 0, emptyList(), emptyList(), 0)
            return
        }

        val actualPackage = root.packageName?.toString().orEmpty()
        if (!DetectionRules.isSupportedPackage(actualPackage)) {
            overlayController.clear()
            saveDiagnostics("No supported app active", 0, emptyList(), emptyList(), 0)
            return
        }

        if (!isPackageEnabled(actualPackage, prefs)) {
            overlayController.clear()
            saveDiagnostics("$actualPackage filter OFF", 0, emptyList(), emptyList(), 0)
            return
        }

        val metrics = resources.displayMetrics
        val result = DetectionRules.findTargets(
            root = root,
            packageName = actualPackage,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            density = metrics.density
        )
        overlayController.update(result.targets)
        saveDiagnostics(
            app = actualPackage,
            nodes = result.nodesVisited,
            candidates = result.candidateLabels,
            matches = result.matchedLabels,
            targetCount = result.targets.size
        )
    }

    private fun saveDiagnostics(
        app: String,
        nodes: Int,
        candidates: List<String>,
        matches: List<String>,
        targetCount: Int
    ) {
        getSharedPreferences(BlockerPreferences.FILE_NAME, MODE_PRIVATE).edit()
            .putString(BlockerPreferences.LAST_SCAN_APP, app)
            .putInt(BlockerPreferences.LAST_SCAN_NODES, nodes)
            .putString(BlockerPreferences.LAST_SCAN_CANDIDATES, candidates.joinToString(" | "))
            .putString(BlockerPreferences.LAST_SCAN_MATCHES, matches.joinToString(" | "))
            .putInt(BlockerPreferences.LAST_SCAN_TARGETS, targetCount)
            .putLong(BlockerPreferences.LAST_SCAN_TIME, System.currentTimeMillis())
            .apply()
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
        pendingScan = null
        if (::overlayController.isInitialized) overlayController.clear()
    }

    override fun onDestroy() {
        pendingScan?.let(handler::removeCallbacks)
        pendingScan = null
        if (::overlayController.isInitialized) overlayController.clear()
        super.onDestroy()
    }
}
