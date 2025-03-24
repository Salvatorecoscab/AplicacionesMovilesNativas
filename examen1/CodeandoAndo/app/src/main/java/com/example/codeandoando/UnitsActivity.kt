package com.example.codeandoando

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.codeandoando.databinding.ActivityUnitsBinding
import java.util.Random

class UnitsActivity : AppCompatActivity(), FragmentResult.OnResultInteractionListener {

    private lateinit var binding: ActivityUnitsBinding
    private lateinit var backButton: ImageButton
    private lateinit var helpButton: ImageButton
    private lateinit var asciiCodeTextView: TextView
    private lateinit var inputTextEditText: EditText
    private lateinit var resultAsciiTextView: TextView
    private lateinit var checkButton: Button
    private lateinit var generateButton: Button
    private val words = listOf(
        "camara", "circuito", "binario", "digital", "internet", "codigo", "software", "hardware",
        "computadora", "teclado", "pantalla", "raton", "memoria", "procesador", "algoritmo",
        "redes", "servidor", "aplicacion", "programacion", "sensor", "robot", "drone", "wifi",
        "bluetooth", "pixel"
    )
    private var currentWord: String = ""
    private val TAG = "UnitsActivity"
    private var userInput: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backButton = binding.backButton
        helpButton = binding.helpButton
        asciiCodeTextView = binding.asciiCodeTextView
        inputTextEditText = binding.inputTextEditText
        resultAsciiTextView = binding.resultAsciiTextView
        checkButton = binding.checkButton
        generateButton = binding.generateButton

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        helpButton.setOnClickListener {
            showAsciiHint()
        }

        generateButton.setOnClickListener {
            Log.d(TAG, "Generate button clicked")
            generateNewChallenge()
        }

        checkButton.setOnClickListener {
            Log.d(TAG, "Check button clicked")
            checkAnswer()
        }

        generateNewChallenge()
        Log.d(TAG, "onCreate finished")
    }

    private fun generateNewChallenge() {
        Log.d(TAG, "generateNewChallenge() called")
        currentWord = getRandomWord()
        val asciiCode = convertToAscii(currentWord)
        asciiCodeTextView.text = asciiCode
        inputTextEditText.text.clear()
        resultAsciiTextView.text = ""
        Log.d(TAG, "New challenge generated: $currentWord -> $asciiCode")
    }

    private fun getRandomWord(): String {
        val randomIndex = Random().nextInt(words.size)
        return words[randomIndex]
    }

    private fun convertToAscii(word: String): String {
        val stringBuilder = StringBuilder()
        for (char in word) {
            stringBuilder.append(char.code).append(" ")
        }
        return stringBuilder.toString().trim()
    }

    private fun showAsciiHint() {
        val hintText = buildAsciiHint()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.ascii_hint_title))
            .setMessage(hintText)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun checkAnswer() {
        Log.d(TAG, "checkAnswer() called")
        val userAnswer = inputTextEditText.text.toString().trim().lowercase() // Usando inputTextEditText
        Log.d(TAG, "User answer: $userAnswer, Correct word: $currentWord")
        if (userAnswer == currentWord) {
            Log.d(TAG, "Answer is correct")
            resultAsciiTextView.text = getString(R.string.correct)
            Toast.makeText(this, getString(R.string.correct_toast), Toast.LENGTH_SHORT).show()
            showResult(true) // Pasar true para indicar respuesta correcta
        } else {
            Log.d(TAG, "Answer is incorrect")
            resultAsciiTextView.text = getString(R.string.incorrect)
            Toast.makeText(this, getString(R.string.incorrect_toast), Toast.LENGTH_SHORT).show()
            showResult(false) // Pasar false para indicar respuesta incorrecta
        }
    }

    private fun showResult(isCorrect: Boolean) {
        Log.d(TAG, "showResult() called. isCorrect: $isCorrect")
        val resultFragment = FragmentResult.newInstance(isCorrect)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, resultFragment)
            .commit()
        Log.d(TAG, "Result fragment added")
    }

    override fun onPlayAgain() {
        Log.d(TAG, "onPlayAgain() called")
        generateNewChallenge()
        try {
            supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentById(R.id.main)!!).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing fragment: ${e.message}")
        }
    }

    override fun onExit() {
        Log.d(TAG, "onExit() called")
        finish()
    }
    private fun buildAsciiHint(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        val hintBuilder = StringBuilder()
        for (char in alphabet) {
            hintBuilder.append("$char: ${char.code}\n")
        }
        return hintBuilder.toString()
    }
}