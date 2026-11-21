package com.connectnexdesh.shortsblocker

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

internal data class DetectionResult(
    val targets: List<Rect>,
    val nodesVisited: Int,
    val candidateLabels: List<String>,
    val matchedLabels: List<String>
)

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
    ): DetectionResult {
        if (!isSupportedPackage(packageName)) {
            return DetectionResult(emptyList(), 0, emptyList(), emptyList())
        }

        val found = LinkedHashMap<String, Rect>()
        val candidates = LinkedHashSet<String>()
        val matches = LinkedHashSet<String>()
        var visited = 0
        val maxNodes = 3500

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (visited >= maxNodes || depth > 36) return
            visited += 1

            if (node.isVisibleToUser) {
                val labels = collectLabels(node)
                for (rawLabel in labels) {
                    val label = normalize(rawLabel)
                    if (isDiagnosticCandidate(label) && candidates.size < 12) {
                        candidates.add(label.take(64))
                    }
                    if (!isBlockedLabel(packageName, label)) continue

                    matches.add(label.take(64))

                    val navigationBounds = findBottomNavigationBounds(
                        node = node,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        density = density
                    )
                    val target = if (navigationBounds != null) {
                        val tabMask = bottomTabCellBounds(
                            node = node,
                            navigationBounds = navigationBounds,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                        val selected = isSelectedInPath(node, navigationBounds)
                        if (selected) {
                            val feedMask = selectedFeedBounds(
                                navigationBounds = navigationBounds,
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                                density = density
                            )
                            if (!feedMask.isEmpty) found[key(feedMask)] = feedMask
                        }
                        tabMask
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
        return DetectionResult(
            targets = found.values.take(14),
            nodesVisited = visited,
            candidateLabels = candidates.toList(),
            matchedLabels = matches.toList()
        )
    }

    private fun collectLabels(node: AccessibilityNodeInfo): List<String> {
        val labels = LinkedHashSet<String>()
        fun add(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) labels.add(text)
        }

        add(node.text)
        add(node.contentDescription)
        add(node.hintText)
        add(node.viewIdResourceName)

        if (Build.VERSION.SDK_INT >= 28) {
            add(node.paneTitle)
            add(node.tooltipText)
        }
        if (Build.VERSION.SDK_INT >= 30) {
            add(node.stateDescription)
        }

        return labels.toList()
    }

    private fun normalize(raw: String): String =
        raw.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    private fun isDiagnosticCandidate(label: String): Boolean =
        label.contains("short") ||
            label.contains("reel") ||
            label.contains("clip") ||
            label.contains("শর্টস") ||
            label.contains("রিলস") ||
            label.contains("रील")

    internal fun isBlockedLabel(packageName: String, raw: String): Boolean {
        val label = normalize(raw)
        if (label.isBlank()) return false

        return when (packageName) {
            YOUTUBE -> {
                val startsWithShorts = label == "shorts" ||
                    label.startsWith("shorts,") ||
                    label.startsWith("shorts ·") ||
                    label.startsWith("shorts |") ||
                    label.startsWith("shorts tab") ||
                    label.startsWith("shorts button") ||
                    label.startsWith("youtube shorts") ||
                    label.contains("shorts shelf") ||
                    label.contains("shorts feed") ||
                    label.contains("shorts_tab") ||
                    label.contains("shorts_tab_button")
                startsWithShorts ||
                    label == "শর্টস" ||
                    label.startsWith("ইউটিউব শর্টস") ||
                    label.contains("শর্টস")
            }

            INSTAGRAM, FACEBOOK -> {
                val startsWithReels = label == "reel" ||
                    label == "reels" ||
                    label.startsWith("reels,") ||
                    label.startsWith("reels ·") ||
                    label.startsWith("reels |") ||
                    label.startsWith("reels tab") ||
                    label.startsWith("reels button") ||
                    label.startsWith("watch reels") ||
                    label.startsWith("reel by ") ||
                    label.contains("reels feed") ||
                    label.contains("reels_tab") ||
                    label.contains("clips_tab")
                startsWithReels ||
                    label == "রিল" ||
                    label == "রিলস" ||
                    label.contains("রিলস") ||
                    label == "रील" ||
                    label == "रील्स" ||
                    label.contains("रील्स")
            }

            else -> false
        }
    }

    private fun findBottomNavigationBounds(
        node: AccessibilityNodeInfo,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0

        while (current != null && depth < 7) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)
            val minHeight = (40 * density).toInt()
            val maxHeight = (150 * density).toInt()
            val looksLikeBottomNavigation =
                bounds.width() >= screenWidth * 0.78f &&
                    bounds.height() in minHeight..maxHeight &&
                    bounds.top >= screenHeight * 0.62f &&
                    bounds.bottom >= screenHeight * 0.86f

            if (looksLikeBottomNavigation) return bounds
            current = current.parent
            depth += 1
        }
        return null
    }

    private fun isSelectedInPath(
        node: AccessibilityNodeInfo,
        navigationBounds: Rect
    ): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)
            if (current.isSelected || current.isChecked) {
                if (navigationBounds.contains(bounds) || bounds.intersect(navigationBounds)) {
                    return true
                }
            }
            current = current.parent
            depth += 1
        }
        return false
    }

    private fun bottomTabCellBounds(
        node: AccessibilityNodeInfo,
        navigationBounds: Rect,
        screenWidth: Int,
        screenHeight: Int
    ): Rect {
        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)
        if (nodeBounds.isEmpty) return Rect()

        val centerX = nodeBounds.centerX().coerceIn(0, screenWidth)
        val cellWidth = (screenWidth / 5f).toInt().coerceAtLeast(1)
        val left = (centerX - cellWidth / 2).coerceAtLeast(0)
        val right = (left + cellWidth).coerceAtMost(screenWidth)
        val top = navigationBounds.top.coerceAtLeast(0)
        val bottom = navigationBounds.bottom.coerceAtMost(screenHeight)
        return Rect(left, top, right, bottom)
    }

    private fun selectedFeedBounds(
        navigationBounds: Rect,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ): Rect {
        val top = (24 * density).toInt().coerceAtMost(screenHeight)
        val bottom = navigationBounds.top.coerceAtMost(screenHeight)
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

        val minRegionHeight = (120 * density).toInt()
        val maxRegionHeight = (screenHeight * 0.72f).toInt()
        var parent = node.parent
        var candidate: Rect? = null
        var level = 0

        while (parent != null && level < 6) {
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
            val horizontal = (if (packageName == YOUTUBE) 40 else 46) * density
            val vertical = (if (packageName == YOUTUBE) 28 else 40) * density
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
