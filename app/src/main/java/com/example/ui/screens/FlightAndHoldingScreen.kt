package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AltitudeUnit
import com.example.domain.E6BMath
import com.example.domain.HoldingEntryType
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.AvionicsStepperInput
import com.example.ui.components.CockpitCard
import com.example.ui.components.HoldingPatternCanvas
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.abs

@Composable
fun FlightAndHoldingScreen(
    state: E6BUiState,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val subTabs = listOf("Holding", "TOD/VNAV", "Turn/Cold", "ETP/PNR")

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
                        selected = state.flightSubTab == idx,
                        onClick = { viewModel.updateState { it.copy(flightSubTab = idx) } },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = subTabs.size),
                        modifier = Modifier.testTag("flight_subtab_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        when (state.flightSubTab) {
            0 -> item { HoldingEntrySection(state, viewModel) }
            1 -> item { VnavTodSection(state, viewModel) }
            2 -> item { TurnAndColdAltSection(state, viewModel) }
            else -> item { EtpPnrSection(state, viewModel) }
        }
    }
}

@Composable
private fun HoldingEntrySection(state: E6BUiState, viewModel: E6BViewModel) {
    val cockpit = LocalCockpitColors.current
    val hold = state.holdingResult
    val entryColor = when (hold.entryType) {
        HoldingEntryType.DIRECT -> cockpit.safeGreen
        HoldingEntryType.TEARDROP -> MaterialTheme.colorScheme.secondary
        HoldingEntryType.PARALLEL -> cockpit.dangerRed
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard(borderColor = entryColor.copy(alpha = 0.6f)) {
            SectionHeader(
                title = "Holding Pattern Entry",
                icon = Icons.Default.Loop,
                subtitle = hold.entryType.description,
                trailing = {
                    IconButton(
                        onClick = {
                            viewModel.saveSnapshotToLog(
                                category = "HOLDING",
                                title = "${hold.entryType.title} • Inbound ${E6BMath.formatHeading(hold.inboundCourseDeg)}",
                                summary = "HDG ${E6BMath.formatHeading(hold.aircraftHeadingDeg)} | Outbound ${E6BMath.formatHeading(hold.entryHeadingDeg)} | ${if (hold.isRightTurns) "Std Right" else "Left Turns"}"
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Save Holding Entry"
                        )
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            HoldingPatternCanvas(holding = hold)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "Recommended Entry",
                    value = hold.entryType.title,
                    accentColor = entryColor,
                    modifier = Modifier.weight(1.4f)
                )
                TelemetryBadge(
                    label = "Initial Outbound",
                    value = E6BMath.formatHeading(hold.entryHeadingDeg),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CockpitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.holdRightTurns,
                    onClick = { viewModel.updateState { it.copy(holdRightTurns = true) } },
                    label = { Text("Standard (Right Turns)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !state.holdRightTurns,
                    onClick = { viewModel.updateState { it.copy(holdRightTurns = false) } },
                    label = { Text("Non-Std (Left Turns)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            AvionicsStepperInput(
                label = "Inbound Course",
                valueText = state.holdInboundText,
                unit = "°",
                stepAmount = 10.0,
                testTag = "input_hold_inbound",
                onValueChange = { v -> viewModel.updateState { it.copy(holdInboundText = v) } },
                onStep = { d ->
                    val next = E6BMath.normalizeDeg((state.holdInboundText.toDoubleOrNull() ?: 270.0) + d)
                    viewModel.updateState { it.copy(holdInboundText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Aircraft Heading",
                valueText = state.holdHeadingText,
                unit = "°",
                stepAmount = 10.0,
                testTag = "input_hold_heading",
                onValueChange = { v -> viewModel.updateState { it.copy(holdHeadingText = v) } },
                onStep = { d ->
                    val next = E6BMath.normalizeDeg((state.holdHeadingText.toDoubleOrNull() ?: 315.0) + d)
                    viewModel.updateState { it.copy(holdHeadingText = next.toInt().toString()) }
                }
            )
        }
    }
}

@Composable
private fun VnavTodSection(state: E6BUiState, viewModel: E6BViewModel) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val vnav = state.vnavResult
    val altStep = if (units.altitudeUnit == AltitudeUnit.M) 150.0 else 500.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard {
            SectionHeader(
                title = "Top of Descent & VNAV",
                icon = Icons.Default.TrendingDown,
                subtitle = "3:1 Rule Estimate: ${units.formatDistance(vnav.ruleOfThreeNm, 1)}",
                trailing = {
                    IconButton(
                        onClick = {
                            viewModel.saveSnapshotToLog(
                                category = "TOD / VNAV",
                                title = "TOD ${units.formatDistance(vnav.todDistanceNm, 1)} @ -${units.formatVerticalSpeed(vnav.requiredFpm)}",
                                summary = "Lose ${units.formatAltitude(vnav.altitudeToLoseFt)} | Path ${"%.2f".format(vnav.descentAngleDeg)}° (${"%.1f".format(vnav.gradientPercent)}%)"
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Save VNAV"
                        )
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "TOD Distance",
                    value = units.formatDistance(vnav.todDistanceNm, 1),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Required VS",
                    value = units.formatVerticalSpeed(vnav.requiredFpm),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryBadge(
                    label = "Descent Time",
                    value = E6BMath.formatDurationMin(vnav.timeToDescentMin),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Flight Path",
                    value = "${"%.2f".format(vnav.descentAngleDeg)}° (${"%.1f".format(vnav.gradientPercent)}%)",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CockpitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Specify Vertical Speed (${units.altitudeUnit.vsSymbol}) instead of Angle", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = state.vnavUseFpm,
                    onCheckedChange = { checked -> viewModel.updateState { it.copy(vnavUseFpm = checked) } }
                )
            }
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Cruise Altitude",
                valueText = state.vnavCruiseAltText,
                unit = units.altitudeUnit.symbol,
                stepAmount = altStep,
                testTag = "input_vnav_cruise",
                onValueChange = { v -> viewModel.updateState { it.copy(vnavCruiseAltText = v) } },
                onStep = { d ->
                    val next = ((state.vnavCruiseAltText.toDoubleOrNull() ?: 9500.0) + d).coerceAtLeast(0.0)
                    viewModel.updateState { it.copy(vnavCruiseAltText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Target Altitude",
                valueText = state.vnavTargetAltText,
                unit = units.altitudeUnit.symbol,
                stepAmount = altStep,
                testTag = "input_vnav_target",
                onValueChange = { v -> viewModel.updateState { it.copy(vnavTargetAltText = v) } },
                onStep = { d ->
                    val next = ((state.vnavTargetAltText.toDoubleOrNull() ?: 1500.0) + d).coerceAtLeast(0.0)
                    viewModel.updateState { it.copy(vnavTargetAltText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Groundspeed",
                valueText = state.vnavGsText,
                unit = units.speedUnit.symbol,
                stepAmount = 10.0,
                testTag = "input_vnav_gs",
                onValueChange = { v -> viewModel.updateState { it.copy(vnavGsText = v) } },
                onStep = { d ->
                    val next = ((state.vnavGsText.toDoubleOrNull() ?: 135.0) + d).coerceAtLeast(20.0)
                    viewModel.updateState { it.copy(vnavGsText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            if (state.vnavUseFpm) {
                val vsStep = if (units.altitudeUnit == AltitudeUnit.M) 0.5 else 100.0
                AvionicsStepperInput(
                    label = "Descent Rate",
                    valueText = state.vnavFpmText,
                    unit = units.altitudeUnit.vsSymbol,
                    stepAmount = vsStep,
                    testTag = "input_vnav_fpm",
                    onValueChange = { v -> viewModel.updateState { it.copy(vnavFpmText = v) } },
                    onStep = { d ->
                        val next = ((state.vnavFpmText.toDoubleOrNull() ?: 700.0) + d).coerceAtLeast(0.5)
                        val fmt = if (units.altitudeUnit == AltitudeUnit.M) "%.1f".format(next) else next.toInt().toString()
                        viewModel.updateState { it.copy(vnavFpmText = fmt) }
                    }
                )
            } else {
                AvionicsStepperInput(
                    label = "Descent Angle",
                    valueText = state.vnavAngleText,
                    unit = "°",
                    stepAmount = 0.5,
                    testTag = "input_vnav_angle",
                    onValueChange = { v -> viewModel.updateState { it.copy(vnavAngleText = v) } },
                    onStep = { d ->
                        val next = ((state.vnavAngleText.toDoubleOrNull() ?: 3.0) + d).coerceIn(0.5, 12.0)
                        viewModel.updateState { it.copy(vnavAngleText = "%.1f".format(next)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun TurnAndColdAltSection(state: E6BUiState, viewModel: E6BViewModel) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val turn = state.turnResult
    val cold = state.trueAltitudeResult
    val altStep = if (units.altitudeUnit == AltitudeUnit.M) 150.0 else 500.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard {
            SectionHeader(
                title = "Turn Radius & Load Factor",
                icon = Icons.Default.Flight,
                subtitle = "Standard 3°/sec Bank Angle: ${"%.1f".format(turn.standardRateBankDeg)}°"
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryBadge(
                    label = "Turn Rate",
                    value = "${"%.2f".format(turn.rateOfTurnDegPerSec)} °/s",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Turn Radius",
                    value = units.formatDistance(turn.turnRadiusNm, 2),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Load / Stall",
                    value = "${"%.2f".format(turn.loadFactorG)}G (${"%.2f".format(turn.stallSpeedIncreaseFactor)}x)",
                    accentColor = if (turn.loadFactorG > 1.5) cockpit.warningAmber else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1.2f)
                )
            }
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "TAS",
                valueText = state.turnTasText,
                unit = units.speedUnit.symbol,
                stepAmount = 10.0,
                testTag = "input_turn_tas",
                onValueChange = { v -> viewModel.updateState { it.copy(turnTasText = v) } },
                onStep = { d ->
                    val next = ((state.turnTasText.toDoubleOrNull() ?: 120.0) + d).coerceIn(30.0, 950.0)
                    viewModel.updateState { it.copy(turnTasText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Bank Angle",
                valueText = state.turnBankText,
                unit = "°",
                stepAmount = 5.0,
                testTag = "input_turn_bank",
                onValueChange = { v -> viewModel.updateState { it.copy(turnBankText = v) } },
                onStep = { d ->
                    val next = ((state.turnBankText.toDoubleOrNull() ?: 20.0) + d).coerceIn(5.0, 70.0)
                    viewModel.updateState { it.copy(turnBankText = next.toInt().toString()) }
                }
            )
        }

        CockpitCard {
            SectionHeader(
                title = "Cold-Weather True Altitude",
                icon = Icons.Default.AcUnit,
                subtitle = "ICAO Cold Temperature Terrain Clearance Correction"
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryBadge(
                    label = "True Altitude",
                    value = units.formatAltitude(cold.trueAltitudeFt),
                    accentColor = if (cold.coldTempErrorFt < -200) cockpit.dangerRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Alt Error",
                    value = units.formatSignedAltitude(cold.coldTempErrorFt),
                    accentColor = if (cold.coldTempErrorFt < 0) cockpit.dangerRed else cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Indicated Alt",
                valueText = state.coldIndicatedAltText,
                unit = units.altitudeUnit.symbol,
                stepAmount = altStep,
                testTag = "input_cold_ind",
                onValueChange = { v -> viewModel.updateState { it.copy(coldIndicatedAltText = v) } },
                onStep = { d ->
                    val next = ((state.coldIndicatedAltText.toDoubleOrNull() ?: 5000.0) + d).coerceAtLeast(0.0)
                    viewModel.updateState { it.copy(coldIndicatedAltText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Station OAT",
                valueText = state.coldOatText,
                unit = units.tempUnit.symbol,
                stepAmount = 5.0,
                testTag = "input_cold_oat",
                onValueChange = { v -> viewModel.updateState { it.copy(coldOatText = v) } },
                onStep = { d ->
                    val next = ((state.coldOatText.toDoubleOrNull() ?: -18.0) + d).coerceIn(-80.0, 120.0)
                    viewModel.updateState { it.copy(coldOatText = next.toInt().toString()) }
                }
            )
        }
    }
}

@Composable
private fun EtpPnrSection(state: E6BUiState, viewModel: E6BViewModel) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val etp = state.etpPnrResult
    val totalDistNm = units.distanceUnit.toNm(state.etpTotalDistText.toDoubleOrNull() ?: 420.0)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitCard {
            SectionHeader(
                title = "ETP & Point of No Return",
                icon = Icons.Default.Route,
                subtitle = "Out GS: ${units.formatSpeed(etp.outboundGsKt, 0)} • Return GS: ${units.formatSpeed(etp.returnGsKt, 0)}"
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryBadge(
                    label = "ETP Distance",
                    value = units.formatDistance(etp.etpDistanceNm, 1),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Time to ETP",
                    value = E6BMath.formatDurationMin(etp.etpTimeMin),
                    accentColor = cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryBadge(
                    label = "PNR Distance",
                    value = units.formatDistance(etp.pnrDistanceNm, 1),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryBadge(
                    label = "Time to PNR",
                    value = E6BMath.formatDurationMin(etp.pnrTimeMin),
                    accentColor = if (abs(etp.pnrDistanceNm) < totalDistNm) cockpit.warningAmber else cockpit.safeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        CockpitCard {
            AvionicsStepperInput(
                label = "Total Route Dist",
                valueText = state.etpTotalDistText,
                unit = units.distanceUnit.symbol,
                stepAmount = 25.0,
                testTag = "input_etp_dist",
                onValueChange = { v -> viewModel.updateState { it.copy(etpTotalDistText = v) } },
                onStep = { d ->
                    val next = ((state.etpTotalDistText.toDoubleOrNull() ?: 420.0) + d).coerceAtLeast(10.0)
                    viewModel.updateState { it.copy(etpTotalDistText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Safe Endurance",
                valueText = state.etpSafeEnduranceHrText,
                unit = "HRS",
                stepAmount = 0.5,
                testTag = "input_etp_endur",
                onValueChange = { v -> viewModel.updateState { it.copy(etpSafeEnduranceHrText = v) } },
                onStep = { d ->
                    val next = ((state.etpSafeEnduranceHrText.toDoubleOrNull() ?: 4.5) + d).coerceAtLeast(0.5)
                    viewModel.updateState { it.copy(etpSafeEnduranceHrText = "%.1f".format(next)) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Cruise TAS",
                valueText = state.etpTasText,
                unit = units.speedUnit.symbol,
                stepAmount = 5.0,
                testTag = "input_etp_tas",
                onValueChange = { v -> viewModel.updateState { it.copy(etpTasText = v) } },
                onStep = { d ->
                    val next = ((state.etpTasText.toDoubleOrNull() ?: 145.0) + d).coerceAtLeast(40.0)
                    viewModel.updateState { it.copy(etpTasText = next.toInt().toString()) }
                }
            )
            Spacer(Modifier.height(8.dp))
            AvionicsStepperInput(
                label = "Wind Speed",
                valueText = state.etpWindSpdText,
                unit = units.speedUnit.symbol,
                stepAmount = 5.0,
                testTag = "input_etp_wspd",
                onValueChange = { v -> viewModel.updateState { it.copy(etpWindSpdText = v) } },
                onStep = { d ->
                    val next = ((state.etpWindSpdText.toDoubleOrNull() ?: 25.0) + d).coerceAtLeast(0.0)
                    viewModel.updateState { it.copy(etpWindSpdText = next.toInt().toString()) }
                }
            )
        }
    }
}
