package com.example.menu  // Asegúrate de que coincide con tu proyecto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {  // Cambiado el nombre de la clase

    private lateinit var txtResultado: TextView
    private var numero1: String = ""
    private var numero2: String = ""
    private var operador: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator) // Asegúrate de que el layout tiene este nombre

        txtResultado = findViewById(R.id.txtResultado)
        // Encontrar el botón de regreso
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Lógica para regresar a MainActivity
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Terminar esta actividad para evitar que el usuario regrese con el botón de atrás
        }
    }

    fun onClickNumero(view: android.view.View) {
        val button = view as Button
        val numero = button.text.toString()

        if (operador == null) {
            numero1 += numero
            txtResultado.text = numero1
        } else {
            numero2 += numero
            txtResultado.text = numero2
        }
    }

    fun onClickOperacion(view: android.view.View) {
        val button = view as Button
        operador = button.text.toString()
    }

    fun onClickResultado(view: android.view.View) {
        if (numero1.isNotEmpty() && numero2.isNotEmpty() && operador != null) {
            val resultado = when (operador) {
                "+" -> numero1.toDouble() + numero2.toDouble()
                "-" -> numero1.toDouble() - numero2.toDouble()
                "*" -> numero1.toDouble() * numero2.toDouble()
                "/" -> if (numero2.toDouble() != 0.0) numero1.toDouble() / numero2.toDouble() else "Error"
                else -> "Error"
            }

            txtResultado.text = resultado.toString()
            numero1 = resultado.toString()
            numero2 = ""
            operador = null
        }
    }

    fun onClickLimpiar(view: android.view.View) {
        numero1 = ""
        numero2 = ""
        operador = null
        txtResultado.text = "0"
    }
}
