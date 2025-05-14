package com.example.locationmapapp

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase para medir y registrar métricas de rendimiento
 */
class PerformanceMetrics private constructor() {
    companion object {
        private val metrics = mutableMapOf<String, MutableList<Long>>()

        /**
         * Registra una métrica con un nombre y valor
         */
        fun recordMetric(name: String, value: Long) {
            if (value < 0) {
                println("Advertencia: Se intentó registrar un valor negativo para la métrica $name")
                return
            }
            metrics.getOrPut(name) { mutableListOf() }.add(value)
            println("MÉTRICA - $name: $value")
        }

        /**
         * Obtiene un resumen de todas las métricas registradas
         */
        fun getMetricsSummary(): Map<String, Map<String, Long>> {
            val summary = mutableMapOf<String, Map<String, Long>>()

            metrics.forEach { (name, values) ->
                val metricSummary = mutableMapOf<String, Long>()

                if (values.isNotEmpty()) {
                    metricSummary["min"] = (values.minOrNull() ?: 0).toLong()
                    metricSummary["max"] = (values.maxOrNull() ?: 0).toLong()
                    metricSummary["avg"] = (values.sum() / values.size).toLong()
                    metricSummary["count"] = values.size.toLong()
                } else {
                    metricSummary["min"] = 0
                    metricSummary["max"] = 0
                    metricSummary["avg"] = 0
                    metricSummary["count"] = 0
                }

                summary[name] = metricSummary
            }

            return summary
        }
        /**
         * Exporta todas las métricas a un archivo CSV
         */
        fun exportMetricsToFile(context: Context): String {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "performance_metrics_$timestamp.csv"

            val content = StringBuilder()

            // Información contextual
            content.append("Export Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

            // Encabezado del CSV
            content.append("Metric,Min,Max,Average,Count\n")

            val summary = getMetricsSummary()
            summary.forEach { (name, metrics) ->
                content.append("\"$name\",\"${metrics["min"] ?: 0}\",\"${metrics["max"] ?: 0}\",\"${metrics["avg"] ?: 0}\",\"${metrics["count"] ?: 0}\"\n")
            }

            // Añadir información del dispositivo
            content.append("\nDevice Info\n")
            content.append("Model,${Build.MODEL}\n")
            content.append("Android Version,${Build.VERSION.RELEASE}\n")
            content.append("SDK Level,${Build.VERSION.SDK_INT}\n")

            return try {
                val file = File(context.getExternalFilesDir(null), fileName)
                FileOutputStream(file).use {
                    it.write(content.toString().toByteArray())
                }
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                "Error al exportar métricas: ${e.message}"
            }
        }

        /**
         * Resetea todas las métricas registradas
         */
        fun resetMetrics() {
            metrics.clear()
        }
    }
}