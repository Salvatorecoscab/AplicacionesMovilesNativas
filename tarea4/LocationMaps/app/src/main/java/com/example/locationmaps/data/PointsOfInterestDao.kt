package com.example.locationmaps.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PointOfInterestDao {
    @Query("SELECT * FROM points_of_interest ORDER BY dateAdded DESC")
    fun getAllPOIs(): LiveData<List<PointOfInterest>>

    @Query("SELECT * FROM points_of_interest WHERE category = :category")
    fun getPOIsByCategory(category: String): LiveData<List<PointOfInterest>>

    @Query("SELECT * FROM points_of_interest WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPOIs(query: String): LiveData<List<PointOfInterest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(poi: PointOfInterest): Long

    @Query("SELECT * FROM points_of_interest WHERE id = :id")
    fun getPoiById(id: Long): LiveData<PointOfInterest?>
    @Update
    fun update(poi: PointOfInterest): Int

    @Delete
    fun delete(poi: PointOfInterest): Int
}