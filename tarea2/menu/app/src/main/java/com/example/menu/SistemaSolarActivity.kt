package com.example.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class SistemaSolarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sistemasolar)

        // Back Button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Planet Buttons
        val btnMercurio = findViewById<Button>(R.id.btnMercurio)
        val btnVenus = findViewById<Button>(R.id.btnVenus)
        val btnTierra = findViewById<Button>(R.id.btnTierra)
        val btnMarte = findViewById<Button>(R.id.btnMarte)
        val btnJupiter = findViewById<Button>(R.id.btnJupiter)
        val btnSaturno = findViewById<Button>(R.id.btnSaturno)
        val btnUrano = findViewById<Button>(R.id.btnUrano)
        val btnNeptuno = findViewById<Button>(R.id.btnNeptuno)

        btnMercurio.setOnClickListener {
            val intent = Intent(this, MercurioActivity::class.java)
            startActivity(intent)
        }

        btnVenus.setOnClickListener {
            val intent = Intent(this, VenusActivity::class.java)
            startActivity(intent)
        }

        btnTierra.setOnClickListener {
            val intent = Intent(this, TierraActivity::class.java)
            startActivity(intent)
        }

        btnMarte.setOnClickListener {
            val intent = Intent(this, MarteActivity::class.java)
            startActivity(intent)
        }

        btnJupiter.setOnClickListener {
            val intent = Intent(this, JupiterActivity::class.java)
            startActivity(intent)
        }

        btnSaturno.setOnClickListener {
            val intent = Intent(this, SaturnoActivity::class.java)
            startActivity(intent)
        }

        btnUrano.setOnClickListener {
            val intent = Intent(this, UranoActivity::class.java)
            startActivity(intent)
        }

        btnNeptuno.setOnClickListener {
            val intent = Intent(this, NeptunoActivity::class.java)
            startActivity(intent)
        }
    }
}