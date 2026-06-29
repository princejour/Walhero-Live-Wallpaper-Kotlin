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
        private var surfaceHolder: SurfaceHolder? = null
        private var isVisibleNow = false
        private var isPrepared = false
        private val prefs: SharedPreferences = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            reload()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleNow = visible
            val mediaPlayer = player
            if (mediaPlayer == null) {
                if (visible) reload()
                return
            }
            try {
                applyAudioState()
                if (visible && isPrepared && !mediaPlayer.isPlaying) mediaPlayer.start()
                if (!visible && mediaPlayer.isPlaying) mediaPlayer.pause()
            } catch (_: Exception) {
                releasePlayer()
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            if (key == MainActivity.KEY_VIDEO_URI || key == MainActivity.KEY_AUDIO_ENABLED) {
                reload()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            surfaceHolder = null
            releasePlayer()
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            releasePlayer()
            super.onDestroy()
        }

        private fun reload() {
            val holder = surfaceHolder ?: return
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
                val newPlayer = MediaPlayer()
                newPlayer.setDataSource(applicationContext, Uri.parse(uriText))
                newPlayer.setSurface(holder.surface)
                newPlayer.isLooping = true
                newPlayer.setOnPreparedListener { mp ->
                    isPrepared = true
                    applyAudioState()
                    if (isVisibleNow) mp.start()
                }
                newPlayer.setOnErrorListener { _, _, _ ->
                    releasePlayer()
                    true
                }
                player = newPlayer
                applyAudioState()
                newPlayer.prepareAsync()
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
            isPrepared = false
            val oldPlayer = player
            player = null
            if (oldPlayer != null) {
                try {
                    oldPlayer.stop()
                } catch (_: Exception) {
                }
                try {
                    oldPlayer.release()
                } catch (_: Exception) {
                }
            }
        }
    }
}
