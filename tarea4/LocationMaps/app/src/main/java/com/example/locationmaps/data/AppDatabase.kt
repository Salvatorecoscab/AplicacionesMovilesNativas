package com.example.locationmaps.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.locationmaps.utils.DateConverter

@Database(
    entities = [PointOfInterest::class, ExploredZone::class, VisitedPlace::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    // Mantén solo un método por tipo de DAO
    abstract fun pointOfInterestDao(): PointOfInterestDao
    abstract fun exploredZoneDao(): ExploredZoneDao
    abstract fun visitedPlaceDao(): VisitedPlaceDao

    // Si necesitas mantener compatibilidad con código existente,
    // implementa métodos no abstractos que llamen a los anteriores
    fun poiDao(): PointOfInterestDao {
        return pointOfInterestDao()
    }

    fun zoneDao(): ExploredZoneDao {
        return exploredZoneDao()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "location_explorer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}