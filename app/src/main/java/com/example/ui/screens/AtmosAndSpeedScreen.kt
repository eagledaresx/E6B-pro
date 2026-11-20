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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.AltitudeUnit
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.AvionicsStepperInput
import com.example.ui.components.CockpitCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.pow

@Composable
fun AtmosAndSpeedScreen(
    state: E6BUiState,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val atmos = state.atmosphereResult
    val airspeed = state.airspeedResult

    // Koch Chart approximation: ~13% takeoff distance increase per 1,000 ft DA above sea level
    val daThousands = (atmos.densityAltitudeFt / 1000.0).coerceAtLeast(0.0)
    val takeoffMultiplier = 1.125.pow(daThousands)
    val climbPenaltyPct = ((1.0 - (1.0 / 1.09.pow(daThousands))) * 100.0).coerceIn(0.0, 75.0)

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
        // Atmosphere Results Card
        item {
            CockpitCard {
                SectionHeader(
                    title = "Density & Pressure Altitude",
                    icon = Icons.Default.Thermostat,
                    subtitle = "Includes Virtual Temp Humidity Factor (${units.formatSignedAltitude(atmos.humidityCorrectionFt)})",
                    trailing = {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveSnapshotToLog(
                                    category = "ATMOSPHERE",
                                    title = "DA ${units.formatAltitude(atmos.densityAltitudeFt)} • TAS ${units.formatSpeed(airspeed.trueAirspeedKt)}",
                                    summary = "PA ${units.formatAltitude(atmos.pressureAltitudeFt)} | ISA ${units.formatTempDelta(atmos.isaDeviationC)} | Mach ${"%.3f".format(airspeed.machNumber)}"
                                )
                            },
                            modifier = Modifier.testTag("save_atmos_btn")
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Atmosphere Snapshot")
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "Density Alt",
                        value = units.formatAltitude(atmos.densityAltitudeFt),
                        accentColor = if (atmos.densityAltitudeFt > 6000.0) cockpit.warningAmber else cockpit.safeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Pressure Alt",
                        value = units.formatAltitude(atmos.pressureAltitudeFt),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "ISA Temp",
                        value = units.formatTemp(atmos.isaTempC, 1),
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "ISA Dev",
                        value = units.formatTempDelta(atmos.isaDeviationC, 1),
                        accentColor = if (atmos.isaDeviationC > 15.0) cockpit.warningAmber else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Cloud Base AGL",
                        value = units.formatAltitude(atmos.cloudBaseAglFt),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Airspeed & Compressibility Card
        item {
            CockpitCard {
                SectionHeader(
                    title = "Airspeed & Mach Computer",
                    icon = Icons.Default.Speed,
                    subtitle = "Compressible Subsonic TAS • Speed of Sound: ${units.formatSpeed(airspeed.speedOfSoundKt, 0)}"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "True Airspeed",
                        value = units.formatSpeed(airspeed.trueAirspeedKt, 1),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Mach",
                        value = "M ${"%.3f".format(airspeed.machNumber)}",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "EAS",
                        value = units.formatSpeed(airspeed.equivalentAirspeedKt, 1),
                        accentColor = cockpit.safeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Koch Performance Advisory: Takeoff roll ~${"%.2f".format(takeoffMultiplier)}x Sea Level • Climb rate reduced ~${"%.0f".format(climbPenaltyPct)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (takeoffMultiplier > 1.6) cockpit.warningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Inputs Card
        item {
            CockpitCard {
                SectionHeader(
                    title = "Atmospheric & Pitot Inputs",
                    icon = Icons.Default.Cloud,
                    trailing = {
                        FilledTonalButton(
                            onClick = { viewModel.toggleAltimeterUnit() },
                            modifier = Modifier.testTag("toggle_altimeter_unit")
                        ) {
                            Text("Baro: ${units.pressureUnit.symbol}")
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val altStep = if (units.altitudeUnit == AltitudeUnit.M) 150.0 else 500.0
                    AvionicsStepperInput(
                        label = "Field / Cruise Alt",
                        valueText = state.fieldElevText,
                        unit = units.altitudeUnit.symbol,
                        stepAmount = altStep,
                        testTag = "input_atmos_elev",
                        onValueChange = { v -> viewModel.updateState { it.copy(fieldElevText = v) } },
                        onStep = { d ->
                            val next = ((state.fieldElevText.toDoubleOrNull() ?: 1250.0) + d).coerceIn(-1500.0, 50000.0)
                            viewModel.updateState { it.copy(fieldElevText = next.toInt().toString()) }
                        }
                    )
                    AvionicsStepperInput(
                        label = "Altimeter",
                        valueText = state.altimeterText,
                        unit = units.pressureUnit.symbol,
                        stepAmount = if (state.altimeterIsHpa) 2.0 else 0.05,
                        testTag = "input_atmos_baro",
                        onValueChange = { v -> viewModel.updateState { it.copy(altimeterText = v) } },
                        onStep = { d ->
                            val def = if (state.altimeterIsHpa) 1013.25 else 29.92
                            val next = (state.altimeterText.toDoubleOrNull() ?: def) + d
                            val fmt = if (state.altimeterIsHpa) "%.1f".format(next) else "%.2f".format(next)
                            viewModel.updateState { it.copy(altimeterText = fmt) }
                        }
                    )
                    AvionicsStepperInput(
                        label = "OAT",
                        valueText = state.oatText,
                        unit = units.tempUnit.symbol,
                        stepAmount = 2.0,
                        testTag = "input_atmos_oat",
                        onValueChange = { v -> viewModel.updateState { it.copy(oatText = v) } },
                        onStep = { d ->
                            val next = ((state.oatText.toDoubleOrNull() ?: 24.0) + d).coerceIn(-85.0, 140.0)
                            viewModel.updateState { it.copy(oatText = next.toInt().toString()) }
                        }
                    )
                    AvionicsStepperInput(
                        label = "Dewpoint",
                        valueText = state.dewpointText,
                        unit = units.tempUnit.symbol,
                        stepAmount = 2.0,
                        testTag = "input_atmos_dew",
                        onValueChange = { v -> viewModel.updateState { it.copy(dewpointText = v) } },
                        onStep = { d ->
                            val next = ((state.dewpointText.toDoubleOrNull() ?: 12.0) + d).coerceIn(-85.0, 120.0)
                            viewModel.updateState { it.copy(dewpointText = next.toInt().toString()) }
                        }
                    )
                    AvionicsStepperInput(
                        label = "Calibrated Airspeed",
                        valueText = state.casText,
                        unit = units.speedUnit.symbol,
                        stepAmount = 5.0,
                        testTag = "input_atmos_cas",
                        onValueChange = { v -> viewModel.updateState { it.copy(casText = v) } },
                        onStep = { d ->
                            val next = ((state.casText.toDoubleOrNull() ?: 118.0) + d).coerceIn(20.0, 1100.0)
                            viewModel.updateState { it.copy(casText = next.toInt().toString()) }
                        }
                    )
                }
            }
        }
    }
}
