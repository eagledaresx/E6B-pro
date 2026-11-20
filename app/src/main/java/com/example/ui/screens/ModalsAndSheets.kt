package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.E6BViewModel
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.JetBrainsMonoFamily

private data class ConversionSpec(
    val label: String,
    val fromUnit: String,
    val toUnit: String,
    val forward: (Double) -> Double,
    val reverse: (Double) -> Double
)

@Composable
fun UnitConverterDialog(
    onDismiss: () -> Unit
) {
    val specs = remember {
        listOf(
            ConversionSpec("NM ↔ SM", "NM", "SM", { it * 1.15078 }, { it / 1.15078 }),
            ConversionSpec("NM ↔ KM", "NM", "KM", { it * 1.852 }, { it / 1.852 }),
            ConversionSpec("FT ↔ M", "FT", "M", { it * 0.3048 }, { it / 0.3048 }),
            ConversionSpec("KT ↔ MPH", "KT", "MPH", { it * 1.15078 }, { it / 1.15078 }),
            ConversionSpec("KT ↔ KM/H", "KT", "KM/H", { it * 1.852 }, { it / 1.852 }),
            ConversionSpec("inHg ↔ hPa", "inHg", "hPa", { it * 33.86389 }, { it / 33.86389 }),
            ConversionSpec("°C ↔ °F", "°C", "°F", { it * 1.8 + 32.0 }, { (it - 32.0) / 1.8 }),
            ConversionSpec("GAL ↔ L", "US GAL", "LITERS", { it * 3.78541 }, { it / 3.78541 }),
            ConversionSpec("100LL GAL ↔ LBS", "GAL", "LBS", { it * 6.0 }, { it / 6.0 }),
            ConversionSpec("Jet-A GAL ↔ LBS", "GAL", "LBS", { it * 6.7 }, { it / 6.7 }),
            ConversionSpec("LBS ↔ KG", "LBS", "KG", { it * 0.453592 }, { it / 0.453592 })
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var invertDirection by remember { mutableStateOf(false) }
    var inputVal by remember { mutableStateOf("100") }
    val spec = specs[selectedIndex]
    val fromUnit = if (!invertDirection) spec.fromUnit else spec.toUnit
    val toUnit = if (!invertDirection) spec.toUnit else spec.fromUnit
    val numeric = inputVal.toDoubleOrNull() ?: 0.0
    val converted = if (!invertDirection) spec.forward(numeric) else spec.reverse(numeric)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
        title = { Text("Aviation Unit Converter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(specs) { idx, s ->
                        FilterChip(
                            selected = idx == selectedIndex,
                            onClick = { selectedIndex = idx },
                            label = { Text(s.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$fromUnit → $toUnit",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { invertDirection = !invertDirection }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap direction")
                        Spacer(Modifier.width(4.dp))
                        Text("Swap Units")
                    }
                }
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    label = { Text("Input ($fromUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier.fillMaxWidth()
                )
                TelemetryBadge(
                    label = "Converted ($toUnit)",
                    value = "${"%.2f".format(converted)} $toUnit",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun ImportExportDialog(
    viewModel: E6BViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var replaceNavLog by remember { mutableStateOf(false) }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { text ->
                if (!text.isNullOrBlank()) {
                    rawText = text
                }
            }
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(viewModel.buildExportJson())
                }
            }.onSuccess {
                viewModel.updateState { it.copy(statusBannerMessage = "Exported E6B Pro backup to phone storage") }
                onDismiss()
            }
        }
    }

    val sampleCsv = """
        FROM,TO,ALT,TC,DIST,TAS,WDIR,WSPD,VAR,GPH
        KSQL,KOAK,3500,042,18,118,290,14,-13,9.2
        KOAK,KCCR,4500,065,22,120,295,16,-13,9.2
        KCCR,KLVK,3500,145,19,118,280,15,-13,9.2
    """.trimIndent()

    val samplePhoneBundleJson = """
        {
          "format": "E6B_PRO_V1",
          "aircraftProfiles": [
            {
              "name": "Diamond DA40 NG",
              "tailNumber": "N40NG",
              "fuelType": "Jet-A",
              "fuelWeightPerGal": 6.7,
              "maxTakeoffWeightLbs": 2888.0,
              "maxLandingWeightLbs": 2822.0,
              "cruiseTasKt": 146.0,
              "fuelBurnGph": 7.4
            }
          ],
          "navLogs": [
            {
              "legOrder": 1,
              "fromFix": "KSJC",
              "toFix": "KMOD",
              "altitudeFt": 7500.0,
              "trueCourseDeg": 078.0,
              "distanceNm": 56.0,
              "tasKt": 146.0,
              "windDirDeg": 310.0,
              "windSpdKt": 20.0,
              "magVarDeg": -13.0,
              "fuelBurnGph": 7.4
            },
            {
              "legOrder": 2,
              "fromFix": "KMOD",
              "toFix": "KTVL",
              "altitudeFt": 11500.0,
              "trueCourseDeg": 052.0,
              "distanceNm": 98.0,
              "tasKt": 148.0,
              "windDirDeg": 290.0,
              "windSpdKt": 26.0,
              "magVarDeg": -13.0,
              "fuelBurnGph": 7.2
            }
          ]
        }
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import / Export Phone E6B") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Import E6B aircraft profiles & flight plans from your phone storage (JSON or CSV), or export your current E6B Pro database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            openDocLauncher.launch(arrayOf("application/json", "text/csv", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_phone_file_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Pick File")
                    }
                    OutlinedButton(
                        onClick = { createDocLauncher.launch("e6b_pro_export.json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_phone_file_btn")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save File")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { rawText = samplePhoneBundleJson },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_phone_bundle_btn")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Phone E6B Bundle")
                    }
                    FilledTonalButton(
                        onClick = {
                            val json = viewModel.buildExportJson()
                            rawText = json
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("E6B Pro Export", json))
                            viewModel.updateState { it.copy(statusBannerMessage = "Copied current E6B JSON to clipboard") }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_export_json_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Copy JSON")
                    }
                }
                TextButton(
                    onClick = { rawText = sampleCsv },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Load Sample Phone CSV NavLog")
                }
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("E6B JSON or CSV Payload") },
                    minLines = 5,
                    maxLines = 8,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_payload_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Checkbox(checked = replaceNavLog, onCheckedChange = { replaceNavLog = it })
                    Text("Replace existing NavLog legs on import", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = rawText.isNotBlank(),
                onClick = { viewModel.importFromPhoneText(rawText, replaceNavLog) },
                modifier = Modifier.testTag("confirm_import_btn")
            ) {
                Text("Import Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy & Pilot Notice") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Effective Date: November 18, 2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "1. 100% On-Device & Offline Storage\n" +
                        "E6B Pro stores all aircraft weight-and-balance profiles, navigation logs (NavLogs), and saved cockpit calculations strictly on your device using a local Room SQLite database. No personal data or flight data is ever transmitted to external servers.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "2. Zero Tracking & Zero Permissions\n" +
                        "E6B Pro does not collect personal identifiers, GPS location, analytics, or advertising telemetry. File import/export uses the zero-permission Android System File Picker only when initiated by you.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "3. Data Control & Deletion\n" +
                        "You can delete individual aircraft profiles, NavLog legs, or saved calculations at any time inside the app, or remove all data by clearing app storage or uninstalling E6B Pro.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "4. Pilot-in-Command (PIC) Disclaimer\n" +
                        "Always verify all performance, fuel, and weight & balance figures against your aircraft's official FAA/EASA-approved Pilot's Operating Handbook (POH/AFM) prior to flight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Acknowledge")
            }
        }
    )
}

