package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface E6BDao {
    // Aircraft Profiles
    @Query("SELECT * FROM aircraft_profiles ORDER BY isDefault DESC, id ASC")
    fun getAllAircraft(): Flow<List<AircraftProfileEntity>>

    @Query("SELECT COUNT(*) FROM aircraft_profiles")
    suspend fun getAircraftCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAircraft(profile: AircraftProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAircraft(profiles: List<AircraftProfileEntity>)

    @Update
    suspend fun updateAircraft(profile: AircraftProfileEntity)

    @Query("DELETE FROM aircraft_profiles WHERE id = :id")
    suspend fun deleteAircraftById(id: Long)

    // NavLog Legs
    @Query("SELECT * FROM navlog_legs ORDER BY legOrder ASC, id ASC")
    fun getAllNavLegs(): Flow<List<NavLogLegEntity>>

    @Query("SELECT COUNT(*) FROM navlog_legs")
    suspend fun getNavLegCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNavLeg(leg: NavLogLegEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNavLegs(legs: List<NavLogLegEntity>)

    @Update
    suspend fun updateNavLeg(leg: NavLogLegEntity)

    @Query("DELETE FROM navlog_legs WHERE id = :id")
    suspend fun deleteNavLegById(id: Long)

    @Query("DELETE FROM navlog_legs")
    suspend fun clearNavLogs()

    // Saved Calculations
    @Query("SELECT * FROM saved_calculations ORDER BY timestampMs DESC")
    fun getAllSavedCalculations(): Flow<List<SavedCalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedCalculation(calc: SavedCalculationEntity)

    @Query("DELETE FROM saved_calculations WHERE id = :id")
    suspend fun deleteSavedCalculationById(id: Long)
}
