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

    private lateinit var status: TextView
    private lateinit var audioStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Walhero Live Wallpaper"

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(24, 48, 24, 24)

        val titleText = TextView(this)
        titleText.text = "Walhero Live Wallpaper"
        titleText.textSize = 22f
        titleText.gravity = Gravity.CENTER
        root.addView(titleText, fullWidth())

        status = TextView(this)
        status.gravity = Gravity.CENTER
        root.addView(status, fullWidth())

        audioStatus = TextView(this)
        audioStatus.gravity = Gravity.CENTER
        root.addView(audioStatus, fullWidth())

        root.addView(button("Choose video") { chooseVideo() }, fullWidth())
        root.addView(button("Sound on / off") { toggleAudio() }, fullWidth())
        root.addView(button("Apply to home screen") { applyHomeWallpaper() }, fullWidth())
        root.addView(button("Apply to lock screen") { applyLockWallpaper() }, fullWidth())
        root.addView(button("Apply to both screens") { applyBothWallpapers() }, fullWidth())
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
            contentResolver.takePersistableUriPermission(
                uri,
                data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VIDEO_URI, uri.toString())
            .apply()
        refreshStatus()
        Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show()
    }

    private fun applyHomeWallpaper() {
        openWallpaperChooser(TargetRequest.HOME)
    }

    private fun applyLockWallpaper() {
        openWallpaperChooser(TargetRequest.LOCK)
    }

    private fun applyBothWallpapers() {
        openWallpaperChooser(TargetRequest.BOTH)
    }

    private fun openWallpaperChooser(target: String) {
        if (getVideoUri(this) == null) {
            Toast.makeText(this, "Choose a video first", Toast.LENGTH_LONG).show()
            return
        }
        TargetRequest.write(this, target)
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
        TargetRequest.clear(this)
        refreshStatus()
    }

    private fun refreshStatus() {
        status.text = if (getVideoUri(this) == null) "No video selected" else "Video ready"
        audioStatus.text = if (isAudioEnabled(this)) "Sound enabled" else "Sound disabled"
    }

    private fun button(text: String, action: () -> Unit): Button {
        val button = Button(this)
        button.text = text
        button.setAllCaps(false)
        button.setOnClickListener { action() }
        return button
    }

    private fun fullWidth(): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        return params
    }
}
