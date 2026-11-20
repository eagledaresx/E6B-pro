package com.example.domain

import kotlin.math.roundToInt

enum class MeasurementSystemPreset(val title: String, val subtitle: String) {
    IMPERIAL("Imperial (Aviation)", "KT • NM • FT • inHg • °C • GAL • LBS"),
    METRIC("Metric (SI)", "KM/H • KM • M • hPa • °C • L • KG"),
    ICAO_HYBRID("ICAO Hybrid", "KT • NM • FT • hPa • °C • L • KG"),
    US_STATUTE("US Statute", "MPH • SM • FT • inHg • °F • GAL • LBS"),
    CUSTOM("Custom Units", "User-configured mix of measurement units")
}

enum class SpeedUnit(val symbol: String, val factorFromKt: Double) {
    KT("KT", 1.0),
    KMH("KM/H", 1.852),
    MPH("MPH", 1.15078);

    fun fromKt(kt: Double): Double = kt * factorFromKt
    fun toKt(value: Double): Double = value / factorFromKt
}

enum class DistanceUnit(val symbol: String, val factorFromNm: Double) {
    NM("NM", 1.0),
    KM("KM", 1.852),
    SM("SM", 1.15078);

    fun fromNm(nm: Double): Double = nm * factorFromNm
    fun toNm(value: Double): Double = value / factorFromNm
}

enum class AltitudeUnit(
    val symbol: String,
    val vsSymbol: String,
    val factorFromFt: Double,
    val vsFactorFromFpm: Double
) {
    FT("FT", "FPM", 1.0, 1.0),
    M("M", "M/S", 0.3048, 0.00508);

    fun fromFt(ft: Double): Double = ft * factorFromFt
    fun toFt(value: Double): Double = value / factorFromFt
    fun fromFpm(fpm: Double): Double = fpm * vsFactorFromFpm
    fun toFpm(value: Double): Double = value / vsFactorFromFpm
}

enum class PressureUnit(val symbol: String) {
    IN_HG("inHg"),
    HPA("hPa");

    fun fromInHg(inHg: Double): Double = if (this == HPA) inHg * 33.86389 else inHg
    fun toInHg(value: Double): Double = if (this == HPA) value / 33.86389 else value
}

enum class TempUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun fromCelsius(c: Double): Double = if (this == FAHRENHEIT) c * 1.8 + 32.0 else c
    fun toCelsius(value: Double): Double = if (this == FAHRENHEIT) (value - 32.0) / 1.8 else value
    fun deltaFromCelsius(deltaC: Double): Double = if (this == FAHRENHEIT) deltaC * 1.8 else deltaC
}

enum class VolumeUnit(val symbol: String, val rateSymbol: String, val factorFromGal: Double) {
    US_GAL("GAL", "GPH", 1.0),
    LITERS("L", "LPH", 3.78541);

    fun fromGal(gal: Double): Double = gal * factorFromGal
    fun toGal(value: Double): Double = value / factorFromGal
}

enum class WeightUnit(
    val symbol: String,
    val armSymbol: String,
    val factorFromLbs: Double,
    val armFactorFromIn: Double
) {
    LBS("LBS", "in", 1.0, 1.0),
    KG("KG", "cm", 0.45359237, 2.54);

    fun fromLbs(lbs: Double): Double = lbs * factorFromLbs
    fun toLbs(value: Double): Double = value / factorFromLbs
    fun armFromIn(inches: Double): Double = inches * armFactorFromIn
    fun armToIn(value: Double): Double = value / armFactorFromIn
}

data class UnitPreferences(
    val speedUnit: SpeedUnit = SpeedUnit.KT,
    val distanceUnit: DistanceUnit = DistanceUnit.NM,
    val altitudeUnit: AltitudeUnit = AltitudeUnit.FT,
    val pressureUnit: PressureUnit = PressureUnit.IN_HG,
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val volumeUnit: VolumeUnit = VolumeUnit.US_GAL,
    val weightUnit: WeightUnit = WeightUnit.LBS
) {
    val activePreset: MeasurementSystemPreset
        get() = when {
            speedUnit == SpeedUnit.KT &&
                distanceUnit == DistanceUnit.NM &&
                altitudeUnit == AltitudeUnit.FT &&
                pressureUnit == PressureUnit.IN_HG &&
                tempUnit == TempUnit.CELSIUS &&
                volumeUnit == VolumeUnit.US_GAL &&
                weightUnit == WeightUnit.LBS -> MeasurementSystemPreset.IMPERIAL

            speedUnit == SpeedUnit.KMH &&
                distanceUnit == DistanceUnit.KM &&
                altitudeUnit == AltitudeUnit.M &&
                pressureUnit == PressureUnit.HPA &&
                tempUnit == TempUnit.CELSIUS &&
                volumeUnit == VolumeUnit.LITERS &&
                weightUnit == WeightUnit.KG -> MeasurementSystemPreset.METRIC

            speedUnit == SpeedUnit.KT &&
                distanceUnit == DistanceUnit.NM &&
                altitudeUnit == AltitudeUnit.FT &&
                pressureUnit == PressureUnit.HPA &&
                tempUnit == TempUnit.CELSIUS &&
                volumeUnit == VolumeUnit.LITERS &&
                weightUnit == WeightUnit.KG -> MeasurementSystemPreset.ICAO_HYBRID

            speedUnit == SpeedUnit.MPH &&
                distanceUnit == DistanceUnit.SM &&
                altitudeUnit == AltitudeUnit.FT &&
                pressureUnit == PressureUnit.IN_HG &&
                tempUnit == TempUnit.FAHRENHEIT &&
                volumeUnit == VolumeUnit.US_GAL &&
                weightUnit == WeightUnit.LBS -> MeasurementSystemPreset.US_STATUTE

            else -> MeasurementSystemPreset.CUSTOM
        }

    val isMetricPrimary: Boolean
        get() = activePreset == MeasurementSystemPreset.METRIC

    fun formatSpeed(kt: Double, decimals: Int = 0): String {
        val v = speedUnit.fromKt(kt)
        return "%.${decimals}f %s".format(v, speedUnit.symbol)
    }

    fun formatDistance(nm: Double, decimals: Int = 1): String {
        val v = distanceUnit.fromNm(nm)
        return "%.${decimals}f %s".format(v, distanceUnit.symbol)
    }

    fun formatAltitude(ft: Double, decimals: Int = 0): String {
        val v = altitudeUnit.fromFt(ft)
        return "%.${decimals}f %s".format(v, altitudeUnit.symbol)
    }

    fun formatSignedAltitude(ft: Double): String {
        val v = altitudeUnit.fromFt(ft)
        return "%+.0f %s".format(v, altitudeUnit.symbol)
    }

    fun formatVerticalSpeed(fpm: Double): String {
        val v = altitudeUnit.fromFpm(fpm)
        return if (altitudeUnit == AltitudeUnit.M) {
            "%.2f %s".format(v, altitudeUnit.vsSymbol)
        } else {
            "%.0f %s".format(v, altitudeUnit.vsSymbol)
        }
    }

    fun formatTemp(celsius: Double, decimals: Int = 1): String {
        val v = tempUnit.fromCelsius(celsius)
        return "%.${decimals}f %s".format(v, tempUnit.symbol)
    }

    fun formatTempDelta(deltaC: Double, decimals: Int = 1): String {
        val v = tempUnit.deltaFromCelsius(deltaC)
        return "%+.${decimals}f %s".format(v, tempUnit.symbol)
    }

    fun formatVolume(gal: Double, decimals: Int = 1): String {
        val v = volumeUnit.fromGal(gal)
        return "%.${decimals}f %s".format(v, volumeUnit.symbol)
    }

    fun formatWeight(lbs: Double, decimals: Int = 0): String {
        val v = weightUnit.fromLbs(lbs)
        return "%.${decimals}f %s".format(v, weightUnit.symbol)
    }

    fun formatArm(inches: Double, decimals: Int = 1): String {
        val v = weightUnit.armFromIn(inches)
        return "%.${decimals}f %s".format(v, weightUnit.armSymbol)
    }

    fun formatNumberForInput(value: Double, decimals: Int = 0): String {
        return if (decimals <= 0) {
            value.roundToInt().toString()
        } else {
            "%.${decimals}f".format(value)
        }
    }

    companion object {
        fun fromPreset(preset: MeasurementSystemPreset): UnitPreferences = when (preset) {
            MeasurementSystemPreset.IMPERIAL -> UnitPreferences(
                speedUnit = SpeedUnit.KT,
                distanceUnit = DistanceUnit.NM,
                altitudeUnit = AltitudeUnit.FT,
                pressureUnit = PressureUnit.IN_HG,
                tempUnit = TempUnit.CELSIUS,
                volumeUnit = VolumeUnit.US_GAL,
                weightUnit = WeightUnit.LBS
            )
            MeasurementSystemPreset.METRIC -> UnitPreferences(
                speedUnit = SpeedUnit.KMH,
                distanceUnit = DistanceUnit.KM,
                altitudeUnit = AltitudeUnit.M,
                pressureUnit = PressureUnit.HPA,
                tempUnit = TempUnit.CELSIUS,
                volumeUnit = VolumeUnit.LITERS,
                weightUnit = WeightUnit.KG
            )
            MeasurementSystemPreset.ICAO_HYBRID -> UnitPreferences(
                speedUnit = SpeedUnit.KT,
                distanceUnit = DistanceUnit.NM,
                altitudeUnit = AltitudeUnit.FT,
                pressureUnit = PressureUnit.HPA,
                tempUnit = TempUnit.CELSIUS,
                volumeUnit = VolumeUnit.LITERS,
                weightUnit = WeightUnit.KG
            )
            MeasurementSystemPreset.US_STATUTE -> UnitPreferences(
                speedUnit = SpeedUnit.MPH,
                distanceUnit = DistanceUnit.SM,
                altitudeUnit = AltitudeUnit.FT,
                pressureUnit = PressureUnit.IN_HG,
                tempUnit = TempUnit.FAHRENHEIT,
                volumeUnit = VolumeUnit.US_GAL,
                weightUnit = WeightUnit.LBS
            )
            MeasurementSystemPreset.CUSTOM -> UnitPreferences()
        }
    }
}
