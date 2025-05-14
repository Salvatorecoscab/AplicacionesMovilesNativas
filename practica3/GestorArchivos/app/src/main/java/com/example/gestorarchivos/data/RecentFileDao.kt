package com.example.gestorarchivos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastAccessTimestamp DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 30): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files WHERE isDirectory = :isDirectory ORDER BY lastAccessTimestamp DESC LIMIT :limit")
    fun getRecentFilesByType(isDirectory: Boolean, limit: Int = 30): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)

    @Query("DELETE FROM recent_files WHERE filePath = :filePath")
    suspend fun deleteRecentFile(filePath: String)

    @Query("DELETE FROM recent_files WHERE lastAccessTimestamp < :timestamp")
    suspend fun deleteOldFiles(timestamp: Long)

    @Query("SELECT COUNT(*) FROM recent_files")
    suspend fun getRecentFilesCount(): Int

    @Query("DELETE FROM recent_files WHERE filePath NOT IN (SELECT filePath FROM recent_files ORDER BY lastAccessTimestamp DESC LIMIT :keepCount)")
    suspend fun keepMostRecent(keepCount: Int = 100)
}
