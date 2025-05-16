package com.example.gestorarchivos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_files")
data class FavoriteFile(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val fileSize: Long,
    val dateAdded: Long = System.currentTimeMillis()
)
