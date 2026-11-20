package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.CgVertex
import com.example.domain.WeightStation
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [
        AircraftProfileEntity::class,
        NavLogLegEntity::class,
        SavedCalculationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class E6BDatabase : RoomDatabase() {
    abstract fun e6bDao(): E6BDao

    companion object {
        @Volatile
        private var INSTANCE: E6BDatabase? = null

        fun getInstance(context: Context): E6BDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    E6BDatabase::class.java,
                    "e6b_pro_cockpit.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun stationsToJson(stations: List<WeightStation>): String {
            val arr = JSONArray()
            stations.forEach { s ->
                arr.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("weightLbs", s.weightLbs)
                    put("armIn", s.armIn)
                    put("maxWeightLbs", s.maxWeightLbs)
                    put("isFuel", s.isFuel)
                })
            }
            return arr.toString()
        }

        fun jsonToStations(json: String): List<WeightStation> {
            val list = mutableListOf<WeightStation>()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WeightStation(
                        id = obj.optString("id", "st_$i"),
                        name = obj.optString("name", "Station ${i + 1}"),
                        weightLbs = obj.optDouble("weightLbs", 0.0),
                        armIn = obj.optDouble("armIn", 40.0),
                        maxWeightLbs = obj.optDouble("maxWeightLbs", 9999.0),
                        isFuel = obj.optBoolean("isFuel", false)
                    )
                )
            }
            return list
        }

        fun envelopeToJson(vertices: List<CgVertex>): String {
            val arr = JSONArray()
            vertices.forEach { v ->
                arr.put(JSONObject().apply {
                    put("armIn", v.armIn)
                    put("weightLbs", v.weightLbs)
                })
            }
            return arr.toString()
        }

        fun jsonToEnvelope(json: String): List<CgVertex> {
            val list = mutableListOf<CgVertex>()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CgVertex(
                        armIn = obj.optDouble("armIn", 35.0),
                        weightLbs = obj.optDouble("weightLbs", 1500.0)
                    )
                )
            }
            return list
        }

        fun defaultAircraftProfiles(): List<AircraftProfileEntity> = listOf(
            AircraftProfileEntity(
                name = "Cessna 172S Skyhawk",
                tailNumber = "N172SP",
                fuelType = "100LL",
                fuelWeightPerGal = 6.0,
                maxTakeoffWeightLbs = 2550.0,
                maxLandingWeightLbs = 2550.0,
                cruiseTasKt = 122.0,
                fuelBurnGph = 9.5,
                isDefault = true,
                stationsJson = stationsToJson(
                    listOf(
                        WeightStation("empty", "Basic Empty Weight", 1663.0, 39.2, 2000.0, false),
                        WeightStation("front", "Pilot + Front Pax", 340.0, 37.0, 450.0, false),
                        WeightStation("rear", "Rear Passengers", 150.0, 73.0, 450.0, false),
                        WeightStation("bag1", "Baggage Area 1", 35.0, 95.0, 120.0, false),
                        WeightStation("fuel", "Main Fuel (53 gal max)", 240.0, 48.0, 318.0, true)
                    )
                ),
                envelopeJson = envelopeToJson(
                    listOf(
                        CgVertex(35.0, 1500.0),
                        CgVertex(35.0, 1950.0),
                        CgVertex(41.0, 2550.0),
                        CgVertex(47.3, 2550.0),
                        CgVertex(47.3, 1500.0)
                    )
                )
            ),
            AircraftProfileEntity(
                name = "Piper PA-28-181 Archer III",
                tailNumber = "N281PA",
                fuelType = "100LL",
                fuelWeightPerGal = 6.0,
                maxTakeoffWeightLbs = 2550.0,
                maxLandingWeightLbs = 2550.0,
                cruiseTasKt = 128.0,
                fuelBurnGph = 10.2,
                isDefault = true,
                stationsJson = stationsToJson(
                    listOf(
                        WeightStation("empty", "Basic Empty Weight", 1690.0, 86.2, 2000.0, false),
                        WeightStation("front", "Front Seats", 340.0, 80.5, 450.0, false),
                        WeightStation("rear", "Rear Seats", 140.0, 118.1, 450.0, false),
                        WeightStation("bag1", "Baggage Compartment", 40.0, 142.8, 200.0, false),
                        WeightStation("fuel", "Usable Fuel (48 gal max)", 228.0, 95.0, 288.0, true)
                    )
                ),
                envelopeJson = envelopeToJson(
                    listOf(
                        CgVertex(82.0, 1500.0),
                        CgVertex(82.0, 2050.0),
                        CgVertex(88.6, 2550.0),
                        CgVertex(93.0, 2550.0),
                        CgVertex(93.0, 1500.0)
                    )
                )
            ),
            AircraftProfileEntity(
                name = "Cirrus SR22 G6",
                tailNumber = "N226SR",
                fuelType = "100LL",
                fuelWeightPerGal = 6.0,
                maxTakeoffWeightLbs = 3600.0,
                maxLandingWeightLbs = 3600.0,
                cruiseTasKt = 183.0,
                fuelBurnGph = 17.5,
                isDefault = true,
                stationsJson = stationsToJson(
                    listOf(
                        WeightStation("empty", "Basic Empty Weight", 2365.0, 141.2, 2700.0, false),
                        WeightStation("front", "Pilot + Front Pax", 350.0, 143.5, 500.0, false),
                        WeightStation("rear", "Rear Passengers", 170.0, 180.0, 500.0, false),
                        WeightStation("bag1", "Baggage Bay", 45.0, 208.0, 130.0, false),
                        WeightStation("fuel", "Wing Fuel (92 gal max)", 390.0, 154.0, 552.0, true)
                    )
                ),
                envelopeJson = envelopeToJson(
                    listOf(
                        CgVertex(137.8, 2100.0),
                        CgVertex(137.8, 2900.0),
                        CgVertex(141.5, 3600.0),
                        CgVertex(148.1, 3600.0),
                        CgVertex(148.1, 2100.0)
                    )
                )
            ),
            AircraftProfileEntity(
                name = "Beechcraft Bonanza A36",
                tailNumber = "N36BA",
                fuelType = "100LL",
                fuelWeightPerGal = 6.0,
                maxTakeoffWeightLbs = 3650.0,
                maxLandingWeightLbs = 3650.0,
                cruiseTasKt = 172.0,
                fuelBurnGph = 15.8,
                isDefault = true,
                stationsJson = stationsToJson(
                    listOf(
                        WeightStation("empty", "Basic Empty Weight", 2520.0, 80.1, 2900.0, false),
                        WeightStation("front", "Row 1 Seats", 340.0, 85.0, 450.0, false),
                        WeightStation("mid", "Row 2 Seats", 165.0, 121.0, 450.0, false),
                        WeightStation("bag1", "Aft Baggage", 40.0, 152.0, 200.0, false),
                        WeightStation("fuel", "Fuel (74 gal max)", 330.0, 75.0, 444.0, true)
                    )
                ),
                envelopeJson = envelopeToJson(
                    listOf(
                        CgVertex(74.0, 2400.0),
                        CgVertex(77.5, 3100.0),
                        CgVertex(81.0, 3650.0),
                        CgVertex(87.7, 3650.0),
                        CgVertex(87.7, 2400.0)
                    )
                )
            )
        )

        fun defaultNavLogs(): List<NavLogLegEntity> = listOf(
            NavLogLegEntity(
                legOrder = 1,
                fromFix = "KPAO",
                toFix = "KSNS",
                altitudeFt = 5500.0,
                trueCourseDeg = 148.0,
                distanceNm = 52.0,
                tasKt = 122.0,
                windDirDeg = 290.0,
                windSpdKt = 18.0,
                magVarDeg = -13.0,
                fuelBurnGph = 9.5
            ),
            NavLogLegEntity(
                legOrder = 2,
                fromFix = "KSNS",
                toFix = "KPRB",
                altitudeFt = 7500.0,
                trueCourseDeg = 142.0,
                distanceNm = 76.0,
                tasKt = 124.0,
                windDirDeg = 300.0,
                windSpdKt = 22.0,
                magVarDeg = -13.0,
                fuelBurnGph = 9.2
            ),
            NavLogLegEntity(
                legOrder = 3,
                fromFix = "KPRB",
                toFix = "KSBP",
                altitudeFt = 5500.0,
                trueCourseDeg = 176.0,
                distanceNm = 27.0,
                tasKt = 120.0,
                windDirDeg = 270.0,
                windSpdKt = 15.0,
                magVarDeg = -12.0,
                fuelBurnGph = 9.5
            )
        )
    }
}
