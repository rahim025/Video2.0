package com.example.lockvideo

import android.app.KeyguardManager
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
        private var prepared = false
        private var surfaceReady = false
        private var stamp = 0L

        private val videoFile get() = File(filesDir, "lock.mp4")
        private val imageFile get() = File(filesDir, "home.img")

        // Vidéo si écran verrouillé, image fixe si accueil.
        // Si le système ne distingue pas clairement, on regarde si le téléphone est verrouillé.
        private fun wantVideo(): Boolean {
            val f = wallpaperFlags
            val lock = (f and WallpaperManager.FLAG_LOCK) != 0
            val sys = (f and WallpaperManager.FLAG_SYSTEM) != 0
            return when {
                lock && !sys -> true
                sys && !lock -> false
                else -> (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            refresh()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            refresh()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) refresh()
            else if (prepared) player?.pause()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        private fun refresh() {
            if (!surfaceReady) return
            if (wantVideo()) {
                if (player != null && videoFile.lastModified() == stamp) {
                    if (prepared) player?.start()
                } else {
                    releasePlayer()
                    startVideo()
                }
            } else {
                releasePlayer()
                drawHome(surfaceHolder)
            }
        }

        private fun startVideo() {
            if (!videoFile.exists()) return
            stamp = videoFile.lastModified()
            prepared = false
            try {
                player = MediaPlayer().apply {
                    setSurface(surfaceHolder.surface)
                    setDataSource(videoFile.path)
                    isLooping = true
                    setVolume(0f, 0f)
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    setOnPreparedListener {
                        prepared = true
                        it.start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            } catch (e: Exception) {
                releasePlayer()
            }
        }

        private fun releasePlayer() {
            prepared = false
            try { player?.release() } catch (_: Exception) {}
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
