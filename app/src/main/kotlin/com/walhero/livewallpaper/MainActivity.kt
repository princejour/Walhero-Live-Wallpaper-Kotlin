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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        const val PREFS = "walhero_kotlin_prefs"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_AUDIO_ENABLED = "audio_enabled"
        private const val REQUEST_VIDEO = 4001

        fun getVideoUri(context: Context): String? {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_VIDEO_URI, null)
        }

        fun isAudioEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUDIO_ENABLED, false)
        }
    }

    private lateinit var statusText: TextView
    private lateinit var audioStatusText: TextView
    private lateinit var chooseButton: Button
    private lateinit var soundButton: Button
    private lateinit var clearButton: Button

    private val bgTop = Color.rgb(8, 19, 37)
    private val bgBottom = Color.rgb(12, 31, 62)
    private val cardBg = Color.rgb(20, 40, 74)
    private val cardBorder = Color.rgb(33, 78, 134)
    private val accent = Color.rgb(30, 144, 255)
    private val accent2 = Color.rgb(77, 184, 255)
    private val textPrimary = Color.rgb(244, 249, 255)
    private val textSecondary = Color.rgb(175, 201, 232)
    private val danger = Color.rgb(204, 58, 82)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        window.statusBarColor = bgTop
        window.navigationBarColor = bgTop
        setContentView(buildPremiumUi())
        refreshStatus()
    }

    private fun buildPremiumUi(): ScrollView {
        val scroll = ScrollView(this)
        scroll.setBackground(gradient(bgTop, bgBottom, 0f))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(18), dp(20), dp(18), dp(18))
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val header = verticalCard()
        header.gravity = Gravity.CENTER
        header.setPadding(dp(18), dp(18), dp(18), dp(18))
        header.addView(text("◉", 42f, accent2, true, Gravity.CENTER))
        header.addView(text(getString(R.string.title_main), 24f, textPrimary, true, Gravity.CENTER))
        header.addView(text(getString(R.string.subtitle_main), 13f, textSecondary, false, Gravity.CENTER))
        root.addView(header, full(dp(0), dp(0), dp(0), dp(14)))

        val statusCard = verticalCard()
        statusCard.setPadding(dp(18), dp(18), dp(18), dp(18))
        statusCard.addView(text(getString(R.string.status_title), 16f, textPrimary, true, Gravity.START))
        statusText = text("", 18f, textPrimary, true, Gravity.START)
        audioStatusText = text("", 13f, textSecondary, false, Gravity.START)
        statusCard.addView(statusText, full(dp(0), dp(8), dp(0), dp(0)))
        statusCard.addView(audioStatusText, full(dp(0), dp(4), dp(0), dp(0)))
        root.addView(statusCard, full(dp(0), dp(0), dp(0), dp(14)))

        chooseButton = button(getString(R.string.choose_video), true)
        chooseButton.setOnClickListener { chooseVideo() }
        root.addView(chooseButton, full(dp(0), dp(0), dp(0), dp(12), dp(54)))

        val applyButton = button(getString(R.string.apply_wallpaper), true)
        applyButton.setOnClickListener { applyLiveWallpaper() }
        root.addView(applyButton, full(dp(0), dp(0), dp(0), dp(12), dp(58)))

        soundButton = button(getString(R.string.enable_sound), false)
        soundButton.setOnClickListener { toggleAudio() }
        root.addView(soundButton, full(dp(0), dp(0), dp(0), dp(12), dp(54)))

        clearButton = button(getString(R.string.clear_video), false)
        clearButton.background = solid(danger, dp(14).toFloat(), danger)
        clearButton.setOnClickListener { clearVideo() }
        root.addView(clearButton, full(dp(0), dp(0), dp(0), dp(14), dp(54)))

        val noteCard = verticalCard()
        noteCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        noteCard.addView(text(getString(R.string.note_title), 15f, textPrimary, true, Gravity.START))
        noteCard.addView(text(getString(R.string.system_note), 13f, textSecondary, false, Gravity.START), full(dp(0), dp(8), dp(0), dp(0)))
        root.addView(noteCard, full(dp(0), dp(0), dp(0), dp(18)))

        val footer = text("© WBJ", 16f, accent2, true, Gravity.CENTER)
        footer.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        footer.setShadowLayer(10f, 0f, 0f, accent)
        root.addView(footer, full(dp(0), dp(2), dp(0), dp(10)))

        return scroll
    }

    private fun chooseVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VIDEO || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VIDEO_URI, uri.toString())
            .apply()
        Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun applyLiveWallpaper() {
        if (getVideoUri(this) == null) {
            Toast.makeText(this, "Choose a video first", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, VideoWallpaperService::class.java)
        )
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (_: Exception) {
                Toast.makeText(this, "Open system wallpaper settings and choose Walhero Live Wallpaper", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleAudio() {
        val next = !isAudioEnabled(this)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUDIO_ENABLED, next)
            .apply()
        refreshStatus()
    }

    private fun clearVideo() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_VIDEO_URI)
            .remove(KEY_AUDIO_ENABLED)
            .apply()
        Toast.makeText(this, "Video cleared", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun refreshStatus() {
        val hasVideo = getVideoUri(this) != null
        val soundEnabled = isAudioEnabled(this)
        statusText.text = if (hasVideo) getString(R.string.video_ready) else getString(R.string.no_video)
        audioStatusText.text = if (soundEnabled) getString(R.string.sound_enabled) else getString(R.string.sound_disabled)
        chooseButton.text = if (hasVideo) getString(R.string.change_video) else getString(R.string.choose_video)
        soundButton.text = if (soundEnabled) getString(R.string.disable_sound) else getString(R.string.enable_sound)
        clearButton.isEnabled = hasVideo
        clearButton.alpha = if (hasVideo) 1f else 0.45f
    }

    private fun verticalCard(): LinearLayout {
        val view = LinearLayout(this)
        view.orientation = LinearLayout.VERTICAL
        view.background = solid(cardBg, dp(18).toFloat(), cardBorder)
        return view
    }

    private fun button(label: String, primary: Boolean): Button {
        val b = Button(this)
        b.text = label
        b.textSize = 16f
        b.setTextColor(textPrimary)
        b.setAllCaps(false)
        b.typeface = Typeface.DEFAULT_BOLD
        b.background = if (primary) gradient(accent, accent2, dp(14).toFloat()) else solid(Color.rgb(13, 27, 46), dp(14).toFloat(), cardBorder)
        return b
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean, gravity: Int): TextView {
        val t = TextView(this)
        t.text = value
        t.textSize = size
        t.setTextColor(color)
        t.gravity = gravity
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun gradient(start: Int, end: Int, radius: Float): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end))
        g.cornerRadius = radius
        return g
    }

    private fun solid(color: Int, radius: Float, stroke: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = radius
        g.setStroke(dp(1), stroke)
        return g
    }

    private fun full(left: Int, top: Int, right: Int, bottom: Int, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        p.setMargins(left, top, right, bottom)
        return p
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
