package com.walhero.livewallpaper

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

open class BaseVideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = object : Engine() {
        private var player: MediaPlayer? = null
        private var holderRef: SurfaceHolder? = null
        private var visibleNow = false
        override fun onSurfaceCreated(holder: SurfaceHolder) { super.onSurfaceCreated(holder); holderRef = holder; start() }
        override fun onVisibilityChanged(visible: Boolean) { super.onVisibilityChanged(visible); visibleNow = visible; if (visible) player?.start() else player?.pause() }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { super.onSurfaceDestroyed(holder); stop() }
        override fun onDestroy() { stop(); super.onDestroy() }
        private fun start() {
            val holder = holderRef ?: return
            val uriText = MainActivity.getVideoUri(applicationContext) ?: return
            stop()
            try {
                player = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(uriText))
                    setSurface(holder.surface)
                    isLooping = true
                    val volume = if (MainActivity.isAudioEnabled(applicationContext)) 1f else 0f
                    setVolume(volume, volume)
                    setOnPreparedListener { if (visibleNow) it.start() }
                    setOnErrorListener { _, _, _ -> stop(); true }
                    prepareAsync()
                }
            } catch (_: Exception) { stop() }
        }
        private fun stop() {
            val p = player
            player = null
            if (p != null) { try { p.stop() } catch (_: Exception) {}; try { p.release() } catch (_: Exception) {} }
        }
    }
}
