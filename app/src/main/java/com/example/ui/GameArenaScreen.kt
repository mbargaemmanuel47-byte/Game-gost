package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SynthwaveAudioEngine
import com.example.data.ControlMode
import com.example.data.GameMode
import com.example.data.GhostRecord
import com.example.data.Vec2
import com.example.game.EngineEvent
import com.example.game.GameConstants
import com.example.game.GameEngine
import com.example.game.Obstacle
import com.example.game.RunPhase
import com.example.ui.components.BouncyPillButton
import com.example.ui.components.SoftGlassCard
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OutlineGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun triggerHaptic(context: Context, enabled: Boolean, ms: Long, amplitude: Int) {
    if (!enabled) return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }
    } catch (_: Exception) {
    }
}

private fun ghostPaletteColor(colorIndex: Int): Color = when (colorIndex % 4) {
    0 -> NeonMagenta
    1 -> NeonViolet
    2 -> NeonAmber
    else -> NeonCoral
}

@Composable
fun GameArenaScreen(
    runConfig: ActiveRunConfig,
    controlMode: ControlMode,
    musicOn: Boolean,
    sfxOn: Boolean,
    hapticsOn: Boolean,
    bestEndlessScore: Long,
    savedGhostsForSector: List<GhostRecord>,
    audioEngine: SynthwaveAudioEngine,
    onToggleMute: () -> Unit,
    onSelectControlMode: (ControlMode) -> Unit = {},
    onRunFinished: (
        mode: GameMode,
        sectorIndex: Int,
        score: Long,
        distance: Long,
        nearMisses: Long,
        cleared: Boolean,
        crashPoint: Vec2?,
        trailPoints: List<Vec2>
    ) -> Unit,
    onRetry: (GameMode, Int) -> Unit,
    onExitArena: () -> Unit
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    var localRunGhosts by remember(runConfig.mode, runConfig.sectorIndex) {
        mutableStateOf(savedGhostsForSector)
    }
    LaunchedEffect(savedGhostsForSector) {
        if (savedGhostsForSector.size > localRunGhosts.size) {
            localRunGhosts = savedGhostsForSector
        }
    }

    val engine = remember(runConfig.runInstanceId) {
        GameEngine(
            mode = runConfig.mode,
            sectorIndex = runConfig.sectorIndex,
            controlMode = controlMode,
            savedGhosts = localRunGhosts
        )
    }
    engine.controlMode = controlMode

    var frameTick by remember(runConfig.runInstanceId) { mutableLongStateOf(0L) }
    var currentPhase by remember(runConfig.runInstanceId) { mutableStateOf(RunPhase.READY) }
    var hasReportedFinish by remember(runConfig.runInstanceId) { mutableStateOf(false) }

    BackHandler {
        when (engine.phase) {
            RunPhase.RUNNING -> {
                engine.pause()
                currentPhase = engine.phase
            }
            else -> onExitArena()
        }
    }

    DisposableEffect(runConfig.runInstanceId) {
        onDispose {
            audioEngine.isGameplayActive = false
            audioEngine.isTurning = false
        }
    }

    // 60/120fps vsync loop
    LaunchedEffect(runConfig.runInstanceId) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                } else {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    lastNanos = frameNanos
                    val events = engine.step(dt)
                    for (ev in events) {
                        when (ev) {
                            EngineEvent.OrbCollected -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.ORB)
                                triggerHaptic(context, hapticsOn, 16L, 110)
                            }
                            EngineEvent.NearMiss -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.NEAR_MISS)
                                triggerHaptic(context, hapticsOn, 22L, 150)
                            }
                            EngineEvent.Crashed -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.CRASH)
                                triggerHaptic(context, hapticsOn, 100L, 240)
                            }
                            EngineEvent.SectorCleared -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.CLEAR)
                                triggerHaptic(context, hapticsOn, 60L, 200)
                            }
                        }
                    }

                    audioEngine.isGameplayActive = (engine.phase == RunPhase.RUNNING)
                    audioEngine.isTurning = (engine.activeTurnSign != 0f)
                    audioEngine.intensity = ((engine.currentSpeed - 278f) / 260f).coerceIn(0.2f, 1f)

                    if (currentPhase != engine.phase) {
                        currentPhase = engine.phase
                    }

                    if (!hasReportedFinish && (engine.phase == RunPhase.CRASHED || engine.phase == RunPhase.SECTOR_CLEARED)) {
                        hasReportedFinish = true
                        val cleared = engine.phase == RunPhase.SECTOR_CLEARED
                        val crashPt = if (!cleared) Vec2(engine.shipX, engine.shipY) else null
                        if (!cleared && crashPt != null && engine.playerTrail.size >= 4) {
                            val optimistic = GhostRecord(
                                id = "local_${runConfig.runInstanceId}",
                                sectorIndex = if (runConfig.mode == GameMode.ENDLESS) 0L else runConfig.sectorIndex.toLong(),
                                score = engine.score,
                                distance = engine.distanceTraveled.toLong(),
                                crashX = crashPt.x.toDouble(),
                                crashY = crashPt.y.toDouble(),
                                packedPoints = GhostRecord.packPoints(engine.playerTrail),
                                colorIndex = (localRunGhosts.size % 4).toLong()
                            )
                            localRunGhosts = (listOf(optimistic) + localRunGhosts).take(6)
                        }
                        onRunFinished(
                            runConfig.mode,
                            runConfig.sectorIndex,
                            engine.score,
                            engine.distanceTraveled.toLong(),
                            engine.nearMissCount,
                            cleared,
                            crashPt,
                            engine.playerTrail.toList()
                        )
                    }

                    frameTick++
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Zero-lag touch Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_arena_canvas")
                .pointerInput(runConfig.runInstanceId, controlMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        engine.onTouchChanged(true, down.position.x / w)
                        currentPhase = engine.phase
                        triggerHaptic(context, hapticsOn, 10L, 75)

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointer = event.changes.firstOrNull { it.pressed }
                            if (activePointer == null) {
                                engine.onTouchChanged(false, 0.5f)
                                break
                            } else {
                                engine.onTouchChanged(true, activePointer.position.x / w)
                            }
                        }
                    }
                }
        ) {
            val tick = frameTick
            drawArenaFrame(engine, tick, textMeasurer)
        }

        // Ultra-Clean Floating Top HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = if (runConfig.mode == GameMode.SECTOR) {
                                "Secteur ${runConfig.sectorIndex}"
                            } else {
                                "Survie Infinie"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonMagenta
                        )
                        Text(
                            text = "${engine.score}",
                            style = MaterialTheme.typography.displayMedium,
                            color = NeonCyan,
                            modifier = Modifier.testTag("hud_score_text")
                        )
                    }
                    if (engine.comboMultiplier > 1) {
                        Surface(
                            color = NeonAmber.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, NeonAmber),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "x${engine.comboMultiplier}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = GlassSurface,
                        border = BorderStroke(0.8.dp, OutlineGlow),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta)
                            )
                            Text(
                                text = "${engine.activeGhosts.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_mute_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = GlassSurface,
                            contentColor = if (musicOn || sfxOn) NeonCyan else TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = if (musicOn || sfxOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Audio"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (engine.phase == RunPhase.RUNNING) {
                                engine.pause()
                                currentPhase = engine.phase
                            } else if (engine.phase == RunPhase.PAUSED) {
                                engine.resume()
                                currentPhase = engine.phase
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_pause_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = GlassSurface,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (currentPhase == RunPhase.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = stringResource(R.string.pause)
                        )
                    }
                }
            }

            if (runConfig.mode == GameMode.SECTOR) {
                Spacer(modifier = Modifier.height(6.dp))
                val progress = ((-engine.shipY) / engine.targetDistance).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = NeonLime,
                    trackColor = GlassSurface
                )
            }
        }

        // Intuitive Ready Visual Guide Overlay (disappears smoothly on touch)
        AnimatedVisibility(
            visible = currentPhase == RunPhase.READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            if (controlMode == ControlMode.SPLIT) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NeonCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = NeonCyan)
                                Spacer(Modifier.width(4.dp))
                                Text("Gauche", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = NeonMagenta.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.55f)),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Droite", style = MaterialTheme.typography.titleMedium, color = NeonMagenta)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NeonMagenta)
                            }
                        }
                    }
                    Surface(
                        color = GlassSurface,
                        border = BorderStroke(1.dp, OutlineGlow),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Appuie d'un côté pour tourner • Relâche pour filer droit",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = GlassSurface,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = NeonCyan)
                            Text(
                                text = "Maintiens pour tourner • Relâche pour filer",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Soft Frosted Pause Overlay with Quick Control Switcher
        AnimatedVisibility(
            visible = currentPhase == RunPhase.PAUSED,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SoftGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .widthIn(max = 390.dp),
                containerColor = MidnightIndigo.copy(alpha = 0.94f),
                accentBorder = NeonCyan.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Petite pause",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NeonCyan
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(ControlMode.SPLIT to "Gauche/Droite", ControlMode.ALTERNATING to "1 Doigt").forEach { (modeOpt, label) ->
                            val selected = controlMode == modeOpt
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    engine.controlMode = modeOpt
                                    onSelectControlMode(modeOpt)
                                },
                                shape = CircleShape,
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = VoidBlack,
                                    containerColor = DeepSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = OutlineGlow,
                                    selectedBorderColor = NeonCyan
                                )
                            )
                        }
                    }

                    BouncyPillButton(
                        onClick = {
                            engine.resume()
                            currentPhase = engine.phase
                        },
                        containerColor = NeonCyan,
                        contentColor = VoidBlack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("resume_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.resume), style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onExitArena,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_exit_button"),
                        border = BorderStroke(1.dp, OutlineGlow)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.back_hub), color = TextPrimary)
                    }
                }
            }
        }

        // Joyful Soft Crash / Sector Clear Overlay
        AnimatedVisibility(
            visible = currentPhase == RunPhase.CRASHED || currentPhase == RunPhase.SECTOR_CLEARED,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .navigationBarsPadding()
        ) {
            val isCleared = currentPhase == RunPhase.SECTOR_CLEARED
            val isNewRecord = runConfig.mode == GameMode.ENDLESS && engine.score > bestEndlessScore
            val accent = if (isCleared) NeonLime else NeonMagenta

            SoftGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp),
                containerColor = MidnightIndigo.copy(alpha = 0.95f),
                accentBorder = accent.copy(alpha = 0.72f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isCleared) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = if (isCleared) "Secteur ${runConfig.sectorIndex} réussi !" else "Signal interrompu !",
                        style = MaterialTheme.typography.headlineLarge,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isCleared) {
                            "Tu as franchi le portail avec brio"
                        } else {
                            engine.crashReason.labelFr
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (isNewRecord) {
                        Surface(
                            color = NeonAmber.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonAmber),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Nouveau record !",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonAmber,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge("Score", "${engine.score}", NeonCyan)
                        StatBadge("Distance", "${engine.distanceTraveled.toInt()} m", TextPrimary)
                        StatBadge("Frôlements", "${engine.nearMissCount}", NeonAmber)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (isCleared) {
                        BouncyPillButton(
                            onClick = { onRetry(GameMode.SECTOR, runConfig.sectorIndex + 1) },
                            containerColor = NeonCyan,
                            contentColor = VoidBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("next_sector_button")
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.next_sector), style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    BouncyPillButton(
                        onClick = { onRetry(runConfig.mode, runConfig.sectorIndex) },
                        containerColor = if (isCleared) DeepSurface else NeonMagenta,
                        contentColor = if (isCleared) TextPrimary else VoidBlack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("retry_instant_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.retry_instant), style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onExitArena,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gameover_hub_button"),
                        border = BorderStroke(1.dp, OutlineGlow)
                    ) {
                        Text(stringResource(R.string.back_hub), color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

private fun DrawScope.drawArenaFrame(
    engine: GameEngine,
    frameTick: Long,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val arenaPixelWidth = min(w, h * 0.72f)
    val scale = arenaPixelWidth / (GameConstants.WORLD_HALF_WIDTH * 2f + 24f)

    val shakePhase = (frameTick % 7L).toFloat()
    val shakeX = if (engine.screenShake > 0.2f) sin(shakePhase * 2.1f) * engine.screenShake else 0f
    val shakeY = if (engine.screenShake > 0.2f) cos(shakePhase * 3.3f) * engine.screenShake else 0f

    val originX = w * 0.5f + shakeX
    val shipScreenY = h * 0.74f + shakeY

    fun toScreen(wx: Float, wy: Float): Offset =
        Offset(originX + wx * scale, shipScreenY + (wy - engine.shipY) * scale)

    // 1. Haunted Twilight Mist Sky & Parallax Fog Clouds
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF060814), MidnightIndigo, Color(0xFF070918))
        )
    )

    val leftWallX = originX - GameConstants.WORLD_HALF_WIDTH * scale
    val rightWallX = originX + GameConstants.WORLD_HALF_WIDTH * scale

    // Drifting ambient spectral nebula clouds
    val fogBandSpacing = 520f
    val topWorldY = engine.shipY - (shipScreenY / scale) - 180f
    val botWorldY = engine.shipY + ((h - shipScreenY) / scale) + 180f
    val firstFogBand = ((topWorldY * 0.45f) / fogBandSpacing).toInt()
    val lastFogBand = ((botWorldY * 0.45f) / fogBandSpacing).toInt()
    for (band in firstFogBand..lastFogBand) {
        val fogWorldY = (band * fogBandSpacing) / 0.45f
        val fogScreenY = shipScreenY + (fogWorldY - engine.shipY * 0.45f) * scale
        val fogOffsetX = sin(band * 1.7f) * (GameConstants.WORLD_HALF_WIDTH * 0.55f)
        val fogCenter = Offset(originX + fogOffsetX * scale, fogScreenY)
        val fogColor = if (band % 2 == 0) NeonViolet else NeonCyan
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fogColor.copy(alpha = 0.08f), Color.Transparent),
                center = fogCenter,
                radius = arenaPixelWidth * 0.55f
            ),
            radius = arenaPixelWidth * 0.55f,
            center = fogCenter
        )
    }

    // Active Steering Edge Touch Glow
    if (engine.phase == RunPhase.RUNNING && engine.activeTurnSign != 0f) {
        val touchGlowX = if (engine.activeTurnSign < 0f) w * 0.10f else w * 0.90f
        val glowColor = if (engine.activeTurnSign < 0f) NeonCyan else NeonMagenta
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(touchGlowX, h * 0.75f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(touchGlowX, h * 0.75f)
        )
    }

    // 2. Drifting Spectral Fireflies & Faint Mist Horizons
    val starStep = 140f
    val firstStarRow = (topWorldY / starStep).toInt()
    val lastStarRow = (botWorldY / starStep).toInt()
    for (row in firstStarRow..lastStarRow) {
        val wy = row * starStep
        val sy = shipScreenY + (wy - engine.shipY) * scale
        val hash1 = ((row * 73856093) xor 19349663)
        val wx1 = (((hash1 and 0xFF) / 255f) * 2f - 1f) * (GameConstants.WORLD_HALF_WIDTH - 24f)
        val drift = sin(engine.elapsedSec * 1.4f + row) * 10f
        val sx1 = originX + (wx1 + drift) * scale
        val starAlpha = if (row % 2 == 0) 0.34f else 0.18f
        val starColor = when (row % 3) {
            0 -> NeonCyan
            1 -> NeonViolet
            else -> NeonMagenta
        }
        drawCircle(
            color = starColor.copy(alpha = starAlpha * 0.4f),
            radius = 5.5f * scale,
            center = Offset(sx1, sy)
        )
        drawCircle(
            color = starColor.copy(alpha = starAlpha),
            radius = if (row % 4 == 0) 2.5f * scale else 1.5f * scale,
            center = Offset(sx1, sy)
        )
    }

    // 3. Haunted Mist Corridor Curtains & Luminous Boundary Threads
    val mistDepth = 26f * scale
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(NeonViolet.copy(alpha = 0.24f), Color.Transparent),
            startX = leftWallX,
            endX = leftWallX + mistDepth
        ),
        topLeft = Offset(leftWallX, 0f),
        size = Size(mistDepth, h)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, NeonViolet.copy(alpha = 0.24f)),
            startX = rightWallX - mistDepth,
            endX = rightWallX
        ),
        topLeft = Offset(rightWallX - mistDepth, 0f),
        size = Size(mistDepth, h)
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.22f),
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 9f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.85f),
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 2.2f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.22f),
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 9f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.85f),
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 2.2f * scale
    )
    // Glowing boundary rune beads scrolling along walls
    val runeStep = 220f
    val firstRune = (topWorldY / runeStep).toInt()
    val lastRune = (botWorldY / runeStep).toInt()
    for (rIdx in firstRune..lastRune) {
        val ry = shipScreenY + (rIdx * runeStep - engine.shipY) * scale
        drawCircle(color = NeonViolet.copy(alpha = 0.55f), radius = 3.2f * scale, center = Offset(leftWallX, ry))
        drawCircle(color = NeonViolet.copy(alpha = 0.55f), radius = 3.2f * scale, center = Offset(rightWallX, ry))
    }

    // 4. Sector Finish Ethereal Portal
    if (engine.mode == GameMode.SECTOR) {
        val portalY = -engine.targetDistance
        if (portalY in topWorldY..botWorldY) {
            val pScreenY = shipScreenY + (portalY - engine.shipY) * scale
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, NeonLime.copy(alpha = 0.36f), Color.Transparent),
                    startY = pScreenY - 38f * scale,
                    endY = pScreenY + 38f * scale
                ),
                topLeft = Offset(leftWallX, pScreenY - 38f * scale),
                size = Size(rightWallX - leftWallX, 76f * scale)
            )
            drawLine(
                color = NeonLime.copy(alpha = 0.9f),
                start = Offset(leftWallX, pScreenY),
                end = Offset(rightWallX, pScreenY),
                strokeWidth = 4.5f * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(leftWallX + 8f * scale, pScreenY),
                end = Offset(rightWallX - 8f * scale, pScreenY),
                strokeWidth = 1.6f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    // 5. Spectral Ghost Echo Trails, Mini Phantom Wisps & Tombstone Soul Lanterns
    for (ghost in engine.activeGhosts) {
        val gColor = ghostPaletteColor(ghost.colorIndex)
        val pts = ghost.points
        if (pts.size >= 2) {
            val path = Path()
            var started = false
            for (pt in pts) {
                if (pt.y < topWorldY - 220f || pt.y > botWorldY + 220f) {
                    if (started) continue
                }
                val sPt = toScreen(pt.x, pt.y)
                if (!started) {
                    path.moveTo(sPt.x, sPt.y)
                    started = true
                } else {
                    path.lineTo(sPt.x, sPt.y)
                }
            }
            if (started) {
                // Wide smoky ectoplasm wake
                drawPath(
                    path = path,
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.08f else 0.14f),
                    style = Stroke(width = 20f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inner ethereal soul stream
                drawPath(
                    path = path,
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.40f else 0.62f),
                    style = Stroke(
                        width = 3.2f * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(18f * scale, 10f * scale),
                            phase = -engine.elapsedSec * 28f
                        )
                    )
                )
            }
        }

        // Miniature translucent Phantom Spirit gliding along the echo trajectory
        val cursor = ghost.cursorAt(engine.distanceTraveled)
        if (cursor != null && cursor.y in topWorldY..botWorldY) {
            val sc = toScreen(cursor.x, cursor.y)
            drawMiniPhantomSpirit(
                center = sc,
                radiusPx = 9.5f * scale,
                color = gColor,
                wobblePhase = engine.elapsedSec * 7f + ghost.colorIndex * 1.4f
            )
        }

        // Spectral Soul Lantern / Tombstone Sigil at crash point
        val cp = ghost.crashPoint
        if (cp != null && cp.y in topWorldY..botWorldY) {
            val sc = toScreen(cp.x, cp.y)
            val pulseRing = 14f + sin(engine.elapsedSec * 4.5f + ghost.colorIndex) * 3f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gColor.copy(alpha = 0.30f), Color.Transparent),
                    center = sc,
                    radius = pulseRing * 1.5f * scale
                ),
                radius = pulseRing * 1.5f * scale,
                center = sc
            )
            // Tiny glowing spectral rune cross + halo
            val runeR = 7.5f * scale
            drawCircle(
                color = gColor.copy(alpha = 0.85f),
                radius = runeR,
                center = sc,
                style = Stroke(width = 1.8f * scale)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(sc.x, sc.y - runeR * 0.65f),
                end = Offset(sc.x, sc.y + runeR * 0.65f),
                strokeWidth = 1.8f * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(sc.x - runeR * 0.5f, sc.y - runeR * 0.1f),
                end = Offset(sc.x + runeR * 0.5f, sc.y - runeR * 0.1f),
                strokeWidth = 1.8f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    // 6. Haunted Spectral Obstacles (Misty Gate Pylons, Crystalline Runes & Spectral Scythes)
    for (obs in engine.obstacles) {
        if (obs.y < topWorldY - 120f || obs.y > botWorldY + 120f) continue
        when (obs) {
            is Obstacle.BarrierGate -> {
                val leftEnd = obs.gapCenterX - obs.gapHalfWidth
                val rightStart = obs.gapCenterX + obs.gapHalfWidth
                val topY = obs.y - obs.thickness * 0.5f
                val hPx = obs.thickness * scale
                val midY = topScreenY(shipScreenY, obs.y, engine.shipY, scale)

                // Faint shimmering safe doorway aura between pylons so opening is instantly clear
                val safeLeftPx = originX + leftEnd * scale
                val safeRightPx = originX + rightStart * scale
                drawLine(
                    color = NeonCyan.copy(alpha = 0.16f),
                    start = Offset(safeLeftPx, midY),
                    end = Offset(safeRightPx, midY),
                    strokeWidth = 2.2f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * scale, 10f * scale))
                )

                if (leftEnd > -GameConstants.WORLD_HALF_WIDTH) {
                    val pLeft = toScreen(-GameConstants.WORLD_HALF_WIDTH, topY)
                    val wPx = (leftEnd + GameConstants.WORLD_HALF_WIDTH) * scale
                    drawSpectralBarrierWing(pLeft, wPx, hPx, NeonCoral, isRightSide = false, scale = scale)
                }
                if (rightStart < GameConstants.WORLD_HALF_WIDTH) {
                    val pRight = toScreen(rightStart, topY)
                    val wPx = (GameConstants.WORLD_HALF_WIDTH - rightStart) * scale
                    drawSpectralBarrierWing(pRight, wPx, hPx, NeonCoral, isRightSide = true, scale = scale)
                }
            }
            is Obstacle.MovingSweeper -> {
                val cx = obs.currentCenterX(engine.elapsedSec)
                val center = toScreen(cx, obs.y)
                drawSpectralScytheSweeper(
                    center = center,
                    halfWidthPx = obs.halfLength * scale,
                    halfThicknessPx = obs.thickness * 0.5f * scale,
                    scale = scale,
                    elapsedSec = engine.elapsedSec
                )
            }
            is Obstacle.DiamondMonolith -> {
                val center = toScreen(obs.centerX, obs.y)
                drawHauntedCrystalMonolith(
                    center = center,
                    radiusPx = obs.radius * scale,
                    scale = scale,
                    elapsedSec = engine.elapsedSec
                )
            }
        }
    }

    // 7. Collectible Will-o'-the-Wisp Orbs ("Feux Follets")
    val wispPulse = 1f + sin(engine.elapsedSec * 6.5f) * 0.12f
    for (orb in engine.orbs) {
        if (engine.collectedOrbIds.contains(orb.id)) continue
        if (orb.y < topWorldY || orb.y > botWorldY) continue
        val center = toScreen(orb.x, orb.y)
        val sway = sin(engine.elapsedSec * 8f + orb.id * 1.7f) * 3.5f * scale
        val r = orb.radius * scale

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.34f), NeonLime.copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = r * 1.9f * wispPulse
            ),
            radius = r * 1.9f * wispPulse,
            center = center
        )

        // Wispy spirit-flame teardrop curling up
        val flame = Path().apply {
            moveTo(center.x + sway * 0.6f, center.y - r * 1.25f * wispPulse)
            quadraticTo(center.x + r * 0.78f, center.y - r * 0.1f, center.x, center.y + r * 0.62f)
            quadraticTo(center.x - r * 0.78f, center.y - r * 0.1f, center.x + sway * 0.6f, center.y - r * 1.25f * wispPulse)
            close()
        }
        drawPath(flame, color = NeonCyan.copy(alpha = 0.85f))
        drawCircle(color = Color.White, radius = r * 0.34f, center = Offset(center.x, center.y + r * 0.08f))
    }

    // 8. Ectoplasmic Player Mist Ribbon Trail ("Traînée Fantôme")
    if (engine.playerTrail.size >= 2) {
        val visiblePoints = ArrayList<Offset>(64)
        for (pt in engine.playerTrail) {
            if (pt.y > botWorldY + 140f) continue
            visiblePoints.add(toScreen(pt.x, pt.y))
        }
        val shipPos = toScreen(engine.shipX, engine.shipY)
        if (visiblePoints.isNotEmpty()) {
            visiblePoints.add(shipPos)
            val total = visiblePoints.size
            // Draw soft gradient-faded mist segments so tail dissolves organically into air
            for (i in 0 until total - 1) {
                val p1 = visiblePoints[i]
                val p2 = visiblePoints[i + 1]
                val t = (i + 1).toFloat() / total.toFloat()
                val mistAlpha = (t * t * 0.34f).coerceIn(0.02f, 0.34f)
                val coreAlpha = (t * 0.86f).coerceIn(0.06f, 0.86f)
                val segColor = if (t < 0.45f) NeonViolet else NeonCyan

                drawLine(
                    color = segColor.copy(alpha = mistAlpha),
                    start = p1,
                    end = p2,
                    strokeWidth = (8f + t * 16f) * scale,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = NeonCyan.copy(alpha = coreAlpha),
                    start = p1,
                    end = p2,
                    strokeWidth = (1.8f + t * 3.8f) * scale,
                    cap = StrokeCap.Round
                )
                if (t > 0.55f) {
                    drawLine(
                        color = Color.White.copy(alpha = (t - 0.55f) * 1.6f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.6f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    // 9. Ectoplasm Particles
    for (p in engine.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val pColor = when (p.colorIndex) {
            0 -> NeonCyan
            1 -> NeonMagenta
            else -> NeonViolet
        }
        drawCircle(
            color = pColor.copy(alpha = alpha * 0.85f),
            radius = p.size * scale * alpha,
            center = toScreen(p.x, p.y)
        )
    }

    // 10. Predictive Spectral Sightline + Living Ghost Spirit Character
    val shipCenter = toScreen(engine.shipX, engine.shipY)
    if (engine.phase != RunPhase.CRASHED) {
        // Predictive 4-dot sightline ahead of the ghost
        var px = engine.shipX
        var py = engine.shipY
        var pHeading = engine.headingRad
        val stepSimDt = 0.075f
        val turnRate = 3.45f + (engine.currentSpeed - 278f).coerceAtLeast(0f) * 0.0022f
        for (dotIdx in 1..4) {
            if (engine.activeTurnSign != 0f) {
                pHeading += engine.activeTurnSign * turnRate * stepSimDt
                if (engine.controlMode != ControlMode.CONTINUOUS) {
                    pHeading = pHeading.coerceIn((-PI * 0.86).toFloat(), (-PI * 0.14).toFloat())
                }
            }
            px += cos(pHeading) * engine.currentSpeed * stepSimDt
            py += sin(pHeading) * engine.currentSpeed * stepSimDt
            val dotScreen = toScreen(px, py)
            val dotAlpha = (0.48f - dotIdx * 0.09f).coerceAtLeast(0.10f)
            val dotColor = if (engine.activeTurnSign < 0f) NeonCyan else if (engine.activeTurnSign > 0f) NeonMagenta else Color.White
            drawCircle(
                color = dotColor.copy(alpha = dotAlpha),
                radius = (3.2f - dotIdx * 0.42f) * scale,
                center = dotScreen
            )
        }

        if (engine.activeTurnSign != 0f) {
            val haloColor = if (engine.activeTurnSign < 0f) NeonCyan else NeonMagenta
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor.copy(alpha = 0.25f), Color.Transparent),
                    center = shipCenter,
                    radius = 30f * scale
                ),
                radius = 30f * scale,
                center = shipCenter
            )
        }

        val deg = Math.toDegrees(engine.headingRad.toDouble()).toFloat() + 90f
        drawPlayerGhostCharacter(
            center = shipCenter,
            rotationDeg = deg,
            radiusPx = 17.5f * scale,
            wavePhase = engine.elapsedSec * 9.5f,
            turnSign = engine.activeTurnSign,
            scale = scale
        )
    }

    // 11. Playful Popups
    for (pop in engine.popups) {
        val alpha = (pop.life / pop.maxLife).coerceIn(0f, 1f)
        val pos = toScreen(pop.x, pop.y)
        val pColor = if (pop.colorIndex == 0) NeonCyan else NeonAmber
        drawText(
            textMeasurer = textMeasurer,
            text = pop.text,
            topLeft = Offset(pos.x - 40f, pos.y),
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = pColor.copy(alpha = alpha)
            )
        )
    }

    // 12. Crash / Clear Flash
    if (engine.flashAlpha > 0.01f) {
        val flashColor = if (engine.phase == RunPhase.SECTOR_CLEARED) NeonLime else NeonMagenta
        drawRect(color = flashColor.copy(alpha = engine.flashAlpha * 0.28f))
    }
}

private fun topScreenY(shipScreenY: Float, worldY: Float, shipWorldY: Float, scale: Float): Float =
    shipScreenY + (worldY - shipWorldY) * scale

private fun DrawScope.drawPlayerGhostCharacter(
    center: Offset,
    rotationDeg: Float,
    radiusPx: Float,
    wavePhase: Float,
    turnSign: Float,
    scale: Float
) {
    // Soft luminous outer aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonCyan.copy(alpha = 0.36f), NeonViolet.copy(alpha = 0.14f), Color.Transparent),
            center = center,
            radius = radiusPx * 2.1f
        ),
        radius = radiusPx * 2.1f,
        center = center
    )

    rotate(degrees = rotationDeg, pivot = center) {
        val tailShift = sin(wavePhase) * radiusPx * 0.14f
        val tailAlt = cos(wavePhase) * radiusPx * 0.14f

        // Expressive spectral ghost cloak with 3 undulating lobes at bottom
        val ghostBody = Path().apply {
            moveTo(center.x, center.y - radiusPx * 1.15f)
            // Right curved dome & cloak wing
            cubicTo(
                center.x + radiusPx * 0.85f, center.y - radiusPx * 1.12f,
                center.x + radiusPx * 1.02f, center.y - radiusPx * 0.10f,
                center.x + radiusPx * 0.86f, center.y + radiusPx * 0.92f + tailShift
            )
            // Right tail scallop
            quadraticTo(
                center.x + radiusPx * 0.56f, center.y + radiusPx * 0.56f,
                center.x + radiusPx * 0.28f, center.y + radiusPx * 0.98f + tailAlt
            )
            // Center tail scallop
            quadraticTo(
                center.x, center.y + radiusPx * 0.62f,
                center.x - radiusPx * 0.28f, center.y + radiusPx * 0.98f - tailAlt
            )
            // Left tail scallop
            quadraticTo(
                center.x - radiusPx * 0.56f, center.y + radiusPx * 0.56f,
                center.x - radiusPx * 0.86f, center.y + radiusPx * 0.92f - tailShift
            )
            // Left curved dome back to crown
            cubicTo(
                center.x - radiusPx * 1.02f, center.y - radiusPx * 0.10f,
                center.x - radiusPx * 0.85f, center.y - radiusPx * 1.12f,
                center.x, center.y - radiusPx * 1.15f
            )
            close()
        }

        // Translucent ectoplasm outer bloom stroke
        drawPath(
            path = ghostBody,
            color = NeonCyan.copy(alpha = 0.34f),
            style = Stroke(width = 9f * scale, join = StrokeJoin.Round, cap = StrokeCap.Round)
        )
        // Frosted spectral gradient body (luminous white-mint crown fading into soft cyan-violet mist)
        drawPath(
            path = ghostBody,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEFFFFF), NeonCyan.copy(alpha = 0.94f), NeonViolet.copy(alpha = 0.72f)),
                startY = center.y - radiusPx * 1.15f,
                endY = center.y + radiusPx * 1.05f
            )
        )
        drawPath(
            path = ghostBody,
            color = Color.White.copy(alpha = 0.92f),
            style = Stroke(width = 1.8f * scale, join = StrokeJoin.Round)
        )

        // Cute expressive Ghost Eyes that glance softly in steering direction!
        val eyeOffsetX = radiusPx * 0.31f
        val eyeY = center.y - radiusPx * 0.22f
        val eyeR = radiusPx * 0.16f
        val lookShift = turnSign * radiusPx * 0.08f
        for (side in listOf(-1f, 1f)) {
            val ex = center.x + side * eyeOffsetX + lookShift
            drawCircle(
                color = VoidBlack,
                radius = eyeR,
                center = Offset(ex, eyeY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = eyeR * 0.38f,
                center = Offset(ex - eyeR * 0.22f, eyeY - eyeR * 0.28f)
            )
        }
    }
}

private fun DrawScope.drawMiniPhantomSpirit(
    center: Offset,
    radiusPx: Float,
    color: Color,
    wobblePhase: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.32f), Color.Transparent),
            center = center,
            radius = radiusPx * 2f
        ),
        radius = radiusPx * 2f,
        center = center
    )
    val sway = sin(wobblePhase) * radiusPx * 0.16f
    val path = Path().apply {
        moveTo(center.x, center.y - radiusPx)
        cubicTo(
            center.x + radiusPx * 0.9f, center.y - radiusPx * 0.9f,
            center.x + radiusPx, center.y + radiusPx * 0.1f,
            center.x + radiusPx * 0.78f, center.y + radiusPx * 0.85f + sway
        )
        quadraticTo(center.x, center.y + radiusPx * 0.45f, center.x - radiusPx * 0.78f, center.y + radiusPx * 0.85f - sway)
        cubicTo(
            center.x - radiusPx, center.y + radiusPx * 0.1f,
            center.x - radiusPx * 0.9f, center.y - radiusPx * 0.9f,
            center.x, center.y - radiusPx
        )
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.78f))
    drawPath(path = path, color = Color.White.copy(alpha = 0.75f), style = Stroke(width = 1.2f))
    drawCircle(color = VoidBlack, radius = radiusPx * 0.16f, center = Offset(center.x - radiusPx * 0.26f, center.y - radiusPx * 0.18f))
    drawCircle(color = VoidBlack, radius = radiusPx * 0.16f, center = Offset(center.x + radiusPx * 0.26f, center.y - radiusPx * 0.18f))
}

private fun DrawScope.drawSpectralBarrierWing(
    topLeft: Offset,
    widthPx: Float,
    heightPx: Float,
    color: Color,
    isRightSide: Boolean,
    scale: Float
) {
    val radius = CornerRadius(heightPx * 0.5f, heightPx * 0.5f)
    // Soft ectoplasm mist halo around beam
    drawRoundRect(
        color = color.copy(alpha = 0.20f),
        topLeft = Offset(topLeft.x - 2f, topLeft.y - heightPx * 0.45f),
        size = Size(widthPx + 4f, heightPx * 1.9f),
        cornerRadius = CornerRadius(heightPx * 0.95f, heightPx * 0.95f)
    )
    // Frosted amethyst-coral beam
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.92f), NeonViolet.copy(alpha = 0.78f)),
            startY = topLeft.y,
            endY = topLeft.y + heightPx
        ),
        topLeft = topLeft,
        size = Size(widthPx, heightPx),
        cornerRadius = radius
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.48f),
        topLeft = Offset(topLeft.x + heightPx * 0.25f, topLeft.y + heightPx * 0.26f),
        size = Size((widthPx - heightPx * 0.5f).coerceAtLeast(2f), heightPx * 0.28f),
        cornerRadius = CornerRadius(heightPx * 0.15f, heightPx * 0.15f)
    )

    // Haunted Lantern Pylon Orb marking the safe opening tip
    val tipX = if (isRightSide) topLeft.x + heightPx * 0.25f else topLeft.x + widthPx - heightPx * 0.25f
    val tipY = topLeft.y + heightPx * 0.5f
    val tipCenter = Offset(tipX, tipY)
    drawCircle(color = color.copy(alpha = 0.35f), radius = heightPx * 0.95f, center = tipCenter)
    drawCircle(color = Color.White, radius = heightPx * 0.42f, center = tipCenter)
    drawCircle(color = NeonAmber.copy(alpha = 0.85f), radius = 2.2f * scale, center = tipCenter)
}

private fun DrawScope.drawHauntedCrystalMonolith(
    center: Offset,
    radiusPx: Float,
    scale: Float,
    elapsedSec: Float
) {
    val pulse = 1f + sin(elapsedSec * 4.5f + center.x * 0.02f) * 0.08f
    val r = radiusPx
    val diamond = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    val innerR = r * 0.48f * pulse
    val innerDiamond = Path().apply {
        moveTo(center.x, center.y - innerR)
        lineTo(center.x + innerR, center.y)
        lineTo(center.x, center.y + innerR)
        lineTo(center.x - innerR, center.y)
        close()
    }

    // Outer haunted violet mist aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonViolet.copy(alpha = 0.26f), Color.Transparent),
            center = center,
            radius = r * 1.65f
        ),
        radius = r * 1.65f,
        center = center
    )

    // Deep obsidian translucent prism
    drawPath(diamond, color = MidnightIndigo.copy(alpha = 0.92f))
    drawPath(
        diamond,
        brush = Brush.verticalGradient(
            colors = listOf(NeonCoral.copy(alpha = 0.32f), NeonViolet.copy(alpha = 0.38f)),
            startY = center.y - r,
            endY = center.y + r
        )
    )
    drawPath(diamond, color = NeonCoral.copy(alpha = 0.32f), style = Stroke(width = 10f * scale, join = StrokeJoin.Round))
    drawPath(diamond, color = NeonCoral, style = Stroke(width = 2.6f * scale, join = StrokeJoin.Round))

    // Glowing Haunted Core Rune inside crystal
    drawPath(innerDiamond, color = NeonMagenta.copy(alpha = 0.35f))
    drawPath(innerDiamond, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1.6f * scale, join = StrokeJoin.Round))
    drawCircle(color = NeonAmber, radius = 3.2f * scale, center = center)
}

private fun DrawScope.drawSpectralScytheSweeper(
    center: Offset,
    halfWidthPx: Float,
    halfThicknessPx: Float,
    scale: Float,
    elapsedSec: Float
) {
    // Tapered double-blade spectral scythe silhouette
    val blade = Path().apply {
        moveTo(center.x - halfWidthPx, center.y)
        quadraticTo(center.x, center.y - halfThicknessPx * 1.35f, center.x + halfWidthPx, center.y)
        quadraticTo(center.x, center.y + halfThicknessPx * 1.35f, center.x - halfWidthPx, center.y)
        close()
    }
    drawPath(
        path = blade,
        color = NeonAmber.copy(alpha = 0.24f),
        style = Stroke(width = 11f * scale, join = StrokeJoin.Round)
    )
    drawPath(
        path = blade,
        brush = Brush.horizontalGradient(
            colors = listOf(NeonViolet, NeonAmber, NeonViolet),
            startX = center.x - halfWidthPx,
            endX = center.x + halfWidthPx
        )
    )
    drawPath(
        path = blade,
        color = Color.White.copy(alpha = 0.72f),
        style = Stroke(width = 1.6f * scale, join = StrokeJoin.Round)
    )

    // Floating haunted sentinel eye orb at blade center
    val eyePulse = 1f + sin(elapsedSec * 7f) * 0.14f
    drawCircle(color = NeonAmber.copy(alpha = 0.3f), radius = halfThicknessPx * 1.7f * eyePulse, center = center)
    drawCircle(color = MidnightIndigo, radius = halfThicknessPx * 0.9f, center = center)
    drawCircle(color = NeonAmber, radius = halfThicknessPx * 0.45f, center = center)
}
