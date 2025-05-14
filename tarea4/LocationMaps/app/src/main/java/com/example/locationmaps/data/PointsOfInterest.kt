package com.example.locationmaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "points_of_interest")
data class PointOfInterest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val photo: ByteArray? = null,  // Cambiar photoPath a photo
    val notes: String? = null,
    val dateAdded: Date = Date(),
    val isVisited: Boolean = false,
    val photoPath: String? = null
) {
    // Importante: implementar equals y hashCode para ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PointOfInterest

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (category != other.category) return false
        if (photo != null) {
            if (other.photo == null) return false
            if (!photo.contentEquals(other.photo)) return false
        } else if (other.photo != null) return false
        if (notes != other.notes) return false
        if (dateAdded != other.dateAdded) return false
        if (isVisited != other.isVisited) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + isVisited.hashCode()
        return result
    }
}