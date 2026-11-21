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
                    if (!isBlockedLabel(packageName, label)) continue

                    val selectedBounds = findSelectedTabBounds(
                        node = node,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        density = density
                    )

                    val target = if (selectedBounds != null) {
                        selectedFeedBounds(
                            node = node,
                            selectedBounds = selectedBounds,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            density = density
                        )
                    } else {
                        chooseMaskBounds(
                            node = node,
                            packageName = packageName,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            density = density
                        )
                    }

                    if (!target.isEmpty) found[key(target)] = target
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

    internal fun isBlockedLabel(packageName: String, raw: String): Boolean {
        val label = raw.lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
        if (label.isBlank()) return false

        return when (packageName) {
            YOUTUBE -> label == "shorts" ||
                label == "youtube shorts" ||
                label.startsWith("shorts tab") ||
                label.startsWith("shorts button") ||
                label.contains("shorts shelf") ||
                label == "শর্টস" ||
                label == "ইউটিউব শর্টস" ||
                label.contains("শর্টস")

            INSTAGRAM, FACEBOOK -> label == "reel" ||
                label == "reels" ||
                label.startsWith("reels tab") ||
                label.startsWith("reels button") ||
                label.startsWith("watch reels") ||
                label.contains("reel by ") ||
                label.contains("reels feed") ||
                label == "রিল" ||
                label == "রিলস" ||
                label.contains("রিলস") ||
                label.contains("রিল ভিডিও") ||
                label == "रील" ||
                label == "रील्स" ||
                label.contains("रील्स")

            else -> false
        }
    }

    private fun findSelectedTabBounds(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0

        while (current != null && depth < 4) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)
            val isSelected = current.isSelected || current.isChecked
            val looksLikeBottomTab = bounds.top >= screenHeight * 0.62f &&
                bounds.top <= screenHeight * 0.96f &&
                bounds.width() <= screenWidth * 0.75f &&
                bounds.height() <= 120 * density

            if (isSelected && looksLikeBottomTab && !bounds.isEmpty) return bounds

            current = current.parent
            depth += 1
        }

        return null
    }

    private fun selectedFeedBounds(
        node: AccessibilityNodeInfo,
        selectedBounds: Rect,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect {
        var current = node.parent
        var level = 0
        var navigationTop = -1

        while (current != null && level < 6) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)
            val looksLikeBottomNavigation = bounds.width() >= screenWidth * 0.80f &&
                bounds.height() in (40 * density).toInt()..(140 * density).toInt() &&
                bounds.top >= screenHeight * 0.62f &&
                bounds.bottom >= screenHeight * 0.88f

            if (looksLikeBottomNavigation) {
                navigationTop = bounds.top
                break
            }

            current = current.parent
            level += 1
        }

        if (navigationTop < 0) {
            navigationTop = selectedBounds.top - (68 * density).toInt()
        }

        val top = (24 * density).toInt().coerceAtMost(screenHeight)
        val bottom = navigationTop.coerceAtMost(screenHeight)
        if (bottom - top < (screenHeight * 0.20f)) return Rect()

        return Rect(0, top, screenWidth, bottom)
    }

    private fun chooseMaskBounds(
        node: AccessibilityNodeInfo,
        packageName: String,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect {
        val original = Rect()
        node.getBoundsInScreen(original)
        if (original.isEmpty) return Rect()

        val minRegionHeight = (140 * density).toInt()
        val maxRegionHeight = (screenHeight * 0.72f).toInt()
        var parent = node.parent
        var candidate: Rect? = null
        var level = 0

        while (parent != null && level < 5) {
            val bounds = Rect()
            parent.getBoundsInScreen(bounds)
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
        listOf(rect.left, rect.top, rect.right, rect.bottom).joinToString(":")
}
