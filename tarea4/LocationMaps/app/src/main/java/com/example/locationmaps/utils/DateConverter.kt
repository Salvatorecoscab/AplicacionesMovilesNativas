package com.example.locationmaps.utils

import androidx.room.TypeConverter
import java.util.Date

/**
 * Conversor de tipos para Room que permite almacenar y recuperar objetos Date
 */
class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}