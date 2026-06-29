package com.walhero.livewallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        const val PREFS = "walhero_live_wallpaper_prefs"
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
    private lateinit var soundText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(28, 56, 28, 28)

        val titleText = TextView(this)
        titleText.text = getString(R.string.app_name)
        titleText.textSize = 24f
        titleText.gravity = Gravity.CENTER
        root.addView(titleText, fullWidth())

        statusText = TextView(this)
        statusText.gravity = Gravity.CENTER
        statusText.textSize = 16f
        root.addView(statusText, fullWidth())

        soundText = TextView(this)
        soundText.gravity = Gravity.CENTER
        soundText.textSize = 16f
        root.addView(soundText, fullWidth())

        root.addView(button("Choose video") { chooseVideo() }, fullWidth())
        root.addView(button("Sound on / off") { toggleAudio() }, fullWidth())
        root.addView(button("Set live wallpaper") { openWallpaperChooser() }, fullWidth())
        root.addView(button("Clear video") { clearVideo() }, fullWidth())

        setContentView(root)
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

        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VIDEO_URI, uri.toString())
            .apply()

        refreshStatus()
        Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
    }

    private fun openWallpaperChooser() {
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
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
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
        refreshStatus()
        Toast.makeText(this, "Video cleared", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus() {
        statusText.text = if (getVideoUri(this) == null) "No video selected" else "Video ready"
        soundText.text = if (isAudioEnabled(this)) "Sound enabled" else "Sound disabled"
    }

    private fun button(text: String, action: () -> Unit): Button {
        val button = Button(this)
        button.text = text
        button.textSize = 16f
        button.setAllCaps(false)
        button.setOnClickListener { action() }
        return button
    }

    private fun fullWidth(): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 10, 0, 10)
        return params
    }
}
