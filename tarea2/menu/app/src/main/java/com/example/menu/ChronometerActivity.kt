package com.example.menu
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChronometerActivity : AppCompatActivity() {  // Cambiado el nombre de la clase

    private lateinit var tvTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnReset: Button

    private var seconds = 0
    private var running = false
    private var handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chronometer) // Asegúrate de que el layout tiene este nombre

        tvTimer = findViewById(R.id.tvTimer)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)
        // Encontrar el botón de regreso
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Lógica para regresar a MainActivity
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Terminar esta actividad para evitar que el usuario regrese con el botón de atrás
        }
        btnStart.setOnClickListener {
            running = true
            handler.post(updateTimer)
        }

        btnPause.setOnClickListener {
            running = false
            handler.removeCallbacks(updateTimer)
        }

        btnReset.setOnClickListener {
            running = false
            handler.removeCallbacks(updateTimer)
            seconds = 0
            tvTimer.text = "00:00:00"
        }
    }

    private val updateTimer = object : Runnable {
        override fun run() {
            if (running) {
                seconds++
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
                handler.postDelayed(this, 1000)
            }
        }
    }
}
