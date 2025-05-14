package com.example.locationmaps.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.locationmaps.api.PlacesApiService
import com.example.locationmaps.data.AppDatabase
import com.example.locationmaps.data.ExploredZone
import com.example.locationmaps.data.LocationExplorerRepository
import com.example.locationmaps.data.PointOfInterest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LocationExplorerRepository
    val allPOIs: LiveData<List<PointOfInterest>>
    val allZones: LiveData<List<ExploredZone>>
    val exploredZones: LiveData<List<ExploredZone>>
    val unexploredZones: LiveData<List<ExploredZone>>

    private val _explorationProgress = MutableLiveData<Float>(0f)
    val explorationProgress: LiveData<Float> = _explorationProgress

    private val _suggestedZones = MutableLiveData<List<ExploredZone>>(emptyList())
    val suggestedZones: LiveData<List<ExploredZone>> = _suggestedZones

    private val _nearbyPlaces = MutableLiveData<List<PointOfInterest>>(emptyList())
    val nearbyPlaces: LiveData<List<PointOfInterest>> = _nearbyPlaces

    init {
        val database = AppDatabase.getDatabase(application)
        val poiDao = database.poiDao()
        val zoneDao = database.zoneDao()
        val visitedPlaceDao = database.visitedPlaceDao()
        val placesApi = PlacesApiService.create()

        repository = LocationExplorerRepository(poiDao, zoneDao, visitedPlaceDao, placesApi)

        allPOIs = repository.allPOIs
        allZones = repository.allZones
        exploredZones = repository.exploredZones
        unexploredZones = repository.unexploredZones

        // Inicializar el progreso de exploración
        updateExplorationProgress()
    }

    // Métodos para POIs
    fun insertPOI(poi: PointOfInterest) = viewModelScope.launch {
        repository.insertPOI(poi)
    }

    fun updatePOI(poi: PointOfInterest) = viewModelScope.launch {
        repository.updatePOI(poi)
    }

    fun deletePOI(poi: PointOfInterest) = viewModelScope.launch {
        repository.deletePOI(poi)
    }

    fun searchPOIs(query: String) = viewModelScope.launch {
        val results = repository.searchPOIs(query)
    }

    // Métodos para zonas
    fun insertZone(zone: ExploredZone) = viewModelScope.launch {
        repository.insertZone(zone)
        updateExplorationProgress()
    }

    fun updateZone(zone: ExploredZone) = viewModelScope.launch {
        repository.updateZone(zone)
        updateExplorationProgress()
    }

    // Métodos para ubicación actual
    fun recordVisit(latitude: Double, longitude: Double) = viewModelScope.launch {
        repository.recordVisit(latitude, longitude)
        updateExplorationProgress()
        suggestNextZones(latitude, longitude)
    }

    fun fetchNearbyPlaces(latitude: Double, longitude: Double, radius: Int = 1000) = viewModelScope.launch {
        val places = repository.fetchNearbyPlaces(latitude, longitude, radius)
        _nearbyPlaces.postValue(places)
    }

    private fun updateExplorationProgress() = viewModelScope.launch {
        _explorationProgress.postValue(repository.calculateExplorationProgress())
    }

    private fun suggestNextZones(latitude: Double, longitude: Double) = viewModelScope.launch {
        val suggestions = repository.suggestNextZones(latitude, longitude)
        _suggestedZones.postValue(suggestions)
    }
    fun getPoiById(id: Long): LiveData<PointOfInterest?> {
        return repository.getPoiById(id)
    }
    // Métodos para fotos
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getApplication<Application>().getExternalFilesDir("photos")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}