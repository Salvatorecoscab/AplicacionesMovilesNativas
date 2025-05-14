package com.example.locationmaps

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.locationmaps.data.PointOfInterest
import com.example.locationmaps.viewmodel.LocationExplorerViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import coil.load

class PoiDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: LocationExplorerViewModel
    private var poiId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi_detail)

        // Configurar el toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Obtener el ID del POI desde el intent
        poiId = intent.getLongExtra("poi_id", -1)
        if (poiId == -1L) {
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[LocationExplorerViewModel::class.java]
        viewModel.getPoiById(poiId).observe(this) { poi ->
            poi?.let { updateUI(it) }
        }

        // Configurar botones
        findViewById<Button>(R.id.navigateToPoiButton).setOnClickListener {
            // Devolver resultado a MapActivity para navegar
            setResult(RESULT_OK, intent.apply {
                putExtra("navigate_to_poi", poiId)
            })
            finish()
        }

        findViewById<Button>(R.id.editPoiButton).setOnClickListener {
            // Implementar edición en una mejora futura
        }
    }

    private fun updateUI(poi: PointOfInterest) {
        // Actualizar título
        supportActionBar?.title = poi.name

        // Actualizar campos
        findViewById<TextView>(R.id.poiNameTextView).text = poi.name
        findViewById<TextView>(R.id.poiCategoryTextView).text = poi.category
        findViewById<TextView>(R.id.poiDescriptionTextView).text = poi.description

        // Formato de fecha
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        findViewById<TextView>(R.id.poiDateTextView).text = "Añadido: ${dateFormat.format(poi.dateAdded)}"

        // Coordenadas
        findViewById<TextView>(R.id.poiLocationTextView).text =
            "Lat: ${poi.latitude.format(6)}, Lng: ${poi.longitude.format(6)}"

        // Cargar imagen si existe
        val photoImageView = findViewById<ImageView>(R.id.poiPhotoImageView)
        poi.photoPath?.let { photoPath ->
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                photoImageView.visibility = View.VISIBLE
                photoImageView.load(photoFile) {
                    crossfade(true)
                    placeholder(R.drawable.ic_photo_placeholder)
                    error(R.drawable.ic_photo_error)
                }

                // Permitir que la imagen se vea en pantalla completa al hacer click
                photoImageView.setOnClickListener {
                    showFullScreenImage(photoPath)
                }
            } else {
                photoImageView.visibility = View.GONE
            }
        } ?: run {
            photoImageView.visibility = View.GONE
        }
    }

    private fun showFullScreenImage(photoPath: String) {
        // Lanzar actividad de visor de imágenes
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtra("photo_path", photoPath)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Extensión para formatear doubles
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}