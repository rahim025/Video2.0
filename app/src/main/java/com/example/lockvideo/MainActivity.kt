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
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 120, 48, 48)

        addButton(layout, "1. Choisir la vidéo (écran verrouillé)") { pick("video/*", 1) }
        addButton(layout, "2. Choisir l'image de l'écran d'accueil") { pick("image/*", 2) }
        addButton(layout, "3. Appliquer le fond d'écran") { applyWallpaper() }
        setContentView(layout)
    }

    private fun addButton(layout: LinearLayout, label: String, action: () -> Unit) {
        val b = Button(this)
        b.text = label
        b.setOnClickListener {
            try {
                action()
            } catch (e: Exception) {
                toast("Erreur : " + e.message)
            }
        }
        layout.addView(b)
    }

    private fun tryStart(i: Intent): Boolean {
        return try {
            startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun applyWallpaper() {
        val component = ComponentName(this, VideoWallpaperService::class.java)
        // 1. Méthode standard
        if (tryStart(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            )
        ) return
        // 2. Sélecteur Android de base
        if (tryStart(
                Intent().setClassName(
                    "com.android.wallpaper.livepicker",
                    "com.android.wallpaper.livepicker.LiveWallpaperActivity"
                )
            )
        ) return
        // 3. Liste des fonds d'écran animés
        if (tryStart(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) return
        // 4. Choix général du fond d'écran
        if (tryStart(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Fond d'écran"))) return
        // 5. Réglages d'affichage
        toast("Ouvre Paramètres > Fond d'écran > Fonds d'écran animés > Vidéo écran verrouillé")
        tryStart(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))
    }

    private fun pick(type: String, code: Int) {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = type
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(i, code)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val target = File(filesDir, if (requestCode == 1) "lock.mp4" else "home.img")
        Thread {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                runOnUiThread { toast("Enregistré ✓") }
            } catch (e: Exception) {
                runOnUiThread { toast("Erreur copie : " + e.message) }
            }
        }.start()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
