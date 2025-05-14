package com.example.gestorarchivos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ThumbnailCacheManager(private val context: Context) {

    // Caché en memoria (LRU = Least Recently Used)
    private val memoryCache: LruCache<String, Bitmap>

    // Directorio para caché en disco
    private val diskCacheDir: File

    init {
        // Asignar 1/8 de la memoria disponible para el caché
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // Tamaño en KB
                return bitmap.byteCount / 1024
            }
        }

        // Inicializar directorio de caché
        diskCacheDir = File(context.cacheDir, "thumbnails")
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    // Generar clave única para cada imagen basada en su ruta y última modificación
    private fun generateKey(imagePath: String): String {
        val file = File(imagePath)
        val input = "$imagePath-${file.lastModified()}"
        val digest = MessageDigest.getInstance("MD5")
        digest.update(input.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // Obtener miniatura (primero de memoria, luego de disco, finalmente generarla)
    suspend fun getThumbnail(imagePath: String, width: Int = 200, height: Int = 200): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val key = generateKey(imagePath)

                // 1. Intentar obtener de la caché en memoria
                memoryCache.get(key)?.let { return@withContext it }

                // 2. Intentar obtener de la caché en disco
                val cacheFile = File(diskCacheDir, key)
                if (cacheFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    bitmap?.let {
                        // Guardar en caché de memoria
                        memoryCache.put(key, it)
                        return@withContext it
                    }
                }

                // 3. Generar la miniatura
                val originalFile = File(imagePath)
                if (!originalFile.exists()) return@withContext null

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createImageThumbnail(
                        originalFile,
                        android.util.Size(width, height),
                        null
                    )
                } else {
                    // Para versiones anteriores
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(imagePath, options)

                    options.inSampleSize = calculateInSampleSize(options, width, height)
                    options.inJustDecodeBounds = false

                    BitmapFactory.decodeFile(imagePath, options)
                }

                bitmap?.let {
                    // Guardar en caché de memoria
                    memoryCache.put(key, it)

                    // Guardar en caché de disco
                    FileOutputStream(cacheFile).use { fos ->
                        it.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                }

                return@withContext bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    // Calcular el factor de muestreo para reducir el tamaño de la imagen
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // Limpiar caché antigua (llamar periódicamente o cuando la app esté inactiva)
    fun clearOldCache(maxAgeInMillis: Long = 30 * 24 * 60 * 60 * 1000L) { // 30 días por defecto
        try {
            val now = System.currentTimeMillis()
            diskCacheDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeInMillis) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Limpiar toda la caché
    fun clearAllCache() {
        memoryCache.evictAll()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
}
