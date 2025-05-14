package com.example.gestorarchivos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

class ImageViewerActivity : BaseActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnRotateLeft: Button
    private lateinit var btnRotateRight: Button
    private lateinit var btnBack: Button
    private lateinit var controlsContainer: View
    private lateinit var rootLayout: ConstraintLayout

    private var originalBitmap: Bitmap? = null
    private var currentRotation = 0f
    private var scaleFactor = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // Variables para controlar desplazamiento
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f
    private var isDragging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        btnRotateLeft = findViewById(R.id.btnRotateLeft)
        btnRotateRight = findViewById(R.id.btnRotateRight)
        btnBack = findViewById(R.id.btnBack)
        controlsContainer = findViewById(R.id.controlsContainer)
        rootLayout = findViewById(R.id.rootLayout) // Asegúrate de agregar este ID a tu layout

        // Recuperar la ruta de la imagen
        val imagePath = intent.getStringExtra("IMAGE_PATH") ?: return finish()

        try {
            // Cargar la imagen desde la ruta
            originalBitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(originalBitmap)

            // Configurar detector de gestos para zoom
            scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

            // Configurar rotación
            btnRotateLeft.setOnClickListener {
                rotateImage(-90f)
            }

            btnRotateRight.setOnClickListener {
                rotateImage(90f)
            }

            btnBack.setOnClickListener {
                finish()
            }

            // Configurar gestos en el rootLayout en lugar de imageView
            rootLayout.setOnTouchListener { _, event ->
                handleTouch(event)
                true
            }

        } catch (e: Exception) {
            finish()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        // Primero, pasar el evento al detector de escala
        scaleGestureDetector.onTouchEvent(event)

        // Si estamos haciendo zoom, no hacer desplazamiento
        if (scaleGestureDetector.isInProgress) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Guardar la posición inicial
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && scaleFactor > 1.0f) {
                    // Calcular la distancia de desplazamiento
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    // Actualizar la posición
                    posX += dx
                    posY += dy

                    // Aplicar la nueva posición
                    imageView.translationX = posX
                    imageView.translationY = posY

                    // Actualizar la última posición
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                // Si el desplazamiento fue pequeño, considerar como clic
                if (Math.abs(event.x - lastTouchX) < 10 && Math.abs(event.y - lastTouchY) < 10) {
                    toggleControlsVisibility()
                }
                return true
            }
        }
        return false
    }

    private fun rotateImage(degrees: Float) {
        originalBitmap?.let {
            currentRotation = (currentRotation + degrees) % 360
            val matrix = Matrix()
            matrix.postRotate(currentRotation)
            val rotatedBitmap = Bitmap.createBitmap(
                it, 0, 0, it.width, it.height, matrix, true
            )
            imageView.setImageBitmap(rotatedBitmap)
        }
    }

    private fun toggleControlsVisibility() {
        if (controlsContainer.visibility == View.VISIBLE) {
            controlsContainer.visibility = View.GONE
            btnBack.visibility = View.GONE
        } else {
            controlsContainer.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor

            // Limitar el factor de escala para evitar zoom excesivo
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f))

            // Aplicar el zoom
            imageView.scaleX = scaleFactor
            imageView.scaleY = scaleFactor

            return true
        }
    }

    // Resetear el zoom y la posición al doble toque
    private fun resetZoomAndPosition() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        imageView.scaleX = 1.0f
        imageView.scaleY = 1.0f
        imageView.translationX = 0f
        imageView.translationY = 0f
    }
}
