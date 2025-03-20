package com.example.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnCalculator = findViewById<Button>(R.id.btnCalculator)
        val btnChronometer = findViewById<Button>(R.id.btnChronometer)
        val btnCreative = findViewById<Button>(R.id.btnCreative)
        btnCalculator.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        btnChronometer.setOnClickListener {
            val intent = Intent(this, ChronometerActivity::class.java)
            startActivity(intent)
        }
        btnCreative.setOnClickListener {
            val intent = Intent(this, CreativeActivity::class.java)
            startActivity(intent)
        }
    }
}
