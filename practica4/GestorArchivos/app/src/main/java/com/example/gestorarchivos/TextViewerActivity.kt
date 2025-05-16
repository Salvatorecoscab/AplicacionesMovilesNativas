package com.example.gestorarchivos



import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class TextViewerActivity : BaseActivity() {  // Extiende BaseActivity

    private lateinit var textContent: TextView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_viewer)

        textContent = findViewById(R.id.textContent)
        toolbar = findViewById(R.id.toolbar)

        // Configurar la barra de herramientas
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Recuperar la ruta del archivo
        val filePath = intent.getStringExtra("FILE_PATH")
        if (filePath == null) {
            textContent.text = "Error: No se especificó archivo"
            return
        }

        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            textContent.text = "Error: El archivo no existe o no es válido"
            return
        }

        // Establecer el título con el nombre del archivo
        title = file.name

        try {
            // Leer el contenido del archivo
            val content = readTextFile(file)
            textContent.text = content
        } catch (e: Exception) {
            textContent.text = "Error al leer el archivo: ${e.message}"
        }
    }

    private fun readTextFile(file: File): String {
        val stringBuilder = StringBuilder()

        try {
            val bufferedReader = BufferedReader(FileReader(file))
            var line: String?

            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
                stringBuilder.append("\n")
            }

            bufferedReader.close()
        } catch (e: Exception) {
            return "Error al leer el archivo: ${e.message}"
        }

        return stringBuilder.toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar clic en el botón de atrás en la barra de herramientas
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
