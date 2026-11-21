package com.connectnexdesh.shortsblocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences(BlockerPreferences.FILE_NAME, MODE_PRIVATE)
    }

    private lateinit var serviceStatus: TextView
    private lateinit var masterSwitch: Switch
    private val appSwitches = mutableListOf<Switch>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(245, 247, 251))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        val title = TextView(this).apply {
            text = "Shorts Blocker"
            textSize = 28f
            setTextColor(Color.rgb(20, 28, 45))
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        page.addView(title)

        val subtitle = TextView(this).apply {
            text = "Keep the social apps. Hide the endless scroll."
            textSize = 14f
            setTextColor(Color.rgb(91, 101, 119))
            setPadding(0, dp(7), 0, dp(22))
        }
        page.addView(subtitle)

        val statusCard = card()
        serviceStatus = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(50, 60, 80))
            setPadding(dp(15), dp(14), dp(15), dp(14))
        }
        statusCard.addView(serviceStatus)
        page.addView(statusCard)

        val accessButton = Button(this).apply {
            text = "Open Accessibility Settings"
            isAllCaps = false
            setOnClickListener { showAccessibilityDisclosure() }
        }
        page.addView(accessButton, marginParams(top = 10, bottom = 18))

        val controlsCard = card()
        val sectionTitle = TextView(this).apply {
            text = "Blocking controls"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(20, 28, 45))
            setPadding(dp(15), dp(16), dp(15), dp(8))
        }
        controlsCard.addView(sectionTitle)

        masterSwitch = addSwitchRow(
            controlsCard,
            "Enable blocker",
            "Turn all filtering on or off",
            BlockerPreferences.MASTER,
            true
        )

        addDivider(controlsCard)

        val youtubeSwitch = addSwitchRow(
            controlsCard,
            "YouTube Shorts",
            "Hide explicitly labelled Shorts controls and shelves",
            BlockerPreferences.YOUTUBE,
            true
        )
        val instagramSwitch = addSwitchRow(
            controlsCard,
            "Instagram Reels",
            "Hide explicitly labelled Reels controls and cards",
            BlockerPreferences.INSTAGRAM,
            true
        )
        val facebookSwitch = addSwitchRow(
            controlsCard,
            "Facebook Reels",
            "Hide explicitly labelled Reels controls and cards",
            BlockerPreferences.FACEBOOK,
            true
        )
        appSwitches.addAll(listOf(youtubeSwitch, instagramSwitch, facebookSwitch))

        masterSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(BlockerPreferences.MASTER, checked).apply()
            appSwitches.forEach { it.isEnabled = checked }
        }
        page.addView(controlsCard)

        val note = TextView(this).apply {
            text = "How it works\nThe service checks visible accessibility text in the selected apps for explicit “Shorts” or “Reels” labels, then covers matching screen areas. It does not modify those apps. Some feed items may not expose a detectable label, and covered areas can appear blank."
            textSize = 13f
            setTextColor(Color.rgb(91, 101, 119))
            setPadding(dp(2), dp(18), dp(2), dp(10))
        }
        page.addView(note)

        val privacy = TextView(this).apply {
            text = "Privacy: matching happens on-device. This prototype does not store or transmit screen content and does not request internet access."
            textSize = 12f
            setTextColor(Color.rgb(105, 115, 130))
        }
        page.addView(privacy)

        setContentView(scroll)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::serviceStatus.isInitialized) refreshStatus()
        if (::masterSwitch.isInitialized) {
            val enabled = prefs.getBoolean(BlockerPreferences.MASTER, true)
            masterSwitch.isChecked = enabled
            appSwitches.forEach { it.isEnabled = enabled }
        }
    }

    private fun showAccessibilityDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility permission")
            .setMessage(
                "Shorts Blocker uses Android Accessibility Service to inspect visible accessibility text in YouTube, Instagram, and Facebook while you use those apps. It looks for explicit Shorts/Reels labels and places a local overlay over matching areas, which also blocks taps there. Matching is performed on this device; this prototype does not record, store, or transmit screen content. Detection may miss items or cover the wrong area when social apps change their interface. You can turn the service off at any time in Android Settings."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }

    private fun refreshStatus() {
        val enabled = isAccessibilityServiceEnabled()
        serviceStatus.text = if (enabled) {
            "Accessibility service: ON\nThe blocker can inspect the supported apps while enabled."
        } else {
            "Accessibility service: OFF\nEnable Shorts Blocker in Android Accessibility Settings to start."
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(this, BlockerAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                info.resolveInfo.serviceInfo.packageName == expected.packageName &&
                    info.resolveInfo.serviceInfo.name == expected.className
            }
    }

    private fun addSwitchRow(
        parent: LinearLayout,
        heading: String,
        description: String,
        key: String,
        defaultValue: Boolean
    ): Switch {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(12))
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = heading
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(35, 45, 63))
        }
        val detail = TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(Color.rgb(105, 115, 130))
            setPadding(0, dp(4), dp(8), 0)
        }
        textColumn.addView(title)
        textColumn.addView(detail)
        row.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        }
        row.addView(toggle)
        parent.addView(row)
        return toggle
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(Color.rgb(232, 235, 241))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
            }
            elevation = dp(2).toFloat()
        }.also {
            it.layoutParams = marginParams(top = 6, bottom = 8)
        }
    }

    private fun marginParams(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

internal object BlockerPreferences {
    const val FILE_NAME = "shorts_blocker_settings"
    const val MASTER = "master_enabled"
    const val YOUTUBE = "youtube_enabled"
    const val INSTAGRAM = "instagram_enabled"
    const val FACEBOOK = "facebook_enabled"
}
