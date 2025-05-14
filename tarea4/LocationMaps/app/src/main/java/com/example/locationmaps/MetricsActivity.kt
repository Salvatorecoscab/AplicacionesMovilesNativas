package com.example.locationmaps

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MetricsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metrics)

        // Recupera datos de SharedPreferences en lugar de Intent extras
        val sharedPreferences = getSharedPreferences("MapMetrics", MODE_PRIVATE)

        val osmLoadTime = sharedPreferences.getLong("osmLoadTime", 0)
        val osmMemoryUsage = sharedPreferences.getLong("osmMemoryUsage", 0)
        val googleLoadTime = sharedPreferences.getLong("googleLoadTime", 0)
        val googleMemoryUsage = sharedPreferences.getLong("googleMemoryUsage", 0)

        // Logs para depuración
        Log.d("MetricsDebug", "OSM Load Time: $osmLoadTime ms")
        Log.d("MetricsDebug", "OSM Memory Usage: $osmMemoryUsage bytes")
        Log.d("MetricsDebug", "Google Load Time: $googleLoadTime ms")
        Log.d("MetricsDebug", "Google Memory Usage: $googleMemoryUsage bytes")

        // Vincula los TextViews
        val osmMetricsTextView: TextView = findViewById(R.id.osmMetricsTextView)
        val googleMetricsTextView: TextView = findViewById(R.id.googleMetricsTextView)

        // Muestra las métricas
        osmMetricsTextView.text = """
        OpenStreetMap Metrics:
        - Tiempo de carga: $osmLoadTime ms
        - Uso de memoria: $osmMemoryUsage bytes
    """.trimIndent()

        googleMetricsTextView.text = """
        Google Maps Metrics:
        - Tiempo de carga: $googleLoadTime ms
        - Uso de memoria: $googleMemoryUsage bytes
    """.trimIndent()
    }
}