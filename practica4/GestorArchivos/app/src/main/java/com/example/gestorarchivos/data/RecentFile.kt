package com.example.gestorarchivos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val fileSize: Long,
    val lastAccessTimestamp: Long = System.currentTimeMillis(),
    val mimeType: String? = null
)
