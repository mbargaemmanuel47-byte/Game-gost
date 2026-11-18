package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ControlMode
import com.example.data.GameMode
import com.example.data.GhostRecord
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.game.ProceduralLevelGenerator
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OutlineGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

enum class HubTab {
    PLAY,
    GRAVEYARD,
    SETTINGS
}

@Composable
fun MainHubScreen(
    hubState: UiState<HubUiData>,
    selectedSector: Int,
    statusBanner: String?,
    onClearBanner: () -> Unit,
    onSelectSector: (Int) -> Unit,
    onLaunchRun: (GameMode, Int) -> Unit,
    onUpdatePreferences: (ControlMode, Boolean, Boolean, Boolean) -> Unit,
    onPurgeAllGhosts: (List<String>) -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(HubTab.PLAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (currentTab != HubTab.PLAY) {
        BackHandler { currentTab = HubTab.PLAY }
    }

    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusBanner)
            onClearBanner()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(VoidBlack, MidnightIndigo, VoidBlack))
            )
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MidnightIndigo.copy(alpha = 0.96f),
                        contentColor = TextPrimary
                    ) {
                        HubTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = { Text(tabLabel(tab)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VoidBlack,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MidnightIndigo.copy(alpha = 0.96f),
                        contentColor = TextPrimary
                    ) {
                        Spacer(Modifier.height(16.dp))
                        HubTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = { Text(tabLabel(tab)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = VoidBlack,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (hubState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan)
                            }
                        }
                        is UiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                                    border = BorderStroke(1.dp, NeonCoral)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Erreur de synchronisation", style = MaterialTheme.typography.titleLarge, color = NeonCoral)
                                        Text(hubState.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val data = hubState.data
                            AnimatedContent(targetState = currentTab, label = "hub_tab") { tab ->
                                when (tab) {
                                    HubTab.PLAY -> PlayTabContent(
                                        profile = data.profile,
                                        ghosts = data.ghosts,
                                        selectedSector = selectedSector.coerceIn(1, data.profile.highestSector.toInt().coerceAtLeast(1)),
                                        onSelectSector = onSelectSector,
                                        onLaunchRun = onLaunchRun,
                                        onToggleAudio = {
                                            val nextAudio = !(data.profile.musicOn || data.profile.sfxOn)
                                            onUpdatePreferences(
                                                data.profile.controlModeEnum(),
                                                nextAudio,
                                                nextAudio,
                                                data.profile.hapticsOn
                                            )
                                        }
                                    )
                                    HubTab.GRAVEYARD -> GraveyardTabContent(
                                        ghosts = data.ghosts,
                                        recentRuns = data.recentRuns,
                                        onPurgeAllGhosts = {
                                            onPurgeAllGhosts(data.ghosts.map { it.id })
                                        },
                                        onReplaySector = onLaunchRun
                                    )
                                    HubTab.SETTINGS -> SettingsTabContent(
                                        profile = data.profile,
                                        isCloudLinked = data.isCloudLinked,
                                        onUpdatePreferences = onUpdatePreferences
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayTabContent(
    profile: UserProfile,
    ghosts: List<GhostRecord>,
    selectedSector: Int,
    onSelectSector: (Int) -> Unit,
    onLaunchRun: (GameMode, Int) -> Unit,
    onToggleAudio: () -> Unit
) {
    val maxSector = profile.highestSector.toInt().coerceAtLeast(1)
    val sectorGhostCount = ghosts.count { it.sectorIndex == selectedSector.toLong() }
    val endlessGhostCount = ghosts.count { it.sectorIndex == 0L }
    val audioActive = profile.musicOn || profile.sfxOn

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Neon Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GHOST LINE",
                            style = MaterialTheme.typography.headlineLarge,
                            color = NeonCyan
                        )
                        Text(
                            text = "Pilote : ${profile.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onToggleAudio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hub_audio_toggle")
                    ) {
                        Icon(
                            imageVector = if (audioActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Audio Synthwave",
                            tint = if (audioActive) NeonCyan else TextSecondary
                        )
                    }
                }
            }

            // Telemetry KPI Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiChip(
                        label = "RECORD INFINI",
                        value = "${profile.bestEndlessScore}",
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    KpiChip(
                        label = "SECTEUR MAX",
                        value = "$maxSector",
                        accent = NeonMagenta,
                        modifier = Modifier.weight(1f)
                    )
                    KpiChip(
                        label = "FRÔLEMENTS",
                        value = "${profile.totalNearMisses}",
                        accent = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Procedural Sectors Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.9f)),
                    border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.mode_sector_title).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonCyan
                                )
                                Text(
                                    text = stringResource(R.string.mode_sector_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                color = NeonMagenta.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.7f)),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${sectorGhostCount + 2} SPECTRES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonMagenta,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Sector selector chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sectors = (1..maxSector).toList()
                            items(sectors) { sec ->
                                val isSelected = sec == selectedSector
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSector(sec) },
                                    label = {
                                        Text(
                                            text = "SECTEUR $sec",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = VoidBlack,
                                        containerColor = VoidBlack.copy(alpha = 0.6f),
                                        labelColor = TextPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = OutlineGlow,
                                        selectedBorderColor = NeonCyan
                                    )
                                )
                            }
                        }

                        val distM = ProceduralLevelGenerator.sectorTargetDistance(selectedSector).toInt()
                        Text(
                            text = "Distance cible : ${distM}m • Seed #${selectedSector * 7919 + 1337}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { onLaunchRun(GameMode.SECTOR, selectedSector) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("launch_sector_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = VoidBlack
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "LANCER SECTEUR $selectedSector",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Endless Survival Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.85f)),
                    border = BorderStroke(1.5.dp, NeonMagenta.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = NeonMagenta)
                                Text(
                                    text = stringResource(R.string.mode_endless_title).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonMagenta
                                )
                            }
                            Text(
                                text = "$endlessGhostCount fantômes",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonViolet
                            )
                        }

                        Text(
                            text = stringResource(R.string.mode_endless_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { onLaunchRun(GameMode.ENDLESS, 0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("launch_endless_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonMagenta,
                                contentColor = VoidBlack
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "SURVIE INFINIE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Current control reminder pill
            item {
                Surface(
                    color = DeepSurface.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, OutlineGlow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Contrôle actif : ${profile.controlModeEnum().labelFr}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiChip(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DeepSurface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, OutlineGlow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun tabLabel(tab: HubTab): String = when (tab) {
    HubTab.PLAY -> stringResource(R.string.nav_play)
    HubTab.GRAVEYARD -> stringResource(R.string.nav_graveyard)
    HubTab.SETTINGS -> stringResource(R.string.nav_settings)
}

private fun tabIcon(tab: HubTab, selected: Boolean) = when (tab) {
    HubTab.PLAY -> if (selected) Icons.Filled.RocketLaunch else Icons.Outlined.RocketLaunch
    HubTab.GRAVEYARD -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    HubTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
