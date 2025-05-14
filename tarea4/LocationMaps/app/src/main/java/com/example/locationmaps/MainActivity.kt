package com.example.locationmaps


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openOSMButton: Button = findViewById(R.id.openOSMButton)
        val openGoogleMapsButton: Button = findViewById(R.id.openGoogleMapsButton)

        openOSMButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        openGoogleMapsButton.setOnClickListener {
            val intent = Intent(this, GoogleMapsActivity::class.java)
            startActivity(intent)
        }
        val metricsButton: Button = findViewById(R.id.metricsButton)
        metricsButton.setOnClickListener {
            val intent = Intent(this, MetricsActivity::class.java)
            startActivity(intent)
        }
    }
}