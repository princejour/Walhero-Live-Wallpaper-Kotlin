package com.walhero.livewallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var player: MediaPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private var isVisibleNow = false
        private var isPrepared = false
        private var currentFlags = 0
        private val prefs: SharedPreferences = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)

        init {
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            currentFlags = readWallpaperFlags()
            reload()
        }

        override fun onWallpaperFlagsChanged(which: Int) {
            super.onWallpaperFlagsChanged(which)
            currentFlags = which
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
            if (key == MainActivity.KEY_VIDEO_URI || key == MainActivity.KEY_AUDIO_ENABLED || key == TargetRequest.KEY_TARGET) {
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
            val videoUri = resolveVideoForThisEngine()
            if (videoUri == null) {
                releasePlayer()
                return
            }
            startVideo(holder, videoUri)
        }

        private fun resolveVideoForThisEngine(): String? {
            val video = MainActivity.getVideoUri(applicationContext) ?: return null
            val target = TargetRequest.read(applicationContext)
            val flags = readWallpaperFlags()
            val homeEngine = flags and WallpaperManager.FLAG_SYSTEM != 0
            val lockEngine = flags and WallpaperManager.FLAG_LOCK != 0

            return when (target) {
                TargetRequest.HOME -> if (lockEngine && !homeEngine) null else video
                TargetRequest.LOCK -> if (homeEngine && !lockEngine) null else video
                TargetRequest.BOTH -> video
                else -> video
            }
        }

        private fun readWallpaperFlags(): Int {
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    return getWallpaperFlags()
                } catch (_: Exception) {
                }
            }
            return currentFlags
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
