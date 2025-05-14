package com.example.locationmaps

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locationmaps.adapters.ExploredZoneAdapter
import com.example.locationmaps.data.ExploredZone
import com.example.locationmaps.viewmodel.LocationExplorerViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ZonesActivity : AppCompatActivity() {

    private lateinit var viewModel: LocationExplorerViewModel
    private lateinit var adapter: ExploredZoneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zones)

        viewModel = ViewModelProvider(this)[LocationExplorerViewModel::class.java]

        val recyclerView: RecyclerView = findViewById(R.id.zonesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ExploredZoneAdapter(
            zones = emptyList(),
            onZoneClicked = { zone ->
                // Regresar a la actividad de mapa con las coordenadas de la zona
                val resultIntent = intent
                resultIntent.putExtra("zoneLat", zone.centerLatitude)
                resultIntent.putExtra("zoneLng", zone.centerLongitude)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        )
        recyclerView.adapter = adapter

        viewModel.allZones.observe(this) { zones ->
            adapter.updateZones(zones)
            updateExplorationProgress(zones)
        }

        findViewById<FloatingActionButton>(R.id.addZoneButton).setOnClickListener {
            showAddZoneDialog()
        }
    }

    private fun updateExplorationProgress(zones: List<ExploredZone>) {
        val totalZones = zones.size
        val exploredZones = zones.count { it.isExplored }

        val progressBar = findViewById<android.widget.ProgressBar>(R.id.zonesProgressBar)
        val progressText = findViewById<android.widget.TextView>(R.id.zonesProgressText)

        if (totalZones > 0) {
            val progressPercentage = (exploredZones * 100) / totalZones
            progressBar.progress = progressPercentage
            progressText.text = "$progressPercentage% explorado ($exploredZones/$totalZones)"
        } else {
            progressBar.progress = 0
            progressText.text = "0% explorado (0/0)"
        }
    }

    private fun showAddZoneDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_zone, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Añadir Zona para Explorar")
            .setView(dialogView)
            .setPositiveButton("Guardar", null) // Establecemos null para manejar el clic manualmente
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        // Obtenemos el botón positivo después de mostrar el diálogo
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nameEditText = dialogView.findViewById<EditText>(R.id.zoneNameEditText)
            val latitudeEditText = dialogView.findViewById<EditText>(R.id.zoneLatitudeEditText)
            val longitudeEditText = dialogView.findViewById<EditText>(R.id.zoneLongitudeEditText)
            val radiusEditText = dialogView.findViewById<EditText>(R.id.zoneRadiusEditText)

            val name = nameEditText.text.toString()
            val latitudeStr = latitudeEditText.text.toString()
            val longitudeStr = longitudeEditText.text.toString()
            val radiusStr = radiusEditText.text.toString()

            if (name.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty() || radiusStr.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val latitude = latitudeStr.toDouble()
                val longitude = longitudeStr.toDouble()
                val radius = radiusStr.toFloat()

                val zone = ExploredZone(
                    name = name,
                    centerLatitude = latitude,
                    centerLongitude = longitude,
                    radius = radius,
                    isExplored = false
                )

                viewModel.insertZone(zone)
                dialog.dismiss()
                Toast.makeText(this, "Zona añadida correctamente", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Valores numéricos no válidos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}