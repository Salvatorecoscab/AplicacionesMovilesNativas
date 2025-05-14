package com.example.locationmaps.traffic

data class RoadSegment(
    val id: Int,
    val name: String,
    val type: String = "road",  // Tipo de vía (primary, secondary, etc.)
    val startPoint: LatLng,
    val endPoint: LatLng,
    var trafficDensity: Double,  // 0.0 - 1.0 (fluido a congestionado)
    val coordinates: List<LatLng> = listOf(startPoint, endPoint)
)