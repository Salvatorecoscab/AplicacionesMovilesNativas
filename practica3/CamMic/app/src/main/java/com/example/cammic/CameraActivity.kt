package com.example.cammic // Asegúrate que el paquete sea el correcto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
// import android.widget.ImageView // Ya no se usa aquí
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // Si no usas BaseActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Si BaseActivity existe y hereda de AppCompatActivity, úsala:
 class CameraActivity : BaseActivity() {
//class /CameraActivity : AppCompatActivity() { // O usa tu BaseActivity

    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonOpenGallery: Button // Nuevo botón
    private var currentPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            currentPhotoUri?.let { uri ->
                Log.d("CameraActivity", "Foto capturada guardada en: $uri")
                val intent = Intent(this, EditImageActivity::class.java).apply {
                    putExtra(EditImageActivity.EXTRA_IMAGE_URI, uri.toString())
                }
                startActivity(intent)
            } ?: run {
                Log.e("CameraActivity", "currentPhotoUri es null después de tomar la foto.")
                Toast.makeText(this, "Error al obtener la URI de la foto.", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            Log.d("CameraActivity", "Toma de foto cancelada.")
            Toast.makeText(this, "Toma de foto cancelada.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("CameraActivity", "Error al tomar la foto. Result code: ${result.resultCode}")
            Toast.makeText(this, "Error al tomar la foto.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("CameraActivity", "Permiso de cámara concedido.")
            startCameraIntent()
        } else {
            Log.w("CameraActivity", "Permiso de cámara denegado.")
            Toast.makeText(this, "Permiso de cámara denegado. No se puede tomar la foto.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        buttonOpenGallery = findViewById(R.id.buttonOpenGallery) // Inicializar nuevo botón

        buttonTakePhoto.setOnClickListener {
            if (checkCameraPermission()) {
                startCameraIntent()
            } else {
                requestCameraPermission()
            }
        }

        buttonOpenGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Asegurarse de que el directorio exista
        storageDir?.mkdirs()
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // currentPhotoPath = absolutePath // Opcional
        }
    }

    private fun startCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Log.e("CameraActivity", "Error creando el archivo de imagen", ex)
                Toast.makeText(this, "Error al preparar para tomar la foto.", Toast.LENGTH_SHORT).show()
                return
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                currentPhotoUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePhotoLauncher.launch(takePictureIntent)
                Log.d("CameraActivity", "Lanzando intent de cámara para guardar en $photoURI")
            }
        } else {
            Log.e("CameraActivity", "No hay aplicación de cámara disponible.")
            Toast.makeText(this, "No hay aplicación de cámara disponible.", Toast.LENGTH_SHORT).show()
        }
    }
}
