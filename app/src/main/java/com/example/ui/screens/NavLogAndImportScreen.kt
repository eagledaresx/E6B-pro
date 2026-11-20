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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.SavedCalculationEntity
import com.example.domain.ComputedNavLeg
import com.example.domain.E6BMath
import com.example.domain.UnitPreferences
import com.example.ui.E6BUiState
import com.example.ui.E6BViewModel
import com.example.ui.components.CockpitCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCockpitColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NavLogAndImportScreen(
    state: E6BUiState,
    legs: List<ComputedNavLeg>,
    savedCalculations: List<SavedCalculationEntity>,
    viewModel: E6BViewModel,
    contentPadding: PaddingValues
) {
    val cockpit = LocalCockpitColors.current
    val units = state.units
    var showAddLegDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val totalDistNm = legs.sumOf { it.distanceNm }
    val totalEteMin = legs.sumOf { it.eteMinutes }
    val totalFuelGal = legs.sumOf { it.legFuelGal }
    val avgGsKt = if (totalEteMin > 0.1) totalDistNm / (totalEteMin / 60.0) else 0.0

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
        // Phone Import & Export Card
        item {
            CockpitCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                SectionHeader(
                    title = "Import / Export Phone E6B",
                    icon = Icons.Default.FileDownload,
                    subtitle = "Load JSON/CSV NavLogs & Aircraft from phone storage"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setShowImportExport(true) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("open_import_export_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import / Export E6B")
                    }
                    OutlinedButton(
                        onClick = { showPrivacyPolicyDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_privacy_policy_btn")
                    ) {
                        Text("Privacy Policy")
                    }
                }
            }
        }

        // Cross-Country NavLog Summary
        item {
            CockpitCard {
                SectionHeader(
                    title = "Cross-Country NavLog",
                    icon = Icons.Default.Navigation,
                    subtitle = "${legs.size} Leg(s) • Auto-Solved WCA, MH, GS, ETE & Fuel",
                    trailing = {
                        Row {
                            if (legs.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearNavLogs() }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all legs")
                                }
                            }
                            FilledTonalButton(
                                onClick = { showAddLegDialog = true },
                                modifier = Modifier.testTag("add_nav_leg_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Leg")
                                Spacer(Modifier.width(4.dp))
                                Text("Leg")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryBadge(
                        label = "Total Dist",
                        value = units.formatDistance(totalDistNm, 1),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Total ETE",
                        value = E6BMath.formatDurationMin(totalEteMin),
                        accentColor = cockpit.safeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryBadge(
                        label = "Trip Fuel",
                        value = units.formatVolume(totalFuelGal, 1),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (legs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    val reserveTotalGal = totalFuelGal + (legs.first().fuelBurnGph * 0.75)
                    Text(
                        text = "Average Groundspeed: ${units.formatSpeed(avgGsKt, 1)} • +45 min VFR Reserve: ${units.formatVolume(reserveTotalGal, 1)} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (legs.isEmpty()) {
            item {
                CockpitCard {
                    Text(
                        text = "No NavLog legs yet. Tap '+ Leg' to plan a route or 'Import / Export E6B' to load from your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(legs, key = { it.id }) { leg ->
                CockpitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LEG ${leg.legOrder}: ${leg.fromFix} → ${leg.toFix}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${units.formatAltitude(leg.altitudeFt)} • TC ${E6BMath.formatHeading(leg.trueCourseDeg)} • ${units.formatDistance(leg.distanceNm, 1)} • Wind ${E6BMath.formatHeading(leg.windDirDeg)}/${units.formatSpeed(leg.windSpdKt, 0)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteNavLeg(leg.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete leg ${leg.fromFix}")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.height(10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryBadge(
                            label = "Mag Hdg",
                            value = E6BMath.formatHeading(leg.magneticHeadingDeg),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "GS",
                            value = units.formatSpeed(leg.groundSpeedKt, 0),
                            accentColor = cockpit.safeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "ETE",
                            value = E6BMath.formatDurationMin(leg.eteMinutes),
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "Fuel",
                            value = units.formatVolume(leg.legFuelGal, 1),
                            accentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Saved Calculation Logbook
        item {
            CockpitCard {
                SectionHeader(
                    title = "Cockpit Logbook",
                    icon = Icons.Default.Bookmark,
                    subtitle = "${savedCalculations.size} Saved E6B Snapshot(s)"
                )
            }
        }

        if (savedCalculations.isEmpty()) {
            item {
                CockpitCard {
                    Text(
                        text = "Tap 'Save' in Wind Triangle, Atmos, Flight, or Weight & Balance to archive flight calculations here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val fmt = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
            items(savedCalculations, key = { it.id }) { calc ->
                CockpitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${calc.category} • ${fmt.format(Date(calc.timestampMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = calc.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = calc.summary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSavedCalculation(calc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete saved log")
                        }
                    }
                }
            }
        }
    }

    if (showAddLegDialog) {
        AddNavLegDialog(
            units = units,
            onDismiss = { showAddLegDialog = false },
            onConfirm = { from, to, altFt, tc, distNm, tasKt, wDir, wSpdKt, mVar, gph ->
                viewModel.addNavLeg(from, to, altFt, tc, distNm, tasKt, wDir, wSpdKt, mVar, gph)
                showAddLegDialog = false
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }
}

@Composable
private fun AddNavLegDialog(
    units: UnitPreferences,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double, Double, Double, Double, Double, Double) -> Unit
) {
    var from by remember { mutableStateOf("KSBP") }
    var to by remember { mutableStateOf("KSBA") }
    var alt by remember { mutableStateOf(units.altitudeUnit.fromFt(5500.0).roundToInt().toString()) }
    var tc by remember { mutableStateOf("128") }
    var dist by remember { mutableStateOf(units.distanceUnit.fromNm(72.0).roundToInt().toString()) }
    var tas by remember { mutableStateOf(units.speedUnit.fromKt(122.0).roundToInt().toString()) }
    var wDir by remember { mutableStateOf("280") }
    var wSpd by remember { mutableStateOf(units.speedUnit.fromKt(18.0).roundToInt().toString()) }
    var mVar by remember { mutableStateOf("-12") }
    var gph by remember { mutableStateOf("%.1f".format(units.volumeUnit.fromGal(9.5))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add NavLog Waypoint Leg") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text("From") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text("To") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tc, onValueChange = { tc = it }, label = { Text("TC (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = dist, onValueChange = { dist = it }, label = { Text("Dist (${units.distanceUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = alt, onValueChange = { alt = it }, label = { Text("Alt (${units.altitudeUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = tas, onValueChange = { tas = it }, label = { Text("TAS (${units.speedUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = wDir, onValueChange = { wDir = it }, label = { Text("Wind Dir (°)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = wSpd, onValueChange = { wSpd = it }, label = { Text("Wind (${units.speedUnit.symbol})") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = mVar, onValueChange = { mVar = it }, label = { Text("Var (+W/-E)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = gph, onValueChange = { gph = it }, label = { Text("Burn (${units.volumeUnit.rateSymbol})") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val altFt = units.altitudeUnit.toFt(alt.toDoubleOrNull() ?: units.altitudeUnit.fromFt(5500.0))
                val distNm = units.distanceUnit.toNm(dist.toDoubleOrNull() ?: units.distanceUnit.fromNm(50.0))
                val tasKt = units.speedUnit.toKt(tas.toDoubleOrNull() ?: units.speedUnit.fromKt(120.0))
                val wSpdKt = units.speedUnit.toKt(wSpd.toDoubleOrNull() ?: units.speedUnit.fromKt(15.0))
                val flowGph = units.volumeUnit.toGal(gph.toDoubleOrNull() ?: units.volumeUnit.fromGal(9.5))
                onConfirm(
                    from,
                    to,
                    altFt,
                    tc.toDoubleOrNull() ?: 360.0,
                    distNm,
                    tasKt,
                    wDir.toDoubleOrNull() ?: 270.0,
                    wSpdKt,
                    mVar.toDoubleOrNull() ?: 0.0,
                    flowGph
                )
            }) { Text("Add Leg") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
