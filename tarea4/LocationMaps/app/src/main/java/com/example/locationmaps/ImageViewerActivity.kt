package com.example.locationmaps

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import coil.load

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Configurar el toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Foto de POI"

        // Obtener ruta de la foto
        val photoPath = intent.getStringExtra("photo_path")
        if (photoPath == null) {
            finish()
            return
        }

        // Configurar la vista de la imagen
        val photoImageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val photoFile = File(photoPath)

        if (photoFile.exists()) {
            photoImageView.load(photoFile) {
                crossfade(true)
                placeholder(R.drawable.ic_photo_placeholder)
                error(R.drawable.ic_photo_error)
            }
        } else {
            finish()
        }

        // Permitir el modo pantalla completa al hacer click
        photoImageView.setOnClickListener {
            toggleFullscreenMode()
        }
    }

    private var isInFullscreenMode = false

    private fun toggleFullscreenMode() {
        isInFullscreenMode = !isInFullscreenMode

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isInFullscreenMode) {
            // Ocultar UI del sistema
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            supportActionBar?.hide()
        } else {
            // Mostrar UI del sistema
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            supportActionBar?.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}