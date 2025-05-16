package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.content.ContentValues.TAG
import android.content.Intent

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.media.AudioAttributes // Importar AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.guessthenum.GameActivity.Companion

// Asumo que tienes una BaseActivity, si no, puedes cambiarlo a AppCompatActivity
// y quitar las referencias a BaseActivity o implementar tu propia BaseActivity
// si la necesitas para temas dinámicos u otra funcionalidad común.
// class MainActivity : BaseActivity() {
class MainActivity : BaseActivity()  { // Cambio temporal si BaseActivity no está definida aún
    private var pendingThemeChange = false


    private var soundPool: SoundPool? = null
    private var soundIdButtonClick: Int = 0
    private var soundsLoaded = false
    private val MAX_STREAMS = 5 // Número máximo de sonidos simultáneos
    private fun updateThemeButtonText(button: Button) {
        val currentTheme = themeHelper.getCurrentTheme()
        val buttonText = if (currentTheme == "ipn") {
            "tema ESCOM"
        } else {
            "tema IPN"
        }
        button.text = buttonText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initializeSoundPool() // Inicializar SoundPool y cargar sonidos
        // Ajustar el padding para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los botones
        val buttonNewGame = findViewById<Button>(R.id.buttonNewGame)
        val buttonLoadGame = findViewById<Button>(R.id.buttonLoadGame)
        val buttonExit = findViewById<Button>(R.id.buttonExit)
        val btnToggleTheme = findViewById<Button>(R.id.btnToggleTheme)
        updateThemeButtonText(btnToggleTheme)

        btnToggleTheme.setOnClickListener {
            playSound(soundIdButtonClick)
            // Cambiar tema
            val newTheme = themeHelper.toggleTheme(this)
            pendingThemeChange = true
            updateThemeButtonText(btnToggleTheme)
            recreate()

        }
        // Listener para el botón "Nuevo Juego"
        buttonNewGame.setOnClickListener {
            playSound(soundIdButtonClick)
            val intent = Intent(this, DifficultySelectionActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Nuevo Juego presionado", Toast.LENGTH_SHORT).show()

        }

        // Listener para el botón "Cargar Juego"
        buttonLoadGame.setOnClickListener {
            playSound(soundIdButtonClick)
            val intent = Intent(this, LoadGameActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Cargar Juego presionado", Toast.LENGTH_SHORT).show()

        }

        // Listener para el botón "Salir"
        buttonExit.setOnClickListener {
            playSound(soundIdButtonClick)
            // Cierra la aplicación
            finishAffinity() // Cierra todas las activities de la app
            // O si solo quieres cerrar esta activity:
            // finish()
            Toast.makeText(this, "Saliendo...", Toast.LENGTH_SHORT).show()

        }
    }
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                // Podrías necesitar contar cuántos sonidos se han cargado
                // si tienes muchos, pero para unos pocos esto es simple.
                // Para un manejo más robusto, usa un contador.
                soundsLoaded = true
                Log.d(TAG, "Sonido cargado correctamente.")
            } else {
                Log.e(TAG, "Error al cargar sonido, estado: $status")
            }
        }

        // Cargar los sonidos (asegúrate de tener estos archivos en res/raw)
        // Si un archivo no existe, la app crasheará al intentar cargarlo.
        // Es buena práctica verificar la existencia o usar try-catch si es necesario.
        try {
            soundIdButtonClick = soundPool?.load(this, R.raw.button_click, 1) ?: 0

        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar sonidos. Revisa la carpeta res/raw.", Toast.LENGTH_LONG).show()
        }
    }
    private fun playSound(soundID: Int, loop: Int = 0) { // loop 0 = no loop, -1 = loop forever
        if (soundsLoaded && soundID != 0) {
            soundPool?.play(soundID, 1.0f, 1.0f, 1, loop, 1.0f)
        } else if (soundID == 0) {
            Log.w(GameActivity.TAG, "Intentando reproducir soundID 0 (no cargado o error).")
        }
        else {
            Log.w(GameActivity.TAG, "Sonidos aún no cargados, no se puede reproducir.")
        }
    }
}
