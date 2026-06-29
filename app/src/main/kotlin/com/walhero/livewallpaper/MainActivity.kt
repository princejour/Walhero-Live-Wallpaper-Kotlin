package com.walhero.livewallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        const val PREFS = "walhero_premium_wallpaper_prefs"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_AUDIO_ENABLED = "audio_enabled"
        const val KEY_TARGET = "target"
        const val TARGET_HOME = "HOME"
        const val TARGET_SCREEN = "SCREEN"
        const val TARGET_BOTH = "BOTH"
        private const val REQUEST_VIDEO = 4001

        fun getVideoUri(context: Context): String? {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_VIDEO_URI, null)
        }

        fun isAudioEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUDIO_ENABLED, false)
        }
    }

    private lateinit var statusText: TextView
    private lateinit var soundText: TextView
    private lateinit var targetText: TextView
    private lateinit var homeTarget: TextView
    private lateinit var screenTarget: TextView
    private lateinit var bothTarget: TextView
    private var selectedTarget: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        window.statusBarColor = Color.parseColor("#05060A")
        window.navigationBarColor = Color.parseColor("#05060A")
        selectedTarget = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TARGET, null)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#05060A"))
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(22), dp(34), dp(22), dp(22))
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val logo = TextView(this)
        logo.text = "WALHERO"
        logo.textSize = 30f
        logo.typeface = Typeface.DEFAULT_BOLD
        logo.setTextColor(Color.WHITE)
        logo.gravity = Gravity.CENTER
        root.addView(logo, fullWidth())

        val subtitle = TextView(this)
        subtitle.text = "Premium Video Live Wallpaper"
        subtitle.textSize = 15f
        subtitle.setTextColor(Color.parseColor("#C7C7D1"))
        subtitle.gravity = Gravity.CENTER
        root.addView(subtitle, fullWidth())

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(18), dp(18), dp(18), dp(18))
        card.background = rounded("#11131C", "#2B3040", 22)
        root.addView(card, cardParams())

        statusText = infoLine()
        soundText = infoLine()
        targetText = infoLine()
        card.addView(statusText, fullWidth())
        card.addView(soundText, fullWidth())
        card.addView(targetText, fullWidth())

        root.addView(actionButton("Choose video") { chooseVideo() }, fullWidth())
        root.addView(actionButton("Sound on / off") { toggleAudio() }, fullWidth())

        val targetTitle = TextView(this)
        targetTitle.text = "Choose target before applying"
        targetTitle.textSize = 16f
        targetTitle.typeface = Typeface.DEFAULT_BOLD
        targetTitle.setTextColor(Color.WHITE)
        targetTitle.gravity = Gravity.CENTER
        root.addView(targetTitle, fullWidth())

        homeTarget = targetButton("Home screen", TARGET_HOME)
        screenTarget = targetButton("Screen lock", TARGET_SCREEN)
        bothTarget = targetButton("Both screens", TARGET_BOTH)
        root.addView(homeTarget, fullWidth())
        root.addView(screenTarget, fullWidth())
        root.addView(bothTarget, fullWidth())

        root.addView(applyButton("Apply selected live wallpaper") { openWallpaperChooser() }, fullWidth())
        root.addView(secondaryButton("Clear video and target") { clearAll() }, fullWidth())

        val copyright = TextView(this)
        copyright.text = "© 2026 Walhero. All rights reserved."
        copyright.textSize = 12f
        copyright.setTextColor(Color.parseColor("#8C91A3"))
        copyright.gravity = Gravity.CENTER
        root.addView(copyright, fullWidth())

        setContentView(scroll)
        refreshStatus()
    }

    private fun chooseVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VIDEO || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
        }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_VIDEO_URI, uri.toString()).apply()
        refreshStatus()
        Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
    }

    private fun selectTarget(target: String) {
        selectedTarget = target
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TARGET, target).apply()
        refreshStatus()
    }

    private fun openWallpaperChooser() {
        if (getVideoUri(this) == null) {
            Toast.makeText(this, "Choose a video first", Toast.LENGTH_LONG).show()
            return
        }
        val target = selectedTarget
        if (target == null) {
            Toast.makeText(this, "Choose target first", Toast.LENGTH_LONG).show()
            return
        }
        val serviceClass = when (target) {
            TARGET_HOME -> HomeWallpaperService::class.java
            TARGET_SCREEN -> VideoWallpaperService::class.java
            TARGET_BOTH -> VideoWallpaperService::class.java
            else -> VideoWallpaperService::class.java
        }
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, serviceClass))
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun toggleAudio() {
        val next = !isAudioEnabled(this)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUDIO_ENABLED, next).apply()
        refreshStatus()
    }

    private fun clearAll() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        selectedTarget = null
        refreshStatus()
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus() {
        statusText.text = if (getVideoUri(this) == null) "Video: not selected" else "Video: ready"
        soundText.text = if (isAudioEnabled(this)) "Sound: enabled" else "Sound: disabled"
        targetText.text = "Target: ${selectedTarget ?: "not selected"}"
        setTargetStyle(homeTarget, selectedTarget == TARGET_HOME)
        setTargetStyle(screenTarget, selectedTarget == TARGET_SCREEN)
        setTargetStyle(bothTarget, selectedTarget == TARGET_BOTH)
    }

    private fun infoLine(): TextView {
        val text = TextView(this)
        text.textSize = 15f
        text.setTextColor(Color.parseColor("#E6E8F2"))
        text.gravity = Gravity.CENTER
        return text
    }

    private fun targetButton(label: String, target: String): TextView {
        val view = TextView(this)
        view.text = label
        view.textSize = 16f
        view.gravity = Gravity.CENTER
        view.setPadding(dp(16), dp(14), dp(16), dp(14))
        view.setOnClickListener { selectTarget(target) }
        return view
    }

    private fun setTargetStyle(view: TextView, selected: Boolean) {
        view.setTextColor(if (selected) Color.BLACK else Color.WHITE)
        view.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.background = if (selected) rounded("#D6B25E", "#FFE49A", 18) else rounded("#171A25", "#353A4E", 18)
    }

    private fun actionButton(text: String, action: () -> Unit): TextView {
        val view = baseButton(text)
        view.background = rounded("#191D2A", "#3D4357", 18)
        view.setOnClickListener { action() }
        return view
    }

    private fun applyButton(text: String, action: () -> Unit): TextView {
        val view = baseButton(text)
        view.setTextColor(Color.BLACK)
        view.typeface = Typeface.DEFAULT_BOLD
        view.background = rounded("#D6B25E", "#FFE49A", 18)
        view.setOnClickListener { action() }
        return view
    }

    private fun secondaryButton(text: String, action: () -> Unit): TextView {
        val view = baseButton(text)
        view.setTextColor(Color.parseColor("#E6E8F2"))
        view.background = rounded("#0E1018", "#2B3040", 18)
        view.setOnClickListener { action() }
        return view
    }

    private fun baseButton(text: String): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = 16f
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(16), dp(15), dp(16), dp(15))
        return view
    }

    private fun rounded(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = dp(radiusDp).toFloat()
        drawable.setColor(Color.parseColor(fill))
        drawable.setStroke(dp(1), Color.parseColor(stroke))
        return drawable
    }

    private fun fullWidth(): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(8), 0, dp(8))
        return params
    }

    private fun cardParams(): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, dp(18), 0, dp(18))
        return params
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
