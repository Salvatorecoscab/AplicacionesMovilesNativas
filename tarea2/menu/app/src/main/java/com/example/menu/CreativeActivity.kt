package com.example.menu
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CreativeActivity : AppCompatActivity() {  // Cambiado el nombre de la clase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galaxy)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val btnSistemaSolar = findViewById<Button>(R.id.btnSistemaSolar)

        btnSistemaSolar.setOnClickListener {
            val intent = Intent(this, SistemaSolarActivity::class.java)
            startActivity(intent)
        }


    }
}
