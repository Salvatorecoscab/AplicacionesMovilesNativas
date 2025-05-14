package com.example.gestorarchivos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteFileDao {
    @Query("SELECT * FROM favorite_files ORDER BY fileName ASC")
    fun getAllFavorites(): Flow<List<FavoriteFile>>

    @Query("SELECT * FROM favorite_files WHERE isDirectory = :isDirectory ORDER BY fileName ASC")
    fun getFavoritesByType(isDirectory: Boolean): Flow<List<FavoriteFile>>

    @Query("SELECT COUNT(*) FROM favorite_files WHERE filePath = :filePath")
    suspend fun isFavorite(filePath: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favoriteFile: FavoriteFile)

    @Query("DELETE FROM favorite_files WHERE filePath = :filePath")
    suspend fun removeFromFavorites(filePath: String)

    @Query("DELETE FROM favorite_files")
    suspend fun clearAllFavorites()
}
