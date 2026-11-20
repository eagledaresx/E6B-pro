package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.AtmosAndSpeedScreen
import com.example.ui.screens.FlightAndHoldingScreen
import com.example.ui.screens.ImportExportDialog
import com.example.ui.screens.NavLogAndImportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UnitConverterDialog
import com.example.ui.screens.WeightBalanceScreen
import com.example.ui.screens.WindAndSlideRuleScreen
import com.example.ui.theme.E6BProTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCockpitColors

@Composable
fun E6BApp(viewModel: E6BViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aircraftProfiles by viewModel.aircraftProfiles.collectAsStateWithLifecycle()
    val navLegs by viewModel.computedNavLegs.collectAsStateWithLifecycle()
    val savedCalcs by viewModel.savedCalculations.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusBannerMessage) {
        val msg = state.statusBannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (state.activeTab != E6BTab.WIND) {
        BackHandler {
            viewModel.selectTab(E6BTab.WIND)
        }
    }

    val tabIcons = mapOf(
        E6BTab.WIND to Icons.Default.Explore,
        E6BTab.ATMOS to Icons.Default.Thermostat,
        E6BTab.FLIGHT to Icons.Default.Flight,
        E6BTab.WB to Icons.Default.Balance,
        E6BTab.NAVLOG to Icons.Default.Route,
        E6BTab.SETTINGS to Icons.Default.Settings
    )

    E6BProTheme(nightRedMode = state.nightRedMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CockpitHeaderBar(
                        nightRedMode = state.nightRedMode,
                        activeSystemLabel = state.units.activePreset.title.uppercase(),
                        isSettingsOpen = state.activeTab == E6BTab.SETTINGS,
                        onToggleNightRed = { viewModel.toggleNightRedMode() },
                        onOpenConverter = { viewModel.setShowUnitConverter(true) },
                        onOpenImportExport = { viewModel.setShowImportExport(true) },
                        onOpenSettings = { viewModel.selectTab(E6BTab.SETTINGS) }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            E6BTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = state.activeTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcons[tab] ?: Icons.Default.Explore,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label.substringBefore(" "),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            E6BTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = state.activeTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcons[tab] ?: Icons.Default.Explore,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label.substringBefore(" ")) },
                                    modifier = Modifier.testTag("rail_tab_${tab.name}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            when (state.activeTab) {
                                E6BTab.WIND -> WindAndSlideRuleScreen(state, viewModel, innerPadding)
                                E6BTab.ATMOS -> AtmosAndSpeedScreen(state, viewModel, innerPadding)
                                E6BTab.FLIGHT -> FlightAndHoldingScreen(state, viewModel, innerPadding)
                                E6BTab.WB -> WeightBalanceScreen(state, aircraftProfiles, viewModel, innerPadding)
                                E6BTab.NAVLOG -> NavLogAndImportScreen(state, navLegs, savedCalcs, viewModel, innerPadding)
                                E6BTab.SETTINGS -> SettingsScreen(state, viewModel, innerPadding)
                            }
                        }
                    }
                }
            }

            if (state.showUnitConverter) {
                UnitConverterDialog(onDismiss = { viewModel.setShowUnitConverter(false) })
            }
            if (state.showImportExportDialog) {
                ImportExportDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowImportExport(false) }
                )
            }
        }
    }
}

@Composable
private fun CockpitHeaderBar(
    nightRedMode: Boolean,
    activeSystemLabel: String,
    isSettingsOpen: Boolean,
    onToggleNightRed: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenImportExport: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val cockpit = LocalCockpitColors.current
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
            .height(68.dp)
    ) {
        if (!nightRedMode) {
            Image(
                painter = painterResource(id = R.drawable.img_cockpit_header_1795012968970),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            bgColor.copy(alpha = 0.92f),
                            bgColor.copy(alpha = 0.65f),
                            bgColor.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E6B PRO FLIGHT COMPUTER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (nightRedMode) "NIGHT RED • $activeSystemLabel" else "UNITS: $activeSystemLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (nightRedMode) cockpit.dangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenImportExport,
                    modifier = Modifier.testTag("topbar_import_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Import or Export E6B Data",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onOpenConverter,
                    modifier = Modifier.testTag("topbar_converter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Quick Aviation Unit Converter",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onToggleNightRed,
                    modifier = Modifier.testTag("topbar_night_red_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = "Toggle Night Cockpit Red Mode",
                        tint = if (nightRedMode) cockpit.dangerRed else Color.White.copy(alpha = 0.85f)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("topbar_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings and Measurement Units",
                        tint = if (isSettingsOpen) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
