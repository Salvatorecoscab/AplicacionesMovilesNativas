package com.example.locationmaps.data

import android.location.Location
import androidx.lifecycle.LiveData
import com.example.locationmaps.api.PlacesApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class LocationExplorerRepository(
    private val poiDao: PointOfInterestDao,
    private val zoneDao: ExploredZoneDao,
    private val visitedPlaceDao: VisitedPlaceDao,
    private val placesApi: PlacesApiService
) {
    // Puntos de interés
    val allPOIs: LiveData<List<PointOfInterest>> = poiDao.getAllPOIs()
    val allZones: LiveData<List<ExploredZone>> = zoneDao.getAllZones()
    val exploredZones: LiveData<List<ExploredZone>> = zoneDao.getExploredZones()
    val unexploredZones: LiveData<List<ExploredZone>> = zoneDao.getUnexploredZones()
    val visitedPlaces: LiveData<List<VisitedPlace>> = visitedPlaceDao.getAllVisitedPlaces()

    suspend fun insertPOI(poi: PointOfInterest): Long {
        return withContext(Dispatchers.IO) {
            poiDao.insert(poi)
        }
    }

    suspend fun updatePOI(poi: PointOfInterest): Int {
        return withContext(Dispatchers.IO) {
            poiDao.update(poi)
        }
    }

    suspend fun deletePOI(poi: PointOfInterest): Int {
        return withContext(Dispatchers.IO) {
            poiDao.delete(poi)
        }
    }

    fun searchPOIs(query: String): LiveData<List<PointOfInterest>> {
        return poiDao.searchPOIs(query)
    }

    fun getPOIsByCategory(category: String): LiveData<List<PointOfInterest>> {
        return poiDao.getPOIsByCategory(category)
    }

    // Zonas exploradas
    suspend fun insertZone(zone: ExploredZone): Long {
        return withContext(Dispatchers.IO) {
            zoneDao.insert(zone)
        }
    }

    suspend fun updateZone(zone: ExploredZone): Int {
        return withContext(Dispatchers.IO) {
            zoneDao.update(zone)
        }
    }

    suspend fun recordVisit(latitude: Double, longitude: Double) {
        withContext(Dispatchers.IO) {
            // Registrar la visita
            visitedPlaceDao.insert(VisitedPlace(latitude = latitude, longitude = longitude))

            // Comprobar zonas cercanas para marcarlas como exploradas
            val unexploredZones = zoneDao.getUnexploredZonesList()
            for (zone in unexploredZones) {
                val distance = calculateDistance(
                    latitude, longitude,
                    zone.centerLatitude, zone.centerLongitude
                )

                if (distance <= zone.radius) {
                    // Marcar la zona como explorada
                    val updatedZone = zone.copy(isExplored = true, dateExplored = Date())
                    zoneDao.update(updatedZone)
                }
            }
        }
    }
    suspend fun fetchNearbyPlaces(latitude: Double, longitude: Double, radius: Int = 1000): List<PointOfInterest> {
        return withContext(Dispatchers.IO) {
            try {
                val response = placesApi.getNearbyPlaces(latitude, longitude, radius)
                if (response.isSuccessful) {
                    val places = response.body()?.features ?: emptyList()
                    places.map { feature ->
                        PointOfInterest(
                            name = feature.properties.name,
                            description = feature.properties.kinds,
                            latitude = feature.geometry.coordinates[1], // Lat is second in GeoJSON
                            longitude = feature.geometry.coordinates[0], // Lng is first in GeoJSON
                            category = feature.properties.kinds.split(",").firstOrNull() ?: "other"
                        )
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Los siguientes métodos son problemáticos porque intentan acceder al valor de LiveData
    // desde un hilo de fondo. En una implementación real necesitaríamos usar
    // observeForever o crear consultas síncronas adicionales.
    fun getPoiById(id: Long): LiveData<PointOfInterest?> {
        return poiDao.getPoiById(id)
    }
    suspend fun calculateExplorationProgress(): Float {
        return withContext(Dispatchers.IO) {
            // Como solución provisional, usamos una consulta directa
            val exploredCount = 0 // Aquí se necesitaría una consulta directa a la base de datos
            val totalCount = 1    // Aquí se necesitaría una consulta directa a la base de datos
            if (totalCount == 0) 0f else exploredCount.toFloat() / totalCount
        }
    }

    suspend fun suggestNextZones(currentLat: Double, currentLng: Double, limit: Int = 3): List<ExploredZone> {
        return withContext(Dispatchers.IO) {
            // Como solución provisional, devolvemos una lista vacía
            // En una implementación real, necesitaríamos una consulta directa a la base de datos
            emptyList()
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0] // Distancia en metros
    }
}