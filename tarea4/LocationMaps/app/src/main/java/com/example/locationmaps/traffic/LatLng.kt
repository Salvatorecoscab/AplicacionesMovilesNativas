package com.example.locationmaps.traffic

data class LatLng(
    val lat: Double,
    val lng: Double
)

data class LatLngBounds(
    val southwest: LatLng,
    val northeast: LatLng
)