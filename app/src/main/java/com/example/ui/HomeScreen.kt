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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.example.ui.components.BouncyPillButton
import com.example.ui.components.SoftGlassCard
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
import kotlin.math.cos
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
                    Surface(
                        color = MidnightIndigo.copy(alpha = 0.90f),
                        border = BorderStroke(0.8.dp, OutlineGlow)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = TextPrimary,
                            modifier = Modifier.navigationBarsPadding()
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
                                SoftGlassCard(accentBorder = NeonCoral.copy(alpha = 0.6f)) {
                                    Column(
                                        modifier = Modifier.padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Oups, petite pause réseau", style = MaterialTheme.typography.titleLarge, color = NeonCoral)
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
                                        },
                                        onCycleControlMode = {
                                            val nextMode = when (data.profile.controlModeEnum()) {
                                                ControlMode.SPLIT -> ControlMode.ALTERNATING
                                                ControlMode.ALTERNATING -> ControlMode.CONTINUOUS
                                                ControlMode.CONTINUOUS -> ControlMode.SPLIT
                                            }
                                            onUpdatePreferences(
                                                nextMode,
                                                data.profile.musicOn,
                                                data.profile.sfxOn,
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
            animation = tween(durationMillis = 10000, easing = LinearEasing),
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
                center = Offset(w * 0.2f, h * 0.14f),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.2f, h * 0.14f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonViolet.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.45f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.85f, h * 0.45f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonMagenta.copy(alpha = 0.11f), Color.Transparent),
                center = Offset(w * 0.65f, h * 0.82f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.65f, h * 0.82f)
        )

        val wave1 = Path()
        val wave2 = Path()
        val steps = 32
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val y = t * h
            val x1 = w * 0.80f + sin(t * 5f + phase) * 32f
            val x2 = w * 0.16f + sin(t * 4f - phase * 0.85f) * 26f
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
            style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = wave2,
            color = NeonMagenta.copy(alpha = 0.10f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
    onToggleAudio: () -> Unit,
    onCycleControlMode: () -> Unit
) {
    val maxSector = profile.highestSector.toInt().coerceAtLeast(1)
    val sectorGhostCount = ghosts.count { it.sectorIndex == selectedSector.toLong() }
    val endlessGhostCount = ghosts.count { it.sectorIndex == 0L }
    val audioActive = profile.musicOn || profile.sfxOn
    val controlMode = profile.controlModeEnum()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playful Minimalist Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Ghost Line",
                            style = MaterialTheme.typography.displayMedium,
                            color = NeonCyan
                        )
                        Text(
                            text = when (controlMode) {
                                ControlMode.SPLIT -> "Gauche ou droite pour tourner • Relâche pour filer"
                                ControlMode.ALTERNATING -> "Appui long pour tourner • Relâche pour filer"
                                ControlMode.CONTINUOUS -> "Appui long 360° • Relâche pour filer"
                            },
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

            // Live Mini-Glider Preview Strip + Quick Control Pill
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    accentBorder = OutlineGlow
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniGliderPreviewStrip(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(VoidBlack.copy(alpha = 0.5f))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MinimalStat(label = "Record", value = "${profile.bestEndlessScore}", accent = NeonCyan)
                            MinimalStat(label = "Secteur", value = "$maxSector", accent = NeonMagenta)
                            MinimalStat(label = "Frôlements", value = "${profile.totalNearMisses}", accent = NeonAmber)
                        }
                    }
                }
            }

            // Hero Sector Mode Card
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = NeonCyan.copy(alpha = 0.42f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Secteurs",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Atteins le portail vert en esquivant tes échos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                color = NeonMagenta.copy(alpha = 0.18f),
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

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        containerColor = DeepSurface.copy(alpha = 0.7f),
                                        labelColor = TextSecondary
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

                        BouncyPillButton(
                            onClick = { onLaunchRun(GameMode.SECTOR, selectedSector) },
                            containerColor = NeonCyan,
                            contentColor = VoidBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .testTag("launch_sector_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Jouer • Secteur $selectedSector",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Survie Infinie Card
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = NeonMagenta.copy(alpha = 0.42f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
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

                        BouncyPillButton(
                            onClick = { onLaunchRun(GameMode.ENDLESS, 0) },
                            containerColor = NeonMagenta,
                            contentColor = VoidBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("launch_endless_button")
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

            // Quick Intuitive Control Switcher Chip
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = onCycleControlMode,
                        color = GlassSurface,
                        border = BorderStroke(1.dp, OutlineGlow),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pilotage : ${controlMode.labelFr}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniGliderPreviewStrip(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mini_glider")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glider_progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.5f

        val ghostPath = Path()
        val cyanPath = Path()
        val samples = 40
        for (i in 0..samples) {
            val f = i.toFloat() / samples.toFloat()
            val x = f * w
            val gy = centerY + sin(f * 3f * PI.toFloat() + 1.2f) * (h * 0.24f)
            val cy = centerY + sin(f * 3f * PI.toFloat() + t * 2f * PI.toFloat()) * (h * 0.25f)
            if (i == 0) {
                ghostPath.moveTo(x, gy)
                cyanPath.moveTo(x, cy)
            } else {
                ghostPath.lineTo(x, gy)
                cyanPath.lineTo(x, cy)
            }
        }

        drawPath(
            path = ghostPath,
            color = NeonMagenta.copy(alpha = 0.26f),
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = cyanPath,
            color = NeonCyan.copy(alpha = 0.35f),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Glowing Will-o'-the-Wisp ahead
        val orbX = w * 0.78f
        val orbY = centerY + sin(0.78f * 3f * PI.toFloat() + t * 2f * PI.toFloat()) * (h * 0.25f)
        drawCircle(color = NeonCyan.copy(alpha = 0.25f), radius = 14f, center = Offset(orbX, orbY))
        drawCircle(color = NeonCyan, radius = 6.5f, center = Offset(orbX, orbY))
        drawCircle(color = Color.White, radius = 2.8f, center = Offset(orbX, orbY))

        // Cute floating Ghost Spirit gliding across banner
        val shipX = w * 0.34f
        val phaseAngle = 0.34f * 3f * PI.toFloat() + t * 2f * PI.toFloat()
        val shipY = centerY + sin(phaseAngle) * (h * 0.25f)
        val slope = cos(phaseAngle) * 24f
        rotate(degrees = 90f + slope, pivot = Offset(shipX, shipY)) {
            val r = 13f
            val tailWave = sin(t * 10f * PI.toFloat()) * 2.2f
            val ghost = Path().apply {
                moveTo(shipX, shipY - r * 1.15f)
                cubicTo(shipX + r * 0.85f, shipY - r, shipX + r, shipY, shipX + r * 0.82f, shipY + r * 0.9f + tailWave)
                quadraticTo(shipX + r * 0.4f, shipY + r * 0.55f, shipX, shipY + r * 0.95f - tailWave)
                quadraticTo(shipX - r * 0.4f, shipY + r * 0.55f, shipX - r * 0.82f, shipY + r * 0.9f + tailWave)
                cubicTo(shipX - r, shipY, shipX - r * 0.85f, shipY - r, shipX, shipY - r * 1.15f)
                close()
            }
            drawPath(ghost, color = NeonCyan.copy(alpha = 0.35f), style = Stroke(width = 7f, join = StrokeJoin.Round))
            drawPath(
                ghost,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEFFFFF), NeonCyan, NeonViolet.copy(alpha = 0.78f)),
                    startY = shipY - r,
                    endY = shipY + r
                )
            )
            drawCircle(color = VoidBlack, radius = 2.1f, center = Offset(shipX - 4f, shipY - 2.5f))
            drawCircle(color = VoidBlack, radius = 2.1f, center = Offset(shipX + 4f, shipY - 2.5f))
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
