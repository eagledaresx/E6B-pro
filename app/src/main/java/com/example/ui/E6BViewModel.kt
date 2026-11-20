package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AircraftProfileEntity
import com.example.data.E6BDatabase
import com.example.data.E6BRepository
import com.example.data.NavLogLegEntity
import com.example.data.SavedCalculationEntity
import com.example.domain.AirspeedResult
import com.example.domain.AltitudeUnit
import com.example.domain.AtmosphereResult
import com.example.domain.CgVertex
import com.example.domain.ComputedNavLeg
import com.example.domain.DistanceUnit
import com.example.domain.E6BMath
import com.example.domain.EtpPnrResult
import com.example.domain.HoldingResult
import com.example.domain.MeasurementSystemPreset
import com.example.domain.PressureUnit
import com.example.domain.RunwayWindResult
import com.example.domain.SpeedUnit
import com.example.domain.TempUnit
import com.example.domain.TrueAltitudeResult
import com.example.domain.TurnPerformanceResult
import com.example.domain.UnitPreferences
import com.example.domain.VnavResult
import com.example.domain.VolumeUnit
import com.example.domain.WeightBalanceResult
import com.example.domain.WeightStation
import com.example.domain.WeightUnit
import com.example.domain.WindSolveMode
import com.example.domain.WindTriangleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class E6BTab(val label: String) {
    WIND("Wind & E6B"),
    ATMOS("Atmos & IAS"),
    FLIGHT("Flight & Hold"),
    WB("Weight & CG"),
    NAVLOG("NavLog & Import"),
    SETTINGS("Settings & Units")
}

data class E6BUiState(
    val activeTab: E6BTab = E6BTab.WIND,
    val nightRedMode: Boolean = false,
    val units: UnitPreferences = UnitPreferences(),
    val showUnitConverter: Boolean = false,
    val showImportExportDialog: Boolean = false,

    // Tab 1: Wind Triangle + Runway + Whiz Wheel (stored in active user units)
    val windSubTab: Int = 0, // 0 = Wind Triangle, 1 = Whiz Wheel, 2 = Runway Wind
    val windMode: WindSolveMode = WindSolveMode.FIND_HEADING_GS,
    val windCourseText: String = "090",
    val windHeadingText: String = "095",
    val windTasText: String = "125",
    val windGsText: String = "115",
    val windDirText: String = "045",
    val windSpdText: String = "22",
    val windVarText: String = "-12",
    val windDevText: String = "1",
    val runwayNumberText: String = "28",
    val runwayGustText: String = "28",
    val whizWheelRotationDeg: Double = E6BMath.computeRotationForRate(125.0),
    val whizDistanceNmText: String = "75",
    val whizFuelGphText: String = "9.5",

    // Tab 2: Atmosphere & Speed (stored in active user units)
    val fieldElevText: String = "1250",
    val altimeterText: String = "29.92",
    val oatText: String = "24",
    val dewpointText: String = "12",
    val casText: String = "118",

    // Tab 3: Flight & Holding (stored in active user units)
    val flightSubTab: Int = 0, // 0 = Holding, 1 = TOD/VNAV, 2 = Turn & Cold Alt, 3 = ETP/PNR
    val holdInboundText: String = "270",
    val holdHeadingText: String = "315",
    val holdRightTurns: Boolean = true,
    val vnavCruiseAltText: String = "9500",
    val vnavTargetAltText: String = "1500",
    val vnavGsText: String = "135",
    val vnavAngleText: String = "3.0",
    val vnavUseFpm: Boolean = false,
    val vnavFpmText: String = "700",
    val turnTasText: String = "120",
    val turnBankText: String = "20",
    val coldIndicatedAltText: String = "5000",
    val coldStationElevText: String = "1000",
    val coldOatText: String = "-18",
    val etpTotalDistText: String = "420",
    val etpTasText: String = "145",
    val etpCourseText: String = "085",
    val etpWindDirText: String = "260",
    val etpWindSpdText: String = "25",
    val etpSafeEnduranceHrText: String = "4.5",

    // Tab 4: Weight & Balance (fuel burn stored in active volumeUnit)
    val selectedAircraftId: Long = -1L,
    val wbFuelBurnGalText: String = "28",

    // Import / Export & status feedback
    val statusBannerMessage: String? = null
) {
    val altimeterIsHpa: Boolean
        get() = units.pressureUnit == PressureUnit.HPA

    val windResult: WindTriangleResult
        get() = E6BMath.solveWindTriangle(
            mode = windMode,
            courseOrTrackDeg = windCourseText.toDoubleOrNull() ?: 90.0,
            tasKt = units.speedUnit.toKt(windTasText.toDoubleOrNull() ?: units.speedUnit.fromKt(125.0)),
            windDirDeg = windDirText.toDoubleOrNull() ?: 45.0,
            windSpdKt = units.speedUnit.toKt(windSpdText.toDoubleOrNull() ?: 0.0),
            headingDeg = windHeadingText.toDoubleOrNull() ?: 90.0,
            groundSpeedKt = units.speedUnit.toKt(windGsText.toDoubleOrNull() ?: units.speedUnit.fromKt(125.0)),
            magVarDeg = windVarText.toDoubleOrNull() ?: 0.0,
            compassDevDeg = windDevText.toDoubleOrNull() ?: 0.0
        )

    val runwayWindResult: RunwayWindResult
        get() = E6BMath.solveRunwayWind(
            runwayNumber = runwayNumberText.toIntOrNull() ?: 28,
            windDirDeg = windDirText.toDoubleOrNull() ?: 45.0,
            windSpeedKt = units.speedUnit.toKt(windSpdText.toDoubleOrNull() ?: units.speedUnit.fromKt(22.0)),
            gustSpeedKt = units.speedUnit.toKt(runwayGustText.toDoubleOrNull() ?: units.speedUnit.fromKt(22.0))
        )

    val atmosphereResult: AtmosphereResult
        get() {
            val defaultBaro = if (units.pressureUnit == PressureUnit.HPA) 1013.25 else 29.92
            val rawAlt = altimeterText.toDoubleOrNull() ?: defaultBaro
            val inHg = units.pressureUnit.toInHg(rawAlt)
            val elevFt = units.altitudeUnit.toFt(fieldElevText.toDoubleOrNull() ?: 0.0)
            val oatC = units.tempUnit.toCelsius(oatText.toDoubleOrNull() ?: units.tempUnit.fromCelsius(15.0))
            val dewC = units.tempUnit.toCelsius(dewpointText.toDoubleOrNull() ?: units.tempUnit.fromCelsius(5.0))
            return E6BMath.solveAtmosphere(
                fieldElevationFt = elevFt,
                altimeterInHg = inHg,
                oatCelsius = oatC,
                dewpointCelsius = dewC
            )
        }

    val airspeedResult: AirspeedResult
        get() {
            val casKt = units.speedUnit.toKt(casText.toDoubleOrNull() ?: units.speedUnit.fromKt(115.0))
            val oatC = units.tempUnit.toCelsius(oatText.toDoubleOrNull() ?: units.tempUnit.fromCelsius(15.0))
            return E6BMath.solveAirspeed(
                casKt = casKt,
                pressureAltitudeFt = atmosphereResult.pressureAltitudeFt,
                oatCelsius = oatC
            )
        }

    val holdingResult: HoldingResult
        get() = E6BMath.solveHoldingEntry(
            inboundCourseDeg = holdInboundText.toDoubleOrNull() ?: 270.0,
            aircraftHeadingDeg = holdHeadingText.toDoubleOrNull() ?: 315.0,
            isRightTurns = holdRightTurns
        )

    val vnavResult: VnavResult
        get() {
            val cruiseFt = units.altitudeUnit.toFt(vnavCruiseAltText.toDoubleOrNull() ?: units.altitudeUnit.fromFt(9500.0))
            val targetFt = units.altitudeUnit.toFt(vnavTargetAltText.toDoubleOrNull() ?: units.altitudeUnit.fromFt(1500.0))
            val gsKt = units.speedUnit.toKt(vnavGsText.toDoubleOrNull() ?: units.speedUnit.fromKt(135.0))
            val fpmInput = if (vnavUseFpm) {
                vnavFpmText.toDoubleOrNull()?.let { units.altitudeUnit.toFpm(it) }
            } else null
            return E6BMath.solveVnav(
                cruiseAltFt = cruiseFt,
                targetAltFt = targetFt,
                groundSpeedKt = gsKt,
                descentAngleDeg = vnavAngleText.toDoubleOrNull() ?: 3.0,
                verticalSpeedFpmInput = fpmInput
            )
        }

    val turnResult: TurnPerformanceResult
        get() = E6BMath.solveTurnPerformance(
            tasKt = units.speedUnit.toKt(turnTasText.toDoubleOrNull() ?: units.speedUnit.fromKt(120.0)),
            bankAngleDeg = turnBankText.toDoubleOrNull() ?: 20.0
        )

    val trueAltitudeResult: TrueAltitudeResult
        get() = E6BMath.solveTrueAltitude(
            indicatedAltFt = units.altitudeUnit.toFt(coldIndicatedAltText.toDoubleOrNull() ?: units.altitudeUnit.fromFt(5000.0)),
            stationElevationFt = units.altitudeUnit.toFt(coldStationElevText.toDoubleOrNull() ?: units.altitudeUnit.fromFt(1000.0)),
            oatCelsius = units.tempUnit.toCelsius(coldOatText.toDoubleOrNull() ?: units.tempUnit.fromCelsius(-18.0))
        )

    val etpPnrResult: EtpPnrResult
        get() = E6BMath.solveEtpPnr(
            totalDistanceNm = units.distanceUnit.toNm(etpTotalDistText.toDoubleOrNull() ?: units.distanceUnit.fromNm(420.0)),
            tasKt = units.speedUnit.toKt(etpTasText.toDoubleOrNull() ?: units.speedUnit.fromKt(145.0)),
            courseDeg = etpCourseText.toDoubleOrNull() ?: 85.0,
            windDirDeg = etpWindDirText.toDoubleOrNull() ?: 260.0,
            windSpdKt = units.speedUnit.toKt(etpWindSpdText.toDoubleOrNull() ?: units.speedUnit.fromKt(25.0)),
            safeEnduranceHours = etpSafeEnduranceHrText.toDoubleOrNull() ?: 4.5
        )
}

class E6BViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = E6BDatabase.getInstance(application).e6bDao()
    private val repository = E6BRepository(dao)
    private val prefs = application.getSharedPreferences("e6b_pro_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<E6BUiState> = _uiState.asStateFlow()

    val aircraftProfiles: StateFlow<List<AircraftProfileEntity>> =
        repository.aircraftProfiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val computedNavLegs: StateFlow<List<ComputedNavLeg>> =
        repository.computedNavLegs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawNavLegs: StateFlow<List<NavLogLegEntity>> =
        repository.rawNavLegs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCalculations: StateFlow<List<SavedCalculationEntity>> =
        repository.savedCalculations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private fun loadInitialState(): E6BUiState {
        val savedUnits = UnitPreferences(
            speedUnit = runCatching { SpeedUnit.valueOf(prefs.getString("speedUnit", SpeedUnit.KT.name)!!) }.getOrDefault(SpeedUnit.KT),
            distanceUnit = runCatching { DistanceUnit.valueOf(prefs.getString("distanceUnit", DistanceUnit.NM.name)!!) }.getOrDefault(DistanceUnit.NM),
            altitudeUnit = runCatching { AltitudeUnit.valueOf(prefs.getString("altitudeUnit", AltitudeUnit.FT.name)!!) }.getOrDefault(AltitudeUnit.FT),
            pressureUnit = runCatching { PressureUnit.valueOf(prefs.getString("pressureUnit", PressureUnit.IN_HG.name)!!) }.getOrDefault(PressureUnit.IN_HG),
            tempUnit = runCatching { TempUnit.valueOf(prefs.getString("tempUnit", TempUnit.CELSIUS.name)!!) }.getOrDefault(TempUnit.CELSIUS),
            volumeUnit = runCatching { VolumeUnit.valueOf(prefs.getString("volumeUnit", VolumeUnit.US_GAL.name)!!) }.getOrDefault(VolumeUnit.US_GAL),
            weightUnit = runCatching { WeightUnit.valueOf(prefs.getString("weightUnit", WeightUnit.LBS.name)!!) }.getOrDefault(WeightUnit.LBS)
        )
        val nightRed = prefs.getBoolean("nightRedMode", false)
        return convertStateInputTexts(E6BUiState(nightRedMode = nightRed), UnitPreferences(), savedUnits)
    }

    private fun persistUnitsAndTheme(units: UnitPreferences, nightRedMode: Boolean) {
        prefs.edit()
            .putString("speedUnit", units.speedUnit.name)
            .putString("distanceUnit", units.distanceUnit.name)
            .putString("altitudeUnit", units.altitudeUnit.name)
            .putString("pressureUnit", units.pressureUnit.name)
            .putString("tempUnit", units.tempUnit.name)
            .putString("volumeUnit", units.volumeUnit.name)
            .putString("weightUnit", units.weightUnit.name)
            .putBoolean("nightRedMode", nightRedMode)
            .apply()
    }

    private fun convertStateInputTexts(
        state: E6BUiState,
        oldUnits: UnitPreferences,
        newUnits: UnitPreferences
    ): E6BUiState {
        fun convSpeed(text: String, defaultKt: Double): String {
            val kt = oldUnits.speedUnit.toKt(text.toDoubleOrNull() ?: oldUnits.speedUnit.fromKt(defaultKt))
            return newUnits.formatNumberForInput(newUnits.speedUnit.fromKt(kt), 0)
        }
        fun convDist(text: String, defaultNm: Double): String {
            val nm = oldUnits.distanceUnit.toNm(text.toDoubleOrNull() ?: oldUnits.distanceUnit.fromNm(defaultNm))
            return newUnits.formatNumberForInput(newUnits.distanceUnit.fromNm(nm), 0)
        }
        fun convAlt(text: String, defaultFt: Double): String {
            val ft = oldUnits.altitudeUnit.toFt(text.toDoubleOrNull() ?: oldUnits.altitudeUnit.fromFt(defaultFt))
            return newUnits.formatNumberForInput(newUnits.altitudeUnit.fromFt(ft), 0)
        }
        fun convVs(text: String, defaultFpm: Double): String {
            val fpm = oldUnits.altitudeUnit.toFpm(text.toDoubleOrNull() ?: oldUnits.altitudeUnit.fromFpm(defaultFpm))
            val converted = newUnits.altitudeUnit.fromFpm(fpm)
            return if (newUnits.altitudeUnit == AltitudeUnit.M) "%.1f".format(converted) else "%.0f".format(converted)
        }
        fun convBaro(text: String): String {
            val defOld = if (oldUnits.pressureUnit == PressureUnit.HPA) 1013.25 else 29.92
            val inHg = oldUnits.pressureUnit.toInHg(text.toDoubleOrNull() ?: defOld)
            val converted = newUnits.pressureUnit.fromInHg(inHg)
            return if (newUnits.pressureUnit == PressureUnit.HPA) "%.1f".format(converted) else "%.2f".format(converted)
        }
        fun convTemp(text: String, defaultC: Double): String {
            val c = oldUnits.tempUnit.toCelsius(text.toDoubleOrNull() ?: oldUnits.tempUnit.fromCelsius(defaultC))
            return newUnits.formatNumberForInput(newUnits.tempUnit.fromCelsius(c), 0)
        }
        fun convVol(text: String, defaultGal: Double, decimals: Int = 1): String {
            val gal = oldUnits.volumeUnit.toGal(text.toDoubleOrNull() ?: oldUnits.volumeUnit.fromGal(defaultGal))
            return newUnits.formatNumberForInput(newUnits.volumeUnit.fromGal(gal), decimals)
        }

        return state.copy(
            units = newUnits,
            windTasText = convSpeed(state.windTasText, 125.0),
            windGsText = convSpeed(state.windGsText, 115.0),
            windSpdText = convSpeed(state.windSpdText, 22.0),
            runwayGustText = convSpeed(state.runwayGustText, 28.0),
            whizDistanceNmText = convDist(state.whizDistanceNmText, 75.0),
            whizFuelGphText = convVol(state.whizFuelGphText, 9.5, decimals = 1),
            fieldElevText = convAlt(state.fieldElevText, 1250.0),
            altimeterText = convBaro(state.altimeterText),
            oatText = convTemp(state.oatText, 24.0),
            dewpointText = convTemp(state.dewpointText, 12.0),
            casText = convSpeed(state.casText, 118.0),
            vnavCruiseAltText = convAlt(state.vnavCruiseAltText, 9500.0),
            vnavTargetAltText = convAlt(state.vnavTargetAltText, 1500.0),
            vnavGsText = convSpeed(state.vnavGsText, 135.0),
            vnavFpmText = convVs(state.vnavFpmText, 700.0),
            turnTasText = convSpeed(state.turnTasText, 120.0),
            coldIndicatedAltText = convAlt(state.coldIndicatedAltText, 5000.0),
            coldStationElevText = convAlt(state.coldStationElevText, 1000.0),
            coldOatText = convTemp(state.coldOatText, -18.0),
            etpTotalDistText = convDist(state.etpTotalDistText, 420.0),
            etpTasText = convSpeed(state.etpTasText, 145.0),
            etpWindSpdText = convSpeed(state.etpWindSpdText, 25.0),
            wbFuelBurnGalText = convVol(state.wbFuelBurnGalText, 28.0, decimals = 0)
        )
    }

    fun applyMeasurementPreset(preset: MeasurementSystemPreset) {
        val targetUnits = UnitPreferences.fromPreset(preset)
        applyUnitPreferences(targetUnits, announcePreset = preset.title)
    }

    fun applyUnitPreferences(newUnits: UnitPreferences, announcePreset: String? = null) {
        updateState { current ->
            val updated = convertStateInputTexts(current, current.units, newUnits)
            persistUnitsAndTheme(newUnits, updated.nightRedMode)
            val label = announcePreset ?: newUnits.activePreset.title
            updated.copy(statusBannerMessage = "Units updated: $label")
        }
    }

    fun updateState(transform: (E6BUiState) -> E6BUiState) {
        _uiState.update(transform)
    }

    fun selectTab(tab: E6BTab) = updateState { it.copy(activeTab = tab) }

    fun toggleNightRedMode() {
        updateState { current ->
            val nextNight = !current.nightRedMode
            persistUnitsAndTheme(current.units, nextNight)
            current.copy(nightRedMode = nextNight)
        }
    }

    fun setShowUnitConverter(show: Boolean) = updateState { it.copy(showUnitConverter = show) }
    fun setShowImportExport(show: Boolean) = updateState { it.copy(showImportExportDialog = show) }
    fun clearStatusBanner() = updateState { it.copy(statusBannerMessage = null) }

    fun rotateWhizWheel(deltaDeg: Double) {
        updateState {
            val next = ((it.whizWheelRotationDeg + deltaDeg) % 360.0 + 360.0) % 360.0
            it.copy(whizWheelRotationDeg = next)
        }
    }

    fun setWhizWheelRate(rateInActiveSpeedUnit: Double) {
        updateState {
            it.copy(whizWheelRotationDeg = E6BMath.computeRotationForRate(rateInActiveSpeedUnit))
        }
    }

    fun toggleAltimeterUnit() {
        val currentUnits = _uiState.value.units
        val nextPressure = if (currentUnits.pressureUnit == PressureUnit.HPA) PressureUnit.IN_HG else PressureUnit.HPA
        applyUnitPreferences(currentUnits.copy(pressureUnit = nextPressure))
    }

    fun saveSnapshotToLog(category: String, title: String, summary: String) {
        viewModelScope.launch {
            repository.saveCalculation(category, title, summary)
            updateState { it.copy(statusBannerMessage = "Saved '$title' to Cockpit Logbook") }
        }
    }

    fun deleteSavedCalculation(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedCalculation(id)
        }
    }

    fun updateAircraftStationWeight(profile: AircraftProfileEntity, stationId: String, newWeightLbs: Double) {
        viewModelScope.launch {
            val stations = E6BDatabase.jsonToStations(profile.stationsJson).map { st ->
                if (st.id == stationId) st.copy(weightLbs = newWeightLbs.coerceAtLeast(0.0)) else st
            }
            repository.updateAircraft(profile.copy(stationsJson = E6BDatabase.stationsToJson(stations)))
        }
    }

    fun addCustomAircraft(
        name: String,
        tailNumber: String,
        emptyWeightLbs: Double,
        emptyArmIn: Double,
        maxTakeoffWeightLbs: Double,
        cruiseTasKt: Double,
        fuelBurnGph: Double
    ) {
        viewModelScope.launch {
            val stations = listOf(
                WeightStation("empty", "Basic Empty Weight", emptyWeightLbs, emptyArmIn, maxTakeoffWeightLbs, false),
                WeightStation("front", "Front Seats", 340.0, emptyArmIn - 2.0, 500.0, false),
                WeightStation("rear", "Rear Seats", 0.0, emptyArmIn + 34.0, 500.0, false),
                WeightStation("bag1", "Baggage", 30.0, emptyArmIn + 56.0, 120.0, false),
                WeightStation("fuel", "Main Fuel", 240.0, emptyArmIn + 9.0, 360.0, true)
            )
            val minArm = emptyArmIn - 4.5
            val maxArm = emptyArmIn + 8.5
            val envelope = listOf(
                CgVertex(minArm, emptyWeightLbs * 0.9),
                CgVertex(minArm, maxTakeoffWeightLbs * 0.78),
                CgVertex(emptyArmIn + 1.5, maxTakeoffWeightLbs),
                CgVertex(maxArm, maxTakeoffWeightLbs),
                CgVertex(maxArm, emptyWeightLbs * 0.9)
            )
            val newId = repository.insertAircraft(
                AircraftProfileEntity(
                    name = name.ifBlank { "Custom Aircraft" },
                    tailNumber = tailNumber.ifBlank { "N100E6" }.uppercase(),
                    fuelType = "100LL",
                    fuelWeightPerGal = 6.0,
                    maxTakeoffWeightLbs = maxTakeoffWeightLbs,
                    maxLandingWeightLbs = maxTakeoffWeightLbs,
                    cruiseTasKt = cruiseTasKt,
                    fuelBurnGph = fuelBurnGph,
                    stationsJson = E6BDatabase.stationsToJson(stations),
                    envelopeJson = E6BDatabase.envelopeToJson(envelope),
                    isDefault = false
                )
            )
            updateState {
                it.copy(
                    selectedAircraftId = newId,
                    statusBannerMessage = "Added aircraft profile ${tailNumber.uppercase()}"
                )
            }
        }
    }

    fun deleteAircraft(profile: AircraftProfileEntity) {
        viewModelScope.launch {
            repository.deleteAircraftById(profile.id)
            updateState { it.copy(selectedAircraftId = -1L, statusBannerMessage = "Deleted ${profile.tailNumber}") }
        }
    }

    fun addNavLeg(
        fromFix: String,
        toFix: String,
        altitudeFt: Double,
        tcDeg: Double,
        distNm: Double,
        tasKt: Double,
        windDirDeg: Double,
        windSpdKt: Double,
        magVarDeg: Double,
        fuelBurnGph: Double
    ) {
        viewModelScope.launch {
            val nextOrder = (rawNavLegs.value.maxOfOrNull { it.legOrder } ?: 0) + 1
            repository.insertNavLeg(
                NavLogLegEntity(
                    legOrder = nextOrder,
                    fromFix = fromFix.ifBlank { "FIX$nextOrder" }.uppercase(),
                    toFix = toFix.ifBlank { "DEST" }.uppercase(),
                    altitudeFt = altitudeFt,
                    trueCourseDeg = E6BMath.normalizeDeg(tcDeg),
                    distanceNm = distNm.coerceAtLeast(0.1),
                    tasKt = tasKt.coerceAtLeast(10.0),
                    windDirDeg = E6BMath.normalizeDeg(windDirDeg),
                    windSpdKt = windSpdKt.coerceAtLeast(0.0),
                    magVarDeg = magVarDeg,
                    fuelBurnGph = fuelBurnGph.coerceAtLeast(0.5)
                )
            )
            updateState { it.copy(statusBannerMessage = "Added leg ${fromFix.uppercase()} → ${toFix.uppercase()}") }
        }
    }

    fun deleteNavLeg(id: Long) {
        viewModelScope.launch {
            repository.deleteNavLegById(id)
        }
    }

    fun clearNavLogs() {
        viewModelScope.launch {
            repository.clearNavLogs()
            updateState { it.copy(statusBannerMessage = "Cleared all NavLog legs") }
        }
    }

    fun buildExportJson(): String =
        repository.buildExportPackageJson(aircraftProfiles.value, rawNavLegs.value)

    fun importFromPhoneText(rawText: String, replaceExistingNavLog: Boolean) {
        viewModelScope.launch {
            try {
                val summary = repository.importFromPhoneText(rawText, replaceExistingNavLog)
                updateState {
                    it.copy(
                        showImportExportDialog = false,
                        statusBannerMessage = summary.notes
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(statusBannerMessage = "Import failed: ${e.message ?: "Invalid format"}")
                }
            }
        }
    }

    fun computeWeightBalance(profile: AircraftProfileEntity): WeightBalanceResult {
        val stations = E6BDatabase.jsonToStations(profile.stationsJson)
        val envelope = E6BDatabase.jsonToEnvelope(profile.envelopeJson)
        val units = uiState.value.units
        val burnInput = uiState.value.wbFuelBurnGalText.toDoubleOrNull() ?: 0.0
        val burnGal = units.volumeUnit.toGal(burnInput)
        val burnLbs = burnGal * profile.fuelWeightPerGal
        return E6BMath.solveWeightAndBalance(
            stations = stations,
            fuelBurnLbs = burnLbs,
            envelope = envelope,
            maxTakeoffWeightLbs = profile.maxTakeoffWeightLbs,
            maxLandingWeightLbs = profile.maxLandingWeightLbs
        )
    }
}
