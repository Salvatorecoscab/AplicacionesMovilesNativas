package com.example.gestorarchivos

import com.example.gestorarchivos.data.RecentFile
import java.io.File

data class FileModel(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val lastModified: Long = file.lastModified(),
    val size: Long = if (file.isDirectory) 0 else file.length()
){
    // Método para convertir a RecentFile
    fun toRecentFile(): RecentFile {
        return RecentFile(
            filePath = file.absolutePath,
            fileName = name,
            isDirectory = isDirectory,
            fileSize = if (file.isFile) file.length() else 0
        )
    }

    companion object {
        // Método para convertir desde RecentFile
        fun fromRecentFile(recentFile: RecentFile): FileModel? {
            val file = File(recentFile.filePath)
            return if (file.exists()) {
                FileModel(file)
            } else null
        }
    }
}
