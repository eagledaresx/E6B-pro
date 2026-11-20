package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aircraft_profiles")
data class AircraftProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tailNumber: String,
    val fuelType: String, // "100LL" (6.0 lb/gal) or "Jet-A" (6.7 lb/gal)
    val fuelWeightPerGal: Double,
    val maxTakeoffWeightLbs: Double,
    val maxLandingWeightLbs: Double,
    val cruiseTasKt: Double,
    val fuelBurnGph: Double,
    val stationsJson: String,
    val envelopeJson: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "navlog_legs")
data class NavLogLegEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val legOrder: Int,
    val fromFix: String,
    val toFix: String,
    val altitudeFt: Double,
    val trueCourseDeg: Double,
    val distanceNm: Double,
    val tasKt: Double,
    val windDirDeg: Double,
    val windSpdKt: Double,
    val magVarDeg: Double, // + West, - East
    val fuelBurnGph: Double
)

@Entity(tableName = "saved_calculations")
data class SavedCalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val category: String,
    val title: String,
    val summary: String
)
