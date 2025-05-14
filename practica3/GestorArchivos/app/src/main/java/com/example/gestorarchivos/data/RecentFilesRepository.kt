package com.example.gestorarchivos.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecentFilesRepository(context: Context) {
    private val recentFileDao = AppDatabase.getDatabase(context).recentFileDao()

    fun getRecentFiles(limit: Int = 30): Flow<List<RecentFile>> {
        return recentFileDao.getRecentFiles(limit)
    }

    fun getRecentFilesByType(isDirectory: Boolean, limit: Int = 30): Flow<List<RecentFile>> {
        return recentFileDao.getRecentFilesByType(isDirectory, limit)
    }

    suspend fun addToRecent(file: File) {
        // Verificar si el archivo aún existe
        if (!file.exists()) return

        val recentFile = RecentFile(
            filePath = file.absolutePath,
            fileName = file.name,
            isDirectory = file.isDirectory,
            fileSize = if (file.isFile) file.length() else 0,
            mimeType = getMimeType(file)
        )

        recentFileDao.insertRecentFile(recentFile)

        // Mantener la lista en un tamaño razonable
        val count = recentFileDao.getRecentFilesCount()
        if (count > 100) {
            recentFileDao.keepMostRecent(100)
        }
    }

    suspend fun removeFromRecent(filePath: String) {
        recentFileDao.deleteRecentFile(filePath)
    }

    suspend fun cleanupOldEntries(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        recentFileDao.deleteOldFiles(cutoffTime)
    }

    private fun getMimeType(file: File): String? {
        return if (file.isFile) {
            when(file.extension.lowercase()) {
                "jpg", "jpeg", "png", "gif" -> "image/${file.extension.lowercase()}"
                "mp4", "avi", "mov" -> "video/${file.extension.lowercase()}"
                "mp3", "wav", "ogg" -> "audio/${file.extension.lowercase()}"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                else -> null
            }
        } else null
    }
}
