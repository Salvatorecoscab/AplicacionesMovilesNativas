package com.example.cammic // Asegúrate que el paquete sea el correcto

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
// import android.provider.MediaStore // No usado directamente aquí para guardar ahora
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
// import androidx.activity.result.ActivityResultLauncher // No es necesario si CropImageContract se usa directamente
// import androidx.activity.result.contract.ActivityResultContracts // No es necesario
import androidx.appcompat.app.AppCompatActivity

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.canhub.cropper.* // Correcto para la librería que estás usando

class EditImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val TAG = "EditImageActivity"
    }

    private lateinit var imageView: ImageView
    private lateinit var buttonCrop: Button
    private lateinit var buttonRotate: Button
    private lateinit var buttonFilterGrayscale: Button
    private lateinit var buttonSave: Button

    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null
    private var originalBitmapForFilterToggle: Bitmap? = null // Para guardar el estado antes del filtro
    private var isGrayscaleFilterActive: Boolean = false      // Flag para el estado del filtro

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                currentImageUri = uri
                loadBitmapFromUri(uri, isCropped = true)
                Log.d(TAG, "Imagen recortada: $uri")
            } ?: run {
                if (result.originalUri != null && result.error == null) {
                    Log.d(TAG, "Recorte cancelado o sin cambios, usando URI original: ${result.originalUri}")
                    // Opcional: recargar si es necesario, aunque si no hay cambios, currentBitmap ya está bien.
                    // currentImageUri = result.originalUri
                    // loadBitmapFromUri(currentImageUri!!, isCropped = true) // O false si no hubo cambio
                } else {
                    Log.e(TAG, "Error al obtener la URI de la imagen recortada o URI nula.")
                }
            }
        } else {
            val exception = result.error
            Toast.makeText(this, "Error al recortar: ${exception?.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error al recortar imagen", exception)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image)

        imageView = findViewById(R.id.imageViewEdit)
        buttonCrop = findViewById(R.id.buttonCrop)
        buttonRotate = findViewById(R.id.buttonRotate)
        buttonFilterGrayscale = findViewById(R.id.buttonFilterGrayscale)
        buttonSave = findViewById(R.id.buttonSave)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString != null) {
            currentImageUri = Uri.parse(uriString)
            loadBitmapFromUri(currentImageUri!!) // Carga inicial
        } else {
            Toast.makeText(this, "No se recibió la imagen para editar.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No se recibió URI de imagen.")
            finish()
            return
        }

        buttonCrop.setOnClickListener {
            currentImageUri?.let { uri ->
                val cropOptions = CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        activityTitle = "Recortar Imagen",
                        cropMenuCropButtonTitle = "HECHO",
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 90,
                        imageSourceIncludeCamera = false,
                        imageSourceIncludeGallery = false
                    )
                )
                cropImageLauncher.launch(cropOptions)
            } ?: Toast.makeText(this, "No hay imagen para recortar", Toast.LENGTH_SHORT).show()
        }


        buttonRotate.setOnClickListener {
            rotateImage(90f)
        }

        buttonFilterGrayscale.setOnClickListener {
            toggleGrayscaleFilter()
        }

        buttonSave.setOnClickListener {
            saveImage()
        }
    }

    private fun loadBitmapFromUri(uri: Uri, isCropped: Boolean = false) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            currentBitmap = tempBitmap
            originalBitmapForFilterToggle = currentBitmap?.copy(
                currentBitmap!!.config ?: Bitmap.Config.ARGB_8888, // CORRECCIÓN AQUÍ
                true
            )
            isGrayscaleFilterActive = false

            imageView.setImageBitmap(currentBitmap)
            Log.d(TAG, "Bitmap cargado desde $uri. ¿Recortado?: $isCropped")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar bitmap desde URI: $uri", e)
            Toast.makeText(this, "Error al cargar imagen.", Toast.LENGTH_SHORT).show()
            currentBitmap = null
            originalBitmapForFilterToggle = null
        }
    }

    private fun rotateImage(degrees: Float) {
        currentBitmap?.let { bmp ->
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            currentBitmap = rotatedBitmap

            originalBitmapForFilterToggle = currentBitmap?.copy(
                currentBitmap!!.config ?: Bitmap.Config.ARGB_8888, // CORRECCIÓN AQUÍ
                true
            )
            isGrayscaleFilterActive = false

            imageView.setImageBitmap(currentBitmap)
            Log.d(TAG, "Imagen rotada ${degrees} grados.")
        } ?: Toast.makeText(this, "No hay imagen para rotar", Toast.LENGTH_SHORT).show()
    }

    private fun toggleGrayscaleFilter() {
        if (currentBitmap == null || originalBitmapForFilterToggle == null) {
            Toast.makeText(this, "No hay imagen para aplicar filtro", Toast.LENGTH_SHORT).show()
            return
        }

        if (isGrayscaleFilterActive) {
            // Desactivar filtro: restaurar desde originalBitmapForFilterToggle
            currentBitmap = originalBitmapForFilterToggle?.copy(
                originalBitmapForFilterToggle!!.config ?: Bitmap.Config.ARGB_8888, // CORRECCIÓN AQUÍ
                true
            )
            imageView.setImageBitmap(currentBitmap)
            isGrayscaleFilterActive = false
            Log.d(TAG, "Filtro escala de grises desactivado.")
        } else {
            // Activar filtro:
            // originalBitmapForFilterToggle ya debería tener la versión a color (posiblemente rotada)
            val newBitmap = Bitmap.createBitmap(originalBitmapForFilterToggle!!.width, originalBitmapForFilterToggle!!.height, Bitmap.Config.ARGB_8888) // Usar ARGB_8888 para el canvas del filtro
            val canvas = Canvas(newBitmap)
            val paint = Paint()
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(originalBitmapForFilterToggle!!, 0f, 0f, paint)

            currentBitmap = newBitmap
            imageView.setImageBitmap(currentBitmap)
            isGrayscaleFilterActive = true
            Log.d(TAG, "Filtro escala de grises aplicado.")
        }
    }

    private fun saveImage() {
        currentBitmap?.let { bmp ->
            val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (imagesDir == null) {
                Toast.makeText(this, "No se puede acceder al almacenamiento.", Toast.LENGTH_SHORT).show()
                return
            }
            if (!imagesDir.exists()) {
                imagesDir.mkdirs() // Crear directorio si no existe
            }
            val fileName = "edited_image_${System.currentTimeMillis()}.jpg"
            val imageFile = File(imagesDir, fileName)
            try {
                FileOutputStream(imageFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                Toast.makeText(this, "Imagen guardada en: ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Imagen guardada en: ${imageFile.absolutePath}")

            } catch (e: IOException) {
                Log.e(TAG, "Error al guardar imagen", e)
                Toast.makeText(this, "Error al guardar imagen.", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "No hay imagen para guardar", Toast.LENGTH_SHORT).show()
    }
}
