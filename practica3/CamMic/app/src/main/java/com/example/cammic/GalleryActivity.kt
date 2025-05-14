package com.example.cammic // Asegúrate que el paquete sea el correcto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // O tu BaseActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

// class GalleryActivity : BaseActivity() {
class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerViewGallery: RecyclerView
    private lateinit var textViewEmptyGallery: TextView
    private lateinit var galleryAdapter: GalleryAdapter
    private val imageList = mutableListOf<Uri>()

    // Launcher para refrescar la galería si EditImageActivity hizo cambios
    // (Opcional, onResume() también puede funcionar pero esto es más específico)
    private val editImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // No necesitamos el resultado específico, pero el hecho de que vuelva aquí
        // es una señal para recargar, o podríamos pasar un booleano indicando si se guardó.
        // Por ahora, onResume() se encargará de recargar.
        Log.d("GalleryActivity", "Regresando de EditImageActivity, resultado: ${result.resultCode}")
        // loadImagesFromStorage() // Podrías recargar aquí si pasas un resultado de EditActivity
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        recyclerViewGallery = findViewById(R.id.recyclerViewGallery)
        textViewEmptyGallery = findViewById(R.id.textViewEmptyGallery)

        setupRecyclerView()
        // No cargamos imágenes aquí, lo haremos en onResume
    }

    override fun onResume() {
        super.onResume()
        // Cargar/Recargar imágenes cada vez que la actividad se vuelve visible
        // Esto asegura que los nuevos guardados o eliminaciones (si los implementas) se reflejen
        loadImagesFromStorage()
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(this, imageList) { imageUri ->
            // Acción al hacer clic en una imagen: abrir EditImageActivity
            val intent = Intent(this, EditImageActivity::class.java).apply {
                putExtra(EditImageActivity.EXTRA_IMAGE_URI, imageUri.toString())
            }
            // Usamos el launcher si queremos un resultado específico, si no, startActivity normal
            // startActivity(intent)
            editImageLauncher.launch(intent)
        }
        recyclerViewGallery.adapter = galleryAdapter
        recyclerViewGallery.layoutManager = GridLayoutManager(this, 3) // 3 columnas
    }

    private fun loadImagesFromStorage() {
        imageList.clear()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && storageDir.exists() && storageDir.isDirectory) {
            val files = storageDir.listFiles { file ->
                // Filtrar por archivos que sean imágenes (puedes ser más específico)
                // y que hayan sido guardados por EditImageActivity o la cámara
                file.isFile && (file.name.startsWith("edited_image_") || file.name.startsWith("JPEG_")) &&
                        (file.extension.equals("jpg", ignoreCase = true) ||
                                file.extension.equals("jpeg", ignoreCase = true) ||
                                file.extension.equals("png", ignoreCase = true))
            }

            files?.sortByDescending { it.lastModified() } // Mostrar las más recientes primero

            files?.forEach { file ->
                // Para pasar a EditImageActivity, necesitamos la URI original del archivo.
                // Uri.fromFile() es suficiente ya que EditImageActivity está en nuestra app
                // y puede manejar uris file://
                imageList.add(Uri.fromFile(file))
            }
        }

        galleryAdapter.updateImages(imageList)

        if (imageList.isEmpty()) {
            textViewEmptyGallery.visibility = View.VISIBLE
            recyclerViewGallery.visibility = View.GONE
        } else {
            textViewEmptyGallery.visibility = View.GONE
            recyclerViewGallery.visibility = View.VISIBLE
        }
        Log.d("GalleryActivity", "Imágenes cargadas: ${imageList.size}")
    }
}
