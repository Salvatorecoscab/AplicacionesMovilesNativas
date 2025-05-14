package com.example.locationmaps

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.example.locationmaps.data.ExploredZone
import com.example.locationmaps.data.PointOfInterest
import com.example.locationmaps.viewmodel.LocationExplorerViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.util.Date
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.locationmaps.adapters.PointOfInterestAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.locationmaps.traffic.*
import java.util.*

class MapActivity : AppCompatActivity(), TrafficCallback {
    // Movido htmlContent dentro de la clase como companion object

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val ZONES_ACTIVITY_REQUEST_CODE = 101

        private val htmlContent = """
            <html>
            <head>
                <link rel='stylesheet' href='https://unpkg.com/leaflet@1.7.1/dist/leaflet.css' />
                <script src='https://unpkg.com/leaflet@1.7.1/dist/leaflet.js'></script>
                <script src='https://unpkg.com/leaflet-rotatedmarker@0.2.0/leaflet.rotatedMarker.js'></script>
                <style>
                    body, html, #map { 
                        width: 100%; 
                        height: 100%; 
                        margin: 0; 
                        padding: 0;
                    }
                    .vehicle-marker {
                        width: 10px;
                        height: 6px;
                        border-radius: 2px;
                        transform-origin: center;
                    }
                    .vehicle-marker.car { background-color: #3498db; }
                    .vehicle-marker.truck { background-color: #e74c3c; width: 12px; }
                    .vehicle-marker.bus { background-color: #f1c40f; width: 12px; }
                    .vehicle-marker.motorcycle { background-color: #2ecc71; width: 8px; }
                    
                    .vehicle-marker.lights-on:before,
                    .vehicle-marker.lights-on:after {
                        content: '';
                        position: absolute;
                        width: 2px;
                        height: 2px;
                        background-color: yellow;
                        border-radius: 50%;
                        box-shadow: 0 0 3px 1px rgba(255, 255, 0, 0.7);
                    }
                    
                    .vehicle-marker.lights-on:before {
                        top: 0;
                        left: 0;
                    }
                    
                    .vehicle-marker.lights-on:after {
                        top: 0;
                        right: 0;
                    }
                    
                    .traffic-light {
                        width: 6px;
                        height: 12px;
                        background-color: #333;
                        border-radius: 1px;
                        position: relative;
                    }
                    
                    .traffic-light:before {
                        content: '';
                        position: absolute;
                        width: 4px;
                        height: 4px;
                        border-radius: 50%;
                        top: 1px;
                        left: 1px;
                    }
                    
                    .traffic-light.red:before { 
                        background-color: red; 
                        box-shadow: 0 0 3px 1px rgba(255, 0, 0, 0.7);
                    }
                    
                    .traffic-light.yellow:before { 
                        background-color: yellow; 
                        box-shadow: 0 0 3px 1px rgba(255, 255, 0, 0.7);
                    }
                    
                    .traffic-light.green:before { 
                        background-color: #2ecc71; 
                        box-shadow: 0 0 3px 1px rgba(46, 204, 113, 0.7);
                    }
                </style>
            </head>
            <body>
                <div id='map'></div>
                <script>
                    // Almacenar objetos
                    var vehicleMarkers = {};
                    var trafficLightMarkers = {};
                    var roadPolylines = {};
                    
                    // Función añadida para crear vehículo con rotación correcta
                    function addVehicle(id, lat, lng, type, angle, lightsOn) {
                        var vehicleClass = 'vehicle-marker ' + type.toLowerCase();
                        if (lightsOn) vehicleClass += ' lights-on';
                        
                        var vehicleIcon = L.divIcon({
                            html: '<div class="' + vehicleClass + '" style="transform: rotate(' + angle + 'deg);"></div>',
                            className: 'vehicle-icon-container',
                            iconSize: [12, 8]
                        });
                        
                        var marker = L.marker([lat, lng], {
                            icon: vehicleIcon,
                            rotationAngle: angle
                        }).addTo(map);
                        
                        vehicleMarkers[id] = marker;
                        return marker;
                    }
                    
                    function addTrafficLight(id, lat, lng, state) {
                        var lightClass = 'traffic-light ' + state.toLowerCase();
                        
                        var lightIcon = L.divIcon({
                            html: '<div class="' + lightClass + '"></div>',
                            className: 'traffic-light-container',
                            iconSize: [8, 14]
                        });
                        
                        var marker = L.marker([lat, lng], {
                            icon: lightIcon
                        }).addTo(map);
                        
                        trafficLightMarkers[id] = marker;
                        return marker;
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    // Añadir estas propiedades
    private lateinit var trafficSimulator: TrafficSimulator
    private var isDayMode = true
    private var isTrafficSimulationEnabled = false
    private val vehicleMarkers = mutableMapOf<Int, String>() // ID del vehículo -> ID del marcador JS
    private val trafficLightMarkers = mutableMapOf<Int, String>()
    private val roadPolylines = mutableMapOf<Int, String>()

    private val zonesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.let {
                val latitude = it.getDoubleExtra("zoneLat", 0.0)
                val longitude = it.getDoubleExtra("zoneLng", 0.0)

                if (latitude != 0.0 && longitude != 0.0) {
                    // Centrar el mapa en la zona seleccionada
                    val script = """
                    map.setView([$latitude, $longitude], 16);
                """.trimIndent()
                    webView.evaluateJavascript(script, null)
                }
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                // La foto se tomó correctamente, convertirla a ByteArray
                currentPhotoUri?.let { photoURI ->
                    val inputStream = contentResolver.openInputStream(photoURI)
                    currentPhotoBytes = inputStream?.readBytes()
                    inputStream?.close()

                    // Actualizar la vista previa
                    val dialogImageView = dialog.findViewById<ImageView>(R.id.poiPhotoImageView)
                    if (dialogImageView != null && currentPhotoBytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(
                            currentPhotoBytes, 0, currentPhotoBytes!!.size
                        )
                        dialogImageView.setImageBitmap(bitmap)
                    }

                    // Eliminar el archivo temporal si es necesario
                    val tempFile = File(photoURI.path ?: "")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var currentPhotoUri: Uri? = null
    private lateinit var dialog: BottomSheetDialog
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var explorationTextView: TextView
    private lateinit var addPoiButton: FloatingActionButton
    private lateinit var showPoiButton: FloatingActionButton
    private lateinit var exploreZonesButton: FloatingActionButton
    private lateinit var locationUpdateHandler: Handler
    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            checkCurrentLocation()
            // Repetir cada 30 segundos
            locationUpdateHandler.postDelayed(this, 30000)
        }
    }
    private var loadStartTime: Long = 0
    private var loadEndTime: Long = 0

    private lateinit var viewModel: LocationExplorerViewModel

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentPhotoBytes: ByteArray? = null
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.explorationProgressBar)
        explorationTextView = findViewById(R.id.explorationTextView)
        addPoiButton = findViewById(R.id.addPoiButton)
        showPoiButton = findViewById(R.id.showPoiButton)
        locationUpdateHandler = Handler(Looper.getMainLooper())

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this)[LocationExplorerViewModel::class.java]

        // Configurar botón de zonas
        exploreZonesButton = findViewById(R.id.exploreZonesButton)
        exploreZonesButton.setOnClickListener {
            val intent = Intent(this, ZonesActivity::class.java)
            zonesActivityLauncher.launch(intent)
        }

        // Inicializar simulador de tráfico
        trafficSimulator = TrafficSimulator(
            TrafficSimulator.MapType.OPEN_STREET_MAP,
            this
        )


            val trafficButton = findViewById<FloatingActionButton>(R.id.trafficSimulationButton)
            trafficButton.setOnClickListener {
                if (isTrafficSimulationEnabled) {
                    clearTrafficSimulation()
                    Toast.makeText(this, "Simulación de tráfico detenida", Toast.LENGTH_SHORT).show()
                } else {
                    startTrafficSimulation()
                }
            }

            // Botón para cambiar entre día y noche
            val dayNightButton = findViewById<FloatingActionButton>(R.id.dayNightButton)
            dayNightButton.setOnClickListener {
                toggleDayNightMode()
            }

        setupObservers()
        setupButtons()
        requestLocationPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Iniciar actualizaciones de ubicación
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    private fun setupObservers() {
        viewModel.explorationProgress.observe(this) { progress ->
            progressBar.progress = (progress * 100).toInt()
            explorationTextView.text = "${(progress * 100).toInt()}% explorado"
        }

        viewModel.nearbyPlaces.observe(this) { places ->
            updateMapWithPOIs(places)
        }

        viewModel.suggestedZones.observe(this) { zones ->
            if (zones.isNotEmpty()) {
                showSuggestedZonesDialog(zones)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener actualizaciones de ubicación
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }

    private fun setupButtons() {
        addPoiButton.setOnClickListener {
            showAddPoiDialog()
        }

        showPoiButton.setOnClickListener {
            showPointsOfInterestDialog()
        }
    }

    private fun requestLocationPermissions() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                loadUserLocation()
            } else {
                Toast.makeText(this, "Permisos de ubicación requeridos", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun loadUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude

                loadMap(currentLatitude, currentLongitude)

                // Registrar visita y buscar lugares cercanos
                viewModel.recordVisit(currentLatitude, currentLongitude)
                viewModel.fetchNearbyPlaces(currentLatitude, currentLongitude)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMap(latitude: Double, longitude: Double) {
        // JavaScript interface para interactuar con el mapa
        class WebAppInterface {
            @JavascriptInterface
            fun onMapLongClick(lat: Double, lng: Double) {
                runOnUiThread {
                    showAddPoiDialog(lat, lng)
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Configuración del mapa existente...
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadStartTime = SystemClock.elapsedRealtime()
                Log.d("OpenStreetMap", "Start loading at: $loadStartTime")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadEndTime = SystemClock.elapsedRealtime()
                Log.d("OpenStreetMap", "Load time: ${loadEndTime - loadStartTime} ms")

                // Inicializar el mapa con coordenadas
                val initScript = """
                    var map = L.map('map').setView([$latitude, $longitude], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19
                    }).addTo(map);
                    
                    // Marcador de ubicación actual
                    var currentLocationMarker = L.marker([$latitude, $longitude]).addTo(map)
                        .bindPopup('Mi ubicación')
                        .openPopup();
                    
                    // Detectar click largo (longpress) en el mapa
                    var clickTimer;
                    map.on('mousedown', function(e) {
                        clickTimer = setTimeout(function() {
                            Android.onMapLongClick(e.latlng.lat, e.latlng.lng);
                        }, 500);
                    });
                    
                    map.on('mouseup', function() {
                        clearTimeout(clickTimer);
                    });
                    
                    // Función para añadir marcadores POI
                    function addPOI(id, lat, lng, name, category) {
                        var marker = L.marker([lat, lng]).addTo(map)
                            .bindPopup('<b>' + name + '</b><br>' + category);
                        return marker;
                    }
                    
                    // Función para añadir zonas explorables
                    function addExplorationZone(id, lat, lng, radius, name, isExplored) {
                        var color = isExplored ? 'green' : 'red';
                        var circle = L.circle([lat, lng], {
                            color: color,
                            fillColor: color,
                            fillOpacity: 0.2,
                            radius: radius
                        }).addTo(map);
                        
                        circle.bindPopup('<b>' + name + '</b><br>' + 
                            (isExplored ? 'Explorado' : 'Por explorar'));
                        return circle;
                    }
                """.trimIndent()

                webView.evaluateJavascript(initScript, null)

                // Inicializar el mapa con los POIs y zonas guardados
                loadSavedPOIs()
                loadExplorationZones()
            }
        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadSavedPOIs() {
        viewModel.allPOIs.observe(this) { pois ->
            updateMapWithPOIs(pois)
        }
    }

    private fun loadExplorationZones() {
        viewModel.allZones.observe(this) { zones ->
            updateMapWithZones(zones)
        }
    }

    private fun updateMapWithPOIs(pois: List<PointOfInterest>) {
        for (poi in pois) {
            val script = """
                addPOI(${poi.id}, ${poi.latitude}, ${poi.longitude}, "${poi.name}", "${poi.category}");
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    private fun updateMapWithZones(zones: List<ExploredZone>) {
        for (zone in zones) {
            val script = """
                addExplorationZone(${zone.id}, ${zone.centerLatitude}, ${zone.centerLongitude}, 
                    ${zone.radius}, "${zone.name}", ${zone.isExplored});
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    private fun showAddPoiDialog(latitude: Double? = null, longitude: Double? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poi, null)
        dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val nameEditText = dialogView.findViewById<EditText>(R.id.poiNameEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.poiDescriptionEditText)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.poiCategorySpinner)
        val photoImageView = dialogView.findViewById<ImageView>(R.id.poiPhotoImageView)
        val takePictureButton = dialogView.findViewById<Button>(R.id.takePictureButton)
        val saveButton = dialogView.findViewById<Button>(R.id.savePOIButton)

        // Configurar el adaptador para el spinner de categorías
        val categories = arrayOf("Restaurante", "Monumento", "Parque", "Museo", "Tienda", "Otro")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        takePictureButton.setOnClickListener {
            // Verificar permisos de cámara
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                dispatchTakePictureIntent()
            }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()

            // Verificar que el spinner tiene un item seleccionado
            val category = if (categorySpinner.selectedItem != null) {
                categorySpinner.selectedItem.toString()
            } else {
                "Otro" // Valor por defecto
            }

            if (name.isNotEmpty()) {
                val poi = PointOfInterest(
                    name = name,
                    description = description,
                    latitude = latitude ?: currentLatitude,
                    longitude = longitude ?: currentLongitude,
                    category = category,
                    // Solo incluir photoPath si no es nulo
                    photoPath = currentPhotoPath
                )
                viewModel.insertPOI(poi)
                dialog.dismiss()
                Toast.makeText(this, "Punto de interés guardado", Toast.LENGTH_SHORT).show()

                // Limpiar los datos de la foto después de guardar
                currentPhotoPath = null
                currentPhotoUri = null
                currentPhotoBytes = null
            } else {
                Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createImageFile(): File {
        // Crear un nombre de archivo único
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageFileName, /* prefijo */
            ".jpg", /* sufijo */
            storageDir /* directorio */
        ).apply {
            // Guardar la ruta para usar con el intent de la cámara
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        try {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Asegurarse de que hay una actividad de cámara para manejar el intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Crear el archivo donde la foto debería ir
                    val photoFile: File = createImageFile()

                    // Continuar solo si el archivo se ha creado correctamente
                    photoFile.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.locationmaps.fileprovider",
                            it
                        )
                        currentPhotoUri = photoURI
                        takePictureLauncher.launch(photoURI)
                    }
                } ?: run {
                    Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (ex: Exception) {
            // Error occurred while creating the File
            Log.e(TAG, "Error creando archivo de imagen: ${ex.message}", ex)
            Toast.makeText(this, "Error creando archivo: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPointsOfInterestDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_points_of_interest, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        // Configura el RecyclerView
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.poiRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Crea e inicializa el adaptador
        val adapter = PointOfInterestAdapter(
            pois = emptyList(),
            onNavigateClicked = { poi ->
                // Navegar al POI en el mapa
                val script = """
                map.setView([${poi.latitude}, ${poi.longitude}], 17);
                L.marker([${poi.latitude}, ${poi.longitude}])
                    .addTo(map)
                    .bindPopup("${poi.name}")
                    .openPopup();
            """.trimIndent()
                webView.evaluateJavascript(script, null)
                dialog.dismiss()
            },
            onDeleteClicked = { poi ->
                // Eliminar el POI
                viewModel.deletePOI(poi)
                Toast.makeText(this, "Punto de interés eliminado", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = adapter

        // Observa los cambios en la lista de POIs
        viewModel.allPOIs.observe(this) { pois ->
            if (pois.isEmpty()) {
                dialogView.findViewById<TextView>(R.id.emptyPoisTextView).visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                dialogView.findViewById<TextView>(R.id.emptyPoisTextView).visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updatePOIs(pois)
            }
        }

        val closeButton = dialogView.findViewById<Button>(R.id.closeDialogButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSuggestedZonesDialog(zones: List<ExploredZone>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_suggested_zones, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        // Aquí implementarías un RecyclerView con la lista de zonas sugeridas
        // Para simplificar, solo mostraremos la primera zona sugerida

        val zoneName = dialogView.findViewById<TextView>(R.id.suggestedZoneNameTextView)
        val navigateButton = dialogView.findViewById<Button>(R.id.navigateToZoneButton)
        val dismissButton = dialogView.findViewById<Button>(R.id.dismissSuggestionButton)

        if (zones.isNotEmpty()) {
            val firstZone = zones[0]
            zoneName.text = firstZone.name

            navigateButton.setOnClickListener {
                // Centrar el mapa en la zona sugerida
                val script = """
                    map.setView([${firstZone.centerLatitude}, ${firstZone.centerLongitude}], 15);
                """.trimIndent()
                webView.evaluateJavascript(script, null)
                dialog.dismiss()
            }
        }

        dismissButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Guarda las métricas cuando el usuario sale de la actividad
    override fun onStop() {
        super.onStop()

        val timeDifference = loadEndTime - loadStartTime
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        Log.d("OpenStreetMap", "Saving metrics - Load time: $timeDifference ms, Memory: $memoryUsage bytes")

        // Guarda los datos en SharedPreferences
        val sharedPreferences = getSharedPreferences("MapMetrics", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("osmLoadTime", timeDifference)
        editor.putLong("osmMemoryUsage", memoryUsage)
        editor.apply()
    }

    // Modificar toggleTrafficSimulation para usar los datos cargados
    private fun toggleTrafficSimulation() {
        isTrafficSimulationEnabled = !isTrafficSimulationEnabled
        if (isTrafficSimulationEnabled) {
            // No necesitamos generar una red de carreteras, ya la tenemos de OSM
            trafficSimulator.start()
            Toast.makeText(this, "Simulación de tráfico iniciada", Toast.LENGTH_SHORT).show()
        } else {
            trafficSimulator.stop()
            clearTrafficSimulation()
            Toast.makeText(this, "Simulación de tráfico detenida", Toast.LENGTH_SHORT).show()
        }
    }

    // Implementar métodos del TrafficCallback
    override fun onVehicleAdded(vehicle: Vehicle) {
        // Corregido: Usar métodos de cadena seguros para JavaScript
        val vehicleType = vehicle.type.name.toLowerCase(Locale.ROOT)
        val lightsClass = if (!isDayMode) " lights-on" else ""

        val script = """
            var vehicleIcon = L.divIcon({
                html: '<div class="vehicle-marker ${vehicleType}${lightsClass}"></div>',
                className: 'vehicle-icon-container',
                iconSize: [20, 10]
            });
            
            var marker = L.marker([${vehicle.lat}, ${vehicle.lng}], {
                icon: vehicleIcon,
                rotationAngle: ${vehicle.angle}
            }).addTo(map);
            
            // Guardar referencia
            vehicleMarkers[${vehicle.id}] = marker;
        """.trimIndent()

        runOnUiThread {
            webView.evaluateJavascript(script) { result ->
                vehicleMarkers[vehicle.id] = "vehicleMarkers[${vehicle.id}]"
            }
        }
    }

    override fun onVehicleMoved(vehicle: Vehicle) {
        vehicleMarkers[vehicle.id]?.let { markerId ->
            val script = """
                var marker = $markerId;
                marker.setLatLng([${vehicle.lat}, ${vehicle.lng}]);
                marker.setRotationAngle(${vehicle.angle});
            """.trimIndent()

            runOnUiThread {
                webView.evaluateJavascript(script, null)
            }
        }
    }

    override fun onVehicleRemoved(vehicle: Vehicle) {
        vehicleMarkers[vehicle.id]?.let { markerId ->
            val script = """
                var marker = $markerId;
                map.removeLayer(marker);
                delete vehicleMarkers[${vehicle.id}];
            """.trimIndent()

            runOnUiThread {
                webView.evaluateJavascript(script, null)
                vehicleMarkers.remove(vehicle.id)
            }
        }
    }

    override fun onTrafficLightAdded(trafficLight: TrafficLight) {
        val colorClass = when (trafficLight.state) {
            TrafficLightState.RED -> "red"
            TrafficLightState.YELLOW -> "yellow"
            TrafficLightState.GREEN -> "green"
        }

        val script = """
            var trafficLightIcon = L.divIcon({
                html: '<div class="traffic-light ${colorClass}"></div>',
                className: 'traffic-light-container',
                iconSize: [12, 30]
            });
            
            var marker = L.marker([${trafficLight.lat}, ${trafficLight.lng}], {
                icon: trafficLightIcon
            }).addTo(map);
            
            // Guardar referencia
            trafficLightMarkers[${trafficLight.id}] = marker;
        """.trimIndent()

        runOnUiThread {
            webView.evaluateJavascript(script) { result ->
                trafficLightMarkers[trafficLight.id] = "trafficLightMarkers[${trafficLight.id}]"
            }
        }
    }

    override fun onTrafficLightChanged(trafficLight: TrafficLight) {
        trafficLightMarkers[trafficLight.id]?.let { markerId ->
            val colorClass = when (trafficLight.state) {
                TrafficLightState.RED -> "red"
                TrafficLightState.YELLOW -> "yellow"
                TrafficLightState.GREEN -> "green"
            }

            val script = """
                var marker = $markerId;
                var element = marker.getElement();
                element.querySelector('.traffic-light').className = 'traffic-light ${colorClass}';
            """.trimIndent()

            runOnUiThread {
                webView.evaluateJavascript(script, null)
            }
        }
    }

    // Este método es más simple y funciona mejor con la implementación actual
    override fun onRoadNetworkLoaded(roadSegments: List<RoadSegment>) {
        // En lugar de confiar en la conversión JSON compleja, simplemente dibuja cada segmento
        for (segment in roadSegments) {
            val color = getTrafficColorForDensity(segment.trafficDensity)

            val script = """
                var polyline = L.polyline([
                    [${segment.startPoint.lat}, ${segment.startPoint.lng}],
                    [${segment.endPoint.lat}, ${segment.endPoint.lng}]
                ], {
                    color: '${color}',
                    weight: 5,
                    opacity: 0.7
                }).addTo(map);
                
                polyline.bindTooltip("${segment.name.replace("\"", "\\\"")}");
                
                // Guardar referencia
                if (!window.roadPolylines) window.roadPolylines = {};
                window.roadPolylines[${segment.id}] = polyline;
            """.trimIndent()

            runOnUiThread {
                webView.evaluateJavascript(script, null)
                roadPolylines[segment.id] = "window.roadPolylines[${segment.id}]"
            }
        }
    }

    override fun onRoadSegmentTrafficChanged(segment: RoadSegment) {
        val script = """
            var polyline = window.roadPolylines[${segment.id}];
            if (polyline) {
                polyline.setStyle({color: '${getTrafficColorForDensity(segment.trafficDensity)}'});
            }
        """.trimIndent()

        runOnUiThread {
            webView.evaluateJavascript(script, null)
        }
    }

    override fun onDayNightModeChanged(isDayMode: Boolean) {
        val script = if (isDayMode) {
            """
                // Actualizar vehículos (quitar luces)
                if (window.vehicleMarkers) {
                    Object.values(window.vehicleMarkers).forEach(function(marker) {
                        var element = marker.getElement();
                        if (element) {
                            var vehicleDiv = element.querySelector('.vehicle-marker');
                            if (vehicleDiv) vehicleDiv.classList.remove('lights-on');
                        }
                    });
                }
            """.trimIndent()
        } else {
            """
                // Actualizar vehículos (añadir luces)
                if (window.vehicleMarkers) {
                    Object.values(window.vehicleMarkers).forEach(function(marker) {
                        var element = marker.getElement();
                        if (element) {
                            var vehicleDiv = element.querySelector('.vehicle-marker');
                            if (vehicleDiv) vehicleDiv.classList.add('lights-on');
                        }
                    });
                }
            """.trimIndent()
        }

        runOnUiThread {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun startTrafficSimulation() {
        // Asegúrate de que tengamos una ubicación
        if (currentLatitude == 0.0 && currentLongitude == 0.0) {
            Toast.makeText(this, "Esperando ubicación...", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar mensaje al usuario
        Toast.makeText(this, "Iniciando simulación de tráfico...", Toast.LENGTH_SHORT).show()

        // Primero, limpiar cualquier simulación previa
        clearTrafficSimulation()

        // Usar un script muy básico y directo
        val script = """
        // Asegurar que estamos trabajando con el mapa ya cargado
        if (typeof map === "undefined") {
            alert("El mapa no está inicializado");
            return;
        }
        
        // Crear un grupo para la simulación para facilitar la limpieza
        window.trafficGroup = L.layerGroup().addTo(map);
        
        // Definir colores para tipos de calles
        const COLORS = {
            main: "#e74c3c",    // Rojo
            secondary: "#3498db", // Azul
            local: "#2ecc71"    // Verde
        };
        
        // Crear una cuadrícula simple centrada en la ubicación actual
        const centerLat = ${currentLatitude};
        const centerLng = ${currentLongitude};
        
        // Crear calles principales (horizontales y verticales)
        for (let i = -2; i <= 2; i++) {
            // Calle horizontal
            L.polyline([
                [centerLat + i * 0.001, centerLng - 0.005],
                [centerLat + i * 0.001, centerLng + 0.005]
            ], {
                color: i === 0 ? COLORS.main : COLORS.secondary,
                weight: i === 0 ? 8 : 6,
                opacity: 0.8
            }).addTo(window.trafficGroup);
            
            // Calle vertical
            L.polyline([
                [centerLat - 0.005, centerLng + i * 0.001],
                [centerLat + 0.005, centerLng + i * 0.001]
            ], {
                color: i === 0 ? COLORS.main : COLORS.secondary,
                weight: i === 0 ? 8 : 6,
                opacity: 0.8
            }).addTo(window.trafficGroup);
        }
        
        // Crear algunas calles locales
        for (let i = -4; i <= 4; i += 2) {
            if (i === 0) continue; // Saltar las calles principales
            
            // Calles horizontales secundarias
            L.polyline([
                [centerLat + i * 0.0005, centerLng - 0.003],
                [centerLat + i * 0.0005, centerLng + 0.003]
            ], {
                color: COLORS.local,
                weight: 4,
                opacity: 0.7
            }).addTo(window.trafficGroup);
            
            // Calles verticales secundarias
            L.polyline([
                [centerLat - 0.003, centerLng + i * 0.0005],
                [centerLat + 0.003, centerLng + i * 0.0005]
            ], {
                color: COLORS.local,
                weight: 4,
                opacity: 0.7
            }).addTo(window.trafficGroup);
        }
        
        // Crear marcadores para vehículos
        window.vehicles = [];
        
        // Función para crear un icono de vehículo
        function createVehicleIcon(color) {
            return L.divIcon({
                html: '<div style="width:12px;height:8px;background-color:' + color + ';border-radius:2px;transform:rotate(' + (Math.random() * 360) + 'deg);"></div>',
                className: 'vehicle-icon',
                iconSize: [12, 8]
            });
        }
        
        // Añadir vehículos en las calles
        for (let i = 0; i < 20; i++) {
            // Posición aleatoria cerca del centro
            const lat = centerLat + (Math.random() * 0.006 - 0.003);
            const lng = centerLng + (Math.random() * 0.006 - 0.003);
            
            // Color aleatorio para el vehículo
            const colors = ['#3498db', '#e74c3c', '#f1c40f', '#2ecc71'];
            const color = colors[Math.floor(Math.random() * colors.length)];
            
            // Crear marcador
            const vehicle = L.marker([lat, lng], {
                icon: createVehicleIcon(color)
            }).addTo(window.trafficGroup);
            
            window.vehicles.push(vehicle);
        }
        
        // Crear semáforos en las intersecciones principales
        for (let i = -1; i <= 1; i++) {
            for (let j = -1; j <= 1; j++) {
                if (i === 0 && j === 0) continue; // Saltar el centro
                
                const lat = centerLat + i * 0.001;
                const lng = centerLng + j * 0.001;
                
                // Crear icono de semáforo
                const lightIcon = L.divIcon({
                    html: '<div style="width:8px;height:8px;background-color:red;border-radius:50%;box-shadow:0 0 5px red;"></div>',
                    className: 'traffic-light',
                    iconSize: [8, 8]
                });
                
                L.marker([lat, lng], {
                    icon: lightIcon
                }).addTo(window.trafficGroup);
            }
        }
        
        // Un semáforo especial en el centro
        const centralLightIcon = L.divIcon({
            html: '<div style="width:10px;height:10px;background-color:green;border-radius:50%;box-shadow:0 0 8px green;"></div>',
            className: 'traffic-light-central',
            iconSize: [10, 10]
        });
        
        L.marker([centerLat, centerLng], {
            icon: centralLightIcon
        }).addTo(window.trafficGroup);
        
        // Informar del éxito
        console.log("Simulación de tráfico creada con éxito");
    """.trimIndent()

        // Ejecutar el script y verificar el resultado
        webView.evaluateJavascript(script) { result ->
            Log.d("TrafficSimulation", "Resultado: $result")
            isTrafficSimulationEnabled = true
        }
    }

    private fun clearTrafficSimulation() {
        if (isTrafficSimulationEnabled) {
            // Script para eliminar todos los elementos de la simulación
            val script = """
            // Eliminar el grupo de capas si existe
            if (window.trafficGroup) {
                window.trafficGroup.clearLayers();
                map.removeLayer(window.trafficGroup);
                delete window.trafficGroup;
                delete window.vehicles;
                console.log("Simulación de tráfico eliminada");
            }
        """.trimIndent()

            webView.evaluateJavascript(script, null)
            isTrafficSimulationEnabled = false
        }
    }

 
    // También agregar el método para actualizar el modo día/noche
    private fun toggleDayNightMode() {
        isDayMode = !isDayMode

        // Actualizar mapa y simulación
        val script = """
        // Actualizar capa base del mapa
        const baseMapUrl = '${if (isDayMode)
            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        else
            "https://cartodb-basemaps-{s}.global.ssl.fastly.net/dark_all/{z}/{x}/{y}.png"}';
        
        // Eliminar capas de mosaico existentes
        map.eachLayer(function(layer) {
            if (layer instanceof L.TileLayer) {
                map.removeLayer(layer);
            }
        });
        
        // Añadir nueva capa base
        L.tileLayer(baseMapUrl, {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);
        
        // Actualizar vehículos si la simulación está activa
        if (window.trafficSimulation && window.trafficSimulation.running) {
            for (const id in window.trafficSimulation.vehicles) {
                try {
                    const vehicle = window.trafficSimulation.vehicles[id];
                    const lightsClass = ${!isDayMode} ? " lights-on" : "";
                    const newIcon = L.divIcon({
                        html: '<div class="vehicle ' + vehicle.type + lightsClass + '" style="transform: rotate(' + vehicle.angle + 'deg);"></div>',
                        className: 'vehicle-container',
                        iconSize: [20, 12],
                        iconAnchor: [10, 6]
                    });
                    vehicle.marker.setIcon(newIcon);
                } catch (e) {}
            }
        }
    """.trimIndent()

        webView.evaluateJavascript(script, null)
        Toast.makeText(this, if (isDayMode) "Modo día activado" else "Modo noche activado", Toast.LENGTH_SHORT).show()
    }

    private fun getTrafficColorForDensity(density: Double): String {
        return when {
            density < 0.3 -> "#2ecc71" // Verde (tráfico fluido)
            density < 0.7 -> "#f1c40f" // Amarillo (tráfico moderado)
            else -> "#e74c3c"          // Rojo (tráfico congestionado)
        }
    }

    // Asegúrate de detener la simulación cuando se destruya la actividad
    override fun onDestroy() {
        super.onDestroy()
        trafficSimulator.stop()
    }

    private fun checkCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLatitude = location.latitude
                currentLongitude = location.longitude

                // Registrar visita y actualizar zonas
                viewModel.recordVisit(currentLatitude, currentLongitude)

                // Actualizar marcador de posición actual en el mapa
                val script = """
                // Actualizar posición del marcador de ubicación actual
                if (currentLocationMarker) {
                    currentLocationMarker.setLatLng([$currentLatitude, $currentLongitude]);
                }
            """.trimIndent()
                webView.evaluateJavascript(script, null)
            }
        }
    }

    // Añadir el launcher para la actividad de detalle
    private val poiDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                if (data.hasExtra("navigate_to_poi")) {
                    val poiId = data.getLongExtra("navigate_to_poi", -1)
                    if (poiId != -1L) {
                        viewModel.getPoiById(poiId).observe(this) { poi ->
                            poi?.let {
                                val script = """
                                map.setView([${poi.latitude}, ${poi.longitude}], 17);
                                L.marker([${poi.latitude}, ${poi.longitude}])
                                    .addTo(map)
                                    .bindPopup("${poi.name}")
                                    .openPopup();
                            """.trimIndent()
                                webView.evaluateJavascript(script, null)
                            }
                        }
                    }
                }
            }
        }
    }
}