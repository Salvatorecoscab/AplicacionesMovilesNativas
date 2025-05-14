package com.example.locationmaps.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExploredZoneDao {
    @Query("SELECT * FROM explored_zones")
    fun getAllZones(): LiveData<List<ExploredZone>>

    @Query("SELECT * FROM explored_zones WHERE isExplored = 1")
    fun getExploredZones(): LiveData<List<ExploredZone>>

    @Query("SELECT * FROM explored_zones WHERE isExplored = 0")
    fun getUnexploredZones(): LiveData<List<ExploredZone>>

    // Añadir este método para obtener una lista de zonas sin explorar de forma síncrona
    @Query("SELECT * FROM explored_zones WHERE isExplored = 0")
    fun getUnexploredZonesList(): List<ExploredZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(zone: ExploredZone): Long

    @Update
    fun update(zone: ExploredZone): Int
}