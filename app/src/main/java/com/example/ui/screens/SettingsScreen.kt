package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AltitudeUnit
import com.example.domain.DistanceUnit
import com.example.domain.MeasurementSystemPreset
import com.example.domain.PressureUnit
import com.example.domain.SpeedUnit
import com.example.domain.TempUnit
import com.example.domain.UnitPreferences
import com.example.domain.VolumeUnit
import com.example.domain.WeightUnit
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.CockpitCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCockpitColors

@Composable
fun SettingsScreen(
    state: E6BUiState,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val activePreset = units.activePreset
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Master Imperial vs Metric Quick Switcher Card
        item {
            CockpitCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) {
                SectionHeader(
                    title = "Global Measurement System",
                    icon = Icons.Default.Public,
                    subtitle = "Synchronizes units across Wind, Atmos, Flight, W&B & NavLog"
                )
                Spacer(Modifier.height(12.dp))

                // Primary Imperial vs Metric Segmented Toggle
                val primarySystems = listOf(
                    MeasurementSystemPreset.IMPERIAL,
                    MeasurementSystemPreset.METRIC
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    primarySystems.forEachIndexed { idx, preset ->
                        SegmentedButton(
                            selected = activePreset == preset,
                            onClick = { viewModel.applyMeasurementPreset(preset) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = primarySystems.size),
                            modifier = Modifier.testTag("preset_toggle_${preset.name}")
                        ) {
                            Text(
                                text = if (preset == MeasurementSystemPreset.IMPERIAL) "IMPERIAL (FAA)" else "METRIC (SI)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Preset Cards Grid (Imperial, Metric, ICAO Hybrid, US Statute)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MeasurementSystemPreset.IMPERIAL,
                        MeasurementSystemPreset.METRIC,
                        MeasurementSystemPreset.ICAO_HYBRID,
                        MeasurementSystemPreset.US_STATUTE
                    ).forEach { preset ->
                        val isSelected = activePreset == preset
                        val accent = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.applyMeasurementPreset(preset) }
                                .testTag("preset_card_${preset.name}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, accent.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active System",
                                        tint = cockpit.safeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Live Unit Telemetry Preview Strip
        item {
            CockpitCard {
                SectionHeader(
                    title = "Active Unit Standard Preview",
                    icon = Icons.Default.Speed,
                    subtitle = "Profile: ${activePreset.title} (Standard ISA Cruise Sample)"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "Cruise Speed",
                        value = units.formatSpeed(125.0, decimals = 0),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Leg Distance",
                        value = units.formatDistance(100.0, decimals = 1),
                        accentColor = cockpit.safeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Altitude",
                        value = units.formatAltitude(5500.0, decimals = 0),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stdBaro = if (units.pressureUnit == PressureUnit.HPA) "1013.2 hPa" else "29.92 inHg"
                    TelemetryBadge(
                        label = "Std Baro",
                        value = stdBaro,
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Fuel / Mass",
                        value = "${units.formatVolume(40.0, 0)} • ${units.formatWeight(2550.0, 0)}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }

        // 3. Granular Per-Dimension Unit Overrides
        item {
            CockpitCard {
                SectionHeader(
                    title = "Granular Unit Customization",
                    icon = Icons.Default.Settings,
                    subtitle = "Override individual cockpit parameters independently"
                )
                Spacer(Modifier.height(12.dp))

                UnitDimensionRow(
                    title = "Airspeed & Wind Speed",
                    icon = Icons.Default.Air,
                    options = SpeedUnit.entries,
                    selected = units.speedUnit,
                    optionLabel = { it.symbol },
                    testTagPrefix = "unit_speed",
                    onSelect = { newSpeed ->
                        viewModel.applyUnitPreferences(units.copy(speedUnit = newSpeed))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Navigation Distance",
                    icon = Icons.Default.Straighten,
                    options = DistanceUnit.entries,
                    selected = units.distanceUnit,
                    optionLabel = { it.symbol },
                    testTagPrefix = "unit_distance",
                    onSelect = { newDist ->
                        viewModel.applyUnitPreferences(units.copy(distanceUnit = newDist))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Altitude & Vertical Speed",
                    icon = Icons.Default.Cloud,
                    options = AltitudeUnit.entries,
                    selected = units.altitudeUnit,
                    optionLabel = { "${it.symbol} (${it.vsSymbol})" },
                    testTagPrefix = "unit_altitude",
                    onSelect = { newAlt ->
                        viewModel.applyUnitPreferences(units.copy(altitudeUnit = newAlt))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Altimeter Barometric Pressure",
                    icon = Icons.Default.Speed,
                    options = PressureUnit.entries,
                    selected = units.pressureUnit,
                    optionLabel = { it.symbol },
                    testTagPrefix = "unit_pressure",
                    onSelect = { newBaro ->
                        viewModel.applyUnitPreferences(units.copy(pressureUnit = newBaro))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Outside Air Temperature (OAT)",
                    icon = Icons.Default.Thermostat,
                    options = TempUnit.entries,
                    selected = units.tempUnit,
                    optionLabel = { it.symbol },
                    testTagPrefix = "unit_temp",
                    onSelect = { newTemp ->
                        viewModel.applyUnitPreferences(units.copy(tempUnit = newTemp))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Fuel Volume & Burn Rate",
                    icon = Icons.Default.LocalGasStation,
                    options = VolumeUnit.entries,
                    selected = units.volumeUnit,
                    optionLabel = { "${it.symbol} (${it.rateSymbol})" },
                    testTagPrefix = "unit_volume",
                    onSelect = { newVol ->
                        viewModel.applyUnitPreferences(units.copy(volumeUnit = newVol))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                UnitDimensionRow(
                    title = "Aircraft Weight & CG Arm",
                    icon = Icons.Default.Balance,
                    options = WeightUnit.entries,
                    selected = units.weightUnit,
                    optionLabel = { "${it.symbol} / ${it.armSymbol}" },
                    testTagPrefix = "unit_weight",
                    onSelect = { newWt ->
                        viewModel.applyUnitPreferences(units.copy(weightUnit = newWt))
                    }
                )
            }
        }

        // 4. Cockpit Display & System Preferences
        item {
            CockpitCard {
                SectionHeader(
                    title = "Cockpit Display & Data",
                    icon = Icons.Default.NightlightRound,
                    subtitle = "Night vision preservation & defaults"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Night Cockpit Red Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Monochrome red phosphor palette for night flight",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.nightRedMode,
                        onCheckedChange = { viewModel.toggleNightRedMode() },
                        modifier = Modifier.testTag("settings_night_red_switch")
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.applyMeasurementPreset(MeasurementSystemPreset.IMPERIAL) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_units_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reset FAA Defaults")
                    }
                    Button(
                        onClick = { showPrivacyPolicy = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_privacy_btn")
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Privacy Policy")
                    }
                }
            }
        }
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun <T> UnitDimensionRow(
    title: String,
    icon: ImageVector,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    testTagPrefix: String,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val label = optionLabel(option)
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("${testTagPrefix}_${label.replace("/", "_").replace(" ", "_")}")
                )
            }
        }
    }
}
