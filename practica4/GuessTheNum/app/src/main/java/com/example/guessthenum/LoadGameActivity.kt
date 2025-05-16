package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // Cambiado de BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

// Cambiado de BaseActivity a AppCompatActivity por si BaseActivity no está definida
// o causa problemas. Si tienes una BaseActivity funcional, puedes revertirlo.
class LoadGameActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadGameAdapter: LoadGameAdapter
    private val savedGamesList = mutableListOf<SavedGame>()
    private lateinit var textViewNoGames: TextView
    private var soundPool: SoundPool? = null
    private var soundIdButtonClick: Int = 0
    private var soundsLoaded = false
    private val MAX_STREAMS = 5 // Número máximo de sonidos simultáneos
    private val gson = Gson()

    companion object {
        private const val SAVED_GAMES_FILE = "saved_hangman_games.json" // Debe coincidir con GameActivity
        private const val TAG = "LoadGameActivity"
    }

    private val gameActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Retornando de GameActivity, recargando lista de juegos guardados.")
            loadGamesFromJson()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeSoundPool() // Inicializar SoundPool y cargar sonidos
        setContentView(R.layout.activity_load_game) // Asegúrate que este layout exista

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.load_game_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()

        loadGamesFromJson()

        val buttonBack = findViewById<Button>(R.id.buttonBackFromLoad)
        buttonBack.setOnClickListener {
            playSound(soundIdButtonClick)
            // Cierra esta activity y vuelve a la anterior (MainActivity)
            finish()
        }

        checkEmptyList()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewLoadGame)
        textViewNoGames = findViewById(R.id.textViewNoGames)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        // La llamada al constructor del adaptador:
        // Los nombres de los parámetros (onItemClick, onDeleteClick) deben coincidir
        // con los definidos en el constructor de LoadGameAdapter.
        loadGameAdapter = LoadGameAdapter(
            savedGamesList,
            onItemClick = { gameToLoad -> // Parámetro nombrado onItemClick
                val intent = Intent(this, GameActivity::class.java).apply {
                    putExtra(GameActivity.EXTRA_LOAD_GAME_ID, gameToLoad.id)
                }
                gameActivityResultLauncher.launch(intent)
            },
            onDeleteClick = { gameToDelete -> // Parámetro nombrado onDeleteClick
                handleDeleteGame(gameToDelete)
            }
        )
        recyclerView.adapter = loadGameAdapter
    }

    private fun loadGamesFromJson() {
        val file = File(filesDir, SAVED_GAMES_FILE)
        if (!file.exists()) {
            Log.d(TAG, "$SAVED_GAMES_FILE no encontrado. No hay juegos guardados.")
            savedGamesList.clear()
            if (::loadGameAdapter.isInitialized) {
                loadGameAdapter.notifyDataSetChanged()
            }
            checkEmptyList()
            return
        }

        try {
            val fileReader = FileReader(file)
            val type = object : TypeToken<List<SavedGame>>() {}.type
            val games: List<SavedGame>? = gson.fromJson(fileReader, type)
            fileReader.close()

            savedGamesList.clear()
            if (games != null) {
                savedGamesList.addAll(games)
                Log.d(TAG, "Cargados ${games.size} juegos desde $SAVED_GAMES_FILE")
            } else {
                Log.d(TAG, "No se pudieron deserializar juegos o el archivo estaba vacío.")
            }
            if (::loadGameAdapter.isInitialized) {
                loadGameAdapter.notifyDataSetChanged()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error cargando juegos desde JSON: ${e.message}", e)
            savedGamesList.clear()
            if (::loadGameAdapter.isInitialized) {
                loadGameAdapter.notifyDataSetChanged()
            }
        }
        checkEmptyList()
    }

    private fun handleDeleteGame(gameToDelete: SavedGame) {
        val position = savedGamesList.indexOf(gameToDelete)
        if (position != -1) {
            savedGamesList.removeAt(position)
            loadGameAdapter.notifyItemRemoved(position)
            loadGameAdapter.notifyItemRangeChanged(position, savedGamesList.size)
            saveGamesListToJson()
            checkEmptyList()
        }
    }

    private fun saveGamesListToJson() {
        try {
            val jsonString = gson.toJson(savedGamesList)
            val fileOutputStream: FileOutputStream = openFileOutput(SAVED_GAMES_FILE, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonString.toByteArray())
            fileOutputStream.close()
            Log.d(TAG, "Lista de juegos actualizada guardada en $SAVED_GAMES_FILE")
        } catch (e: IOException) {
            Log.e(TAG, "Error guardando lista de juegos actualizada: ${e.message}", e)
        }
    }

    private fun checkEmptyList() {
        if (savedGamesList.isEmpty()) {
            recyclerView.visibility = View.GONE
            textViewNoGames.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            textViewNoGames.visibility = View.GONE
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
                Log.d(ContentValues.TAG, "Sonido cargado correctamente.")
            } else {
                Log.e(ContentValues.TAG, "Error al cargar sonido, estado: $status")
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
