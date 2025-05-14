package com.example.cammic // Asegúrate que el paquete sea el correcto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : BaseActivity() {

    private lateinit var buttonGoToPhoto: Button
    private lateinit var buttonGoToAudio: Button
    //add button to change theme
    private lateinit var btnToggleTheme: Button
    private var pendingThemeChange = false
    // Actualiza el texto del botón de tema
    private fun updateThemeButtonText(button: Button) {
        val currentTheme = themeHelper.getCurrentTheme()
        val buttonText = if (currentTheme == "ipn") {
            "tema ESCOM"
        } else {
            "tema IPN"
        }
        button.text = buttonText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Usa el nuevo activity_main.xml

        buttonGoToPhoto = findViewById(R.id.buttonGoToPhoto)
        buttonGoToAudio = findViewById(R.id.buttonGoToAudio)
        btnToggleTheme = findViewById(R.id.buttonChangeTheme)
        buttonGoToPhoto.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        buttonGoToAudio.setOnClickListener {
            val intent = Intent(this, AudioActivity::class.java)
            startActivity(intent)
        }
        updateThemeButtonText(btnToggleTheme)

        btnToggleTheme.setOnClickListener {

            // Cambiar tema
            val newTheme = themeHelper.toggleTheme(this)
            pendingThemeChange = true
            updateThemeButtonText(btnToggleTheme)
            recreate()
        }
    }
}
