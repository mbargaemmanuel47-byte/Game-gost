package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.GlassSurface
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
import kotlin.math.PI
import kotlin.math.sin

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

        AmbientHubBackdrop(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MidnightIndigo.copy(alpha = 0.92f),
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
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
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
                        containerColor = MidnightIndigo.copy(alpha = 0.92f),
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
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
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
                                        Text("Oups, erreur réseau", style = MaterialTheme.typography.titleLarge, color = NeonCoral)
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
private fun AmbientHubBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient_backdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.16f),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.2f, h * 0.16f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonMagenta.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.72f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.85f, h * 0.72f)
        )

        // Subtle flowing ghost ribbons in background
        val wave1 = Path()
        val wave2 = Path()
        val steps = 28
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val y = t * h
            val x1 = w * 0.78f + sin(t * 5f + phase) * 34f
            val x2 = w * 0.18f + sin(t * 4f - phase * 0.8f) * 26f
            if (i == 0) {
                wave1.moveTo(x1, y)
                wave2.moveTo(x2, y)
            } else {
                wave1.lineTo(x1, y)
                wave2.lineTo(x2, y)
            }
        }
        drawPath(
            path = wave1,
            color = NeonCyan.copy(alpha = 0.12f),
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = wave2,
            color = NeonMagenta.copy(alpha = 0.10f),
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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
                .widthIn(max = 580.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Minimalist playful header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ghost Line",
                            style = MaterialTheme.typography.displayMedium,
                            color = NeonCyan
                        )
                        Text(
                            text = "Maintiens pour tourner • Relâche pour filer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onToggleAudio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hub_audio_toggle"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = GlassSurface,
                            contentColor = if (audioActive) NeonCyan else TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = if (audioActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Audio Synthwave"
                        )
                    }
                }
            }

            // Clean airy KPI strip
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GlassSurface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, OutlineGlow.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalStat(label = "Record", value = "${profile.bestEndlessScore}", accent = NeonCyan)
                        MinimalStat(label = "Secteur max", value = "$maxSector", accent = NeonMagenta)
                        MinimalStat(label = "Frôlements", value = "${profile.totalNearMisses}", accent = NeonAmber)
                    }
                }
            }

            // Hero Sector Card (Minimal & Tactile)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Secteurs",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Surface(
                                color = NeonMagenta.copy(alpha = 0.16f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${sectorGhostCount + 2} fantômes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonMagenta,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sectors = (1..maxSector).toList()
                            items(sectors) { sec ->
                                val isSelected = sec == selectedSector
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectSector(sec) },
                                    shape = CircleShape,
                                    label = {
                                        Text(
                                            text = "S$sec",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = VoidBlack,
                                        containerColor = VoidBlack.copy(alpha = 0.45f),
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = OutlineGlow.copy(alpha = 0.7f),
                                        selectedBorderColor = NeonCyan
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { onLaunchRun(GameMode.SECTOR, selectedSector) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("launch_sector_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = VoidBlack
                            ),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Jouer Secteur $selectedSector",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Minimalist Endless Survival Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = BorderStroke(1.2.dp, NeonMagenta.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    text = stringResource(R.string.mode_endless_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "$endlessGhostCount fantômes",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonViolet
                            )
                        }

                        Button(
                            onClick = { onLaunchRun(GameMode.ENDLESS, 0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("launch_endless_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonMagenta,
                                contentColor = VoidBlack
                            ),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Survie Infinie",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalStat(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
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
