package com.connectnexdesh.shortsblocker

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

internal object DetectionRules {

    private const val YOUTUBE = "com.google.android.youtube"
    private const val INSTAGRAM = "com.instagram.android"
    private const val FACEBOOK = "com.facebook.katana"

    fun isSupportedPackage(packageName: String): Boolean =
        packageName == YOUTUBE || packageName == INSTAGRAM || packageName == FACEBOOK

    fun findTargets(
        root: AccessibilityNodeInfo,
        packageName: String,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): List<Rect> {
        if (!isSupportedPackage(packageName)) return emptyList()

        val found = LinkedHashMap<String, Rect>()
        var visited = 0
        val maxNodes = 1800

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (visited >= maxNodes || depth > 28) return
            visited += 1

            if (node.isVisibleToUser) {
                val labels = listOfNotNull(
                    node.text?.toString(),
                    node.contentDescription?.toString()
                )

                for (label in labels) {
                    if (isBlockedLabel(packageName, label)) {
                        val target = chooseMaskBounds(
                            node = node,
                            packageName = packageName,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            density = density
                        )
                        if (!target.isEmpty) {
                            found[key(target)] = target
                        }
                    }
                }
            }

            for (index in 0 until node.childCount) {
                if (visited >= maxNodes) break
                val child = node.getChild(index) ?: continue
                visit(child, depth + 1)
            }
        }

        visit(root, 0)
        return found.values.take(10)
    }

    private fun isBlockedLabel(packageName: String, raw: String): Boolean {
        val label = raw.lowercase(Locale.ROOT)
            .replace(Regex("\\\\s+"), " ")
            .trim()
        if (label.isBlank()) return false

        return when (packageName) {
            YOUTUBE -> label == "shorts" ||
                label == "youtube shorts" ||
                label.startsWith("shorts tab") ||
                label.startsWith("shorts button") ||
                label.contains("shorts shelf")

            INSTAGRAM, FACEBOOK -> label == "reel" ||
                label == "reels" ||
                label.startsWith("reels tab") ||
                label.startsWith("reels button") ||
                label.startsWith("watch reels") ||
                label.contains("reel by ") ||
                label.contains("reels feed")

            else -> false
        }
    }

    private fun chooseMaskBounds(
        node: AccessibilityNodeInfo,
        packageName: String,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect {
        val original = Rect().also(node::getBoundsInScreen)
        if (original.isEmpty) return Rect()

        val minRegionHeight = (90 * density).toInt()
        val maxRegionHeight = (screenHeight * 0.72f).toInt()
        var parent = node.parent
        var candidate: Rect? = null
        var level = 0

        while (parent != null && level < 5) {
            val bounds = Rect().also(parent::getBoundsInScreen)
            val largeEnough = bounds.width() >= screenWidth * 0.72f &&
                bounds.height() >= minRegionHeight &&
                bounds.height() <= maxRegionHeight &&
                bounds.contains(original)

            if (largeEnough) {
                candidate = bounds
                break
            }
            parent = parent.parent
            level += 1
        }

        val result = candidate ?: Rect(original).apply {
            val horizontal = (if (packageName == YOUTUBE) 34 else 42) * density
            val vertical = (if (packageName == YOUTUBE) 24 else 38) * density
            inset((-horizontal).toInt(), (-vertical).toInt())
        }

        result.left = result.left.coerceAtLeast(0)
        result.top = result.top.coerceAtLeast(0)
        result.right = result.right.coerceAtMost(screenWidth)
        result.bottom = result.bottom.coerceAtMost(screenHeight)
        return result
    }

    private fun key(rect: Rect): String =
        "\${rect.left}:\${rect.top}:\${rect.right}:\${rect.bottom}"
}
