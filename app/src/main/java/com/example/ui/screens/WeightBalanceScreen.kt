package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AircraftProfileEntity
import com.example.data.E6BDatabase
import com.example.domain.UnitPreferences
import com.example.domain.WeightUnit
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.AvionicsStepperInput
import com.example.ui.components.CgEnvelopeCanvas
import com.example.ui.components.CockpitCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.LocalCockpitColors
import kotlin.math.roundToInt

@Composable
fun WeightBalanceScreen(
    state: E6BUiState,
    profiles: List<AircraftProfileEntity>,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    val selectedProfile = profiles.firstOrNull { it.id == state.selectedAircraftId } ?: profiles.firstOrNull()
    var showAddAircraftDialog by remember { mutableStateOf(false) }

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
        // Aircraft Selector
        item {
            CockpitCard {
                SectionHeader(
                    title = "Aircraft Fleet Profile",
                    icon = Icons.Default.Balance,
                    subtitle = selectedProfile?.let {
                        "${it.tailNumber} • MTOW ${units.formatWeight(it.maxTakeoffWeightLbs)}"
                    } ?: "Select or add aircraft",
                    trailing = {
                        Row {
                            if (selectedProfile != null && !selectedProfile.isDefault) {
                                IconButton(onClick = { viewModel.deleteAircraft(selectedProfile) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete custom aircraft")
                                }
                            }
                            OutlinedButton(
                                onClick = { showAddAircraftDialog = true },
                                modifier = Modifier.testTag("add_aircraft_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Aircraft")
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles, key = { it.id }) { ac ->
                        FilterChip(
                            selected = selectedProfile?.id == ac.id,
                            onClick = { viewModel.updateState { it.copy(selectedAircraftId = ac.id) } },
                            label = { Text("${ac.tailNumber} (${ac.name.substringBefore(" ")})") },
                            modifier = Modifier.testTag("aircraft_chip_${ac.id}")
                        )
                    }
                }
            }
        }

        if (selectedProfile != null) {
            val stations = E6BDatabase.jsonToStations(selectedProfile.stationsJson)
            val envelope = E6BDatabase.jsonToEnvelope(selectedProfile.envelopeJson)
            val wbResult = viewModel.computeWeightBalance(selectedProfile)
            val allSafe = wbResult.isTakeoffWithinEnvelope && wbResult.isLandingWithinEnvelope && wbResult.isWeightWithinMaxTakeoff

            // Envelope & Status Card
            item {
                val statusColor = if (allSafe) cockpit.safeGreen else cockpit.dangerRed
                CockpitCard(borderColor = statusColor.copy(alpha = 0.7f)) {
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (allSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (allSafe) "CG & WEIGHT WITHIN ENVELOPE" else "CAUTION: CG / WEIGHT OUT OF LIMITS",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = statusColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.saveSnapshotToLog(
                                        category = "WEIGHT & CG",
                                        title = "${selectedProfile.tailNumber} • TOW ${units.formatWeight(wbResult.takeoffWeightLbs)}",
                                        summary = "TO CG ${units.formatArm(wbResult.takeoffCgIn)} | LDG ${units.formatWeight(wbResult.landingWeightLbs)} @ ${units.formatArm(wbResult.landingCgIn)} | ${if (allSafe) "SAFE" else "OUT OF LIMITS"}"
                                    )
                                }
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = "Save W&B Snapshot")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    CgEnvelopeCanvas(
                        envelope = envelope,
                        result = wbResult,
                        weightUnit = units.weightUnit
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryBadge(
                            label = "Takeoff Wt / CG",
                            value = "${units.formatWeight(wbResult.takeoffWeightLbs)} / ${units.formatArm(wbResult.takeoffCgIn)}",
                            accentColor = if (wbResult.isTakeoffWithinEnvelope) cockpit.safeGreen else cockpit.dangerRed,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "Landing Wt / CG",
                            value = "${units.formatWeight(wbResult.landingWeightLbs)} / ${units.formatArm(wbResult.landingCgIn)}",
                            accentColor = if (wbResult.isLandingWithinEnvelope) MaterialTheme.colorScheme.secondary else cockpit.dangerRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryBadge(
                            label = "Zero Fuel Wt",
                            value = units.formatWeight(wbResult.zeroFuelWeightLbs),
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "Zero Fuel CG",
                            value = units.formatArm(wbResult.zeroFuelCgIn, decimals = 2),
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Fuel Burn Input
            item {
                CockpitCard {
                    SectionHeader(
                        title = "Flight Fuel Burn",
                        icon = Icons.Default.LocalGasStation,
                        subtitle = "${selectedProfile.fuelType} • Active Unit: ${units.volumeUnit.symbol}"
                    )
                    Spacer(Modifier.height(8.dp))
                    AvionicsStepperInput(
                        label = "Planned Fuel Burn",
                        valueText = state.wbFuelBurnGalText,
                        unit = units.volumeUnit.symbol,
                        stepAmount = 5.0,
                        testTag = "input_wb_fuelburn",
                        onValueChange = { v -> viewModel.updateState { it.copy(wbFuelBurnGalText = v) } },
                        onStep = { d ->
                            val next = ((state.wbFuelBurnGalText.toDoubleOrNull() ?: 28.0) + d).coerceAtLeast(0.0)
                            viewModel.updateState { it.copy(wbFuelBurnGalText = next.toInt().toString()) }
                        }
                    )
                }
            }

            // Editable Stations
            items(stations, key = { "${selectedProfile.id}_${it.id}" }) { station ->
                val displayWt = units.weightUnit.fromLbs(station.weightLbs).roundToInt()
                val wtStep = if (units.weightUnit == WeightUnit.KG) 10.0 else 20.0
                CockpitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Arm ${units.formatArm(station.armIn)} • Max ${units.formatWeight(station.maxWeightLbs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    AvionicsStepperInput(
                        label = "Weight",
                        valueText = displayWt.toString(),
                        unit = units.weightUnit.symbol,
                        stepAmount = wtStep,
                        testTag = "input_station_${station.id}",
                        onValueChange = { txt ->
                            val parsed = txt.toDoubleOrNull()
                            if (parsed != null) {
                                val newLbs = units.weightUnit.toLbs(parsed)
                                viewModel.updateAircraftStationWeight(selectedProfile, station.id, newLbs)
                            }
                        },
                        onStep = { d ->
                            val currentDisp = units.weightUnit.fromLbs(station.weightLbs)
                            val maxDisp = units.weightUnit.fromLbs(station.maxWeightLbs)
                            val nextDisp = (currentDisp + d).coerceIn(0.0, maxDisp)
                            viewModel.updateAircraftStationWeight(
                                selectedProfile,
                                station.id,
                                units.weightUnit.toLbs(nextDisp)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddAircraftDialog) {
        AddAircraftDialog(
            units = units,
            onDismiss = { showAddAircraftDialog = false },
            onConfirm = { name, tail, ewLbs, armIn, mtowLbs, tasKt, gph ->
                viewModel.addCustomAircraft(name, tail, ewLbs, armIn, mtowLbs, tasKt, gph)
                showAddAircraftDialog = false
            }
        )
    }
}

@Composable
private fun AddAircraftDialog(
    units: UnitPreferences,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("Mooney M20J") }
    var tail by remember { mutableStateOf("N201MJ") }
    var emptyWt by remember { mutableStateOf(units.weightUnit.fromLbs(1810.0).roundToInt().toString()) }
    var emptyArm by remember { mutableStateOf("%.1f".format(units.weightUnit.armFromIn(45.2))) }
    var mtow by remember { mutableStateOf(units.weightUnit.fromLbs(2740.0).roundToInt().toString()) }
    var tas by remember { mutableStateOf(units.speedUnit.fromKt(160.0).roundToInt().toString()) }
    var gph by remember { mutableStateOf("%.1f".format(units.volumeUnit.fromGal(10.5))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Aircraft") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Model Name") }, singleLine = true)
                OutlinedTextField(value = tail, onValueChange = { tail = it }, label = { Text("Tail Number") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emptyWt, onValueChange = { emptyWt = it },
                        label = { Text("Empty (${units.weightUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = emptyArm, onValueChange = { emptyArm = it },
                        label = { Text("Arm (${units.weightUnit.armSymbol})") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mtow, onValueChange = { mtow = it },
                        label = { Text("MTOW (${units.weightUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = tas, onValueChange = { tas = it },
                        label = { Text("Cruise (${units.speedUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                OutlinedTextField(
                    value = gph,
                    onValueChange = { gph = it },
                    label = { Text("Fuel Flow (${units.volumeUnit.rateSymbol})") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val ewLbs = units.weightUnit.toLbs(emptyWt.toDoubleOrNull() ?: units.weightUnit.fromLbs(1800.0))
                val armIn = units.weightUnit.armToIn(emptyArm.toDoubleOrNull() ?: units.weightUnit.armFromIn(45.0))
                val mtowLbs = units.weightUnit.toLbs(mtow.toDoubleOrNull() ?: units.weightUnit.fromLbs(2700.0))
                val tasKt = units.speedUnit.toKt(tas.toDoubleOrNull() ?: units.speedUnit.fromKt(150.0))
                val flowGph = units.volumeUnit.toGal(gph.toDoubleOrNull() ?: units.volumeUnit.fromGal(10.0))
                onConfirm(name, tail, ewLbs, armIn, mtowLbs, tasKt, flowGph)
            }) { Text("Save Aircraft") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
