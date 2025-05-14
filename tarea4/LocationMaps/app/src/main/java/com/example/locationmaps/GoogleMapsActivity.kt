package com.example.locationmaps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class GoogleMapsActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var webView: WebView


    private var loadStartTime: Long = 0 // Tiempo de inicio de carga
    private var loadEndTime: Long = 0   // Tiempo de fin de carga

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_maps)

        webView = findViewById(R.id.webView)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        requestLocationPermissions()
    }
    // Añade esto al método onPause() o onStop()
    override fun onStop() {
        super.onStop()

        // Calcula y guarda las métricas antes de que la actividad se destruya
        val timeDifference = loadEndTime - loadStartTime
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Guarda los datos en SharedPreferences
        val sharedPreferences = getSharedPreferences("MapMetrics", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("googleLoadTime", timeDifference)
        editor.putLong("googleMemoryUsage", memoryUsage)
        editor.apply()
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
                // Maneja el caso en que los permisos no son otorgados
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
                val latitude = location.latitude
                val longitude = location.longitude
                loadGoogleMaps(latitude, longitude)
            }
        }.addOnFailureListener {
            // Maneja el error de ubicación
        }
    }

    private fun loadGoogleMaps(latitude: Double, longitude: Double) {
        val googleMapsUrl = "https://www.google.com/maps/@$latitude,$longitude,15z"

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadStartTime = SystemClock.elapsedRealtime() // Marca el inicio de la carga
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadEndTime = SystemClock.elapsedRealtime() // Marca el fin de la carga
            }
        }
        webView.loadUrl(googleMapsUrl)
    }


}