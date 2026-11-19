package com.example.lockvideo

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 120, 48, 48)
        }
        fun button(label: String, action: () -> Unit) {
            layout.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }
        button("1. Choisir la vidéo (écran verrouillé)") { pick("video/*", 1) }
        button("2. Choisir l'image de l'écran d'accueil") { pick("image/*", 2) }
        button("3. Appliquer le fond d'écran") {
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this, VideoWallpaperService::class.java)
                )
            )
        }
        setContentView(layout)
    }

    private fun pick(type: String, code: Int) {
        startActivityForResult(
            Intent(Intent.ACTION_GET_CONTENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE), code
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        val target = File(filesDir, if (requestCode == 1) "lock.mp4" else "home.img")
        Thread {
            contentResolver.openInputStream(uri)?.use { i -> target.outputStream().use { o -> i.copyTo(o) } }
            runOnUiThread { Toast.makeText(this, "Enregistré ✓", Toast.LENGTH_SHORT).show() }
        }.start()
    }
}
