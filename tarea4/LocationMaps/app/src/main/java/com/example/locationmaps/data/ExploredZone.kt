package com.example.locationmaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.locationmaps.utils.DateConverter
import java.util.Date

/**
 * Entidad que representa una zona geográfica que puede ser explorada.
 * Una zona tiene un centro definido por coordenadas y un radio en metros.
 * Cuando el usuario se encuentra físicamente dentro de este radio, la zona
 * se marca como explorada.
 */
@Entity(tableName = "explored_zones")
@TypeConverters(DateConverter::class)
data class ExploredZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radius: Float, // Radio en metros
    val isExplored: Boolean = false, // Si la zona ha sido visitada
    val dateExplored: Date? = null, // Fecha de exploración
    val description: String? = null, // Descripción opcional de la zona
    val difficulty: Int = 1, // Nivel de dificultad (1-5)
    val points: Int = 10, // Puntos que otorga al explorar
    val dateCreated: Date = Date() // Fecha de creación
)