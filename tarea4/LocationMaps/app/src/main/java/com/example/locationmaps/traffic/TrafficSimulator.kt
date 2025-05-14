package com.example.locationmaps.traffic

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.random.Random

class TrafficSimulator(
    private val mapType: MapType,
    private val callback: TrafficCallback
) {
    enum class MapType { OPEN_STREET_MAP, GOOGLE_MAPS }

    private val vehicles = mutableListOf<Vehicle>()
    private val trafficLights = mutableListOf<TrafficLight>()
    private val roadSegments = mutableListOf<RoadSegment>()
    private val simulationHandler = Handler(Looper.getMainLooper())
    private val simulationRunnable = object : Runnable {
        override fun run() {
            updateSimulation()
            simulationHandler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    private var isRunning = false
    private var isDayMode = true

    companion object {
        private const val UPDATE_INTERVAL = 100L // ms
        private const val MAX_VEHICLES = 50
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            simulationHandler.post(simulationRunnable)
            Log.d("TrafficSimulator", "Simulation started")
        }
    }

    fun stop() {
        isRunning = false
        simulationHandler.removeCallbacks(simulationRunnable)
        Log.d("TrafficSimulator", "Simulation stopped")
    }

    fun setDayNightMode(isDayMode: Boolean) {
        this.isDayMode = isDayMode
        callback.onDayNightModeChanged(isDayMode)
    }
    fun setRoadNetwork(roads: List<RoadSegment>) {
        roadSegments.clear()
        roadSegments.addAll(roads)
        Log.d("TrafficSimulator", "Red de carreteras actualizada con ${roads.size} segmentos")
        callback.onRoadNetworkLoaded(roadSegments)
    }
    fun addVehicle(lat: Double, lng: Double): Vehicle {
        val vehicle = Vehicle(
            id = vehicles.size + 1,
            type = VehicleType.values().random(),
            lat = lat,
            lng = lng,
            angle = Random.nextDouble(360.0),
            speed = 0.5 + Random.nextDouble() * 2.0
        )
        vehicles.add(vehicle)
        callback.onVehicleAdded(vehicle)
        return vehicle
    }

    fun addTrafficLight(lat: Double, lng: Double): TrafficLight {
        val trafficLight = TrafficLight(
            id = trafficLights.size + 1,
            lat = lat,
            lng = lng,
            state = TrafficLightState.RED
        )
        trafficLights.add(trafficLight)
        callback.onTrafficLightAdded(trafficLight)
        return trafficLight
    }

    fun loadRoadNetwork(bounds: LatLngBounds) {
        // En un caso real, obtendríamos datos de la API de OSM o Google Maps
        // Para esta demo, generaremos segmentos de carretera aleatorios
        generateRandomRoadNetwork(bounds)
        callback.onRoadNetworkLoaded(roadSegments)
    }

    private fun generateRandomRoadNetwork(bounds: LatLngBounds) {
        roadSegments.clear()

        // Crear una cuadrícula de carreteras
        val latStep = (bounds.northeast.lat - bounds.southwest.lat) / 10
        val lngStep = (bounds.northeast.lng - bounds.southwest.lng) / 10

        // Crear carreteras horizontales
        for (i in 0..10) {
            val lat = bounds.southwest.lat + (i * latStep)
            val startPoint = LatLng(lat, bounds.southwest.lng)
            val endPoint = LatLng(lat, bounds.northeast.lng)

            val segment = RoadSegment(
                id = roadSegments.size + 1,
                name = "Road H$i",
                startPoint = startPoint,
                endPoint = endPoint,
                trafficDensity = Random.nextDouble()
            )
            roadSegments.add(segment)

            // Añadir semáforos en intersecciones
            if (i % 2 == 0) {
                for (j in 1..9) {
                    val lngPos = bounds.southwest.lng + (j * lngStep)
                    addTrafficLight(lat, lngPos)
                }
            }
        }

        // Crear carreteras verticales
        for (j in 0..10) {
            val lng = bounds.southwest.lng + (j * lngStep)
            val startPoint = LatLng(bounds.southwest.lat, lng)
            val endPoint = LatLng(bounds.northeast.lat, lng)

            val segment = RoadSegment(
                id = roadSegments.size + 1,
                name = "Road V$j",
                startPoint = startPoint,
                endPoint = endPoint,
                trafficDensity = Random.nextDouble()
            )
            roadSegments.add(segment)
        }
    }

    private fun updateSimulation() {
        // Actualiza posiciones de vehículos
        for (vehicle in vehicles) {
            moveVehicle(vehicle)
        }

        // Actualiza semáforos
        for (trafficLight in trafficLights) {
            if (Random.nextInt(100) < 5) { // 5% de probabilidad de cambio
                trafficLight.state = when (trafficLight.state) {
                    TrafficLightState.RED -> TrafficLightState.GREEN
                    TrafficLightState.GREEN -> TrafficLightState.YELLOW
                    TrafficLightState.YELLOW -> TrafficLightState.RED
                }
                callback.onTrafficLightChanged(trafficLight)
            }
        }

        // Actualiza densidad de tráfico
        for (segment in roadSegments) {
            // Ajusta la densidad de tráfico basada en vehículos en la zona
            val vehiclesOnRoad = vehicles.count { isVehicleOnRoad(it, segment) }
            val newDensity = vehiclesOnRoad.toDouble() / MAX_VEHICLES
            if (Math.abs(segment.trafficDensity - newDensity) > 0.1) {
                segment.trafficDensity = newDensity
                callback.onRoadSegmentTrafficChanged(segment)
            }
        }

        // Genera nuevos vehículos aleatoriamente
        if (vehicles.size < MAX_VEHICLES && Random.nextInt(100) < 10) {
            val segment = roadSegments.random()
            val t = Random.nextDouble()
            val lat = segment.startPoint.lat + (segment.endPoint.lat - segment.startPoint.lat) * t
            val lng = segment.startPoint.lng + (segment.endPoint.lng - segment.startPoint.lng) * t

            addVehicle(lat, lng)
        }

        // Elimina vehículos que están fuera de límites
        val iterator = vehicles.iterator()
        while (iterator.hasNext()) {
            val vehicle = iterator.next()
            if (isVehicleOutOfBounds(vehicle)) {
                callback.onVehicleRemoved(vehicle)
                iterator.remove()
            }
        }
    }

    private fun moveVehicle(vehicle: Vehicle) {
        // Encuentra la carretera actual o más cercana
        val currentRoad = vehicle.currentRoad ?: findNearestRoad(vehicle)

        if (currentRoad == null) {
            // Si no hay una carretera cercana, mover aleatoriamente
            moveRandomly(vehicle)
            return
        }

        // Asignar carretera actual si no está asignada
        if (vehicle.currentRoad == null) {
            vehicle.currentRoad = currentRoad
            // Inicializar posición en la carretera (índice del punto en la polilínea)
            vehicle.roadPosition = findClosestPositionOnRoad(vehicle, currentRoad)
            // Establecer dirección inicial
            vehicle.forward = Random.nextBoolean()
        }

        // Obtener la posición actual y siguiente en la carretera
        val (currentPoint, nextPoint) = getNextPointOnRoad(vehicle, currentRoad)

        if (nextPoint == null) {
            // Llegó al final de la carretera, debe tomar una decisión
            handleRoadEnd(vehicle, currentRoad)
            return
        }

        // Calcular dirección basada en los puntos
        val angle = calculateAngleBetweenPoints(
            currentPoint.lat, currentPoint.lng,
            nextPoint.lat, nextPoint.lng
        )
        vehicle.angle = angle

        // Ajustar velocidad basada en densidad de tráfico y semáforos cercanos
        adjustVehicleSpeed(vehicle, currentRoad)

        // Mover vehículo hacia el siguiente punto
        moveTowardsPoint(vehicle, nextPoint, vehicle.speed)

        // Verificar si alcanzó el siguiente punto
        if (hasReachedPoint(vehicle, nextPoint)) {
            // Avanzar al siguiente punto
            updateVehicleRoadPosition(vehicle)
        }

        callback.onVehicleMoved(vehicle)
    }

    private fun getNextPointOnRoad(vehicle: Vehicle, road: RoadSegment): Pair<LatLng, LatLng?> {
        val points = road.coordinates
        val position = vehicle.roadPosition

        if (points.size < 2) {
            return Pair(LatLng(vehicle.lat, vehicle.lng), null)
        }

        val currentPoint = points[position]

        val nextPosition = if (vehicle.forward) {
            if (position + 1 < points.size) position + 1 else null
        } else {
            if (position - 1 >= 0) position - 1 else null
        }

        val nextPoint = nextPosition?.let { points[it] }

        return Pair(currentPoint, nextPoint)
    }

    private fun handleRoadEnd(vehicle: Vehicle, road: RoadSegment) {
        // Encontrar carreteras conectadas
        val connectedRoads = findConnectedRoads(road, vehicle.forward)

        if (connectedRoads.isEmpty()) {
            // Dar la vuelta si no hay carreteras conectadas
            vehicle.forward = !vehicle.forward
            return
        }

        // Elegir una carretera aleatoria, con preferencia por las vías principales
        val mainRoads = connectedRoads.filter { it.type in listOf("primary", "secondary", "tertiary") }
        val nextRoad = if (mainRoads.isNotEmpty()) mainRoads.random() else connectedRoads.random()

        // Encontrar el punto de conexión
        val currentEndPoint = if (vehicle.forward) road.coordinates.last() else road.coordinates.first()

        // Encontrar el punto más cercano en la nueva carretera
        val (newPosition, isForward) = findEntryPointOnRoad(nextRoad, currentEndPoint)

        // Actualizar vehículo
        vehicle.currentRoad = nextRoad
        vehicle.roadPosition = newPosition
        vehicle.forward = isForward
    }

    private fun adjustVehicleSpeed(vehicle: Vehicle, road: RoadSegment) {
        // Base speed según el tipo de carretera
        val baseSpeed = when (road.type) {
            "motorway", "trunk" -> 3.0
            "primary" -> 2.5
            "secondary" -> 2.0
            "tertiary", "residential" -> 1.5
            else -> 1.0
        }

        // Ajustar según densidad de tráfico
        val trafficFactor = 1.0 - road.trafficDensity

        // Verificar semáforos cercanos
        val nearbyTrafficLight = findNearestTrafficLight(vehicle)
        val trafficLightFactor = if (nearbyTrafficLight != null &&
            isVehicleNearTrafficLight(vehicle, nearbyTrafficLight)) {
            when (nearbyTrafficLight.state) {
                TrafficLightState.RED -> 0.0
                TrafficLightState.YELLOW -> 0.3
                TrafficLightState.GREEN -> 1.0
            }
        } else {
            1.0
        }

        // Calcular velocidad final
        vehicle.speed = baseSpeed * trafficFactor * trafficLightFactor
    }

    private fun moveTowardsPoint(vehicle: Vehicle, target: LatLng, speed: Double) {
        val distance = calculateDistance(vehicle.lat, vehicle.lng, target.lat, target.lng)

        // Si está muy cerca, mover directamente al punto
        if (distance < speed * 0.00001) {
            vehicle.lat = target.lat
            vehicle.lng = target.lng
            return
        }

        // Calcular vector de dirección normalizado
        val dx = (target.lng - vehicle.lng) / distance
        val dy = (target.lat - vehicle.lat) / distance

        // Mover el vehículo
        val step = speed * 0.00001
        vehicle.lng += dx * step
        vehicle.lat += dy * step
    }

    private fun findNearestRoad(vehicle: Vehicle): RoadSegment? {
        return roadSegments.minByOrNull {
            distanceToRoad(vehicle.lat, vehicle.lng, it)
        }
    }

    private fun findNearestTrafficLight(vehicle: Vehicle): TrafficLight? {
        return trafficLights.filter {
            calculateDistance(vehicle.lat, vehicle.lng, it.lat, it.lng) < 0.0001
        }.minByOrNull {
            calculateDistance(vehicle.lat, vehicle.lng, it.lat, it.lng)
        }
    }

    private fun isVehicleNearTrafficLight(vehicle: Vehicle, trafficLight: TrafficLight): Boolean {
        return calculateDistance(vehicle.lat, vehicle.lng, trafficLight.lat, trafficLight.lng) < 0.0001
    }

    private fun isVehicleOnRoad(vehicle: Vehicle, road: RoadSegment): Boolean {
        return distanceToRoad(vehicle.lat, vehicle.lng, road) < 0.0001
    }

    private fun isVehicleOutOfBounds(vehicle: Vehicle): Boolean {
        // Verifica si el vehículo está fuera de los límites del mapa
        val minLat = roadSegments.minOf { minOf(it.startPoint.lat, it.endPoint.lat) } - 0.001
        val maxLat = roadSegments.maxOf { maxOf(it.startPoint.lat, it.endPoint.lat) } + 0.001
        val minLng = roadSegments.minOf { minOf(it.startPoint.lng, it.endPoint.lng) } - 0.001
        val maxLng = roadSegments.maxOf { maxOf(it.startPoint.lng, it.endPoint.lng) } + 0.001

        return vehicle.lat < minLat || vehicle.lat > maxLat ||
                vehicle.lng < minLng || vehicle.lng > maxLng
    }

    private fun calculateRoadAngle(road: RoadSegment): Double {
        val deltaLat = road.endPoint.lat - road.startPoint.lat
        val deltaLng = road.endPoint.lng - road.startPoint.lng
        return Math.toDegrees(Math.atan2(deltaLng, deltaLat))
    }

    private fun adjustAngle(currentAngle: Double, targetAngle: Double): Double {
        var diff = (targetAngle - currentAngle) % 360
        if (diff < -180) diff += 360
        if (diff > 180) diff -= 360

        // Suavizar el cambio de dirección
        return currentAngle + diff * 0.1
    }

    private fun distanceToRoad(lat: Double, lng: Double, road: RoadSegment): Double {
        // Cálculo simplificado de distancia a un segmento de línea
        val x = lng
        val y = lat
        val x1 = road.startPoint.lng
        val y1 = road.startPoint.lat
        val x2 = road.endPoint.lng
        val y2 = road.endPoint.lat

        val A = x - x1
        val B = y - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = dot / lenSq

        var xx: Double
        var yy: Double

        if (param < 0) {
            xx = x1
            yy = y1
        } else if (param > 1) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        return calculateDistance(lat, lng, yy, xx)
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    fun getVehicleById(id: Int): Vehicle? {
        return vehicles.find { it.id == id }
    }

    private fun moveRandomly(vehicle: Vehicle) {
        // Mover el vehículo en una dirección aleatoria
        val angle = Random.nextDouble(360.0)
        vehicle.angle = angle

        // Calcular nueva posición
        val radians = Math.toRadians(angle)
        val speed = vehicle.speed * 0.00001
        vehicle.lat += speed * Math.cos(radians)
        vehicle.lng += speed * Math.sin(radians)
    }

    private fun findClosestPositionOnRoad(vehicle: Vehicle, road: RoadSegment): Int {
        if (road.coordinates.isEmpty()) return 0

        // Encuentra el punto más cercano en la carretera
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        for (i in road.coordinates.indices) {
            val point = road.coordinates[i]
            val distance = calculateDistance(vehicle.lat, vehicle.lng, point.lat, point.lng)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        return closestIndex
    }

    private fun updateVehicleRoadPosition(vehicle: Vehicle) {
        val road = vehicle.currentRoad ?: return
        val coordinates = road.coordinates

        if (vehicle.forward) {
            if (vehicle.roadPosition < coordinates.size - 1) {
                vehicle.roadPosition++
            }
        } else {
            if (vehicle.roadPosition > 0) {
                vehicle.roadPosition--
            }
        }
    }

    private fun hasReachedPoint(vehicle: Vehicle, target: LatLng): Boolean {
        val distance = calculateDistance(vehicle.lat, vehicle.lng, target.lat, target.lng)
        return distance < 0.00001 // ~1 metro
    }

    private fun calculateAngleBetweenPoints(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val deltaLat = lat2 - lat1
        val deltaLng = lng2 - lng1
        return Math.toDegrees(Math.atan2(deltaLng, deltaLat))
    }

    private fun findConnectedRoads(road: RoadSegment, fromEnd: Boolean): List<RoadSegment> {
        val connectedRoads = mutableListOf<RoadSegment>()

        // Obtener el punto de referencia (inicio o fin del segmento actual)
        val referencePoint = if (fromEnd) road.coordinates.last() else road.coordinates.first()

        // Buscar carreteras conectadas
        for (otherRoad in roadSegments) {
            if (otherRoad.id == road.id) continue

            // Comprobar si el inicio o el fin de la otra carretera está cerca
            if (isPointNearPoint(referencePoint, otherRoad.coordinates.first()) ||
                isPointNearPoint(referencePoint, otherRoad.coordinates.last())) {
                connectedRoads.add(otherRoad)
            }
        }

        return connectedRoads
    }

    private fun isPointNearPoint(point1: LatLng, point2: LatLng): Boolean {
        val distance = calculateDistance(point1.lat, point1.lng, point2.lat, point2.lng)
        return distance < 0.0001 // ~10 metros
    }

    private fun findEntryPointOnRoad(road: RoadSegment, fromPoint: LatLng): Pair<Int, Boolean> {
        // Encontrar el punto más cercano en la carretera
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        for (i in road.coordinates.indices) {
            val point = road.coordinates[i]
            val distance = calculateDistance(fromPoint.lat, fromPoint.lng, point.lat, point.lng)

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        // Determinar dirección (hacia adelante o hacia atrás)
        val isForward = closestIndex < road.coordinates.size / 2

        return Pair(closestIndex, isForward)
    }
}