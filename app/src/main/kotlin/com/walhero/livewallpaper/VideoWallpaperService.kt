package com.walhero.livewallpaper

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var player: MediaPlayer? = null
        private var holderRef: SurfaceHolder? = null
        private var visibleNow = false
        private var prepared = false
        private val prefs: SharedPreferences = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holderRef = holder
            reload()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            visibleNow = visible
            val mediaPlayer = player
            if (mediaPlayer == null) {
                if (visible) reload()
                return
            }

            try {
                applyAudioState()
                if (visible && prepared && !mediaPlayer.isPlaying) mediaPlayer.start()
                if (!visible && mediaPlayer.isPlaying) mediaPlayer.pause()
            } catch (_: Exception) {
                releasePlayer()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            holderRef = null
            releasePlayer()
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            if (key == MainActivity.KEY_VIDEO_URI || key == MainActivity.KEY_AUDIO_ENABLED) {
                reload()
            }
        }

        private fun reload() {
            val holder = holderRef ?: return
            val videoUri = MainActivity.getVideoUri(applicationContext)
            if (videoUri == null) {
                releasePlayer()
                return
            }
            startVideo(holder, videoUri)
        }

        private fun startVideo(holder: SurfaceHolder, uriText: String) {
            releasePlayer()
            try {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(applicationContext, Uri.parse(uriText))
                mediaPlayer.setSurface(holder.surface)
                mediaPlayer.isLooping = true
                mediaPlayer.setOnPreparedListener { preparedPlayer ->
                    prepared = true
                    applyAudioState()
                    if (visibleNow) preparedPlayer.start()
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    true
                }
                player = mediaPlayer
                applyAudioState()
                mediaPlayer.prepareAsync()
            } catch (_: Exception) {
                releasePlayer()
            }
        }

        private fun applyAudioState() {
            val mediaPlayer = player ?: return
            val volume = if (MainActivity.isAudioEnabled(applicationContext)) 1f else 0f
            try {
                mediaPlayer.setVolume(volume, volume)
            } catch (_: Exception) {
            }
        }

        private fun releasePlayer() {
            prepared = false
            val mediaPlayer = player
            player = null
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop()
                } catch (_: Exception) {
                }
                try {
                    mediaPlayer.release()
                } catch (_: Exception) {
                }
            }
        }
    }
}
