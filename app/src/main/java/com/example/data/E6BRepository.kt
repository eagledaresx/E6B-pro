package com.example.data

import com.example.domain.ComputedNavLeg
import com.example.domain.E6BMath
import com.example.domain.WindSolveMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(
    val aircraftImported: Int,
    val legsImported: Int,
    val notes: String
)

class E6BRepository(private val dao: E6BDao) {

    val aircraftProfiles: Flow<List<AircraftProfileEntity>> = dao.getAllAircraft()

    val computedNavLegs: Flow<List<ComputedNavLeg>> = dao.getAllNavLegs().map { legs ->
        legs.map { leg ->
            val solved = E6BMath.solveWindTriangle(
                mode = WindSolveMode.FIND_HEADING_GS,
                courseOrTrackDeg = leg.trueCourseDeg,
                tasKt = leg.tasKt,
                windDirDeg = leg.windDirDeg,
                windSpdKt = leg.windSpdKt,
                magVarDeg = leg.magVarDeg
            )
            val gs = solved.groundSpeedKt.coerceAtLeast(1.0)
            val eteMin = (leg.distanceNm / gs) * 60.0
            val fuelGal = (eteMin / 60.0) * leg.fuelBurnGph
            ComputedNavLeg(
                id = leg.id,
                legOrder = leg.legOrder,
                fromFix = leg.fromFix,
                toFix = leg.toFix,
                altitudeFt = leg.altitudeFt,
                trueCourseDeg = leg.trueCourseDeg,
                distanceNm = leg.distanceNm,
                tasKt = leg.tasKt,
                windDirDeg = leg.windDirDeg,
                windSpdKt = leg.windSpdKt,
                magVarDeg = leg.magVarDeg,
                fuelBurnGph = leg.fuelBurnGph,
                windCorrectionDeg = solved.windCorrectionAngleDeg,
                trueHeadingDeg = solved.trueHeadingDeg,
                magneticHeadingDeg = solved.magneticHeadingDeg,
                groundSpeedKt = solved.groundSpeedKt,
                eteMinutes = eteMin,
                legFuelGal = fuelGal
            )
        }
    }

    val rawNavLegs: Flow<List<NavLogLegEntity>> = dao.getAllNavLegs()

    val savedCalculations: Flow<List<SavedCalculationEntity>> = dao.getAllSavedCalculations()

    suspend fun ensureSeeded() {
        if (dao.getAircraftCount() == 0) {
            dao.insertAllAircraft(E6BDatabase.defaultAircraftProfiles())
        }
        if (dao.getNavLegCount() == 0) {
            dao.insertAllNavLegs(E6BDatabase.defaultNavLogs())
        }
    }

    suspend fun insertAircraft(profile: AircraftProfileEntity) = dao.insertAircraft(profile)
    suspend fun updateAircraft(profile: AircraftProfileEntity) = dao.updateAircraft(profile)
    suspend fun deleteAircraftById(id: Long) = dao.deleteAircraftById(id)

    suspend fun insertNavLeg(leg: NavLogLegEntity) = dao.insertNavLeg(leg)
    suspend fun updateNavLeg(leg: NavLogLegEntity) = dao.updateNavLeg(leg)
    suspend fun deleteNavLegById(id: Long) = dao.deleteNavLegById(id)
    suspend fun clearNavLogs() = dao.clearNavLogs()

    suspend fun saveCalculation(category: String, title: String, summary: String) {
        dao.insertSavedCalculation(
            SavedCalculationEntity(
                category = category,
                title = title,
                summary = summary
            )
        )
    }

    suspend fun deleteSavedCalculation(id: Long) = dao.deleteSavedCalculationById(id)

    fun buildExportPackageJson(
        aircraft: List<AircraftProfileEntity>,
        legs: List<NavLogLegEntity>
    ): String {
        val root = JSONObject()
        root.put("format", "E6B_PRO_V1")
        root.put("exportedAtMs", System.currentTimeMillis())

        val acArray = JSONArray()
        aircraft.forEach { a ->
            acArray.put(JSONObject().apply {
                put("name", a.name)
                put("tailNumber", a.tailNumber)
                put("fuelType", a.fuelType)
                put("fuelWeightPerGal", a.fuelWeightPerGal)
                put("maxTakeoffWeightLbs", a.maxTakeoffWeightLbs)
                put("maxLandingWeightLbs", a.maxLandingWeightLbs)
                put("cruiseTasKt", a.cruiseTasKt)
                put("fuelBurnGph", a.fuelBurnGph)
                put("stations", JSONArray(a.stationsJson))
                put("envelope", JSONArray(a.envelopeJson))
            })
        }
        root.put("aircraftProfiles", acArray)

        val legArray = JSONArray()
        legs.forEach { l ->
            legArray.put(JSONObject().apply {
                put("legOrder", l.legOrder)
                put("fromFix", l.fromFix)
                put("toFix", l.toFix)
                put("altitudeFt", l.altitudeFt)
                put("trueCourseDeg", l.trueCourseDeg)
                put("distanceNm", l.distanceNm)
                put("tasKt", l.tasKt)
                put("windDirDeg", l.windDirDeg)
                put("windSpdKt", l.windSpdKt)
                put("magVarDeg", l.magVarDeg)
                put("fuelBurnGph", l.fuelBurnGph)
            })
        }
        root.put("navLogs", legArray)
        return root.toString(2)
    }

    /**
     * Imports E6B data from JSON or CSV exported from phone apps (E6B, ForeFlight/Garmin CSV navlogs, or E6B Pro JSON).
     */
    suspend fun importFromPhoneText(rawText: String, replaceExistingNavLog: Boolean = false): ImportSummary {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Import content is empty")
        }

        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            importJson(trimmed, replaceExistingNavLog)
        } else {
            importCsvNavLog(trimmed, replaceExistingNavLog)
        }
    }

    private suspend fun importJson(jsonText: String, replaceNavLog: Boolean): ImportSummary {
        var acCount = 0
        var legCount = 0
        val root = if (jsonText.startsWith("[")) {
            JSONObject().put("navLogs", JSONArray(jsonText))
        } else {
            JSONObject(jsonText)
        }

        val acArray = root.optJSONArray("aircraftProfiles") ?: root.optJSONArray("aircraft")
        if (acArray != null) {
            for (i in 0 until acArray.length()) {
                val obj = acArray.getJSONObject(i)
                val stations = obj.optJSONArray("stations")?.toString()
                    ?: E6BDatabase.defaultAircraftProfiles().first().stationsJson
                val envelope = obj.optJSONArray("envelope")?.toString()
                    ?: E6BDatabase.defaultAircraftProfiles().first().envelopeJson
                dao.insertAircraft(
                    AircraftProfileEntity(
                        name = obj.optString("name", "Imported Aircraft ${i + 1}"),
                        tailNumber = obj.optString("tailNumber", "N-IMP${i + 1}"),
                        fuelType = obj.optString("fuelType", "100LL"),
                        fuelWeightPerGal = obj.optDouble("fuelWeightPerGal", 6.0),
                        maxTakeoffWeightLbs = obj.optDouble("maxTakeoffWeightLbs", 2550.0),
                        maxLandingWeightLbs = obj.optDouble("maxLandingWeightLbs", 2550.0),
                        cruiseTasKt = obj.optDouble("cruiseTasKt", 120.0),
                        fuelBurnGph = obj.optDouble("fuelBurnGph", 9.5),
                        stationsJson = stations,
                        envelopeJson = envelope,
                        isDefault = false
                    )
                )
                acCount++
            }
        }

        val legArray = root.optJSONArray("navLogs") ?: root.optJSONArray("legs")
        if (legArray != null) {
            if (replaceNavLog) dao.clearNavLogs()
            for (i in 0 until legArray.length()) {
                val obj = legArray.getJSONObject(i)
                dao.insertNavLeg(
                    NavLogLegEntity(
                        legOrder = obj.optInt("legOrder", i + 1),
                        fromFix = obj.optString("fromFix", obj.optString("from", "FIX$i")),
                        toFix = obj.optString("toFix", obj.optString("to", "FIX${i + 1}")),
                        altitudeFt = obj.optDouble("altitudeFt", obj.optDouble("alt", 5500.0)),
                        trueCourseDeg = obj.optDouble("trueCourseDeg", obj.optDouble("tc", 360.0)),
                        distanceNm = obj.optDouble("distanceNm", obj.optDouble("dist", 25.0)),
                        tasKt = obj.optDouble("tasKt", obj.optDouble("tas", 120.0)),
                        windDirDeg = obj.optDouble("windDirDeg", obj.optDouble("windDir", 270.0)),
                        windSpdKt = obj.optDouble("windSpdKt", obj.optDouble("windSpd", 10.0)),
                        magVarDeg = obj.optDouble("magVarDeg", obj.optDouble("var", 0.0)),
                        fuelBurnGph = obj.optDouble("fuelBurnGph", obj.optDouble("gph", 9.5))
                    )
                )
                legCount++
            }
        }

        if (acCount == 0 && legCount == 0) {
            throw IllegalArgumentException("No aircraftProfiles or navLogs arrays found in JSON")
        }

        return ImportSummary(
            aircraftImported = acCount,
            legsImported = legCount,
            notes = "Imported $acCount aircraft profile(s) & $legCount NavLog leg(s) via JSON"
        )
    }

    private suspend fun importCsvNavLog(csvText: String, replaceNavLog: Boolean): ImportSummary {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw IllegalArgumentException("Empty CSV data")
        if (replaceNavLog) dao.clearNavLogs()

        var legCount = 0
        lines.forEachIndexed { idx, line ->
            val cols = line.split(",").map { it.trim() }
            // Skip header row if first non-empty column isn't numeric for TC/Dist
            if (idx == 0 && cols.getOrNull(3)?.toDoubleOrNull() == null) {
                return@forEachIndexed
            }
            if (cols.size >= 5) {
                val from = cols.getOrNull(0)?.ifBlank { "WP${idx + 1}" } ?: "WP${idx + 1}"
                val to = cols.getOrNull(1)?.ifBlank { "WP${idx + 2}" } ?: "WP${idx + 2}"
                val alt = cols.getOrNull(2)?.toDoubleOrNull() ?: 5500.0
                val tc = cols.getOrNull(3)?.toDoubleOrNull() ?: 360.0
                val dist = cols.getOrNull(4)?.toDoubleOrNull() ?: 25.0
                val tas = cols.getOrNull(5)?.toDoubleOrNull() ?: 120.0
                val wDir = cols.getOrNull(6)?.toDoubleOrNull() ?: 270.0
                val wSpd = cols.getOrNull(7)?.toDoubleOrNull() ?: 10.0
                val magVar = cols.getOrNull(8)?.toDoubleOrNull() ?: 0.0
                val gph = cols.getOrNull(9)?.toDoubleOrNull() ?: 9.5
                legCount++
                dao.insertNavLeg(
                    NavLogLegEntity(
                        legOrder = legCount,
                        fromFix = from,
                        toFix = to,
                        altitudeFt = alt,
                        trueCourseDeg = tc,
                        distanceNm = dist,
                        tasKt = tas,
                        windDirDeg = wDir,
                        windSpdKt = wSpd,
                        magVarDeg = magVar,
                        fuelBurnGph = gph
                    )
                )
            }
        }
        if (legCount == 0) {
            throw IllegalArgumentException("No valid CSV NavLog rows found (Format: FROM,TO,ALT,TC,DIST,TAS,WDIR,WSPD,VAR,GPH)")
        }
        return ImportSummary(
            aircraftImported = 0,
            legsImported = legCount,
            notes = "Imported $legCount NavLog leg(s) via CSV"
        )
    }
}
