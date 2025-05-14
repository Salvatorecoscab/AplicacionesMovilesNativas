package com.example.locationmapapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PerformanceResultsActivity : AppCompatActivity() {

    private lateinit var metricsTable: TableLayout
    private lateinit var btnExport: Button

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportMetrics()
        } else {
            Toast.makeText(
                this,
                "Permiso de almacenamiento requerido para exportar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_results)

        metricsTable = findViewById(R.id.metricsTable)
        btnExport = findViewById(R.id.btnExport)

        // Obtener y mostrar métricas
        displayMetrics()

        // Configurar botón de exportación
        btnExport.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                exportMetrics()
            }
        }
    }

    private fun displayMetrics() {
        val metrics = PerformanceMetrics.getMetricsSummary()

        // Para cada métrica, agregar una fila a la tabla
        metrics.forEach { (name, values) ->
            val row = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 8, 8, 8)
            }

            // Nombre de la métrica
            row.addView(TextView(this).apply {
                text = name
                setPadding(4, 4, 4, 4)
            })

            // Valores mínimo, máximo y promedio
            row.addView(TextView(this).apply {
                text = formatValue(values["min"] ?: 0)
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
            })

            row.addView(TextView(this).apply {
                text = formatValue(values["max"] ?: 0)
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
            })

            row.addView(TextView(this).apply {
                text = formatValue(values["avg"] ?: 0)
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
            })

            metricsTable.addView(row)
        }
    }

    private fun formatValue(value: Long): String {
        // Simplificar la visualización dependiendo del tipo de métrica
        return when {
            value > 1_000_000 -> String.format("%.1f MB", value / 1_000_000.0)
            value > 1_000 -> String.format("%.1f KB", value / 1_000.0)
            else -> "$value ms"
        }
    }

    private fun exportMetrics() {
        try {
            val filePath = PerformanceMetrics.exportMetricsToFile(this)
            Toast.makeText(
                this,
                "Métricas exportadas a: $filePath",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al exportar métricas: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}