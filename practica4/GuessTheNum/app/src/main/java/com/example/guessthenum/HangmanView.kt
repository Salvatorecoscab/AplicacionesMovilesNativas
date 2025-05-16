package com.example.guessthenum // Asegúrate de que este sea tu paquete correcto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class HangmanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK // Puedes cambiar el color
        strokeWidth = 8f    // Grosor de la línea
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val eyePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var attemptsLeft = 6 // Por defecto, el monito está completo

    /**
     * Establece los intentos restantes para actualizar el dibujo.
     * @param attempts El número de intentos que le quedan al jugador (0-6).
     */
    fun setAttemptsLeft(attempts: Int) {
        if (attempts in 0..6) {
            attemptsLeft = attempts
            invalidate() // Vuelve a dibujar la vista con el nuevo estado
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = getWidth().toFloat()
        val height = getHeight().toFloat()

        // Dimensiones relativas para que escale bien
        // Estas son solo de ejemplo, ajústalas a tu gusto

        val headRadius = height / 12f
        val bodyLength = height / 6f
        val limbLength = height / 7f

        // Coordenadas del poste del ahorcado (ajusta según necesites)
        val gallowsBaseStartX = width * 0.1f
        val gallowsBaseEndX = width * 0.5f
        val gallowsPoleHeight = height * 0.8f
        val gallowsPoleX = width * 0.25f
        val gallowsBeamLength = width * 0.3f
        val ropeDropY = height * 0.2f

        // Punto de anclaje para el monito (debajo de la soga)
        val anchorX = gallowsPoleX + gallowsBeamLength
        val anchorY = ropeDropY + headRadius * 0.5f // Un poco más abajo para la soga

        // 1. Dibujar el poste del ahorcado (siempre visible)
        // Base
        canvas.drawLine(gallowsBaseStartX, gallowsPoleHeight, gallowsBaseEndX, gallowsPoleHeight, paint)
        // Poste vertical
        canvas.drawLine(gallowsPoleX, gallowsPoleHeight, gallowsPoleX, height * 0.1f, paint)
        // Viga horizontal
        canvas.drawLine(gallowsPoleX, height * 0.1f, gallowsPoleX + gallowsBeamLength, height * 0.1f, paint)
        // Soga corta
        canvas.drawLine(gallowsPoleX + gallowsBeamLength, height * 0.1f, gallowsPoleX + gallowsBeamLength, ropeDropY, paint)


        // Lógica de dibujo del monito basada en attemptsLeft (tu lógica invertida)
        // El monito se dibuja completo con 6 intentos y se le quitan partes.

        // Cabeza (siempre si attemptsLeft >= 0)
        if (attemptsLeft >= 0) {
            val headCenterX = anchorX
            val headCenterY = ropeDropY + headRadius
            canvas.drawCircle(headCenterX, headCenterY, headRadius, paint)

            // Ojos de X si attemptsLeft es 0
            if (attemptsLeft == 0) {
                val eyeOffsetX = headRadius * 0.3f
                val eyeOffsetY = headRadius * 0.2f // Ligeramente hacia arriba desde el centro
                val eyeSize = headRadius * 0.25f

                // Ojo izquierdo X
                canvas.drawLine(headCenterX - eyeOffsetX - eyeSize, headCenterY - eyeOffsetY - eyeSize,
                    headCenterX - eyeOffsetX + eyeSize, headCenterY - eyeOffsetY + eyeSize, eyePaint)
                canvas.drawLine(headCenterX - eyeOffsetX + eyeSize, headCenterY - eyeOffsetY - eyeSize,
                    headCenterX - eyeOffsetX - eyeSize, headCenterY - eyeOffsetY + eyeSize, eyePaint)
                // Ojo derecho X
                canvas.drawLine(headCenterX + eyeOffsetX - eyeSize, headCenterY - eyeOffsetY - eyeSize,
                    headCenterX + eyeOffsetX + eyeSize, headCenterY - eyeOffsetY + eyeSize, eyePaint)
                canvas.drawLine(headCenterX + eyeOffsetX + eyeSize, headCenterY - eyeOffsetY - eyeSize,
                    headCenterX + eyeOffsetX - eyeSize, headCenterY - eyeOffsetY + eyeSize, eyePaint)
            }
        }

        // Torso (si attemptsLeft >= 1)
        if (attemptsLeft >= 2) {
            val torsoStartX = anchorX
            val torsoStartY = ropeDropY + headRadius * 2 // Debajo de la cabeza
            val torsoEndX = anchorX
            val torsoEndY = torsoStartY + bodyLength
            canvas.drawLine(torsoStartX, torsoStartY, torsoEndX, torsoEndY, paint)

            // Brazo Izquierdo (si attemptsLeft >= 2)
            if (attemptsLeft >= 3) {
                val armStartX = torsoStartX
                val armStartY = torsoStartY + bodyLength * 0.2f // Un poco abajo del hombro
                canvas.drawLine(armStartX, armStartY, armStartX - limbLength, armStartY + limbLength * 0.5f, paint)
            }

            // Brazo Derecho (si attemptsLeft >= 3)
            if (attemptsLeft >= 4) {
                val armStartX = torsoStartX
                val armStartY = torsoStartY + bodyLength * 0.2f
                canvas.drawLine(armStartX, armStartY, armStartX + limbLength, armStartY + limbLength * 0.5f, paint)
            }

            // Pierna Izquierda (si attemptsLeft >= 4)
            if (attemptsLeft >= 5) {
                val legStartX = torsoEndX
                val legStartY = torsoEndY
                canvas.drawLine(legStartX, legStartY, legStartX - limbLength * 0.7f, legStartY + limbLength, paint)
            }

            // Pierna Derecha (si attemptsLeft >= 5)
            // El monito está completo si attemptsLeft es 6, así que esta siempre se dibuja si las otras también.
            if (attemptsLeft >= 6) { // O podrías poner >= 5, ya que 6 es completo
                val legStartX = torsoEndX
                val legStartY = torsoEndY
                canvas.drawLine(legStartX, legStartY, legStartX + limbLength * 0.7f, legStartY + limbLength, paint)
            }
        }
    }
}
