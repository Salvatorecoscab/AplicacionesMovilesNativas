package com.example.codeandoando

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.codeandoando.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val themeKey = "current_theme"
    private val themeGuinda = "guinda"
    private val themeAzul = "azul"
    private var currentTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        currentTheme = loadThemePreference()

        if (currentTheme == null) {
            currentTheme = themeGuinda
            saveThemePreference(currentTheme)
        }
        applyTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.binaryButton.setOnClickListener {
            Toast.makeText(this, "Ir a Sistema binario", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, BinaryActivity::class.java))
        }

        binding.unitsButton.setOnClickListener {
            Toast.makeText(this, "Ir a decodificar mensaje", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UnitsActivity::class.java))
        }

        binding.themeButton.setOnClickListener {
            toggleTheme()
        }
    }

    private fun saveThemePreference(theme: String?) {
        with(sharedPreferences.edit()) {
            putString(themeKey, theme)
            apply()
        }
    }

    private fun loadThemePreference(): String? {
        return sharedPreferences.getString(themeKey, null)
    }

    private fun applyTheme() {
        when (currentTheme) {
            themeGuinda -> {
                setTheme(R.style.Theme_CodeandoAndo_GuindaClaro)
            }
            themeAzul -> {
                setTheme(R.style.Theme_CodeandoAndo_AzulClaro)
            }
            else -> {
                setTheme(R.style.Theme_CodeandoAndo_GuindaClaro)
            }
        }
    }

    private fun toggleTheme() {
        currentTheme = when (currentTheme) {
            themeGuinda -> {
                Toast.makeText(this, getString(R.string.theme_azul_selected), Toast.LENGTH_SHORT).show()
                themeAzul
            }
            themeAzul -> {
                Toast.makeText(this, getString(R.string.theme_guinda_selected), Toast.LENGTH_SHORT).show()
                themeGuinda
            }
            else -> {
                Toast.makeText(this, getString(R.string.theme_guinda_selected), Toast.LENGTH_SHORT).show()
                themeGuinda
            }
        }
        saveThemePreference(currentTheme)
        recreate()
    }
}