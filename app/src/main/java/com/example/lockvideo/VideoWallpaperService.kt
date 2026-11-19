package com.example.lockvideo

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var player: MediaPlayer? = null
        private var stamp = 0L

        // Écran verrouillé => vidéo ; écran d'accueil => image fixe
        private val isLock get() = (wallpaperFlags and WallpaperManager.FLAG_LOCK) != 0
        private val videoFile get() = File(filesDir, "lock.mp4")
        private val imageFile get() = File(filesDir, "home.img")

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (isLock) startVideo() else drawHome(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            if (!isLock) drawHome(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (!isLock) return
            if (visible) {
                if (videoFile.lastModified() != stamp) { releasePlayer(); startVideo() }
                else player?.start()
            } else player?.pause()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        private fun startVideo() {
            if (!videoFile.exists()) return
            stamp = videoFile.lastModified()
            player = MediaPlayer().apply {
                setSurface(surfaceHolder.surface)
                setDataSource(videoFile.path)
                isLooping = true
                setVolume(0f, 0f)
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                setOnPreparedListener { if (isVisible) it.start() }
                prepareAsync()
            }
        }

        private fun releasePlayer() {
            player?.release()
            player = null
        }

        private fun drawHome(holder: SurfaceHolder) {
            val canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                if (imageFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(imageFile.path) ?: return
                    val s = maxOf(canvas.width / bmp.width.toFloat(), canvas.height / bmp.height.toFloat())
                    canvas.translate((canvas.width - bmp.width * s) / 2, (canvas.height - bmp.height * s) / 2)
                    canvas.scale(s, s)
                    canvas.drawBitmap(bmp, 0f, 0f, null)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
