package com.example.gestorarchivos

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.gestorarchivos.data.FavoritesRepository
import kotlinx.coroutines.launch
import java.io.File

class FileOptionsDialog(
    context: Context,
    private val file: File,
    private val favoritesRepository: FavoritesRepository
) : Dialog(context) {

    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_file_options)

        val tvFileName = findViewById<TextView>(R.id.tvFileName)
        val btnOpenWith = findViewById<Button>(R.id.btnOpenWith)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        tvFileName.text = file.name



        btnOpenWith.setOnClickListener {
            openFileWithExternalApp()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateFavoriteButton(button: Button) {
        button.text = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
    }

    private fun openFileWithExternalApp() {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        // Determinar el tipo MIME
        val mimeType = getMimeType(file)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
}


