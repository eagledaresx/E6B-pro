package com.example.domain

enum class WindSolveMode(val label: String) {
    FIND_HEADING_GS("Find HDG & GS"),
    FIND_WIND("Find Wind Vector"),
    FIND_TRACK_GS("Find Track & GS")
}

data class WindTriangleResult(
    val trueCourseDeg: Double,
    val trueHeadingDeg: Double,
    val magneticHeadingDeg: Double,
    val compassHeadingDeg: Double,
    val groundSpeedKt: Double,
    val trueAirspeedKt: Double,
    val windDirectionDeg: Double,
    val windSpeedKt: Double,
    val windCorrectionAngleDeg: Double, // + right, - left
    val headwindComponentKt: Double,    // + headwind, - tailwind
    val crosswindComponentKt: Double,   // + from right, - from left
    val isPossible: Boolean = true,
    val warningMessage: String? = null
)

data class RunwayWindResult(
    val runwayHeadingDeg: Double,
    val windDirectionDeg: Double,
    val windSpeedKt: Double,
    val gustSpeedKt: Double,
    val headwindKt: Double,        // + headwind, - tailwind
    val crosswindKt: Double,       // + from right, - from left
    val maxGustCrosswindKt: Double,
    val relativeAngleDeg: Double
)

data class AtmosphereResult(
    val pressureAltitudeFt: Double,
    val densityAltitudeFt: Double,
    val isaTempC: Double,
    val isaDeviationC: Double,
    val speedOfSoundKt: Double,
    val cloudBaseAglFt: Double,
    val humidityCorrectionFt: Double
)

data class AirspeedResult(
    val indicatedAirspeedKt: Double,
    val calibratedAirspeedKt: Double,
    val equivalentAirspeedKt: Double,
    val trueAirspeedKt: Double,
    val machNumber: Double,
    val speedOfSoundKt: Double,
    val compressibilityFactor: Double
)

enum class HoldingEntryType(val title: String, val description: String) {
    DIRECT("DIRECT ENTRY", "Cross fix and turn immediately outbound on holding side"),
    TEARDROP("TEARDROP ENTRY", "Cross fix, fly 30° inside holding side for 1 min, then turn inbound"),
    PARALLEL("PARALLEL ENTRY", "Cross fix, fly outbound parallel to inbound course for 1 min, turn to intercept")
}

data class HoldingResult(
    val inboundCourseDeg: Double,
    val outboundCourseDeg: Double,
    val aircraftHeadingDeg: Double,
    val isRightTurns: Boolean,
    val entryType: HoldingEntryType,
    val entryHeadingDeg: Double,
    val relativeInboundAngleDeg: Double
)

data class VnavResult(
    val altitudeToLoseFt: Double,
    val todDistanceNm: Double,
    val timeToDescentMin: Double,
    val requiredFpm: Double,
    val descentAngleDeg: Double,
    val gradientPercent: Double,
    val ruleOfThreeNm: Double
)

data class TurnPerformanceResult(
    val bankAngleDeg: Double,
    val rateOfTurnDegPerSec: Double,
    val timeFor360Sec: Double,
    val turnRadiusFt: Double,
    val turnRadiusNm: Double,
    val loadFactorG: Double,
    val stallSpeedIncreaseFactor: Double,
    val standardRateBankDeg: Double
)

data class TrueAltitudeResult(
    val indicatedAltitudeFt: Double,
    val trueAltitudeFt: Double,
    val coldTempErrorFt: Double,
    val isaDeviationC: Double
)

data class EtpPnrResult(
    val etpDistanceNm: Double,
    val etpTimeMin: Double,
    val pnrDistanceNm: Double,
    val pnrTimeMin: Double,
    val outboundGsKt: Double,
    val returnGsKt: Double
)

data class WeightStation(
    val id: String,
    val name: String,
    val weightLbs: Double,
    val armIn: Double,
    val maxWeightLbs: Double = 9999.0,
    val isFuel: Boolean = false
) {
    val momentLbIn: Double get() = weightLbs * armIn
}

data class CgVertex(
    val armIn: Double,
    val weightLbs: Double
)

data class WeightBalanceResult(
    val zeroFuelWeightLbs: Double,
    val zeroFuelCgIn: Double,
    val takeoffWeightLbs: Double,
    val takeoffCgIn: Double,
    val landingWeightLbs: Double,
    val landingCgIn: Double,
    val totalMomentLbIn: Double,
    val isTakeoffWithinEnvelope: Boolean,
    val isZeroFuelWithinEnvelope: Boolean,
    val isLandingWithinEnvelope: Boolean,
    val isWeightWithinMaxTakeoff: Boolean,
    val isWeightWithinMaxLanding: Boolean
)

data class ComputedNavLeg(
    val id: Long,
    val legOrder: Int,
    val fromFix: String,
    val toFix: String,
    val altitudeFt: Double,
    val trueCourseDeg: Double,
    val distanceNm: Double,
    val tasKt: Double,
    val windDirDeg: Double,
    val windSpdKt: Double,
    val magVarDeg: Double,
    val fuelBurnGph: Double,
    val windCorrectionDeg: Double,
    val trueHeadingDeg: Double,
    val magneticHeadingDeg: Double,
    val groundSpeedKt: Double,
    val eteMinutes: Double,
    val legFuelGal: Double
)
