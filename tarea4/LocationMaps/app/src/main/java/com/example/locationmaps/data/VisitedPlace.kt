package com.example.locationmaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.locationmaps.utils.DateConverter
import java.util.Date

/**
 * Entidad que representa un lugar visitado por el usuario.
 * Cada vez que el usuario visita una ubicación, se registra
 * un objeto VisitedPlace con las coordenadas y la fecha/hora.
 * Esto permite seguir la ruta del usuario y calcular métricas
 * como distancia recorrida, zonas exploradas, etc.
 */
@Entity(tableName = "visited_places")
@TypeConverters(DateConverter::class)
data class VisitedPlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Date = Date(), // Fecha y hora de la visita
    val accuracy: Float? = null, // Precisión de la ubicación en metros (opcional)
    val altitude: Double? = null, // Altitud (opcional)
    val speed: Float? = null, // Velocidad en m/s (opcional)
    val bearing: Float? = null, // Dirección en grados (opcional)
    val poiId: Long? = null, // ID del punto de interés asociado (si existe)
    val zoneId: Long? = null, // ID de la zona explorada asociada (si existe)
    val notes: String? = null // Notas adicionales (opcional)
)