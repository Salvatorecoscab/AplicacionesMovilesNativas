package com.example.locationmaps.traffic

enum class TrafficLightState {
    RED, YELLOW, GREEN
}

data class TrafficLight(
    val id: Int,
    val lat: Double,
    val lng: Double,
    var state: TrafficLightState
)