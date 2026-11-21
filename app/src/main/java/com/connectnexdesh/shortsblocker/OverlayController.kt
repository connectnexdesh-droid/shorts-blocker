package com.connectnexdesh.shortsblocker

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager

internal class OverlayController(context: Context) {

    private val appContext = context
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val activeViews = LinkedHashMap<String, View>()

    fun update(rectangles: List<Rect>) {
        val desired = rectangles
            .filter { !it.isEmpty }
            .distinctBy { key(it) }
            .take(10)
        val desiredKeys = desired.map(::key).toSet()

        val obsolete = activeViews.keys.filterNot { it in desiredKeys }
        obsolete.forEach(::remove)

        desired.forEach { rect ->
            val id = key(rect)
            if (activeViews.containsKey(id)) return@forEach

            val mask = View(appContext).apply {
                setBackgroundColor(maskColor())
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setOnTouchListener { _, _ -> true }
            }

            val params = WindowManager.LayoutParams(
                rect.width(),
                rect.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = rect.left
                y = rect.top
            }

            try {
                windowManager.addView(mask, params)
                activeViews[id] = mask
            } catch (_: RuntimeException) {
                // A stale or invalid window must not crash the accessibility service.
            }
        }
    }

    fun clear() {
        activeViews.keys.toList().forEach(::remove)
    }

    private fun remove(id: String) {
        val view = activeViews.remove(id) ?: return
        try {
            windowManager.removeView(view)
        } catch (_: RuntimeException) {
            // The window may already have been removed by the system.
        }
    }

    private fun maskColor(): Int {
        val nightMode = appContext.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.rgb(18, 18, 18)
        } else {
            Color.WHITE
        }
    }

    private fun key(rect: Rect): String =
        "\${rect.left}:\${rect.top}:\${rect.right}:\${rect.bottom}"
}
