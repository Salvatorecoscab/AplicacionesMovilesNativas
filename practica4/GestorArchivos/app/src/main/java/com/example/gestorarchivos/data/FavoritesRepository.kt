package com.example.gestorarchivos.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

class FavoritesRepository(context: Context) {
    private val favoriteFileDao = AppDatabase.getDatabase(context).favoriteFileDao()

    fun getAllFavorites(): Flow<List<FavoriteFile>> {
        return favoriteFileDao.getAllFavorites()
    }

    fun getFavoritesByType(isDirectory: Boolean): Flow<List<FavoriteFile>> {
        return favoriteFileDao.getFavoritesByType(isDirectory)
    }

    suspend fun isFavorite(file: File): Boolean {
        return favoriteFileDao.isFavorite(file.absolutePath) > 0
    }

    suspend fun addToFavorites(file: File) {
        if (!file.exists()) return

        val favoriteFile = FavoriteFile(
            filePath = file.absolutePath,
            fileName = file.name,
            isDirectory = file.isDirectory,
            fileSize = if (file.isFile) file.length() else 0
        )

        favoriteFileDao.addToFavorites(favoriteFile)
    }

    suspend fun removeFromFavorites(file: File) {
        favoriteFileDao.removeFromFavorites(file.absolutePath)
    }

    suspend fun toggleFavorite(file: File): Boolean {
        val isFavorite = isFavorite(file)
        if (isFavorite) {
            removeFromFavorites(file)
        } else {
            addToFavorites(file)
        }
        return !isFavorite
    }

    suspend fun clearAllFavorites() {
        favoriteFileDao.clearAllFavorites()
    }
}
