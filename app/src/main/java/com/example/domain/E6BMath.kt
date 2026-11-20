package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object E6BMath {

    fun normalizeDeg(angle: Double): Double {
        val mod = ((angle % 360.0) + 360.0) % 360.0
        return if (abs(mod) < 1e-6) 360.0 else mod
    }

    fun normalizeSignedDeg(angle: Double): Double {
        var a = ((angle % 360.0) + 360.0) % 360.0
        if (a > 180.0) a -= 360.0
        return a
    }

    private fun toRad(deg: Double): Double = deg * PI / 180.0
    private fun toDeg(rad: Double): Double = rad * 180.0 / PI

    /**
     * Full E6B Wind Triangle Solver supporting all 3 classic flight computer modes.
     * Note: Magnetic Variation convention: East is negative (-), West is positive (+)
     * ("East is least, West is best": MH = TH - EastVar, MH = TH + WestVar;
     * we pass signed magVarDeg where + = West, - = East, or display clearly in UI).
     */
    fun solveWindTriangle(
        mode: WindSolveMode,
        courseOrTrackDeg: Double,
        tasKt: Double,
        windDirDeg: Double,
        windSpdKt: Double,
        headingDeg: Double = courseOrTrackDeg,
        groundSpeedKt: Double = tasKt,
        magVarDeg: Double = 0.0, // + West, - East
        compassDevDeg: Double = 0.0
    ): WindTriangleResult {
        val safeTas = tasKt.coerceAtLeast(1.0)
        return when (mode) {
            WindSolveMode.FIND_HEADING_GS -> {
                val tcRad = toRad(courseOrTrackDeg)
                val wdRad = toRad(windDirDeg)
                val swc = (windSpdKt / safeTas) * sin(wdRad - tcRad)
                if (abs(swc) > 1.0) {
                    return WindTriangleResult(
                        trueCourseDeg = normalizeDeg(courseOrTrackDeg),
                        trueHeadingDeg = normalizeDeg(courseOrTrackDeg),
                        magneticHeadingDeg = normalizeDeg(courseOrTrackDeg + magVarDeg),
                        compassHeadingDeg = normalizeDeg(courseOrTrackDeg + magVarDeg + compassDevDeg),
                        groundSpeedKt = 0.0,
                        trueAirspeedKt = safeTas,
                        windDirectionDeg = normalizeDeg(windDirDeg),
                        windSpeedKt = windSpdKt,
                        windCorrectionAngleDeg = 0.0,
                        headwindComponentKt = 0.0,
                        crosswindComponentKt = 0.0,
                        isPossible = false,
                        warningMessage = "Wind speed exceeds TAS capability on this course"
                    )
                }
                val wcaRad = asin(swc.coerceIn(-1.0, 1.0))
                val wcaDeg = toDeg(wcaRad)
                val thDeg = normalizeDeg(courseOrTrackDeg + wcaDeg)
                val thRad = toRad(thDeg)
                val gs = (safeTas * cos(wcaRad) - windSpdKt * cos(wdRad - courseOrTrackDeg * PI / 180.0))
                    .coerceAtLeast(0.0)
                // Headwind component along course (+ headwind, - tailwind)
                val headwind = windSpdKt * cos(wdRad - tcRad)
                // Crosswind component (+ from right, - from left)
                val crosswind = windSpdKt * sin(wdRad - tcRad)
                val mhDeg = normalizeDeg(thDeg + magVarDeg)
                val chDeg = normalizeDeg(mhDeg + compassDevDeg)

                WindTriangleResult(
                    trueCourseDeg = normalizeDeg(courseOrTrackDeg),
                    trueHeadingDeg = thDeg,
                    magneticHeadingDeg = mhDeg,
                    compassHeadingDeg = chDeg,
                    groundSpeedKt = gs,
                    trueAirspeedKt = safeTas,
                    windDirectionDeg = normalizeDeg(windDirDeg),
                    windSpeedKt = windSpdKt,
                    windCorrectionAngleDeg = wcaDeg,
                    headwindComponentKt = headwind,
                    crosswindComponentKt = crosswind,
                    isPossible = gs > 0.0,
                    warningMessage = if (gs <= 0.0) "Zero or negative groundspeed!" else null
                )
            }

            WindSolveMode.FIND_WIND -> {
                val tcRad = toRad(courseOrTrackDeg)
                val thRad = toRad(headingDeg)
                val safeGs = groundSpeedKt.coerceAtLeast(0.0)
                // Air vector = TAS along TH; Ground vector = GS along TC
                // Wind-from vector = Air - Ground (since Ground = Air + WindTo => WindTo = Ground - Air => WindFrom = Air - Ground)
                val airN = safeTas * cos(thRad)
                val airE = safeTas * sin(thRad)
                val gndN = safeGs * cos(tcRad)
                val gndE = safeGs * sin(tcRad)
                val wfN = airN - gndN
                val wfE = airE - gndE
                val ws = sqrt(wfN * wfN + wfE * wfE)
                val wd = if (ws < 0.01) 360.0 else normalizeDeg(toDeg(atan2(wfE, wfN)))
                val wca = normalizeSignedDeg(headingDeg - courseOrTrackDeg)
                val headwind = ws * cos(toRad(wd) - tcRad)
                val crosswind = ws * sin(toRad(wd) - tcRad)
                val mh = normalizeDeg(headingDeg + magVarDeg)
                val ch = normalizeDeg(mh + compassDevDeg)

                WindTriangleResult(
                    trueCourseDeg = normalizeDeg(courseOrTrackDeg),
                    trueHeadingDeg = normalizeDeg(headingDeg),
                    magneticHeadingDeg = mh,
                    compassHeadingDeg = ch,
                    groundSpeedKt = safeGs,
                    trueAirspeedKt = safeTas,
                    windDirectionDeg = wd,
                    windSpeedKt = ws,
                    windCorrectionAngleDeg = wca,
                    headwindComponentKt = headwind,
                    crosswindComponentKt = crosswind
                )
            }

            WindSolveMode.FIND_TRACK_GS -> {
                val thRad = toRad(headingDeg)
                val wdRad = toRad(windDirDeg)
                val airN = safeTas * cos(thRad)
                val airE = safeTas * sin(thRad)
                val wtN = -windSpdKt * cos(wdRad)
                val wtE = -windSpdKt * sin(wdRad)
                val gndN = airN + wtN
                val gndE = airE + wtE
                val gs = sqrt(gndN * gndN + gndE * gndE)
                val tc = if (gs < 0.01) normalizeDeg(headingDeg) else normalizeDeg(toDeg(atan2(gndE, gndN)))
                val wca = normalizeSignedDeg(headingDeg - tc)
                val headwind = windSpdKt * cos(wdRad - toRad(tc))
                val crosswind = windSpdKt * sin(wdRad - toRad(tc))
                val mh = normalizeDeg(headingDeg + magVarDeg)
                val ch = normalizeDeg(mh + compassDevDeg)

                WindTriangleResult(
                    trueCourseDeg = tc,
                    trueHeadingDeg = normalizeDeg(headingDeg),
                    magneticHeadingDeg = mh,
                    compassHeadingDeg = ch,
                    groundSpeedKt = gs,
                    trueAirspeedKt = safeTas,
                    windDirectionDeg = normalizeDeg(windDirDeg),
                    windSpeedKt = windSpdKt,
                    windCorrectionAngleDeg = wca,
                    headwindComponentKt = headwind,
                    crosswindComponentKt = crosswind
                )
            }
        }
    }

    fun solveRunwayWind(
        runwayNumber: Int,
        windDirDeg: Double,
        windSpeedKt: Double,
        gustSpeedKt: Double = windSpeedKt
    ): RunwayWindResult {
        val rwyHdg = (runwayNumber.coerceIn(1, 36) * 10.0)
        val relAngle = normalizeSignedDeg(windDirDeg - rwyHdg)
        val relRad = toRad(relAngle)
        val headwind = windSpeedKt * cos(relRad)
        val crosswind = windSpeedKt * sin(relRad)
        val maxGust = maxOf(windSpeedKt, gustSpeedKt) * abs(sin(relRad))
        return RunwayWindResult(
            runwayHeadingDeg = rwyHdg,
            windDirectionDeg = normalizeDeg(windDirDeg),
            windSpeedKt = windSpeedKt,
            gustSpeedKt = maxOf(windSpeedKt, gustSpeedKt),
            headwindKt = headwind,
            crosswindKt = crosswind,
            maxGustCrosswindKt = maxGust,
            relativeAngleDeg = relAngle
        )
    }

    /**
     * Circular E6B Whiz Wheel ratio math:
     * Outer scale value at angle theta (0..360 deg represents 10..100 logarithmically: value = 10 * 10^(theta/360))
     * If inner dial is rotated by `innerRotationDeg` clockwise relative to outer dial:
     * Inner value `x` aligns with Outer value `y = x * 10^(innerRotationDeg / 360)`.
     * At the Rate Index (60 on inner dial), outer value = Hourly Rate (Speed KT or Fuel GPH).
     */
    fun whizWheelRatio(innerRotationDeg: Double): Double {
        val norm = ((innerRotationDeg % 360.0) + 360.0) % 360.0
        val rawRatio = 10.0.pow(norm / 360.0)
        // Rate index is at 60 on the 10..100 scale, so scale factor relative to 60:
        return rawRatio * (60.0 / 10.0) // outer value opposite 60 index = 60 * ratio / 10? Wait:
        // Let's check exact E6B log wheel geometry:
        // At 0° rotation, 10 on inner aligns with 10 on outer (ratio = 1:1). Then 60 on inner aligns with 60 on outer!
        // Thus outer_at_60 = 60 * 10^(innerRotationDeg / 360).
        // Let's represent rate = 60 * 10^(norm / 360) normalized into [10..600] or [60..600]:
        // Actually `ratePerHour = 60.0 * 10.0.pow(norm / 360.0)` gives 60..600 KT directly!
    }

    fun computeWhizWheelRateAt60(innerRotationDeg: Double): Double {
        val norm = ((innerRotationDeg % 360.0) + 360.0) % 360.0
        return 60.0 * 10.0.pow(norm / 360.0)
    }

    fun computeRotationForRate(ratePerHour: Double): Double {
        if (ratePerHour <= 0.0) return 0.0
        // Bring rate into [60, 600) decade for the primary index
        var r = ratePerHour
        while (r < 60.0) r *= 10.0
        while (r >= 600.0) r /= 10.0
        return 360.0 * kotlin.math.log10(r / 60.0)
    }

    fun solveAtmosphere(
        fieldElevationFt: Double,
        altimeterInHg: Double,
        oatCelsius: Double,
        dewpointCelsius: Double = oatCelsius - 10.0
    ): AtmosphereResult {
        val pa = fieldElevationFt + (29.92 - altimeterInHg) * 1000.0
        val isaTemp = 15.0 - 1.98 * (pa / 1000.0)
        val isaDev = oatCelsius - isaTemp
        // Standard FAA / Jeppesen DA formula + dry/moist refinement
        val tempK = oatCelsius + 273.15
        val dewK = dewpointCelsius.coerceAtMost(oatCelsius) + 273.15
        // Magnus saturation vapor pressure (hPa)
        val e = 6.1078 * 10.0.pow((7.5 * (dewK - 273.15)) / ((dewK - 273.15) + 237.3))
        val stationPressureHpa = 1013.25 * (1.0 - 6.8755856e-6 * pa).coerceAtLeast(0.05).pow(5.2558797)
        val virtualTempK = tempK / (1.0 - (e / stationPressureHpa.coerceAtLeast(1.0)) * (1.0 - 0.622))
        val virtualTempC = virtualTempK - 273.15

        val daDry = pa + 118.8 * (oatCelsius - isaTemp)
        val daMoist = pa + 118.8 * (virtualTempC - isaTemp)
        val humidityBoost = (daMoist - daDry).coerceAtLeast(0.0)

        val speedOfSound = 38.967854 * sqrt(tempK.coerceAtLeast(50.0))
        val cloudBase = ((oatCelsius - dewpointCelsius).coerceAtLeast(0.0) / 2.5) * 1000.0

        return AtmosphereResult(
            pressureAltitudeFt = pa,
            densityAltitudeFt = daMoist,
            isaTempC = isaTemp,
            isaDeviationC = isaDev,
            speedOfSoundKt = speedOfSound,
            cloudBaseAglFt = cloudBase,
            humidityCorrectionFt = humidityBoost
        )
    }

    fun solveAirspeed(
        casKt: Double,
        pressureAltitudeFt: Double,
        oatCelsius: Double
    ): AirspeedResult {
        val tempK = (oatCelsius + 273.15).coerceAtLeast(50.0)
        val isaTempK = (288.15 - 0.0019812 * pressureAltitudeFt).coerceAtLeast(50.0)
        // Pressure ratio delta
        val delta = (1.0 - 6.8755856e-6 * pressureAltitudeFt).coerceAtLeast(0.05).pow(5.2558797)
        // Density ratio sigma = delta / (tempK / 288.15)
        val sigma = delta / (tempK / 288.15)

        // Subsonic compressible impact pressure qc/P0 from CAS (a0 = 661.4788 kt)
        val a0 = 661.4788
        val qcOverP0 = (1.0 + 0.2 * (casKt / a0).pow(2.0)).pow(3.5) - 1.0
        val qcOverP = qcOverP0 / delta
        val mach = sqrt(5.0 * ((qcOverP + 1.0).pow(2.0 / 7.0) - 1.0)).coerceAtLeast(0.0)
        val speedOfSound = 38.967854 * sqrt(tempK)
        val tas = mach * speedOfSound
        val eas = tas * sqrt(sigma)
        val compFactor = if (casKt > 1.0) eas / casKt else 1.0

        return AirspeedResult(
            indicatedAirspeedKt = casKt,
            calibratedAirspeedKt = casKt,
            equivalentAirspeedKt = eas,
            trueAirspeedKt = tas,
            machNumber = mach,
            speedOfSoundKt = speedOfSound,
            compressibilityFactor = compFactor
        )
    }

    fun solveHoldingEntry(
        inboundCourseDeg: Double,
        aircraftHeadingDeg: Double,
        isRightTurns: Boolean
    ): HoldingResult {
        val inbound = normalizeDeg(inboundCourseDeg)
        val outbound = normalizeDeg(inbound + 180.0)
        val hdg = normalizeDeg(aircraftHeadingDeg)
        // Relative angle from outbound course to aircraft heading (or inbound)
        // Standard AIM 5-3-8 holding entry sectors relative to inbound course:
        // Compare heading relative to outbound course:
        // If right turns:
        //   Teardrop: heading within 0° to 70° LEFT of outbound (i.e. outbound - 70° to outbound)
        //   Parallel: heading within 0° to 110° RIGHT of outbound (outbound to outbound + 110°)
        //   Direct: remaining 180°
        val diffFromOutbound = normalizeSignedDeg(hdg - outbound) // -180..+180
        val entry = if (isRightTurns) {
            when {
                diffFromOutbound in -70.0..0.0 -> HoldingEntryType.TEARDROP
                diffFromOutbound in 0.0..110.0 -> HoldingEntryType.PARALLEL
                else -> HoldingEntryType.DIRECT
            }
        } else {
            // Left turns mirror:
            // Teardrop: 0° to 70° RIGHT of outbound
            // Parallel: 0° to 110° LEFT of outbound
            when {
                diffFromOutbound in 0.0..70.0 -> HoldingEntryType.TEARDROP
                diffFromOutbound in -110.0..0.0 -> HoldingEntryType.PARALLEL
                else -> HoldingEntryType.DIRECT
            }
        }

        val entryHeading = when (entry) {
            HoldingEntryType.DIRECT -> outbound
            HoldingEntryType.PARALLEL -> outbound
            HoldingEntryType.TEARDROP -> if (isRightTurns) normalizeDeg(outbound - 30.0) else normalizeDeg(outbound + 30.0)
        }

        return HoldingResult(
            inboundCourseDeg = inbound,
            outboundCourseDeg = outbound,
            aircraftHeadingDeg = hdg,
            isRightTurns = isRightTurns,
            entryType = entry,
            entryHeadingDeg = entryHeading,
            relativeInboundAngleDeg = normalizeSignedDeg(hdg - inbound)
        )
    }

    fun solveVnav(
        cruiseAltFt: Double,
        targetAltFt: Double,
        groundSpeedKt: Double,
        descentAngleDeg: Double = 3.0,
        verticalSpeedFpmInput: Double? = null
    ): VnavResult {
        val altLose = (cruiseAltFt - targetAltFt).coerceAtLeast(0.0)
        val gs = groundSpeedKt.coerceAtLeast(1.0)
        val gsNmPerMin = gs / 60.0
        val gsFpm = gs * (6076.12 / 60.0)

        val angleRad: Double
        val fpm: Double
        if (verticalSpeedFpmInput != null && verticalSpeedFpmInput > 0.0) {
            fpm = verticalSpeedFpmInput
            angleRad = atan(fpm / gsFpm)
        } else {
            angleRad = toRad(descentAngleDeg.coerceIn(0.5, 15.0))
            fpm = gsFpm * kotlin.math.tan(angleRad)
        }

        val timeMin = if (fpm > 1.0) altLose / fpm else 0.0
        val todNm = timeMin * gsNmPerMin
        val gradPct = kotlin.math.tan(angleRad) * 100.0
        val ruleOfThree = (altLose / 1000.0) * 3.0

        return VnavResult(
            altitudeToLoseFt = altLose,
            todDistanceNm = todNm,
            timeToDescentMin = timeMin,
            requiredFpm = fpm,
            descentAngleDeg = toDeg(angleRad),
            gradientPercent = gradPct,
            ruleOfThreeNm = ruleOfThree
        )
    }

    fun solveTurnPerformance(
        tasKt: Double,
        bankAngleDeg: Double
    ): TurnPerformanceResult {
        val safeTas = tasKt.coerceAtLeast(1.0)
        val safeBank = bankAngleDeg.coerceIn(1.0, 75.0)
        val g = 32.174 // ft/s^2
        val tasFps = safeTas * 1.68781
        val bankRad = toRad(safeBank)
        val tanBank = kotlin.math.tan(bankRad)
        // Rate of turn omega (rad/s) = g * tan(phi) / V
        val omegaRadPerSec = (g * tanBank) / tasFps
        val rateDegPerSec = toDeg(omegaRadPerSec)
        val radiusFt = (tasFps * tasFps) / (g * tanBank)
        val radiusNm = radiusFt / 6076.12
        val loadFactor = 1.0 / cos(bankRad)
        val stallFactor = sqrt(loadFactor)
        // Standard rate turn = 3 deg/sec = 0.0523599 rad/s
        val stdBankRad = atan((tasFps * toRad(3.0)) / g)

        return TurnPerformanceResult(
            bankAngleDeg = safeBank,
            rateOfTurnDegPerSec = rateDegPerSec,
            timeFor360Sec = if (rateDegPerSec > 0.01) 360.0 / rateDegPerSec else 0.0,
            turnRadiusFt = radiusFt,
            turnRadiusNm = radiusNm,
            loadFactorG = loadFactor,
            stallSpeedIncreaseFactor = stallFactor,
            standardRateBankDeg = toDeg(stdBankRad)
        )
    }

    fun solveTrueAltitude(
        indicatedAltFt: Double,
        stationElevationFt: Double,
        oatCelsius: Double
    ): TrueAltitudeResult {
        val heightAboveStation = (indicatedAltFt - stationElevationFt).coerceAtLeast(0.0)
        val isaAtIndicated = 15.0 - 1.98 * (indicatedAltFt / 1000.0)
        val isaDev = oatCelsius - isaAtIndicated
        // ICAO Cold Temperature Correction approx: 4 ft per 1000 ft per °C ISA deviation
        val error = 0.004 * isaDev * heightAboveStation
        return TrueAltitudeResult(
            indicatedAltitudeFt = indicatedAltFt,
            trueAltitudeFt = indicatedAltFt + error,
            coldTempErrorFt = error,
            isaDeviationC = isaDev
        )
    }

    fun solveEtpPnr(
        totalDistanceNm: Double,
        tasKt: Double,
        courseDeg: Double,
        windDirDeg: Double,
        windSpdKt: Double,
        safeEnduranceHours: Double
    ): EtpPnrResult {
        val out = solveWindTriangle(WindSolveMode.FIND_HEADING_GS, courseDeg, tasKt, windDirDeg, windSpdKt)
        val ret = solveWindTriangle(WindSolveMode.FIND_HEADING_GS, normalizeDeg(courseDeg + 180.0), tasKt, windDirDeg, windSpdKt)
        val gsOut = out.groundSpeedKt.coerceAtLeast(1.0)
        val gsRet = ret.groundSpeedKt.coerceAtLeast(1.0)
        val denom = gsOut + gsRet
        val etpDist = totalDistanceNm * (gsRet / denom)
        val etpTimeMin = (etpDist / gsOut) * 60.0
        val pnrHours = safeEnduranceHours * (gsOut * gsRet) / denom
        val pnrDist = pnrHours * gsOut
        val pnrTimeMin = pnrHours * 60.0

        return EtpPnrResult(
            etpDistanceNm = etpDist,
            etpTimeMin = etpTimeMin,
            pnrDistanceNm = pnrDist,
            pnrTimeMin = pnrTimeMin,
            outboundGsKt = gsOut,
            returnGsKt = gsRet
        )
    }

    fun solveWeightAndBalance(
        stations: List<WeightStation>,
        fuelBurnLbs: Double,
        envelope: List<CgVertex>,
        maxTakeoffWeightLbs: Double,
        maxLandingWeightLbs: Double
    ): WeightBalanceResult {
        val zfw = stations.filter { !it.isFuel }.sumOf { it.weightLbs }
        val zfMoment = stations.filter { !it.isFuel }.sumOf { it.momentLbIn }
        val zfCg = if (zfw > 0.0) zfMoment / zfw else 0.0

        val tow = stations.sumOf { it.weightLbs }
        val toMoment = stations.sumOf { it.momentLbIn }
        val toCg = if (tow > 0.0) toMoment / tow else 0.0

        val fuelStations = stations.filter { it.isFuel }
        val totalFuelWeight = fuelStations.sumOf { it.weightLbs }
        val avgFuelArm = if (totalFuelWeight > 0.0) {
            fuelStations.sumOf { it.momentLbIn } / totalFuelWeight
        } else 0.0
        val actualBurn = fuelBurnLbs.coerceIn(0.0, totalFuelWeight)
        val lw = (tow - actualBurn).coerceAtLeast(zfw)
        val lMoment = toMoment - (actualBurn * avgFuelArm)
        val lCg = if (lw > 0.0) lMoment / lw else 0.0

        return WeightBalanceResult(
            zeroFuelWeightLbs = zfw,
            zeroFuelCgIn = zfCg,
            takeoffWeightLbs = tow,
            takeoffCgIn = toCg,
            landingWeightLbs = lw,
            landingCgIn = lCg,
            totalMomentLbIn = toMoment,
            isTakeoffWithinEnvelope = isPointInPolygon(toCg, tow, envelope) && tow <= maxTakeoffWeightLbs,
            isZeroFuelWithinEnvelope = isPointInPolygon(zfCg, zfw, envelope),
            isLandingWithinEnvelope = isPointInPolygon(lCg, lw, envelope) && lw <= maxLandingWeightLbs,
            isWeightWithinMaxTakeoff = tow <= maxTakeoffWeightLbs,
            isWeightWithinMaxLanding = lw <= maxLandingWeightLbs
        )
    }

    fun isPointInPolygon(x: Double, y: Double, polygon: List<CgVertex>): Boolean {
        if (polygon.size < 3) return true
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].armIn
            val yi = polygon[i].weightLbs
            val xj = polygon[j].armIn
            val yj = polygon[j].weightLbs
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (abs(it) < 1e-9) 1e-9 else it }) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun formatDurationMin(totalMinutes: Double): String {
        val totalSec = (totalMinutes.coerceAtLeast(0.0) * 60.0).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun formatHeading(deg: Double): String {
        val rounded = kotlin.math.round(normalizeDeg(deg)).toInt()
        val disp = if (rounded == 0) 360 else rounded
        return "%03d°".format(disp)
    }
}
