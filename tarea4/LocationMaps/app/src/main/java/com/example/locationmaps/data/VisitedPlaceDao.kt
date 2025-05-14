package com.example.locationmaps.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VisitedPlaceDao {
    @Query("SELECT * FROM visited_places ORDER BY timestamp DESC")
    fun getAllVisitedPlaces(): LiveData<List<VisitedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(place: VisitedPlace): Long
}