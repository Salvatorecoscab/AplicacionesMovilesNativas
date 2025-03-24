package com.example.codeandoando

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.codeandoando.databinding.ActivityBinaryBinding // Importa el binding

class BinaryActivity : AppCompatActivity(), FragmentResult.OnResultInteractionListener {

    private lateinit var binding: ActivityBinaryBinding
    private var randomNumber: Int = 0
    private lateinit var decimalNumberTextView: TextView
    private lateinit var switchBit3: Switch
    private lateinit var switchBit2: Switch
    private lateinit var switchBit1: Switch
    private lateinit var switchBit0: Switch
    private lateinit var checkButton: Button
    private lateinit var generateButton: Button
    private lateinit var resultTextView: TextView

    private val TAG = "BinaryActivity" // Para usar en los logs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBinaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = binding.backButton
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        decimalNumberTextView = binding.decimalNumberTextView
        switchBit3 = binding.switchBit3
        switchBit2 = binding.switchBit2
        switchBit1 = binding.switchBit1
        switchBit0 = binding.switchBit0
        checkButton = binding.checkButton
        generateButton = binding.generateButton
        resultTextView = binding.resultTextView

        generateNewNumber()

        generateButton.setOnClickListener {
            Log.d(TAG, "Generate button clicked")
            generateNewNumber()
        }

        checkButton.setOnClickListener {
            Log.d(TAG, "Check button clicked")
            checkAnswer()
        }

        Log.d(TAG, "onCreate finished")
    }

    private fun generateNewNumber() {
        Log.d(TAG, "generateNewNumber() called")
        randomNumber = (0..15).random()
        decimalNumberTextView.text = randomNumber.toString()
        // Resetear los interruptores
        switchBit3.isChecked = false
        switchBit2.isChecked = false
        switchBit1.isChecked = false
        switchBit0.isChecked = false
        resultTextView.text = "" // Limpiar el resultado anterior
        Log.d(TAG, "New random number generated: $randomNumber")
    }

    private fun checkAnswer() {
        Log.d(TAG, "checkAnswer() called")
        val binaryValue = (if (switchBit3.isChecked) 8 else 0) +
                (if (switchBit2.isChecked) 4 else 0) +
                (if (switchBit1.isChecked) 2 else 0) +
                (if (switchBit0.isChecked) 1 else 0)

        Log.d(TAG, "Binary value entered: $binaryValue")
        Log.d(TAG, "Random number: $randomNumber")

        if (binaryValue == randomNumber) {
            Log.d(TAG, "Answer is correct")
            showResult(true)
        } else {
            Log.d(TAG, "Answer is incorrect")
            showResult(false)
        }
    }

    private fun showResult(isWinner: Boolean) {
        Log.d(TAG, "showResult() called. isWinner: $isWinner")
        val resultFragment = FragmentResult.newInstance(isWinner)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, resultFragment) // Usando el ID correcto del FrameLayout
            .commit()
        Log.d(TAG, "Result fragment added")
    }

    override fun onPlayAgain() {
        Log.d(TAG, "onPlayAgain() called")
        generateNewNumber()
        // Remueve el fragmento actual
        Log.d(TAG, "Removing current fragment")
        try {
            supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentById(R.id.main)!!).commit()
            Log.d(TAG, "Fragment removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing fragment: ${e.message}")
        }
    }

    override fun onExit() {
        Log.d(TAG, "onExit() called")
        finish() // Cierra la BinaryActivity
        Log.d(TAG, "BinaryActivity finished")
    }
}