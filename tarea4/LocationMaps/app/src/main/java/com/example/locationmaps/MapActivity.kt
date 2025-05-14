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
// Eliminar la siguiente importación para evitar conflicto con la data class local LatLng
// import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
// import java.net.URLEncoder // No se está usando URLEncoder directamente aquí

class MapActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val ZONES_ACTIVITY_REQUEST_CODE = 101 // No usado directamente, pero mantenido

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
                    /* ... (tus estilos CSS existentes para vehículos y semáforos) ... */
                </style>
            </head>
            <body>
                <div id='map'></div>
                <script>
                    var map; // Hacer map accesible globalmente en este script
                    var currentLocationMarker; // Para el marcador de ubicación actual
                    var vehicleMarkers = {};
                    var trafficLightMarkers = {};
                    var roadPolylines = {};
                    var tempMarkers = []; // Para marcadores temporales de ruta
                    var currentRoutePolyline = null; // Para la polilínea de la ruta

                    function addVehicle(id, lat, lng, type, angle, lightsOn) {
                        // ... (tu función addVehicle existente) ...
                    }
                    
                    function addTrafficLight(id, lat, lng, state) {
                        // ... (tu función addTrafficLight existente) ...
                    }

                    // Funciones JS que se definirán/usarán en onPageFinished
                    // function addPOI(id, lat, lng, name, category) { ... }
                    // function addExplorationZone(id, lat, lng, radius, name, isExplored) { ... }
                    // function addTempMarker(lat, lng, text) { ... }
                    // function clearTempMarkers() { ... }
                    // function drawRoute(latLngs) { ... }
                    // function clearRoute() { ... }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    // Data class para coordenadas, local a MapActivity
    data class LatLng(val latitude: Double, val longitude: Double)

    // Propiedades para la generación de rutas
    private lateinit var routeButton: FloatingActionButton
    private var isSelectingRoutePoints = false
    private var routeStartPoint: LatLng? = null
    private var routeEndPoint: LatLng? = null

    // Propiedades existentes
    private var isTrafficSimulationEnabled = false
    private val vehicleMarkers = mutableMapOf<Int, String>()
    private val trafficLightMarkers = mutableMapOf<Int, String>()
    private val roadPolylines = mutableMapOf<Int, String>()

    private val zonesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {
                val latitude = it.getDoubleExtra("zoneLat", 0.0)
                val longitude = it.getDoubleExtra("zoneLng", 0.0)
                if (latitude != 0.0 && longitude != 0.0) {
                    webView.evaluateJavascript("map.setView([$latitude, $longitude], 16);", null)
                }
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                currentPhotoUri?.let { photoURI ->
                    val inputStream = contentResolver.openInputStream(photoURI)
                    currentPhotoBytes = inputStream?.readBytes()
                    inputStream?.close()
                    val dialogImageView = dialog.findViewById<ImageView>(R.id.poiPhotoImageView)
                    if (dialogImageView != null && currentPhotoBytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(currentPhotoBytes, 0, currentPhotoBytes!!.size)
                        dialogImageView.setImageBitmap(bitmap)
                    }
                    // Considera no eliminar el archivo temporal aquí si currentPhotoPath lo usa para guardarlo en el POI
                    // val tempFile = File(photoURI.path ?: "")
                    // if (tempFile.exists()) { tempFile.delete() }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var currentPhotoUri: Uri? = null
    private lateinit var dialog: BottomSheetDialog // Reutilizado para varios diálogos, podría ser mejor tener instancias separadas
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
        exploreZonesButton = findViewById(R.id.exploreZonesButton)
        routeButton = findViewById(R.id.routeButton) // Inicializar el botón de ruta

        locationUpdateHandler = Handler(Looper.getMainLooper())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this)[LocationExplorerViewModel::class.java]

        exploreZonesButton.setOnClickListener {
            val intent = Intent(this, ZonesActivity::class.java)
            zonesActivityLauncher.launch(intent)
        }

        setupObservers()
        setupButtons() // Configura addPoiButton y showPoiButton
        setupRoutingButton() // Configura el nuevo botón de ruta
        requestLocationPermissions()
    }

    override fun onResume() {
        super.onResume()
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }

    override fun onStop() {
        super.onStop()
        val timeDifference = loadEndTime - loadStartTime
        if (loadStartTime > 0 && loadEndTime > 0 && timeDifference > 0) { // Solo guardar si hay tiempos válidos
            val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            Log.d("OpenStreetMap", "Saving metrics - Load time: $timeDifference ms, Memory: $memoryUsage bytes")
            val sharedPreferences = getSharedPreferences("MapMetrics", MODE_PRIVATE)
            sharedPreferences.edit()
                .putLong("osmLoadTime", timeDifference)
                .putLong("osmMemoryUsage", memoryUsage)
                .apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearRouteOnMap()
        clearTempMarkers()
    }

    private fun setupObservers() {
        viewModel.explorationProgress.observe(this) { progress ->
            progressBar.progress = (progress * 100).toInt()
            explorationTextView.text = "${(progress * 100).toInt()}% explorado"
        }
        // viewModel.nearbyPlaces.observe... // Asegúrate de que esto es lo que quieres
        // En tu código original, nearbyPlaces y allPOIs actualizan con updateMapWithPOIs.
        // Esto podría llevar a duplicados si no se maneja bien en el JS o al limpiar el mapa.
        viewModel.allPOIs.observe(this) { pois -> // Observa allPOIs para actualizar el mapa
            updateMapWithPOIs(pois)
        }
        viewModel.suggestedZones.observe(this) { zones ->
            if (zones.isNotEmpty()) {
                showSuggestedZonesDialog(zones)
            }
        }
    }

    private fun setupButtons() {
        addPoiButton.setOnClickListener {
            isSelectingRoutePoints = false // Asegurarse de salir del modo ruta
            showAddPoiDialog()
        }
        showPoiButton.setOnClickListener {
            isSelectingRoutePoints = false // Asegurarse de salir del modo ruta
            showPointsOfInterestDialog()
        }
    }

    private fun setupRoutingButton() {
        routeButton.setOnClickListener {
            isSelectingRoutePoints = true
            routeStartPoint = null
            routeEndPoint = null
            clearTempMarkers() // Limpiar marcadores de selección anteriores
            clearRouteOnMap()  // Limpiar ruta dibujada anteriormente
            Toast.makeText(this, "Toca el mapa para seleccionar el punto de inicio...", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestLocationPermissions() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                loadUserLocation()
            } else {
                Toast.makeText(this, "Permisos de ubicación requeridos", Toast.LENGTH_SHORT).show()
            }
        }
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun loadUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                loadMap(currentLatitude, currentLongitude)
                viewModel.recordVisit(currentLatitude, currentLongitude)
                // viewModel.fetchNearbyPlaces(currentLatitude, currentLongitude) // Considera si esto es necesario aquí o en otro lugar
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual. Usando ubicación por defecto.", Toast.LENGTH_LONG).show()
                // Cargar mapa con una ubicación por defecto si es necesario
                loadMap(0.0, 0.0) // O una ubicación predeterminada más útil
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMap(latitude: Double, longitude: Double) {
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadStartTime = SystemClock.elapsedRealtime()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadEndTime = SystemClock.elapsedRealtime()
                Log.d("OpenStreetMap", "Map loaded. Load time: ${loadEndTime - loadStartTime} ms")

                val initScript = """
                    // Acceder a 'map' y 'currentLocationMarker' definidos globalmente en el script del HTML
                    map = L.map('map').setView([$latitude, $longitude], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    }).addTo(map);
                    
                    currentLocationMarker = L.marker([$latitude, $longitude]).addTo(map)
                        .bindPopup('Mi ubicación');
                    if ($latitude !== 0.0 || $longitude !== 0.0) { // Solo abrir popup si la ubicación es válida
                         currentLocationMarker.openPopup();
                    }
                    
                    var clickTimer;
                    map.on('mousedown', function(e) {
                        clickTimer = setTimeout(function() {
                            Android.onMapLongClick(e.latlng.lat, e.latlng.lng);
                        }, 500);
                    });
                    map.on('mouseup', function() { clearTimeout(clickTimer); });
                    map.on('dragstart', function() { clearTimeout(clickTimer); });
                    map.on('click', function(e) { Android.onMapClick(e.latlng.lat, e.latlng.lng); });

                    function addPOI(id, lat, lng, name, category) {
                        L.marker([lat, lng]).addTo(map).bindPopup('<b>' + name + '</b><br>' + category);
                    }
                    function addExplorationZone(id, lat, lng, radius, name, isExplored) {
                        var color = isExplored ? 'green' : 'red';
                        L.circle([lat, lng], { color: color, fillColor: color, fillOpacity: 0.2, radius: radius })
                            .addTo(map).bindPopup('<b>' + name + '</b><br>' + (isExplored ? 'Explorado' : 'Por explorar'));
                    }
                    function addTempMarker(lat, lng, text) {
                         var marker = L.marker([lat, lng], {
                              icon: L.divIcon({
                                  className: 'temp-marker', // Puedes estilizar esto en CSS si quieres
                                  html: '<div style="background-color: blue; color: white; padding: 2px 5px; border-radius: 3px; font-size: 10px;">' + text + '</div>',
                                  iconSize: null // Auto-size
                              })
                         }).addTo(map);
                         tempMarkers.push(marker);
                    }
                    function clearTempMarkers() {
                        tempMarkers.forEach(function(m) { map.removeLayer(m); });
                        tempMarkers = [];
                    }
                    function drawRoute(latLngsJsonString) {
                        if (currentRoutePolyline) { map.removeLayer(currentRoutePolyline); }
                        var latLngsArray = JSON.parse(latLngsJsonString); // Parsear el string JSON
                        currentRoutePolyline = L.polyline(latLngsArray, {color: 'blue', weight: 5}).addTo(map);
                        if (latLngsArray.length > 0) map.fitBounds(currentRoutePolyline.getBounds());
                    }
                    function clearRoute() {
                        if (currentRoutePolyline) { map.removeLayer(currentRoutePolyline); currentRoutePolyline = null; }
                    }
                """.trimIndent()
                webView.evaluateJavascript(initScript, null)
                loadSavedPOIs()
                loadExplorationZones()
            }
        }
        webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadSavedPOIs() {
        viewModel.allPOIs.observe(this) { pois ->
            // Considerar limpiar marcadores de POI anteriores antes de añadir nuevos para evitar duplicados
            // o tener una lógica en JS para actualizar/eliminar marcadores existentes.
            updateMapWithPOIs(pois)
        }
    }

    private fun loadExplorationZones() {
        viewModel.allZones.observe(this) { zones ->
            // Similar a POIs, considerar cómo se actualizan las zonas en el mapa
            updateMapWithZones(zones)
        }
    }

    private fun updateMapWithPOIs(pois: List<PointOfInterest>) {
        // Idealmente, antes de añadir, deberías limpiar los POIs antiguos del mapa
        // o usar IDs para actualizar/evitar duplicados.
        // Por simplicidad, aquí solo los añade.
        pois.forEach { poi ->
            val script = """addPOI(${poi.id}, ${poi.latitude}, ${poi.longitude}, "${poi.name.replace("\"", "\\\"")}", "${poi.category.replace("\"", "\\\"")}");"""
            webView.evaluateJavascript(script, null)
        }
    }

    private fun updateMapWithZones(zones: List<ExploredZone>) {
        zones.forEach { zone ->
            val script = """addExplorationZone(${zone.id}, ${zone.centerLatitude}, ${zone.centerLongitude}, ${zone.radius}, "${zone.name.replace("\"", "\\\"")}", ${zone.isExplored});"""
            webView.evaluateJavascript(script, null)
        }
    }

    // --- Métodos para POIs y Diálogos (sin cambios mayores, solo asegurando que 'dialog' se maneja bien) ---
    private fun showAddPoiDialog(latitude: Double? = null, longitude: Double? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poi, null)
        // Crear una nueva instancia de BottomSheetDialog para evitar conflictos si 'dialog' se usa en otro lado
        val poiDialog = BottomSheetDialog(this)
        poiDialog.setContentView(dialogView)
        this.dialog = poiDialog // Asignar a la propiedad de clase si es necesario para takePictureLauncher

        val nameEditText = dialogView.findViewById<EditText>(R.id.poiNameEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.poiDescriptionEditText)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.poiCategorySpinner)
        // val photoImageView = dialogView.findViewById<ImageView>(R.id.poiPhotoImageView) // Ya se accede en takePictureLauncher
        val takePictureButton = dialogView.findViewById<Button>(R.id.takePictureButton)
        val saveButton = dialogView.findViewById<Button>(R.id.savePOIButton)

        val categories = arrayOf("Restaurante", "Monumento", "Parque", "Museo", "Tienda", "Otro")
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        takePictureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                dispatchTakePictureIntent()
            }
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val category = categorySpinner.selectedItem?.toString() ?: "Otro"

            if (name.isNotEmpty()) {
                viewModel.insertPOI(PointOfInterest(
                    name = name, description = description,
                    latitude = latitude ?: currentLatitude, longitude = longitude ?: currentLongitude,
                    category = category, photoPath = currentPhotoPath
                ))
                poiDialog.dismiss()
                Toast.makeText(this, "Punto de interés guardado", Toast.LENGTH_SHORT).show()
                currentPhotoPath = null; currentPhotoUri = null; currentPhotoBytes = null
            } else {
                Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            }
        }
        poiDialog.show()
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir).apply {
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
                    // y asignar la URI a una variable local no nula para el launcher
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider", // Asegúrate que esta autoridad es correcta
                        photoFile
                    )
                    currentPhotoUri = photoURI // Asigna a la propiedad de la clase para uso posterior

                    // Usar la variable local 'photoURI' que es garantizada no nula aquí
                    takePictureLauncher.launch(photoURI)

                } ?: run {
                    Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (ex: Exception) {
            // Error occurred while creating the File or getting URI
            Log.e(TAG, "Error en dispatchTakePictureIntent: ${ex.message}", ex)
            Toast.makeText(this, "Error al preparar la cámara: ${ex.message}", Toast.LENGTH_SHORT).show()
            currentPhotoUri = null // Resetear si hubo un error
            currentPhotoPath = null // También resetear el path
        }
    }

    private fun showPointsOfInterestDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_points_of_interest, null)
        val poiListDialog = BottomSheetDialog(this) // Nueva instancia
        poiListDialog.setContentView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.poiRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val emptyTextView = dialogView.findViewById<TextView>(R.id.emptyPoisTextView)

        val adapter = PointOfInterestAdapter(
            emptyList(),
            onNavigateClicked = { poi ->
                webView.evaluateJavascript("map.setView([${poi.latitude}, ${poi.longitude}], 17); L.marker([${poi.latitude}, ${poi.longitude}]).addTo(map).bindPopup(\"${poi.name.replace("\"", "\\\"")}\").openPopup();", null)
                poiListDialog.dismiss()
            },
            onDeleteClicked = { poi ->
                viewModel.deletePOI(poi)
                Toast.makeText(this, "Punto de interés eliminado", Toast.LENGTH_SHORT).show()
                // El observer de allPOIs debería actualizar la lista
            }
        )
        recyclerView.adapter = adapter

        viewModel.allPOIs.observe(this) { pois ->
            adapter.updatePOIs(pois)
            if (pois.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
        dialogView.findViewById<Button>(R.id.closeDialogButton).setOnClickListener { poiListDialog.dismiss() }
        poiListDialog.show()
    }

    private fun showSuggestedZonesDialog(zones: List<ExploredZone>) {
        if (zones.isEmpty()) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_suggested_zones, null)
        val suggestedZonesDialog = BottomSheetDialog(this) // Nueva instancia
        suggestedZonesDialog.setContentView(dialogView)

        val firstZone = zones[0]
        dialogView.findViewById<TextView>(R.id.suggestedZoneNameTextView).text = firstZone.name
        dialogView.findViewById<Button>(R.id.navigateToZoneButton).setOnClickListener {
            webView.evaluateJavascript("map.setView([${firstZone.centerLatitude}, ${firstZone.centerLongitude}], 15);", null)
            suggestedZonesDialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.dismissSuggestionButton).setOnClickListener { suggestedZonesDialog.dismiss() }
        suggestedZonesDialog.show()
    }

    // --- Funciones para enrutamiento ---
    private fun addTempMarker(lat: Double, lng: Double, text: String) {
        val script = """addTempMarker($lat, $lng, '$text');"""
        webView.evaluateJavascript(script, null)
    }

    private fun clearTempMarkers() {
        webView.evaluateJavascript("clearTempMarkers();", null)
    }

    private fun showRoutingDialog() {
        val start = routeStartPoint ?: return // Salir si no hay punto de inicio
        val end = routeEndPoint ?: return // Salir si no hay punto final

        val dialogView = layoutInflater.inflate(R.layout.dialog_routing_options, null)
        val routingDialog = BottomSheetDialog(this) // Nueva instancia
        routingDialog.setContentView(dialogView)

        val startPointTextView = dialogView.findViewById<TextView>(R.id.startPointTextView)
        val endPointTextView = dialogView.findViewById<TextView>(R.id.endPointTextView)
        val transportModeSpinner = dialogView.findViewById<Spinner>(R.id.transportModeSpinner)
        val calculateRouteButton = dialogView.findViewById<Button>(R.id.calculateRouteButton)
        val routeResultTextView = dialogView.findViewById<TextView>(R.id.routeResultTextView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeRouteDialogButton)

        startPointTextView.text = "Inicio: ${"%.4f".format(start.latitude)}, ${"%.4f".format(start.longitude)}"
        endPointTextView.text = "Fin: ${"%.4f".format(end.latitude)}, ${"%.4f".format(end.longitude)}"

        val transportModes = resources.getStringArray(R.array.transport_modes)
        transportModeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transportModes)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        calculateRouteButton.setOnClickListener {
            val selectedMode = transportModeSpinner.selectedItem.toString()
            val vehicle = when (selectedMode) {
                "Coche" -> "car"; "Bicicleta" -> "bike"; "A pie" -> "foot"; else -> "car"
            }
            calculateAndDisplayRoute(start, end, vehicle, routeResultTextView)
            clearTempMarkers() // Limpiar marcadores de selección después de calcular
        }

        closeButton.setOnClickListener {
            routingDialog.dismiss()
            clearTempMarkers() // Limpiar marcadores si se cierra el diálogo
            //clearRouteOnMap()  // Limpiar la ruta si estaba dibujada
        }
        routingDialog.setOnDismissListener {
            // Asegurarse de limpiar si se cierra de otra forma (ej. swipe)
            clearTempMarkers()
            //clearRouteOnMap()
        }
        routingDialog.show()
    }

    private fun calculateAndDisplayRoute(start: LatLng, end: LatLng, vehicle: String, resultTextView: TextView) {
        clearRouteOnMap()
        resultTextView.visibility = View.GONE
        resultTextView.text = "Calculando..."
        resultTextView.visibility = View.VISIBLE


        // Reemplaza "YOUR_GRAPHHOPPER_API_KEY" o usa tu propio servidor
        val apiKey = BuildConfig.API_KEY
        //print ln(apiKey)


        val baseUrl = "https://graphhopper.com/api/1/route"

        val urlString = "$baseUrl?point=${start.latitude},${start.longitude}" +
                "&point=${end.latitude},${end.longitude}" +
                "&vehicle=$vehicle&locale=es&instructions=false&calc_points=true&points_encoded=false" +
                "&key=$apiKey"
        Log.d("Routing", "Request URL: $urlString")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.has("paths") && jsonResponse.getJSONArray("paths").length() > 0) {
                        val firstPath = jsonResponse.getJSONArray("paths").getJSONObject(0)
                        val distance = firstPath.getDouble("distance") / 1000 // km
                        val time = firstPath.getLong("time") / 1000 / 60 // minutes
                        val pointsArray = firstPath.getJSONObject("points").getJSONArray("coordinates")

                        val polylineLatLngs = mutableListOf<List<Double>>() // Lista de [lat, lng]
                        for (i in 0 until pointsArray.length()) {
                            val point = pointsArray.getJSONArray(i)
                            polylineLatLngs.add(listOf(point.getDouble(1), point.getDouble(0))) // API da [lng, lat], Leaflet necesita [lat, lng]
                        }

                        val resultText = "Distancia: %.2f km\nTiempo: %d min".format(distance, time)
                        withContext(Dispatchers.Main) {
                            resultTextView.text = resultText
                            drawRouteOnMap(polylineLatLngs)

                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            resultTextView.text = "Ruta no encontrada."
                            Toast.makeText(this@MapActivity, jsonResponse.optString("message", "Ruta no encontrada"), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error HTTP ${connection.responseCode}"
                    Log.e("Routing", "HTTP Error: ${connection.responseCode} - $errorMsg")
                    withContext(Dispatchers.Main) {
                        resultTextView.text = "Error: $errorMsg"
                        Toast.makeText(this@MapActivity, "Error ($errorMsg)", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Routing", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    resultTextView.text = "Error de red: ${e.localizedMessage}"
                    Toast.makeText(this@MapActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun drawRouteOnMap(latLngs: List<List<Double>>) { // Acepta List<List<Double>>
        // Convertir la lista de [lat, lng] a un string JSON para JavaScript
        val latLngArrayJson = latLngs.joinToString(prefix = "[", postfix = "]") { "[${it[0]}, ${it[1]}]" }
        val script = """drawRoute('$latLngArrayJson');""" // Pasar como string JSON
        webView.evaluateJavascript(script, null)
    }

    private fun clearRouteOnMap() {
        webView.evaluateJavascript("clearRoute();", null)
    }

    private fun checkCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLatitude = it.latitude
                currentLongitude = it.longitude
                viewModel.recordVisit(currentLatitude, currentLongitude)
                val script = """
                    if (currentLocationMarker) {
                        currentLocationMarker.setLatLng([$currentLatitude, $currentLongitude]);
                        // map.panTo([$currentLatitude, $currentLongitude]); // Opcional: centrar mapa
                    }
                """.trimIndent()
                webView.evaluateJavascript(script, null)
            }
        }
    }

    // --- Clase interna WebAppInterface ---
    inner class WebAppInterface {
        @JavascriptInterface
        fun onMapLongClick(lat: Double, lng: Double) {
            this@MapActivity.runOnUiThread {
                if (isSelectingRoutePoints) {
                    val clickedLatLng = LatLng(lat, lng) // Usa la LatLng de MapActivity
                    if (routeStartPoint == null) {
                        routeStartPoint = clickedLatLng
                        Toast.makeText(this@MapActivity, "Punto de inicio seleccionado. Toca el mapa para el punto final.", Toast.LENGTH_SHORT).show()
                        addTempMarker(lat, lng, "Inicio")
                    } else if (routeEndPoint == null) {
                        routeEndPoint = clickedLatLng
                        Toast.makeText(this@MapActivity, "Punto final seleccionado.", Toast.LENGTH_SHORT).show()
                        addTempMarker(lat, lng, "Fin")
                        showRoutingDialog()
                        // isSelectingRoutePoints se volverá false si el usuario cierra el diálogo o calcula
                    }
                } else {
                    showAddPoiDialog(lat, lng)
                }
            }
        }

        @JavascriptInterface
        fun onMapClick(lat: Double, lng: Double) {
            this@MapActivity.runOnUiThread {
                if (isSelectingRoutePoints) {
                    val clickedLatLng = LatLng(lat, lng)
                    if (routeStartPoint == null) {
                        routeStartPoint = clickedLatLng
                        Toast.makeText(this@MapActivity, "Punto de inicio seleccionado. Toca el mapa para el punto final.", Toast.LENGTH_SHORT).show()
                        addTempMarker(lat, lng, "Inicio")
                    } else if (routeEndPoint == null) {
                        routeEndPoint = clickedLatLng
                        Toast.makeText(this@MapActivity, "Punto final seleccionado.", Toast.LENGTH_SHORT).show()
                        addTempMarker(lat, lng, "Fin")
                        showRoutingDialog()
                    }
                }
                // Si no está en modo selección de ruta, el click simple no hace nada por ahora
            }
        }
    }

    // Launcher para detalle de POI (no modificado, pero asegúrate que funciona como esperas)
    private val poiDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getLongExtra("navigate_to_poi", -1L)?.takeIf { it != -1L }?.let { poiId ->
                viewModel.getPoiById(poiId).observe(this) { poi ->
                    poi?.let {
                        val script = """
                            map.setView([${it.latitude}, ${it.longitude}], 17);
                            L.marker([${it.latitude}, ${it.longitude}])
                                .addTo(map)
                                .bindPopup("${it.name.replace("\"", "\\\"")}")
                                .openPopup();
                        """.trimIndent()
                        webView.evaluateJavascript(script, null)
                    }
                }
            }
        }
    }
}
