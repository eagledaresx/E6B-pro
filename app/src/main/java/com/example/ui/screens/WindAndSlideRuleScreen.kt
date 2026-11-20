package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.E6BMath
import com.example.domain.WindSolveMode
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.AvionicsStepperInput
import com.example.ui.components.CockpitCard
import com.example.ui.components.RunwayWindCanvas
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.components.WhizWheelCanvas
import com.example.ui.components.WindTriangleCanvas
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.abs

@Composable
fun WindAndSlideRuleScreen(
    state: E6BUiState,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val subTabs = listOf("Wind Triangle", "Whiz Wheel", "Runway Wind")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                subTabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = state.windSubTab == idx,
                        onClick = { viewModel.updateState { it.copy(windSubTab = idx) } },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = subTabs.size),
                        modifier = Modifier.testTag("wind_subtab_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        when (state.windSubTab) {
            0 -> item { WindTriangleSection(state, viewModel) }
            1 -> item { WhizWheelSection(state, viewModel) }
            else -> item { RunwayWindSection(state, viewModel) }
        }
    }
}

@Composable
private fun WindTriangleSection(
    state: E6BUiState,
    viewModel: E6BViewModel
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val res = state.windResult

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Solve mode chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WindSolveMode.entries) { mode ->
                FilterChip(
                    selected = state.windMode == mode,
                    onClick = { viewModel.updateState { it.copy(windMode = mode) } },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("wind_mode_${mode.name}")
                )
            }
        }

        // Vector Graph Card
        CockpitCard {
            SectionHeader(
                title = "Live Wind Triangle",
                icon = Icons.Default.Explore,
                subtitle = if (res.isPossible) "North-Up Vector Solution (${units.speedUnit.symbol})" else res.warningMessage,
                trailing = {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveSnapshotToLog(
                                category = "WIND TRIANGLE",
                                title = "TC ${E6BMath.formatHeading(res.trueCourseDeg)} @ ${units.formatSpeed(res.trueAirspeedKt, 0)}",
                                summary = "TH ${E6BMath.formatHeading(res.trueHeadingDeg)} | MH ${E6BMath.formatHeading(res.magneticHeadingDeg)} | GS ${units.formatSpeed(res.groundSpeedKt, 0)} | WCA ${"%+.1f".format(res.windCorrectionAngleDeg)}°"
                            )
                        },
                        modifier = Modifier.testTag("save_wind_btn")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Wind Calculation")
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            WindTriangleCanvas(result = res)
            Spacer(Modifier.height(10.dp))

            // Primary Telemetry Output Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "True Hdg",
                    value = E6BMath.formatHeading(res.trueHeadingDeg),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Mag Hdg",
                    value = E6BMath.formatHeading(res.magneticHeadingDeg),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Ground Spd",
                    value = units.formatSpeed(res.groundSpeedKt, 0),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wcaSign = if (res.windCorrectionAngleDeg >= 0) "R" else "L"
                TelemetryBadge(
                    label = "WCA",
                    value = "${"%.1f".format(abs(res.windCorrectionAngleDeg))}° $wcaSign",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val hwLabel = if (res.headwindComponentKt >= 0) "Headwind" else "Tailwind"
                TelemetryBadge(
                    label = hwLabel,
                    value = units.formatSpeed(abs(res.headwindComponentKt), 0),
                    accentColor = if (res.headwindComponentKt >= 0) cockpit.warningAmber else cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
                val cwDir = if (res.crosswindComponentKt >= 0) "R" else "L"
                TelemetryBadge(
                    label = "Crosswind",
                    value = "${units.formatSpeed(abs(res.crosswindComponentKt), 0)} $cwDir",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.windMode == WindSolveMode.FIND_WIND) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "Solved Wind Dir",
                        value = E6BMath.formatHeading(res.windDirectionDeg),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Solved Wind Spd",
                        value = units.formatSpeed(res.windSpeedKt, 1),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (state.windMode == WindSolveMode.FIND_TRACK_GS) {
                Spacer(Modifier.height(8.dp))
                TelemetryBadge(
                    label = "Solved True Track",
                    value = E6BMath.formatHeading(res.trueCourseDeg),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Inputs Card
        CockpitCard {
            SectionHeader(
                title = "Flight & Wind Inputs",
                icon = Icons.Default.Air,
                subtitle = "Compass HDG: ${E6BMath.formatHeading(res.compassHeadingDeg)} (Var +W/-E)"
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.windMode != WindSolveMode.FIND_TRACK_GS) {
                    AvionicsStepperInput(
                        label = "True Course",
                        valueText = state.windCourseText,
                        unit = "°",
                        stepAmount = 5.0,
                        testTag = "input_wind_tc",
                        onValueChange = { v -> viewModel.updateState { it.copy(windCourseText = v) } },
                        onStep = { d ->
                            val next = E6BMath.normalizeDeg((state.windCourseText.toDoubleOrNull() ?: 90.0) + d)
                            viewModel.updateState { it.copy(windCourseText = next.toInt().toString()) }
                        }
                    )
                }
                if (state.windMode != WindSolveMode.FIND_HEADING_GS) {
                    AvionicsStepperInput(
                        label = "True Heading",
                        valueText = state.windHeadingText,
                        unit = "°",
                        stepAmount = 5.0,
                        testTag = "input_wind_th",
                        onValueChange = { v -> viewModel.updateState { it.copy(windHeadingText = v) } },
                        onStep = { d ->
                            val next = E6BMath.normalizeDeg((state.windHeadingText.toDoubleOrNull() ?: 90.0) + d)
                            viewModel.updateState { it.copy(windHeadingText = next.toInt().toString()) }
                        }
                    )
                }
                AvionicsStepperInput(
                    label = "True Airspeed",
                    valueText = state.windTasText,
                    unit = units.speedUnit.symbol,
                    stepAmount = 5.0,
                    testTag = "input_wind_tas",
                    onValueChange = { v -> viewModel.updateState { it.copy(windTasText = v) } },
                    onStep = { d ->
                        val next = ((state.windTasText.toDoubleOrNull() ?: 125.0) + d).coerceIn(20.0, 1200.0)
                        viewModel.updateState { it.copy(windTasText = next.toInt().toString()) }
                    }
                )
                if (state.windMode == WindSolveMode.FIND_WIND) {
                    AvionicsStepperInput(
                        label = "Groundspeed",
                        valueText = state.windGsText,
                        unit = units.speedUnit.symbol,
                        stepAmount = 5.0,
                        testTag = "input_wind_gs",
                        onValueChange = { v -> viewModel.updateState { it.copy(windGsText = v) } },
                        onStep = { d ->
                            val next = ((state.windGsText.toDoubleOrNull() ?: 115.0) + d).coerceIn(10.0, 1250.0)
                            viewModel.updateState { it.copy(windGsText = next.toInt().toString()) }
                        }
                    )
                } else {
                    AvionicsStepperInput(
                        label = "Wind From Dir",
                        valueText = state.windDirText,
                        unit = "°",
                        stepAmount = 10.0,
                        testTag = "input_wind_dir",
                        onValueChange = { v -> viewModel.updateState { it.copy(windDirText = v) } },
                        onStep = { d ->
                            val next = E6BMath.normalizeDeg((state.windDirText.toDoubleOrNull() ?: 45.0) + d)
                            viewModel.updateState { it.copy(windDirText = next.toInt().toString()) }
                        }
                    )
                    AvionicsStepperInput(
                        label = "Wind Speed",
                        valueText = state.windSpdText,
                        unit = units.speedUnit.symbol,
                        stepAmount = 2.0,
                        testTag = "input_wind_spd",
                        onValueChange = { v -> viewModel.updateState { it.copy(windSpdText = v) } },
                        onStep = { d ->
                            val next = ((state.windSpdText.toDoubleOrNull() ?: 22.0) + d).coerceIn(0.0, 400.0)
                            viewModel.updateState { it.copy(windSpdText = next.toInt().toString()) }
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvionicsStepperInput(
                        label = "Mag Var",
                        valueText = state.windVarText,
                        unit = "°",
                        stepAmount = 1.0,
                        testTag = "input_wind_var",
                        modifier = Modifier.weight(1f),
                        onValueChange = { v -> viewModel.updateState { it.copy(windVarText = v) } },
                        onStep = { d ->
                            val next = ((state.windVarText.toDoubleOrNull() ?: 0.0) + d).coerceIn(-45.0, 45.0)
                            viewModel.updateState { it.copy(windVarText = next.toInt().toString()) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WhizWheelSection(
    state: E6BUiState,
    viewModel: E6BViewModel
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val ratePerHour = E6BMath.computeWhizWheelRateAt60(state.whizWheelRotationDeg)
    val distInput = state.whizDistanceNmText.toDoubleOrNull() ?: 0.0
    val flowInput = state.whizFuelGphText.toDoubleOrNull() ?: 0.0
    val timeHours = if (ratePerHour > 0.1) distInput / ratePerHour else 0.0
    val fuelBurned = timeHours * flowInput

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard {
            SectionHeader(
                title = "Circular Whiz Wheel (Slide Rule)",
                icon = Icons.Default.Speed,
                subtitle = "Drag inner dial to align 60-Index with GS (${units.speedUnit.symbol}) or Rate"
            )
            Spacer(Modifier.height(10.dp))
            WhizWheelCanvas(
                innerRotationDeg = state.whizWheelRotationDeg,
                onRotationDelta = { delta -> viewModel.rotateWhizWheel(delta) }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "Rate / GS (60)",
                    value = "${"%.1f".format(ratePerHour)} ${units.speedUnit.symbol}",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Leg ETE",
                    value = E6BMath.formatDurationMin(timeHours * 60.0),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Fuel Req",
                    value = "${"%.1f".format(fuelBurned)} ${units.volumeUnit.symbol}",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Quick Align 60-Index (${units.speedUnit.symbol}):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(90, 110, 125, 150, 180, 240, 320, 450)) { spd ->
                    FilterChip(
                        selected = abs(ratePerHour - spd) < 2.0,
                        onClick = { viewModel.setWhizWheelRate(spd.toDouble()) },
                        label = { Text("$spd ${units.speedUnit.symbol}") }
                    )
                }
            }
        }

        CockpitCard {
            SectionHeader(
                title = "Time • Speed • Distance • Fuel",
                icon = Icons.Default.Explore
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Distance",
                valueText = state.whizDistanceNmText,
                unit = units.distanceUnit.symbol,
                stepAmount = 10.0,
                testTag = "input_whiz_dist",
                onValueChange = { v -> viewModel.updateState { it.copy(whizDistanceNmText = v) } },
                onStep = { d ->
                    val next = ((state.whizDistanceNmText.toDoubleOrNull() ?: 75.0) + d).coerceAtLeast(0.0)
                    viewModel.updateState { it.copy(whizDistanceNmText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Fuel Flow",
                valueText = state.whizFuelGphText,
                unit = units.volumeUnit.rateSymbol,
                stepAmount = 0.5,
                testTag = "input_whiz_gph",
                onValueChange = { v -> viewModel.updateState { it.copy(whizFuelGphText = v) } },
                onStep = { d ->
                    val next = ((state.whizFuelGphText.toDoubleOrNull() ?: 9.5) + d).coerceAtLeast(0.5)
                    viewModel.updateState { it.copy(whizFuelGphText = "%.1f".format(next)) }
                }
            )
        }
    }
}

@Composable
private fun RunwayWindSection(
    state: E6BUiState,
    viewModel: E6BViewModel
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val rwy = state.runwayWindResult
    val rwyNum = (state.runwayNumberText.toIntOrNull() ?: 28).coerceIn(1, 36)
    val recip = ((rwyNum + 17) % 36) + 1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard {
            SectionHeader(
                title = "Runway Crosswind Analyzer",
                icon = Icons.Default.FlightLand,
                subtitle = "Active RWY %02d • Reciprocal RWY %02d".format(rwyNum, recip),
                trailing = {
                    Button(
                        onClick = {
                            viewModel.updateState { it.copy(runwayNumberText = "%02d".format(recip)) }
                        }
                    ) {
                        Text("Flip %02d".format(recip))
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            RunwayWindCanvas(runwayWind = rwy)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hwLabel = if (rwy.headwindKt >= 0) "Headwind" else "Tailwind"
                TelemetryBadge(
                    label = hwLabel,
                    value = units.formatSpeed(abs(rwy.headwindKt), 1),
                    accentColor = if (rwy.headwindKt >= 0) cockpit.safeGreen else cockpit.dangerRed,
                    modifier = Modifier.weight(1f)
                )
                val xwSide = if (rwy.crosswindKt >= 0) "RIGHT" else "LEFT"
                TelemetryBadge(
                    label = "Crosswind ($xwSide)",
                    value = units.formatSpeed(abs(rwy.crosswindKt), 1),
                    accentColor = if (abs(rwy.crosswindKt) >= 15.0) cockpit.dangerRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Gust Peak XW",
                    value = units.formatSpeed(rwy.maxGustCrosswindKt, 1),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CockpitCard {
            Text(
                text = "Runway Heading: %03d°".format(rwyNum * 10),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = rwyNum.toFloat(),
                onValueChange = { v ->
                    viewModel.updateState { it.copy(runwayNumberText = "%02d".format(v.toInt().coerceIn(1, 36))) }
                },
                valueRange = 1f..36f,
                steps = 34
            )
            AvionicsStepperInput(
                label = "Wind Direction",
                valueText = state.windDirText,
                unit = "°",
                stepAmount = 10.0,
                testTag = "input_rwy_wdir",
                onValueChange = { v -> viewModel.updateState { it.copy(windDirText = v) } },
                onStep = { d ->
                    val next = E6BMath.normalizeDeg((state.windDirText.toDoubleOrNull() ?: 45.0) + d)
                    viewModel.updateState { it.copy(windDirText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvionicsStepperInput(
                    label = "Sustained",
                    valueText = state.windSpdText,
                    unit = units.speedUnit.symbol,
                    stepAmount = 2.0,
                    testTag = "input_rwy_wspd",
                    modifier = Modifier.weight(1f),
                    onValueChange = { v -> viewModel.updateState { it.copy(windSpdText = v) } },
                    onStep = { d ->
                        val next = ((state.windSpdText.toDoubleOrNull() ?: 22.0) + d).coerceIn(0.0, 240.0)
                        viewModel.updateState { it.copy(windSpdText = next.toInt().toString()) }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Gust Peak",
                valueText = state.runwayGustText,
                unit = units.speedUnit.symbol,
                stepAmount = 2.0,
                testTag = "input_rwy_gust",
                onValueChange = { v -> viewModel.updateState { it.copy(runwayGustText = v) } },
                onStep = { d ->
                    val next = ((state.runwayGustText.toDoubleOrNull() ?: 28.0) + d).coerceIn(0.0, 300.0)
                    viewModel.updateState { it.copy(runwayGustText = next.toInt().toString()) }
                }
            )
        }
    }
}
