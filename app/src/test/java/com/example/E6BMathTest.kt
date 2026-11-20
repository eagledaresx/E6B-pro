package com.example

import com.example.domain.CgVertex
import com.example.domain.E6BMath
import com.example.domain.HoldingEntryType
import com.example.domain.WeightStation
import com.example.domain.WindSolveMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class E6BMathTest {

    @Test
    fun windTriangle_findHeadingAndGs_computesAccurateWcaAndGs() {
        val res = E6BMath.solveWindTriangle(
            mode = WindSolveMode.FIND_HEADING_GS,
            courseOrTrackDeg = 90.0,
            tasKt = 120.0,
            windDirDeg = 360.0, // Direct left crosswind from North
            windSpdKt = 20.0
        )
        assertTrue(res.isPossible)
        // WCA = asin((20/120)*sin(-90)) = -9.59 deg
        assertEquals(-9.59, res.windCorrectionAngleDeg, 0.1)
        assertEquals(80.41, res.trueHeadingDeg, 0.1)
        // GS = sqrt(120^2 - 20^2) = 118.32 kt
        assertEquals(118.32, res.groundSpeedKt, 0.2)
    }

    @Test
    fun windTriangle_findWind_invertsAirAndGroundVectors() {
        val res = E6BMath.solveWindTriangle(
            mode = WindSolveMode.FIND_WIND,
            courseOrTrackDeg = 90.0,
            tasKt = 120.0,
            windDirDeg = 0.0,
            windSpdKt = 0.0,
            headingDeg = 80.41,
            groundSpeedKt = 118.32
        )
        val angularErr = kotlin.math.abs(E6BMath.normalizeSignedDeg(res.windDirectionDeg - 360.0))
        assertTrue("Expected ~360/0 deg, got ${res.windDirectionDeg}", angularErr < 0.5)
        assertEquals(20.0, res.windSpeedKt, 0.3)
    }

    @Test
    fun runwayWind_computesHeadwindAndCrosswind() {
        val rwy = E6BMath.solveRunwayWind(runwayNumber = 27, windDirDeg = 300.0, windSpeedKt = 20.0, gustSpeedKt = 30.0)
        // 30 deg off RWY 270 -> cos(30)*20 = 17.32 headwind, sin(30)*20 = 10 kt right crosswind
        assertEquals(17.32, rwy.headwindKt, 0.1)
        assertEquals(10.0, rwy.crosswindKt, 0.1)
        assertEquals(15.0, rwy.maxGustCrosswindKt, 0.1)
    }

    @Test
    fun whizWheel_rotationAndRateAreConsistent() {
        val rot = E6BMath.computeRotationForRate(150.0)
        val recovered = E6BMath.computeWhizWheelRateAt60(rot)
        assertEquals(150.0, recovered, 0.1)
    }

    @Test
    fun atmosphereAndAirspeed_isaSeaLevelMatchesStandard() {
        val atmos = E6BMath.solveAtmosphere(fieldElevationFt = 0.0, altimeterInHg = 29.92, oatCelsius = 15.0, dewpointCelsius = 0.0)
        assertEquals(0.0, atmos.pressureAltitudeFt, 1.0)
        assertEquals(15.0, atmos.isaTempC, 0.1)
        assertEquals(0.0, atmos.isaDeviationC, 0.1)

        val tasRes = E6BMath.solveAirspeed(casKt = 120.0, pressureAltitudeFt = 0.0, oatCelsius = 15.0)
        assertEquals(120.0, tasRes.trueAirspeedKt, 0.5)
    }

    @Test
    fun holdingEntry_standardRightTurnsClassifiesSectorsCorrectly() {
        // Inbound 360 -> Outbound 180.
        // Teardrop: 110..180. Parallel: 180..290. Direct: 290..110.
        val teardrop = E6BMath.solveHoldingEntry(360.0, 140.0, isRightTurns = true)
        assertEquals(HoldingEntryType.TEARDROP, teardrop.entryType)

        val parallel = E6BMath.solveHoldingEntry(360.0, 230.0, isRightTurns = true)
        assertEquals(HoldingEntryType.PARALLEL, parallel.entryType)

        val direct = E6BMath.solveHoldingEntry(360.0, 45.0, isRightTurns = true)
        assertEquals(HoldingEntryType.DIRECT, direct.entryType)
    }

    @Test
    fun vnavAndTurn_computesReasonableValues() {
        val vnav = E6BMath.solveVnav(cruiseAltFt = 10000.0, targetAltFt = 2000.0, groundSpeedKt = 120.0, descentAngleDeg = 3.0)
        assertEquals(24.0, vnav.ruleOfThreeNm, 0.1)
        assertTrue(vnav.todDistanceNm in 22.0..28.0)

        val turn = E6BMath.solveTurnPerformance(tasKt = 120.0, bankAngleDeg = 60.0)
        assertEquals(2.0, turn.loadFactorG, 0.05)
    }

    @Test
    fun weightAndBalance_detectsInsideAndOutsideEnvelope() {
        val envelope = listOf(
            CgVertex(35.0, 1500.0),
            CgVertex(35.0, 1950.0),
            CgVertex(41.0, 2550.0),
            CgVertex(47.3, 2550.0),
            CgVertex(47.3, 1500.0)
        )
        val safeStations = listOf(
            WeightStation("empty", "Empty", 1650.0, 39.5, isFuel = false),
            WeightStation("front", "Front", 340.0, 37.0, isFuel = false),
            WeightStation("fuel", "Fuel", 240.0, 48.0, isFuel = true)
        )
        val okRes = E6BMath.solveWeightAndBalance(safeStations, 120.0, envelope, 2550.0, 2550.0)
        assertTrue(okRes.isTakeoffWithinEnvelope)
        assertTrue(okRes.isLandingWithinEnvelope)

        val overweightStations = safeStations + WeightStation("cargo", "Heavy", 500.0, 95.0, isFuel = false)
        val badRes = E6BMath.solveWeightAndBalance(overweightStations, 120.0, envelope, 2550.0, 2550.0)
        assertFalse(badRes.isWeightWithinMaxTakeoff)
        assertFalse(badRes.isTakeoffWithinEnvelope)
    }

    @Test
    fun unitPreferences_convertsBetweenImperialAndMetricAccurately() {
        val metric = com.example.domain.UnitPreferences.fromPreset(com.example.domain.MeasurementSystemPreset.METRIC)
        assertEquals(com.example.domain.MeasurementSystemPreset.METRIC, metric.activePreset)
        assertEquals(185.2, metric.speedUnit.fromKt(100.0), 0.05)
        assertEquals(100.0, metric.speedUnit.toKt(185.2), 0.05)
        assertEquals(304.8, metric.altitudeUnit.fromFt(1000.0), 0.05)
        assertEquals(1013.2, metric.pressureUnit.fromInHg(29.92), 0.2)
        assertEquals(37.85, metric.volumeUnit.fromGal(10.0), 0.05)
        assertEquals(453.59, metric.weightUnit.fromLbs(1000.0), 0.1)
        assertEquals(101.6, metric.weightUnit.armFromIn(40.0), 0.05)
    }
}

