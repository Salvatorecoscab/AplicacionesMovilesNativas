package com.example.locationmaps.traffic

enum class VehicleType {
    CAR, TRUCK, BUS, MOTORCYCLE
}

data class Vehicle(
    val id: Int,
    val type: VehicleType,
    var lat: Double,
    var lng: Double,
    var angle: Double,
    var speed: Double,
    // Nuevos campos para navegación
    var currentRoad: RoadSegment? = null,
    var roadPosition: Int = 0,  // Índice en la lista de coordenadas
    var forward: Boolean = true // Dirección en la que se mueve en la carretera
)