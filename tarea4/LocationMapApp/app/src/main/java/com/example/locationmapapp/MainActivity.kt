package com.example.locationmapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class MainActivity() : AppCompatActivity(), Parcelable {

    private lateinit var mapWebView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOpenStreetMap: Button
    private lateinit var btnGoogleMaps: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Variables para métricas de rendimiento
    private var loadStartTime: Long = 0
    private var memoryBaseline: Long = 0

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            startLocationUpdates()
        } else {
            Toast.makeText(
                this,
                "Se requiere permiso de ubicación para mostrar el mapa",
                Toast.LENGTH_LONG
            ).show()
            progressBar.visibility = View.GONE
        }
    }

    constructor(parcel: Parcel) : this() {
        loadStartTime = parcel.readLong()
        memoryBaseline = parcel.readLong()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Iniciar medición de memoria
        memoryBaseline = getUsedMemory()

        mapWebView = findViewById(R.id.mapWebView)
        progressBar = findViewById(R.id.progressBar)
        btnOpenStreetMap = findViewById(R.id.btnOpenStreetMap)
        btnGoogleMaps = findViewById(R.id.btnGoogleMaps)

        // Configurar botones de navegación
        setupNavigationButtons()

        // Configurar WebView
        setupWebView()

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar callback de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapWithLocation(location)
                }
            }
        }

        // Verificar y solicitar permisos
        checkAndRequestLocationPermissions()
    }

    private fun setupNavigationButtons() {
        btnOpenStreetMap.setOnClickListener {
            // Ya estamos en OpenStreetMap, no hacemos nada
        }

        btnGoogleMaps.setOnClickListener {
            // Navegar a la Activity de Google Maps
            val intent = Intent(this, GoogleMapsActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnMetrics).setOnClickListener {
            val intent = Intent(this, PerformanceResultsActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = mapWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        mapWebView.webViewClient = object : WebViewClient() {
            // En la función onPageFinished del webViewClient
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                // Calcular tiempo de carga
                val loadTime = System.currentTimeMillis() - loadStartTime
                PerformanceMetrics.recordMetric("OpenStreetMap_LoadTime", loadTime)

                // Calcular consumo de memoria
                val memoryUsed = getUsedMemory() - memoryBaseline
                PerformanceMetrics.recordMetric("OpenStreetMap_MemoryUsage", memoryUsed)
            }
        }
        mapWebView.setOnLongClickListener { view ->
            val latLng = getLatLngFromView(view) // Implementa esta función para obtener coordenadas
            showPOIDialog(latLng)
            true
        }

        private fun showPOIDialog(latLng: LatLng) {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Guardar Punto de Interés")
                .setMessage("¿Deseas guardar este lugar?")
                .setPositiveButton("Guardar") { _, _ ->
                    savePOI(latLng)
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }

        private fun savePOI(latLng: LatLng) {
            val poi = PointOfInterest(
                id = UUID.randomUUID().toString(),
                name = "Nuevo Punto",
                category = "Favorito",
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                notes = "",
                imagePath = ""
            )
            // Guarda en Room o Firebase
            poiDatabase.save(poi)
            Toast.makeText(this, "Punto guardado", Toast.LENGTH_SHORT).show()
        }

        // Registrar tiempo de inicio de carga
        loadStartTime = System.currentTimeMillis()

        // Cargar el archivo HTML desde assets
        mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Ya tenemos permisos, iniciar seguimiento de ubicación
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Configurar solicitud de ubicación
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 segundos
            fastestInterval = 5000 // 5 segundos
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Iniciar actualizaciones de ubicación
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Obtener última ubicación conocida para inicializar el mapa
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    updateMapWithLocation(it)
                } ?: run {
                    // Si no hay última ubicación conocida, mostrar mensaje
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación actual. Espere un momento.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                // Manejar error al obtener ubicación
                Toast.makeText(
                    this,
                    "Error al obtener la ubicación: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
            }
    }



    // En la función updateMapWithLocation
    private fun updateMapWithLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        // Medir el tiempo de respuesta para actualizar la ubicación
        val startTime = System.currentTimeMillis()

        // Usar JavaScript para actualizar el mapa con las nuevas coordenadas
        mapWebView.evaluateJavascript(
            "updateLocation($latitude, $longitude);",
            {
                // Calcular tiempo de respuesta
                val responseTime = System.currentTimeMillis() - startTime
                PerformanceMetrics.recordMetric("OpenStreetMap_UpdateResponseTime", responseTime)
            }
        )

        // Ocultar el indicador de progreso
        progressBar.visibility = View.GONE
    }

    // Funciones para métricas de rendimiento
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun recordMetric(name: String, value: Long) {
        // En una implementación real, podrías guardar estas métricas en un archivo, base de datos
        // o enviarlas a un servicio analítico. Para este ejercicio, las imprimimos en el log.
        println("MÉTRICA - $name: $value")
    }

    override fun onPause() {
        super.onPause()
        // Detener actualizaciones de ubicación cuando la app está en pausa
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        // Verificar permisos y reiniciar actualizaciones si es necesario
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(loadStartTime)
        parcel.writeLong(memoryBaseline)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }

    }
}