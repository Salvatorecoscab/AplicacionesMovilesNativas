package com.example.cammic // Tu paquete

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AudioActivity : BaseActivity()  {

    // --- Variables para Grabación ---
    private lateinit var buttonStartRecording: Button
    private lateinit var buttonStopRecording: Button
    private lateinit var textViewRecordingStatus: TextView
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false

    // --- Variables para Reproducción ---
    private lateinit var listViewAudioFilesPlayer: ListView
    private lateinit var textViewCurrentFilePlayer: TextView
    private lateinit var buttonPlayPausePlayer: Button
    private lateinit var buttonStopPlayer: Button
    private var audioPlayer: MediaPlayer? = null
    private var audioPlayerFiles: ArrayList<File> = ArrayList()
    private var currentSelectedPlayerFile: File? = null
    private var isAudioPlaying = false
    private lateinit var audioFilesAdapter: ArrayAdapter<String>


    private val requestAudioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("AudioActivity", "Permiso de grabación concedido.")
            // No reintentamos automáticamente la grabación aquí, el usuario puede pulsar de nuevo.
            // O si quieres, puedes añadir una bandera para reintentar.
        } else {
            Log.w("AudioActivity", "Permiso de grabación denegado.")
            Toast.makeText(this, "Permiso de micrófono denegado. No se puede grabar audio.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)

        // Inicializar Vistas de Grabación
        buttonStartRecording = findViewById(R.id.buttonStartRecording)
        buttonStopRecording = findViewById(R.id.buttonStopRecording)
        textViewRecordingStatus = findViewById(R.id.textViewRecordingStatus)

        // Inicializar Vistas de Reproducción
        listViewAudioFilesPlayer = findViewById(R.id.listViewAudioFilesPlayer)
        textViewCurrentFilePlayer = findViewById(R.id.textViewCurrentFilePlayer)
        buttonPlayPausePlayer = findViewById(R.id.buttonPlayPausePlayer)
        buttonStopPlayer = findViewById(R.id.buttonStopPlayer)

        // Configuración inicial UI Grabación
        textViewRecordingStatus.text = "Estado: Inactivo"
        buttonStopRecording.isEnabled = false

        // Configuración inicial UI Reproducción
        audioFilesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listViewAudioFilesPlayer.adapter = audioFilesAdapter
        loadAudioPlayerFiles() // Cargar archivos al inicio
        updatePlayerUI() // Estado inicial de botones del reproductor

        // --- Listeners de Grabación ---
        buttonStartRecording.setOnClickListener {
            if (isAudioPlaying) {
                Toast.makeText(this, "Detén la reproducción antes de grabar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (checkRecordingPermission()) {
                if (!isRecording) { // Evitar múltiples inicios si mediaRecorder es null por otra razón
                    startRecording()
                } else {
                    Toast.makeText(this, "Ya se está grabando audio.", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestRecordingPermission()
            }
        }

        buttonStopRecording.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                Toast.makeText(this, "No hay grabación activa para detener.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Listeners de Reproducción ---
        listViewAudioFilesPlayer.setOnItemClickListener { _, _, position, _ ->
            if (isRecording) {
                Toast.makeText(this, "Detén la grabación para seleccionar un archivo.", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            currentSelectedPlayerFile = audioPlayerFiles[position]
            textViewCurrentFilePlayer.text = "Seleccionado: ${currentSelectedPlayerFile?.name}"
            stopAudioPlayback() // Detener cualquier reproducción anterior al seleccionar nuevo archivo
            updatePlayerUI()
        }

        buttonPlayPausePlayer.setOnClickListener {
            if (isRecording) {
                Toast.makeText(this, "Grabación en curso. No se puede reproducir.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentSelectedPlayerFile?.let { file ->
                if (isAudioPlaying) {
                    pauseAudioPlayback()
                } else {
                    startAudioPlayback(file)
                }
            } ?: Toast.makeText(this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show()
        }

        buttonStopPlayer.setOnClickListener {
            stopAudioPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaRecorder()
        releaseMediaPlayer()
    }

    // --- Métodos de Grabación ---
    private fun checkRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermission() {
        requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        // Detener reproducción si está activa
        if (isAudioPlaying) {
            stopAudioPlayback()
        }
        // Deshabilitar controles del reproductor
        setPlayerControlsEnabled(false)


        releaseMediaRecorder() // Asegurar que esté limpio

        audioFilePath = getAudioFilePath()
        if (audioFilePath == null) {
            Toast.makeText(this, "No se pudo crear el archivo de audio.", Toast.LENGTH_SHORT).show() // Call show() without arguments
            setPlayerControlsEnabled(true) // Call setPlayerControlsEnabled on a separate line
            return
        }

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()

                isRecording = true
                buttonStartRecording.isEnabled = false
                buttonStopRecording.isEnabled = true
                textViewRecordingStatus.text = "Estado: Grabando..."
                Log.d("AudioActivity", "Grabación iniciada. Archivo: $audioFilePath")
            }
        } catch (e: IOException) {
            Log.e("AudioActivity", "Error al preparar/iniciar grabación", e)
            releaseMediaRecorder()
            updateUIAfterRecordingError("Error al iniciar grabación")
            setPlayerControlsEnabled(true) // Reactivar si falla
        } catch (e: SecurityException) {
            Log.e("AudioActivity", "Error de seguridad (¿Permisos?)", e)
            releaseMediaRecorder()
            updateUIAfterRecordingError("Permiso denegado")
            setPlayerControlsEnabled(true) // Reactivar si falla
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                Log.d("AudioActivity", "Grabación detenida. Archivo guardado en: $audioFilePath")
                Toast.makeText(this@AudioActivity, "Grabación finalizada.", Toast.LENGTH_SHORT).show()
                textViewRecordingStatus.text = "Estado: Inactivo\nÚltimo: ${audioFilePath?.substringAfterLast('/')}"
            } catch (e: RuntimeException) {
                Log.e("AudioActivity", "Error al detener grabación", e)
                textViewRecordingStatus.text = "Estado: Error al detener"
            } finally {
                release() // Liberar después de stop()
                mediaRecorder = null
                isRecording = false
                buttonStartRecording.isEnabled = true
                buttonStopRecording.isEnabled = false
                loadAudioPlayerFiles() // Recargar lista para mostrar nuevo archivo
                setPlayerControlsEnabled(true) // Reactivar controles del reproductor
            }
        }
        // Si mediaRecorder ya era null pero isRecording era true (estado inconsistente)
        if (mediaRecorder == null && isRecording) {
            isRecording = false
            buttonStartRecording.isEnabled = true
            buttonStopRecording.isEnabled = false
            textViewRecordingStatus.text = "Estado: Inactivo (error previo)"
            loadAudioPlayerFiles()
            setPlayerControlsEnabled(true)
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.apply {
            try {
                // Solo intentar stop si realmente estaba grabando, para evitar errores si ya se llamó a stop.
                // El estado 'isRecording' es más fiable aquí.
                if (isRecording) {
                    stop()
                }
            } catch (e: RuntimeException) {
                Log.w("AudioActivity", "Error al intentar stop() en releaseMediaRecorder", e)
            }
            release()
            Log.d("AudioActivity", "MediaRecorder liberado.")
        }
        mediaRecorder = null
        isRecording = false // Asegurar que el estado se actualice
        // No cambiar UI de botones aquí, se maneja en stopRecording o startRecording
    }

    private fun updateUIAfterRecordingError(message: String) {
        buttonStartRecording.isEnabled = true
        buttonStopRecording.isEnabled = false
        textViewRecordingStatus.text = "Estado: $message"
        isRecording = false
    }

    private fun getAudioFilePath(): String? {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (storageDir == null) {
            Log.e("AudioActivity", "Directorio de almacenamiento externo no disponible.")
            return null
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val audioFileName = "AUDIO_${timeStamp}.3gp"
        val audioFile = File(storageDir, audioFileName)
        Log.d("AudioActivity", "Ruta del archivo de audio: ${audioFile.absolutePath}")
        return audioFile.absolutePath
    }

    // --- Métodos de Reproducción ---
    private fun loadAudioPlayerFiles() {
        audioPlayerFiles.clear()
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (storageDir != null && storageDir.exists()) {
            storageDir.listFiles { _, name -> name.endsWith(".3gp") }?.let { files ->
                audioPlayerFiles.addAll(files.sortedByDescending { it.lastModified() })
            }
        }

        val fileNames = audioPlayerFiles.map { it.name }
        audioFilesAdapter.clear()
        audioFilesAdapter.addAll(fileNames)
        audioFilesAdapter.notifyDataSetChanged()

        if (audioPlayerFiles.isEmpty()) {
            textViewCurrentFilePlayer.text = "No hay grabaciones."
        }
        // Si el archivo seleccionado previamente ya no existe, deseleccionarlo
        if (currentSelectedPlayerFile != null && !audioPlayerFiles.contains(currentSelectedPlayerFile)) {
            currentSelectedPlayerFile = null
            stopAudioPlayback() // Detiene y actualiza la UI del reproductor
        }
        updatePlayerUI()
    }

    private fun startAudioPlayback(file: File) {
        if (isRecording) {
            Toast.makeText(this, "No se puede reproducir mientras se graba.", Toast.LENGTH_SHORT).show()
            return
        }
        setRecordingControlsEnabled(false) // Deshabilitar controles de grabación

        if (audioPlayer == null) {
            audioPlayer = MediaPlayer()
        } else {
            // Reanudar si estaba pausado y es el mismo archivo
            if (!audioPlayer!!.isPlaying && audioPlayer!!.currentPosition > 0 && currentSelectedPlayerFile?.absolutePath == file.absolutePath) {
                audioPlayer?.start()
                isAudioPlaying = true
                updatePlayerUI()
                return
            }
            audioPlayer?.reset()
        }

        try {
            audioPlayer?.setDataSource(file.absolutePath)
            audioPlayer?.setOnPreparedListener { mp ->
                mp.start()
                isAudioPlaying = true
                textViewCurrentFilePlayer.text = "Reproduciendo: ${file.name}"
                updatePlayerUI()
            }
            audioPlayer?.setOnCompletionListener {
                isAudioPlaying = false // Marcar como no reproduciendo
                // No llamar a stopAudioPlayback() directamente para evitar resetear la selección
                // Solo actualizar la UI
                textViewCurrentFilePlayer.text = "Finalizado: ${file.name}"
                updatePlayerUI()
                setRecordingControlsEnabled(true) // Habilitar grabación de nuevo
            }
            audioPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayer", "MediaPlayer Error: what $what, extra $extra")
                Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show()
                isAudioPlaying = false
                updatePlayerUI()
                setRecordingControlsEnabled(true) // Habilitar grabación de nuevo
                true
            }
            audioPlayer?.prepareAsync()
        } catch (e: IOException) {
            Log.e("AudioPlayer", "Error setDataSource", e)
            isAudioPlaying = false
            updatePlayerUI()
            setRecordingControlsEnabled(true) // Habilitar grabación de nuevo
        }
    }

    private fun pauseAudioPlayback() {
        audioPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isAudioPlaying = false
                updatePlayerUI()
                // Los controles de grabación permanecen deshabilitados mientras esté pausado
            }
        }
    }

    private fun stopAudioPlayback() {
        audioPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset() // Resetear para poder usarlo de nuevo
        }
        isAudioPlaying = false
        // No resetear currentSelectedPlayerFile aquí, solo si se deselecciona o borra
        if (currentSelectedPlayerFile != null) {
            textViewCurrentFilePlayer.text = "Seleccionado: ${currentSelectedPlayerFile!!.name}"
        } else {
            textViewCurrentFilePlayer.text = "Ningún archivo seleccionado"
        }
        updatePlayerUI()
        setRecordingControlsEnabled(true) // Habilitar controles de grabación
    }

    private fun releaseMediaPlayer() {
        audioPlayer?.release()
        audioPlayer = null
        isAudioPlaying = false
    }

    private fun updatePlayerUI() {
        buttonPlayPausePlayer.text = if (isAudioPlaying) "Pausar" else "Reproducir"
        buttonPlayPausePlayer.isEnabled = currentSelectedPlayerFile != null && !isRecording // Habilitar solo si hay archivo y no se está grabando
        buttonStopPlayer.isEnabled = (isAudioPlaying || (audioPlayer?.isPlaying == false && audioPlayer?.currentPosition ?: 0 > 0)) && currentSelectedPlayerFile != null && !isRecording // Habilitar si está reproduciendo o pausado, hay archivo y no grabando
        listViewAudioFilesPlayer.isEnabled = !isRecording && !isAudioPlaying // Deshabilitar lista mientras graba o reproduce
    }

    private fun setRecordingControlsEnabled(enabled: Boolean) {
        buttonStartRecording.isEnabled = enabled && !isRecording // Solo habilitar si no está ya grabando
        // buttonStopRecording se maneja por el estado de isRecording
    }

    private fun setPlayerControlsEnabled(enabled: Boolean) {
        listViewAudioFilesPlayer.isEnabled = enabled
        // Los botones play/stop se actualizan a través de updatePlayerUI
        if (enabled) {
            updatePlayerUI()
        } else {
            buttonPlayPausePlayer.isEnabled = false
            buttonStopPlayer.isEnabled = false
        }
    }
}
