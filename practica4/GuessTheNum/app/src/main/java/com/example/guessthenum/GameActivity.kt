package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes // Importar AudioAttributes
import android.media.SoundPool // Importar SoundPool
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class GameActivity : BaseActivity() {

    companion object {
        private const val CURRENT_GAME_STATE_FILE = "current_hangman_game.json"
        private const val SAVED_GAMES_FILE = "saved_hangman_games.json"
        const val TAG = "GameActivity"
        const val EXTRA_LOAD_GAME_ID = "com.example.guessthenum.LOAD_GAME_ID"

        private val FALLBACK_WORDS_BY_LENGTH: Map<Int, List<String>> = mapOf(
            4 to listOf(
                "CASA", "GATO", "PERA", "MANO", "SOLA", "LUNA", "AGUA", "PATO", "ROJO", "AZUL",
                "RANA", "PICO", "DURO", "FINO", "ALTO", "BAJO", "RICO", "VIDA", "PILA", "NOTA",
                "HOME", "CAT", "PEAR", "HAND", "SUN", "MOON", "WATER", "DUCK", "RED", "BLUE",
                "FROG", "BEAK", "HARD", "FINE", "TALL", "LOW", "RICH", "LIFE", "NOTE", "CODE"
            ),
            5 to listOf(
                "ARBOL", "FLACO", "LIBRO", "QUESO", "PLAYA", "VERDE", "FELIZ", "TRONO", "PLUMA", "BRAVO",
                "NUEVO", "CORAL", "FAROL", "MOTOR", "PIANO", "REGLA", "SALSA", "TENIS", "VIAJE", "ZORRO",
                "TREE", "THIN", "BOOK", "CHEESE", "BEACH", "GREEN", "HAPPY", "THRONE", "FEATHER", "BRAVE",
                "APPLE", "BREAD", "CHAIR", "DANCE", "EARTH", "FIELD", "GLASS", "HEART", "IDEAL", "JOLLY"
            ),
            6 to listOf(
                "FUENTE", "JARDIN", "MUSICA", "PIRATA", "VOLCAN", "PLANTA", "AMABLE", "BARATO", "CENTRO", "DULCES",
                "ESPEJO", "FLORES", "GRANDE", "HEROE", "INVIERNO", "LIMPIO", "MADERA", "OBJETO", "PODER", "QUESOS",
                "SOURCE", "GARDEN", "MUSIC", "PIRATE", "VOLCANO", "PLANT", "FRIEND", "CHEAP", "CENTER", "SWEETS",
                "MIRROR", "FLOWER", "ORANGE", "PURPLE", "SILVER", "SPIRIT", "SUMMER", "TABLE", "WINDOW", "YELLOW"
            ),
            7 to listOf(
                "CABALLO", "ESCUELA", "MONTAÑA", "NARANJA", "SILENCIO", "TORMENTA", "ALFOMBRA", "BOTELLA", "CEREBRO", "DIAMANTE",
                "ENERGIA", "FANTASIA", "GIGANTE", "HISTORIA", "JUSTICIA", "LIBERTAD", "MISTERIO", "PERFUME", "PROBLEMA", "VICTORIA",
                "HORSE", "SCHOOL", "MOUNTAIN", "ORANGE", "SILENCE", "STORM", "CARPET", "BOTTLE", "BRAIN", "DIAMOND",
                "COUNTRY", "EXAMPLE", "FOREVER", "FREEDOM", "JOURNEY", "KITCHEN", "LIBRARY", "MESSAGE", "MYSTERY", "PERFECT"
            ),
            8 to listOf(
                "AVENTURA", "BIBLIOTECA", "CHOCOLATE", "DINOSAURIO", "ELEFANTE", "FANTASMA", "FESTIVAL", "GASOLINA", "HOSPITAL", "INSECTOS",
                "LINTERNA", "MALETERO", "MEDICINA", "NATURALEZA", "ORQUESTA", "PELICULA", "PROGRAMA", "RESPUESTA", "SATELITE", "TERREMOTO",
                "ADVENTURE", "LIBRARY", "CHOCOLATE", "DINOSAUR", "ELEPHANT", "FESTIVAL", "GASOLINE", "HOSPITAL", "LANTERN", "MEDICINE",
                "COMPUTER", "LANGUAGE", "QUESTION", "REMEMBER", "SOLUTION", "STANDARD", "SURPRISE", "TOMORROW", "TOGETHER", "TREASURE"
            ),
            9 to listOf(
                "GUITARRA", "HELICOPTERO", "INTERNET", "JIRAFALES", "MARIPOSA", "UNIVERSO", "BICICLETA", "CALABAZA", "CASCABEL", "DOCUMENTO",
                "ELECTRICIDAD", "ESTUDIANTE", "FILOSOFIA", "GOBIERNO", "INDEPENDENCIA", "LABERINTO", "MATEMATICAS", "PRESIDENTE", "REVOLUCION", "TRADICION",
                "GUITAR", "HELICOPTER", "INTERNET", "BUTTERFLY", "UNIVERSE", "BICYCLE", "PUMPKIN", "DOCUMENT", "ELECTRIC", "STUDENT",
                "EXCELLENT", "IMPORTANT", "KNOWLEDGE", "NECESSARY", "OPERATION", "PRINCIPLE", "RELECTION", "SIGNATURE", "TELEPHONE", "WONDERFUL"
            ),
            10 to listOf(
                "COMPUTADORA", "ESPERANZA", "FELICIDAD", "IMAGINACION", "RESTAURANTE", "TECNOLOGIA", "AGRICULTURA", "ARQUITECTURA", "CONSTITUCION", "DESCUBRIMIENTO",
                "ENCICLOPEDIA", "EXPERIMENTO", "FOTOGRAFIA", "GENERACION", "INVESTIGAR", "LITERATURA", "MEDIOAMBIENTE", "OPORTUNIDAD", "PROFESIONAL", "RESPONSABLE",
                "COMPUTER", "RESTAURANT", "TECHNOLOGY", "AGRICULTURE", "DISCOVERY", "EXPERIMENT", "GENERATION", "IMAGINE", "LITERATURE", "OPPORTUNITY",
                "CONNECTION", "DEFINITION", "DIFFERENCE", "EDUCATION", "ENVIRONMENT", "INFORMATION", "INSTRUCTION", "PERFORMANCE", "POSSIBILITY", "UNDERSTAND"
            )
        )
    }

    // Vistas de UI
    private lateinit var textViewWordDisplay: TextView
    private lateinit var textViewAttemptsLeftValue: TextView
    private lateinit var textViewGuessedLettersValue: TextView
    private lateinit var editTextGuess: EditText
    private lateinit var buttonGuess: Button
    private lateinit var textViewGameStatus: TextView
    private lateinit var buttonPlayAgain: Button
    private lateinit var buttonBackToMenu: Button
    private lateinit var hangmanView: HangmanView

    // Estado del juego
    private var wordToGuess = ""
    private var displayedWord = ""
    private var attemptsLeft = 6
    private val guessedLetters = mutableSetOf<Char>()
    private var gameDifficulty = DifficultySelectionActivity.DIFFICULTY_EASY
    private var currentGameId: String = ""
    private var isGameInProgress = false
    private var gameNumber: String = ""

    private val gson = Gson()

    // SoundPool y IDs de sonido
    private var soundPool: SoundPool? = null
    private var soundIdButtonClick: Int = 0
    private var soundIdCorrectGuess: Int = 0
    private var soundIdWrongGuess: Int = 0
    private var soundIdGameWin: Int = 0
    private var soundIdGameLoss: Int = 0
    private var soundsLoaded = false
    private val MAX_STREAMS = 5 // Número máximo de sonidos simultáneos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.game_main_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        initializeSoundPool() // Inicializar SoundPool y cargar sonidos

        val loadGameId = intent.getStringExtra(EXTRA_LOAD_GAME_ID)
        if (loadGameId != null) {
            loadExistingGame(loadGameId)
        } else {
            gameDifficulty = intent.getStringExtra(DifficultySelectionActivity.EXTRA_DIFFICULTY)
                ?: DifficultySelectionActivity.DIFFICULTY_EASY
            gameNumber = "Juego #${getNextGameNumber()}"
            setupNewGameFramework()
            fetchWordFromApi(determineWordLength(gameDifficulty))
        }

        buttonGuess.setOnClickListener {
            playSound(soundIdButtonClick)
            handleGuess()
        }
        buttonPlayAgain.setOnClickListener {
            playSound(soundIdButtonClick)
            gameNumber = "Juego #${getNextGameNumber()}"
            setupNewGameFramework()
            fetchWordFromApi(determineWordLength(gameDifficulty))
        }
        buttonBackToMenu.setOnClickListener {
            playSound(soundIdButtonClick)
            handleExitGame()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                playSound(soundIdButtonClick) // También al presionar atrás
                handleExitGame()
            }
        })
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
            soundIdCorrectGuess = soundPool?.load(this, R.raw.correct_guess, 1) ?: 0
            soundIdWrongGuess = soundPool?.load(this, R.raw.wrong_guess, 1) ?: 0
            soundIdGameWin = soundPool?.load(this, R.raw.game_win, 1) ?: 0
            soundIdGameLoss = soundPool?.load(this, R.raw.game_loss, 1) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar archivos de sonido desde res/raw. Asegúrate que existan.", e)
        }
    }

    private fun playSound(soundID: Int, loop: Int = 0) { // loop 0 = no loop, -1 = loop forever
        if (soundsLoaded && soundID != 0) {
            soundPool?.play(soundID, 1.0f, 1.0f, 1, loop, 1.0f)
        } else if (soundID == 0) {
            Log.w(TAG, "Intentando reproducir soundID 0 (no cargado o error).")
        }
        else {
            Log.w(TAG, "Sonidos aún no cargados, no se puede reproducir.")
        }
    }


    private fun initializeViews() {
        textViewWordDisplay = findViewById(R.id.textViewWordDisplay)
        textViewAttemptsLeftValue = findViewById(R.id.textViewAttemptsLeftValue)
        textViewGuessedLettersValue = findViewById(R.id.textViewGuessedLettersValue)
        editTextGuess = findViewById(R.id.editTextGuess)
        buttonGuess = findViewById(R.id.buttonGuess)
        textViewGameStatus = findViewById(R.id.textViewGameStatus)
        buttonPlayAgain = findViewById(R.id.buttonPlayAgain)
        buttonBackToMenu = findViewById(R.id.buttonBackToMenuFromGame)
        hangmanView = findViewById(R.id.hangmanView)
    }

    private fun setupNewGameFramework() {
        isGameInProgress = true
        guessedLetters.clear()
        attemptsLeft = 6
        wordToGuess = ""
        displayedWord = ""

        textViewGameStatus.visibility = View.GONE
        buttonPlayAgain.visibility = View.GONE
        editTextGuess.isEnabled = false
        buttonGuess.isEnabled = false
        editTextGuess.text.clear()

        updateAttemptsDisplay()
        updateGuessedLettersDisplay()
        textViewWordDisplay.text = "Cargando..."
    }

    private fun loadExistingGame(gameId: String) {
        setupNewGameFramework()
        val allSavedGames = loadSavedGamesList().toMutableList()
        val gameToLoad = allSavedGames.find { it.id == gameId }

        if (gameToLoad != null) {
            currentGameId = gameToLoad.id
            gameNumber = gameToLoad.gameNumber
            gameDifficulty = gameToLoad.difficulty
            wordToGuess = gameToLoad.secretWord
            displayedWord = gameToLoad.displayedWord
            attemptsLeft = gameToLoad.attemptsLeft
            guessedLetters.addAll(gameToLoad.guessedLetters.mapNotNull { it.firstOrNull() })
            isGameInProgress = true

            updateWordDisplay()
            updateAttemptsDisplay()
            updateGuessedLettersDisplay()
            editTextGuess.isEnabled = true
            buttonGuess.isEnabled = true

            allSavedGames.remove(gameToLoad)
            saveFullGamesList(allSavedGames)
            saveOrUpdateCurrentGameState()
        } else {
            Toast.makeText(this, "Error: No se pudo cargar el juego.", Toast.LENGTH_LONG).show()
            isGameInProgress = false
            finish()
        }
    }

    private fun fetchWordFromApi(length: Int) {
        if (currentGameId.isEmpty()) {
            currentGameId = UUID.randomUUID().toString()
        }
        textViewWordDisplay.text = "Cargando palabra..."
        editTextGuess.isEnabled = false
        buttonGuess.isEnabled = false

        val questionMarks = "?".repeat(length)
        val apiUrl = "https://api.datamuse.com/words?sp=$questionMarks&lang=es&max=100"

        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var fetchedWord: String? = null
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonArray = JSONArray(BufferedReader(InputStreamReader(connection.inputStream)).readText())
                    if (jsonArray.length() > 0) {
                        val validWords = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            val word = jsonArray.getJSONObject(i).getString("word")
                            if (word.length == length && word.matches(Regex("^[a-zA-ZñÑáéíóúÁÉÍÓÚüÜ]+$"))) {
                                validWords.add(word.uppercase(Locale.ROOT))
                            }
                        }
                        if (validWords.isNotEmpty()) {
                            fetchedWord = validWords.random()
                            success = true
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching word from API: ${e.message}", e)
            }

            withContext(Dispatchers.Main) {
                if (success && fetchedWord != null) {
                    wordToGuess = fetchedWord
                    initializeWordDisplay()
                    isGameInProgress = true
                    updateAttemptsDisplay()
                    saveOrUpdateCurrentGameState()
                    editTextGuess.isEnabled = true
                    buttonGuess.isEnabled = true
                } else {
                    handleApiErrorOrNoWordFound(length)
                }
            }
        }
    }

    private fun useFallbackWord(length: Int) {
        val fallbackWordsForLength = FALLBACK_WORDS_BY_LENGTH[length]
        if (fallbackWordsForLength != null && fallbackWordsForLength.isNotEmpty()) {
            wordToGuess = fallbackWordsForLength.random().uppercase(Locale.ROOT)
            Log.d(TAG, "Usando palabra de respaldo: $wordToGuess (longitud: ${wordToGuess.length}, esperada: $length)")

            if (wordToGuess.length == length) {
                initializeWordDisplay()
                isGameInProgress = true
                updateAttemptsDisplay()
                saveOrUpdateCurrentGameState()
                editTextGuess.isEnabled = true
                buttonGuess.isEnabled = true
                Toast.makeText(this@GameActivity, "Usando palabra de respaldo.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Error: Palabra de respaldo '${wordToGuess}' no coincide con longitud esperada $length.")
                Toast.makeText(this@GameActivity, "Error con palabra de respaldo.", Toast.LENGTH_LONG).show()
                textViewWordDisplay.text = "ERROR"
                buttonGuess.isEnabled = false
                isGameInProgress = false
                if(::hangmanView.isInitialized) hangmanView.setAttemptsLeft(0)
            }
        } else {
            Log.e(TAG, "No hay palabras de respaldo para longitud $length o la lista está vacía.")
            Toast.makeText(this@GameActivity, "Error crítico: No hay respaldo para longitud $length.", Toast.LENGTH_LONG).show()
            textViewWordDisplay.text = "ERROR"
            buttonGuess.isEnabled = false
            isGameInProgress = false
            if(::hangmanView.isInitialized) hangmanView.setAttemptsLeft(0)
        }
    }

    private fun handleApiErrorOrNoWordFound(targetLength: Int) {
        Toast.makeText(this@GameActivity, "No se pudo obtener palabra de la API.", Toast.LENGTH_SHORT).show()
        useFallbackWord(targetLength)
    }


    private fun initializeWordDisplay() {
        if (displayedWord.isEmpty() && wordToGuess.isNotEmpty()) {
            displayedWord = "_".repeat(wordToGuess.length)
        }
        updateWordDisplay()
    }

    private fun updateWordDisplay() {
        textViewWordDisplay.text = displayedWord.toCharArray().joinToString(" ")
    }

    private fun updateAttemptsDisplay() {
        textViewAttemptsLeftValue.text = attemptsLeft.toString()
        if(::hangmanView.isInitialized) {
            hangmanView.setAttemptsLeft(attemptsLeft)
        }
    }

    private fun updateGuessedLettersDisplay() {
        textViewGuessedLettersValue.text = guessedLetters.joinToString(", ").uppercase(Locale.ROOT)
    }

    private fun handleGuess() {
        if (!isGameInProgress || wordToGuess.isEmpty()) return

        val guessInput = editTextGuess.text.toString().trim()
        editTextGuess.text.clear()

        if (guessInput.isEmpty() || guessInput.length > 1) {
            Toast.makeText(this, "Ingresa una sola letra", Toast.LENGTH_SHORT).show()
            return
        }
        val guessedChar = guessInput.uppercase(Locale.ROOT)[0]

        if (!guessedChar.isLetter() && guessedChar != 'Ñ') {
            Toast.makeText(this, "Ingresa una letra válida", Toast.LENGTH_SHORT).show()
            return
        }
        if (guessedLetters.contains(guessedChar)) {
            Toast.makeText(this, "Ya intentaste con esa letra", Toast.LENGTH_SHORT).show()
            return
        }

        guessedLetters.add(guessedChar)
        updateGuessedLettersDisplay()

        var letterFound = false
        val newDisplayedWord = displayedWord.toCharArray()
        for (i in wordToGuess.indices) {
            if (wordToGuess[i].equals(guessedChar, ignoreCase = true) ||
                normalizeChar(wordToGuess[i]) == normalizeChar(guessedChar)) {
                newDisplayedWord[i] = wordToGuess[i]
                letterFound = true
            }
        }
        displayedWord = String(newDisplayedWord)
        updateWordDisplay()

        if (!letterFound) {
            attemptsLeft--
            updateAttemptsDisplay()
            playSound(soundIdWrongGuess) // Sonido de fallo
            Toast.makeText(this, "Letra incorrecta", Toast.LENGTH_SHORT).show()
        } else {
            playSound(soundIdCorrectGuess) // Sonido de acierto
        }

        saveOrUpdateCurrentGameState()
        checkGameStatus()
    }

    private fun normalizeChar(char: Char): Char {
        return when (char) {
            'Á', 'À', 'Ä', 'Â' -> 'A'
            'É', 'È', 'Ë', 'Ê' -> 'E'
            'Í', 'Ì', 'Ï', 'Î' -> 'I'
            'Ó', 'Ò', 'Ö', 'Ô' -> 'O'
            'Ú', 'Ù', 'Ü', 'Û' -> 'U'
            else -> char
        }
    }


    private fun checkGameStatus() {
        if (displayedWord.replace(" ", "") == wordToGuess) {
            endGame(true)
        } else if (attemptsLeft <= 0) {
            endGame(false)
        }
    }

    private fun endGame(isWin: Boolean) {
        isGameInProgress = false
        editTextGuess.isEnabled = false
        buttonGuess.isEnabled = false
        textViewGameStatus.visibility = View.VISIBLE
        buttonPlayAgain.visibility = View.VISIBLE

        if (isWin) {
            textViewGameStatus.text = "¡Ganaste! La palabra era: $wordToGuess"
            playSound(soundIdGameWin) // Sonido de victoria
        } else {
            textViewGameStatus.text = "¡Perdiste! La palabra era: $wordToGuess"
            if (::hangmanView.isInitialized) hangmanView.setAttemptsLeft(0)
            playSound(soundIdGameLoss) // Sonido de derrota
        }
        deleteFile(CURRENT_GAME_STATE_FILE)
        Log.d(TAG, "Juego terminado. Archivo $CURRENT_GAME_STATE_FILE eliminado.")
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun createGameStateObject(): SavedGame? {
        if (currentGameId.isEmpty() || gameNumber.isEmpty() || wordToGuess.isEmpty()) {
            Log.w(TAG, "No se puede crear SavedGame, faltan datos esenciales.")
            return null
        }
        return SavedGame(
            id = currentGameId,
            gameNumber = gameNumber,
            dateTime = getCurrentDateTime(),
            difficulty = gameDifficulty,
            attemptsLeft = attemptsLeft,
            secretWord = wordToGuess,
            guessedLetters = guessedLetters.map { it.toString() },
            displayedWord = displayedWord
        )
    }

    private fun saveOrUpdateCurrentGameState() {
        val currentGameState = createGameStateObject() ?: return
        if (!isGameInProgress) return

        try {
            val jsonString = gson.toJson(currentGameState)
            val fileOutputStream: FileOutputStream = openFileOutput(CURRENT_GAME_STATE_FILE, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonString.toByteArray())
            fileOutputStream.close()
            Log.d(TAG, "Estado del juego actual guardado en $CURRENT_GAME_STATE_FILE")
        } catch (e: IOException) {
            Log.e(TAG, "Error guardando estado del juego actual: ${e.message}", e)
        }
    }

    private fun archiveCurrentGame() {
        val currentGameStateFile = File(filesDir, CURRENT_GAME_STATE_FILE)
        if (!currentGameStateFile.exists()) {
            Log.d(TAG, "No hay juego actual para archivar ($CURRENT_GAME_STATE_FILE no existe).")
            return
        }
        try {
            val fileReader = FileReader(currentGameStateFile)
            val currentGame: SavedGame? = gson.fromJson(fileReader, SavedGame::class.java)
            fileReader.close()

            if (currentGame == null) {
                Log.e(TAG, "No se pudo leer el juego actual desde $CURRENT_GAME_STATE_FILE para archivar.")
                currentGameStateFile.delete()
                return
            }

            val savedGamesList = loadSavedGamesList().toMutableList()
            savedGamesList.removeAll { it.id == currentGame.id }
            savedGamesList.add(0, currentGame)
            saveFullGamesList(savedGamesList)
            Log.d(TAG, "Juego actual archivado en $SAVED_GAMES_FILE")
            currentGameStateFile.delete()
            Log.d(TAG, "$CURRENT_GAME_STATE_FILE eliminado después de archivar.")
        } catch (e: IOException) {
            Log.e(TAG, "Error archivando el juego: ${e.message}", e)
        }
    }

    private fun loadSavedGamesList(): List<SavedGame> {
        val file = File(filesDir, SAVED_GAMES_FILE)
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val fileReader = FileReader(file)
            val type = object : TypeToken<List<SavedGame>>() {}.type
            gson.fromJson(fileReader, type) ?: emptyList<SavedGame>()
        } catch (e: IOException) {
            Log.e(TAG, "Error cargando lista de juegos guardados: ${e.message}", e)
            emptyList()
        }
    }

    private fun saveFullGamesList(gamesToSave: List<SavedGame>) {
        try {
            val jsonString = gson.toJson(gamesToSave)
            val fileOutputStream: FileOutputStream = openFileOutput(SAVED_GAMES_FILE, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonString.toByteArray())
            fileOutputStream.close()
            Log.d(TAG, "Lista completa de juegos guardada en $SAVED_GAMES_FILE")
        } catch (e: IOException) {
            Log.e(TAG, "Error guardando lista completa de juegos: ${e.message}", e)
        }
    }

    private fun getNextGameNumber(): Int {
        val savedGames = loadSavedGamesList()
        var maxNum = 0
        savedGames.forEach {
            try {
                val numPart = it.gameNumber.substringAfter("#").trim()
                val num = numPart.toInt()
                if (num > maxNum) {
                    maxNum = num
                }
            } catch (e: NumberFormatException) { /* Ignorar */ }
        }
        return maxNum + 1
    }

    private fun handleExitGame() {
        if (isGameInProgress) {
            if (wordToGuess.isNotEmpty()) {
                archiveCurrentGame()
            } else {
                deleteFile(CURRENT_GAME_STATE_FILE)
            }
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        // No es necesario liberar SoundPool aquí si se hace en onDestroy
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release() // Liberar SoundPool
        soundPool = null
        Log.d(TAG, "SoundPool liberado.")
    }

    private fun determineWordLength(difficulty: String): Int {
        return when (difficulty) {
            DifficultySelectionActivity.DIFFICULTY_EASY -> Random.nextInt(4, 6)
            DifficultySelectionActivity.DIFFICULTY_MEDIUM -> Random.nextInt(6, 8)
            DifficultySelectionActivity.DIFFICULTY_HARD -> Random.nextInt(8, 11)
            else -> 5
        }
    }
}
