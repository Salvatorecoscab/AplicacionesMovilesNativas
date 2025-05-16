package com.example.gestorarchivos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import com.example.gestorarchivos.data.FavoritesRepository
import com.example.gestorarchivos.data.RecentFilesRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : BaseActivity() {
    private lateinit var recentFilesRepository: RecentFilesRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var tvCurrentPath: TextView
    private lateinit var rvFiles: RecyclerView
    private lateinit var btnRequestPermission: Button
    private lateinit var fileAdapter: FileAdapter
    private lateinit var bottomNavigation: BottomNavigationView

    // Estados de visualización
    private var showingFavorites = false
    private var showingRecentFiles = false

    // Un mapa para almacenar el estado de favoritos de los archivos
    private val favoritesStatus = mutableMapOf<String, Boolean>()

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var pendingThemeChange = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val MANAGE_STORAGE_PERMISSION_REQUEST = 1002
        private const val PREF_CURRENT_DIR = "current_directory"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar repositorios
        recentFilesRepository = RecentFilesRepository(this)
        favoritesRepository = FavoritesRepository(this)

        // Inicializar vistas
        tvCurrentPath = findViewById(R.id.tvCurrentPath)
        rvFiles = findViewById(R.id.rvFiles)
        btnRequestPermission = findViewById(R.id.btnRequestPermission)

        // Agregar Bottom Navigation View
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Restaurar directorio actual si está guardado
        val savedPath = getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .getString(PREF_CURRENT_DIR, null)

        if (savedPath != null) {
            val savedDir = File(savedPath)
            if (savedDir.exists() && savedDir.isDirectory) {
                currentDir = savedDir
            }
        }

        // Inicializar adaptador de archivos con soporte para favoritos
        fileAdapter = FileAdapter(
            files = emptyList(),
            onItemClick = { fileModel ->
                handleFileSelection(fileModel)
            },
            onFavoriteClick = { fileModel ->
                toggleFavorite(fileModel.file)
            }
        )

        // Configurar RecyclerView
        rvFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
        }

        // Configurar listeners
        btnRequestPermission.setOnClickListener {
            requestStoragePermission()
        }

        // Configurar navegación inferior
        setupBottomNavigation()

        // Cargar estado de favoritos
        loadFavoritesStatus()

        // Verificar permisos al iniciar
        checkStoragePermission()

        // Configurar botón para cambiar de tema
        val btnToggleTheme = findViewById<Button>(R.id.btnToggleTheme)
        updateThemeButtonText(btnToggleTheme)

        btnToggleTheme.setOnClickListener {
            // Guardar directorio actual antes de cambiar tema
            saveCurrentDirectory()

            // Cambiar tema
            val newTheme = themeHelper.toggleTheme(this)
            pendingThemeChange = true
            updateThemeButtonText(btnToggleTheme)
            recreate()
        }

        // Programar limpieza de caché
        scheduleImageCacheCleaning()
    }

    // Carga el estado de favoritos desde el repositorio
    private fun loadFavoritesStatus() {
        lifecycleScope.launch {
            favoritesRepository.getAllFavorites().collect { favorites ->
                val newFavoritesMap = mutableMapOf<String, Boolean>()
                favorites.forEach { favorite ->
                    newFavoritesMap[favorite.filePath] = true
                }
                favoritesStatus.clear()
                favoritesStatus.putAll(newFavoritesMap)

                // Actualizar UI según la vista actual
                when {
                    showingFavorites -> loadFavoriteFiles()
                    showingRecentFiles -> updateRecentFilesWithFavorites()
                    else -> fileAdapter.setFavoritesStatus(favoritesStatus)
                }
            }
        }
    }

    // Actualiza los íconos de favoritos en la vista de archivos recientes
    private fun updateRecentFilesWithFavorites() {
        fileAdapter.setFavoritesStatus(favoritesStatus)
    }

    // Alterna el estado de favorito de un archivo
    private fun toggleFavorite(file: File) {
        lifecycleScope.launch {
            val newStatus = favoritesRepository.toggleFavorite(file)
            fileAdapter.updateFavoriteStatus(file.absolutePath, newStatus)

            // Si estamos viendo favoritos y se quitó un favorito, actualizar la lista
            if (showingFavorites && !newStatus) {
                loadFavoriteFiles()
            }
        }
    }

    // Carga y muestra archivos favoritos
    private fun loadFavoriteFiles() {
        lifecycleScope.launch {
            val favorites = favoritesRepository.getAllFavorites().first()
            val fileModels = favorites.mapNotNull { favorite ->
                val file = File(favorite.filePath)
                if (file.exists()) {
                    FileModel(file)
                } else {
                    // Si el archivo ya no existe, eliminarlo de favoritos
                    favoritesRepository.removeFromFavorites(file)
                    null
                }
            }

            fileAdapter.updateFiles(fileModels)

            // Actualizar título y estado de vista
            tvCurrentPath.text = "Favoritos"
            showingFavorites = true
            showingRecentFiles = false

            // Marcar todos los archivos como favoritos en la vista
            val favStatusMap = fileModels.associate { it.file.absolutePath to true }
            fileAdapter.setFavoritesStatus(favStatusMap)
        }
    }

    // Configuración de la navegación inferior
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_files -> {
                    showDirectoryContent()
                    true
                }
                R.id.navigation_recent -> {
                    loadRecentFiles()
                    true
                }
                R.id.navigation_favorites -> {
                    loadFavoriteFiles()
                    true
                }
                else -> false
            }
        }
    }

    // Maneja la selección de un archivo
    private fun handleFileSelection(fileModel: FileModel) {
        if (fileModel.isDirectory) {
            navigateToDirectory(fileModel.file)
        } else {
            // Guardar en recientes antes de abrir
            addToRecentFiles(fileModel.file)

            when {
                isImageFile(fileModel.file) -> {
                    val intent = Intent(this, ImageViewerActivity::class.java).apply {
                        putExtra("IMAGE_PATH", fileModel.file.absolutePath)
                    }
                    startActivity(intent)
                }
                isTextFile(fileModel.file) -> {
                    val intent = Intent(this, TextViewerActivity::class.java).apply {
                        putExtra("FILE_PATH", fileModel.file.absolutePath)
                    }
                    startActivity(intent)
                }
                else -> {
                    showFileOptionsDialog(fileModel.file)
                }
            }
        }
    }

    // Muestra el contenido del directorio actual
    private fun showDirectoryContent() {
        tvCurrentPath.text = currentDir.absolutePath
        showingFavorites = false
        showingRecentFiles = false
        loadFiles(currentDir)
    }

    // Carga los archivos de un directorio
    private fun loadFiles(directory: File) {
        val files = directory.listFiles()?.map { file ->
            FileModel(file)
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

        fileAdapter.updateFiles(files)
        fileAdapter.setFavoritesStatus(favoritesStatus)
    }

    // Navega a un directorio específico
    private fun navigateToDirectory(directory: File) {
        if (directory.isDirectory) {
            currentDir = directory

            // Si estamos en otra vista, cambiar a la vista de archivos
            if (showingRecentFiles || showingFavorites) {
                bottomNavigation.selectedItemId = R.id.navigation_files
            } else {
                // Si ya estamos en la vista de archivos, solo cargar el nuevo directorio
                showDirectoryContent()
            }

            // Guardar la nueva ruta
            saveCurrentDirectory()
        }
    }

    // Carga y muestra archivos recientes
    private fun loadRecentFiles() {
        lifecycleScope.launch {
            recentFilesRepository.getRecentFiles(30).collect { recentFiles ->
                val fileModels = recentFiles.mapNotNull { recentFile ->
                    val file = File(recentFile.filePath)
                    if (file.exists()) {
                        FileModel(file)
                    } else {
                        // El archivo ya no existe, eliminarlo de recientes
                        lifecycleScope.launch {
                            recentFilesRepository.removeFromRecent(recentFile.filePath)
                        }
                        null
                    }
                }

                fileAdapter.updateFiles(fileModels)
                fileAdapter.setFavoritesStatus(favoritesStatus)

                // Actualizar título y estado de vista
                tvCurrentPath.text = "Archivos Recientes"
                showingRecentFiles = true
                showingFavorites = false

                // Mostrar mensaje si no hay archivos recientes (opcional)
                // findViewById<TextView>(R.id.tvNoRecentFiles)?.visibility =
                //     if (fileModels.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // Añade un archivo al historial de recientes
    private fun addToRecentFiles(file: File) {
        lifecycleScope.launch {
            recentFilesRepository.addToRecent(file)
        }
    }

    // Programa la limpieza periódica de la caché de imágenes
    private fun scheduleImageCacheCleaning() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = applicationContext.cacheDir
                val maxCacheSize = 200 * 1024 * 1024L // 200MB

                if (getDirSize(cacheDir) > maxCacheSize) {
                    Coil.imageLoader(applicationContext).memoryCache?.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Calcula el tamaño de un directorio
    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    // Verifica si se tienen los permisos de almacenamiento
    private fun checkStoragePermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            showFiles()
        } else {
            btnRequestPermission.visibility = View.VISIBLE
            rvFiles.visibility = View.GONE
        }

        return hasPermission
    }

    // Solicita permisos de almacenamiento
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    // Muestra los archivos cuando se tienen permisos
    private fun showFiles() {
        btnRequestPermission.visibility = View.GONE
        rvFiles.visibility = View.VISIBLE

        // Cargar vista según el estado actual
        when {
            showingFavorites -> loadFavoriteFiles()
            showingRecentFiles -> loadRecentFiles()
            else -> loadFiles(currentDir)
        }
    }

    // Verificar si un archivo es una imagen
    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    // Verificar si un archivo es de texto
    private fun isTextFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("txt", "md", "log", "json", "xml", "html", "css", "js", "kt", "java", "py", "c", "cpp")
    }

    // Muestra el diálogo de opciones para un archivo
    private fun showFileOptionsDialog(file: File) {
        // Modificar tu FileOptionsDialog para pasarle el favoritesRepository
        val dialog = FileOptionsDialog(this, file, favoritesRepository)
        dialog.show()
    }

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

    // Guarda el directorio actual
    private fun saveCurrentDirectory() {
        getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_CURRENT_DIR, currentDir.absolutePath)
            .apply()
    }

    // Maneja el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFiles()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Maneja el resultado de la solicitud de permisos de Android 11+
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MANAGE_STORAGE_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    showFiles()
                } else {
                    Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Maneja el botón de retroceso
    override fun onBackPressed() {
        when {
            showingRecentFiles || showingFavorites -> {
                // Si estamos en favoritos o recientes, volver a la vista de archivos
                bottomNavigation.selectedItemId = R.id.navigation_files
            }
            currentDir.absolutePath != Environment.getExternalStorageDirectory().absolutePath -> {
                // Si no estamos en el directorio raíz, ir al directorio padre
                navigateToDirectory(currentDir.parentFile!!)
            }
            else -> {
                // Si estamos en el directorio raíz, comportamiento normal
                super.onBackPressed()
            }
        }
    }

    // Guardar estado al pausar la actividad
    override fun onPause() {
        super.onPause()
        saveCurrentDirectory()
    }

    // Crear menú de opciones
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Manejar opciones del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_clear_recent -> {
                // Limpiar historial de archivos recientes
                lifecycleScope.launch {
                    recentFilesRepository.cleanupOldEntries(0)
                    if (showingRecentFiles) {
                        loadRecentFiles()
                    }
                    Toast.makeText(this@MainActivity, "Historial limpiado", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_clear_favorites -> {
                // Limpiar favoritos
                lifecycleScope.launch {
                    favoritesRepository.clearAllFavorites()
                    if (showingFavorites) {
                        loadFavoriteFiles()
                    }
                    Toast.makeText(this@MainActivity, "Favoritos limpiados", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_clear_cache -> {
                // Limpiar caché de imágenes
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        Coil.imageLoader(applicationContext).memoryCache?.clear()
                        Toast.makeText(this@MainActivity, "Caché limpiada", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
