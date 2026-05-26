package com.example.core.data

import androidx.room.*
import com.example.core.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<Location>>

    @Query("SELECT * FROM locations ORDER BY name ASC")
    suspend fun getAllLocationsDirectly(): List<Location>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: Location): Long

    @Update
    suspend fun updateLocation(location: Location)

    @Delete
    suspend fun deleteLocation(location: Location)

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Int): Location?
}
