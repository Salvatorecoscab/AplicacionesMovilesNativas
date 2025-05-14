package com.example.locationmaps.traffic

interface TrafficCallback {
    // Sin la propiedad angle
    fun onVehicleAdded(vehicle: Vehicle)
    fun onVehicleMoved(vehicle: Vehicle)
    fun onVehicleRemoved(vehicle: Vehicle)
    fun onTrafficLightAdded(trafficLight: TrafficLight)
    fun onTrafficLightChanged(trafficLight: TrafficLight)
    fun onRoadNetworkLoaded(roadSegments: List<RoadSegment>)
    fun onRoadSegmentTrafficChanged(segment: RoadSegment)
    fun onDayNightModeChanged(isDayMode: Boolean)
}