package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.content.ContentValues.TAG
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Si tienes una BaseActivity configurada, puedes heredar de ella:
// class DifficultySelectionActivity : BaseActivity() {
class DifficultySelectionActivity : BaseActivity() {
    private var soundPool: SoundPool? = null
    private var soundIdButtonClick: Int = 0
    private var soundsLoaded = false
    private val MAX_STREAMS = 5 // Número máximo de sonidos simultáneos
    // Constantes para la dificultad (opcional, pero buena práctica)
    companion object {
        const val EXTRA_DIFFICULTY = "com.example.guessthenum.DIFFICULTY"
        const val DIFFICULTY_EASY = "EASY"
        const val DIFFICULTY_MEDIUM = "MEDIUM"
        const val DIFFICULTY_HARD = "HARD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSoundPool() // Inicializar SoundPool y cargar sonidos
        enableEdgeToEdge()
        // Asegúrate que el XML se llame activity_difficulty_selection o el nombre que le hayas puesto
        setContentView(R.layout.activity_difficulty_selection)

        // Ajustar el padding para las barras del sistema
        // Asegúrate que el ID del layout principal sea el correcto (ej. R.id.difficulty_selection_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.difficulty_selection_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los botones
        val buttonEasy = findViewById<Button>(R.id.buttonEasy)
        val buttonMedium = findViewById<Button>(R.id.buttonMedium)
        val buttonHard = findViewById<Button>(R.id.buttonHard)
        val buttonBackToMenu = findViewById<Button>(R.id.buttonBackToMenu)

        // Listener para el botón "Fácil"
        buttonEasy.setOnClickListener {
            playSound(soundIdButtonClick)
            startGameWithDifficulty(DIFFICULTY_EASY)
        }

        // Listener para el botón "Medio"
        buttonMedium.setOnClickListener {
            playSound(soundIdButtonClick)
            startGameWithDifficulty(DIFFICULTY_MEDIUM)
        }

        // Listener para el botón "Difícil"
        buttonHard.setOnClickListener {
            playSound(soundIdButtonClick)
            startGameWithDifficulty(DIFFICULTY_HARD)
        }

        // Listener para el botón "Volver al Menú"
        buttonBackToMenu.setOnClickListener {
            playSound(soundIdButtonClick)
            // Cierra esta activity y vuelve a la anterior (MainActivity)
            finish()
        }
    }

    private fun startGameWithDifficulty(difficulty: String) {
        // Iniciar la GameActivity, pasando la dificultad seleccionada
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(EXTRA_DIFFICULTY, difficulty)
        }
        startActivity(intent)
        // Opcional: cierra esta activity después de iniciar el juego si no quieres que el usuario vuelva a ella con el botón back.
        // finish()
        Toast.makeText(this, "Iniciando juego en dificultad: $difficulty", Toast.LENGTH_SHORT).show()
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
