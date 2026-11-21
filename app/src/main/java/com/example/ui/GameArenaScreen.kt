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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

    // Merge saved Firestore ghosts with any just-finished local ghost for instant zero-wait retry visibility
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
                                triggerHaptic(context, hapticsOn, 18L, 120)
                            }
                            EngineEvent.NearMiss -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.NEAR_MISS)
                                triggerHaptic(context, hapticsOn, 24L, 160)
                            }
                            EngineEvent.Crashed -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.CRASH)
                                triggerHaptic(context, hapticsOn, 110L, 255)
                            }
                            EngineEvent.SectorCleared -> {
                                audioEngine.triggerSfx(SynthwaveAudioEngine.SfxType.CLEAR)
                                triggerHaptic(context, hapticsOn, 65L, 210)
                            }
                        }
                    }

                    audioEngine.isGameplayActive = (engine.phase == RunPhase.RUNNING)
                    audioEngine.isTurning = (engine.activeTurnSign != 0f)
                    audioEngine.intensity = ((engine.currentSpeed - 330f) / 230f).coerceIn(0.2f, 1f)

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
                        triggerHaptic(context, hapticsOn, 12L, 85)

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
            // Read frameTick to bind Canvas invalidation to vsync
            val tick = frameTick
            drawArenaFrame(engine, tick, textMeasurer)
        }

        // Minimalist Floating Top HUD
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
                // Airy Floating Score & Combo
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

                // Minimalist Ghost Badge + Circular Audio & Pause Controls (48dp touch targets)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = GlassSurface,
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
                        .height(4.dp)
                        .clip(CircleShape),
                    color = NeonCyan,
                    trackColor = GlassSurface
                )
            }
        }

        // Minimalist Ready Prompt Pill
        AnimatedVisibility(
            visible = currentPhase == RunPhase.READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                color = GlassSurface,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.55f)),
                shape = CircleShape
            ) {
                Text(
                    text = "Maintiens pour tourner • Relâche pour filer",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                )
            }
        }

        // Minimalist Pause Overlay
        AnimatedVisibility(
            visible = currentPhase == RunPhase.PAUSED,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 380.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightIndigo.copy(alpha = 0.94f)),
                border = BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.65f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "En pause",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NeonCyan
                    )
                    Button(
                        onClick = {
                            engine.resume()
                            currentPhase = engine.phase
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
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

        // Minimalist Crash / Victory Overlay
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightIndigo.copy(alpha = 0.94f)),
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.8f)),
                shape = MaterialTheme.shapes.large
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
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Text(
                        text = if (isCleared) "Secteur ${runConfig.sectorIndex} conquis !" else "Signal rompu !",
                        style = MaterialTheme.typography.headlineLarge,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isCleared) {
                            "Portail franchi avec style"
                        } else {
                            engine.crashReason.labelFr
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (isNewRecord) {
                        Surface(
                            color = NeonAmber.copy(alpha = 0.18f),
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
                        StatBadge("Distance", "${engine.distanceTraveled.toInt()}m", TextPrimary)
                        StatBadge("Frôlements", "${engine.nearMissCount}", NeonAmber)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (isCleared) {
                        Button(
                            onClick = { onRetry(GameMode.SECTOR, runConfig.sectorIndex + 1) },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("next_sector_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.next_sector), style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Button(
                        onClick = { onRetry(runConfig.mode, runConfig.sectorIndex) },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("retry_instant_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCleared) DeepSurface else NeonMagenta,
                            contentColor = if (isCleared) TextPrimary else VoidBlack
                        )
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
    val shipScreenY = h * 0.72f + shakeY

    fun toScreen(wx: Float, wy: Float): Offset =
        Offset(originX + wx * scale, shipScreenY + (wy - engine.shipY) * scale)

    // 1. Minimalist Deep Space Gradient & Subtle Nebula Bloom
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(VoidBlack, MidnightIndigo, VoidBlack)
        )
    )

    val leftWallX = originX - GameConstants.WORLD_HALF_WIDTH * scale
    val rightWallX = originX + GameConstants.WORLD_HALF_WIDTH * scale

    // Subtle atmospheric radial spotlight centered ahead of the ship
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonViolet.copy(alpha = 0.09f), Color.Transparent),
            center = Offset(originX, shipScreenY - 120f * scale),
            radius = arenaPixelWidth * 0.85f
        ),
        radius = arenaPixelWidth * 0.85f,
        center = Offset(originX, shipScreenY - 120f * scale)
    )

    // 2. Clean Minimalist Starlight & Subtle Horizon Marks (replaces noisy grid)
    val starStep = 140f
    val topWorldY = engine.shipY - (shipScreenY / scale) - 140f
    val botWorldY = engine.shipY + ((h - shipScreenY) / scale) + 140f
    val firstStarRow = (topWorldY / starStep).toInt()
    val lastStarRow = (botWorldY / starStep).toInt()
    for (row in firstStarRow..lastStarRow) {
        val wy = row * starStep
        val sy = shipScreenY + (wy - engine.shipY) * scale
        // Two deterministic drifting starlight dots per band
        val hash1 = ((row * 73856093) xor 19349663)
        val wx1 = (((hash1 and 0xFF) / 255f) * 2f - 1f) * (GameConstants.WORLD_HALF_WIDTH - 24f)
        val sx1 = originX + wx1 * scale
        val starAlpha = if (row % 2 == 0) 0.28f else 0.16f
        val starColor = if (row % 3 == 0) NeonCyan else NeonViolet
        drawCircle(
            color = starColor.copy(alpha = starAlpha),
            radius = if (row % 4 == 0) 2.6f * scale else 1.6f * scale,
            center = Offset(sx1, sy)
        )
        if (row % 2 == 0) {
            drawLine(
                color = Color.White.copy(alpha = 0.045f),
                start = Offset(leftWallX + 12f * scale, sy),
                end = Offset(rightWallX - 12f * scale, sy),
                strokeWidth = 1f
            )
        }
    }

    // 3. Sleek Luminous Corridor Laser Rails
    drawLine(
        color = NeonMagenta.copy(alpha = 0.22f),
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 12f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.9f),
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 2.4f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.22f),
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 12f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.9f),
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 2.4f * scale
    )

    // 4. Sector Finish Portal (in SECTOR mode)
    if (engine.mode == GameMode.SECTOR) {
        val portalY = -engine.targetDistance
        if (portalY in topWorldY..botWorldY) {
            val pScreenY = shipScreenY + (portalY - engine.shipY) * scale
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, NeonLime.copy(alpha = 0.35f), Color.Transparent),
                    startY = pScreenY - 30f * scale,
                    endY = pScreenY + 30f * scale
                ),
                topLeft = Offset(leftWallX, pScreenY - 30f * scale),
                size = Size(rightWallX - leftWallX, 60f * scale)
            )
            drawLine(
                color = NeonLime,
                start = Offset(leftWallX, pScreenY),
                end = Offset(rightWallX, pScreenY),
                strokeWidth = 4.5f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    // 5. Charming Spectral Ghost Lines & Phantom Orbs ("Cimetière d'erreurs")
    for (ghost in engine.activeGhosts) {
        val gColor = ghostPaletteColor(ghost.colorIndex)
        val pts = ghost.points
        if (pts.size >= 2) {
            val path = Path()
            var started = false
            for (pt in pts) {
                if (pt.y < topWorldY - 200f || pt.y > botWorldY + 200f) {
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
                drawPath(
                    path = path,
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.08f else 0.16f),
                    style = Stroke(width = 14f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.42f else 0.65f),
                    style = Stroke(
                        width = 3.4f * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (ghost.isSpectre) PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) else null
                    )
                )
            }
        }

        // Cute glowing phantom orb gliding smoothly along the ghost path
        val cursor = ghost.cursorAt(engine.distanceTraveled)
        if (cursor != null && cursor.y in topWorldY..botWorldY) {
            val sc = toScreen(cursor.x, cursor.y)
            drawCircle(
                color = gColor.copy(alpha = 0.22f),
                radius = 15f * scale,
                center = sc
            )
            drawCircle(
                color = gColor,
                radius = 7f * scale,
                center = sc
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3f * scale,
                center = sc
            )
        }

        // Soft pulsing ring marker at ghost death spot
        val cp = ghost.crashPoint
        if (cp != null && cp.y in topWorldY..botWorldY) {
            val sc = toScreen(cp.x, cp.y)
            val pulseRing = 12f + sin(engine.elapsedSec * 5f + ghost.colorIndex) * 2.5f
            drawCircle(
                color = gColor.copy(alpha = 0.16f),
                radius = pulseRing * scale,
                center = sc
            )
            drawCircle(
                color = gColor.copy(alpha = 0.85f),
                radius = 7.5f * scale,
                center = sc,
                style = Stroke(width = 2.4f * scale)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 2.4f * scale,
                center = sc
            )
        }
    }

    // 6. Sleek Rounded Neon Capsule Obstacles & Crystalline Prisms
    for (obs in engine.obstacles) {
        if (obs.y < topWorldY - 120f || obs.y > botWorldY + 120f) continue
        when (obs) {
            is Obstacle.BarrierGate -> {
                val leftEnd = obs.gapCenterX - obs.gapHalfWidth
                val rightStart = obs.gapCenterX + obs.gapHalfWidth
                val topY = obs.y - obs.thickness * 0.5f
                val hPx = obs.thickness * scale
                if (leftEnd > -GameConstants.WORLD_HALF_WIDTH) {
                    val pLeft = toScreen(-GameConstants.WORLD_HALF_WIDTH, topY)
                    val wPx = (leftEnd + GameConstants.WORLD_HALF_WIDTH) * scale
                    drawNeonCapsule(pLeft, wPx, hPx, NeonCoral)
                }
                if (rightStart < GameConstants.WORLD_HALF_WIDTH) {
                    val pRight = toScreen(rightStart, topY)
                    val wPx = (GameConstants.WORLD_HALF_WIDTH - rightStart) * scale
                    drawNeonCapsule(pRight, wPx, hPx, NeonCoral)
                }
            }
            is Obstacle.MovingSweeper -> {
                val cx = obs.currentCenterX(engine.elapsedSec)
                val pStart = toScreen(cx - obs.halfLength, obs.y - obs.thickness * 0.5f)
                drawNeonCapsule(pStart, obs.halfLength * 2f * scale, obs.thickness * scale, NeonAmber)
            }
            is Obstacle.DiamondMonolith -> {
                val center = toScreen(obs.centerX, obs.y)
                val r = obs.radius * scale
                val diamond = Path().apply {
                    moveTo(center.x, center.y - r)
                    lineTo(center.x + r, center.y)
                    lineTo(center.x, center.y + r)
                    lineTo(center.x - r, center.y)
                    close()
                }
                val innerR = r * 0.52f
                val innerDiamond = Path().apply {
                    moveTo(center.x, center.y - innerR)
                    lineTo(center.x + innerR, center.y)
                    lineTo(center.x, center.y + innerR)
                    lineTo(center.x - innerR, center.y)
                    close()
                }
                drawPath(diamond, color = NeonViolet.copy(alpha = 0.24f))
                drawPath(diamond, color = NeonCoral.copy(alpha = 0.35f), style = Stroke(width = 11f * scale, join = StrokeJoin.Round))
                drawPath(diamond, color = NeonCoral, style = Stroke(width = 3f * scale, join = StrokeJoin.Round))
                drawPath(innerDiamond, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.8f * scale, join = StrokeJoin.Round))
            }
        }
    }

    // 7. Collectible Energy Orbs with Orbiting Ring
    val pulse = 1f + sin(engine.elapsedSec * 7f) * 0.14f
    for (orb in engine.orbs) {
        if (engine.collectedOrbIds.contains(orb.id)) continue
        if (orb.y < topWorldY || orb.y > botWorldY) continue
        val center = toScreen(orb.x, orb.y)
        drawCircle(
            color = NeonCyan.copy(alpha = 0.20f),
            radius = orb.radius * 1.65f * pulse * scale,
            center = center
        )
        drawCircle(
            color = NeonCyan.copy(alpha = 0.85f),
            radius = orb.radius * 0.82f * pulse * scale,
            center = center,
            style = Stroke(width = 2.6f * scale)
        )
        drawCircle(
            color = Color.White,
            radius = orb.radius * 0.38f * scale,
            center = center
        )
    }

    // 8. Luminous Player Ribbon Trail
    if (engine.playerTrail.size >= 2) {
        val playerPath = Path()
        var started = false
        for (pt in engine.playerTrail) {
            if (pt.y > botWorldY + 120f) continue
            val sp = toScreen(pt.x, pt.y)
            if (!started) {
                playerPath.moveTo(sp.x, sp.y)
                started = true
            } else {
                playerPath.lineTo(sp.x, sp.y)
            }
        }
        val shipPos = toScreen(engine.shipX, engine.shipY)
        if (started) {
            playerPath.lineTo(shipPos.x, shipPos.y)
            drawPath(
                path = playerPath,
                color = NeonCyan.copy(alpha = 0.25f),
                style = Stroke(width = 18f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = playerPath,
                color = NeonCyan,
                style = Stroke(width = 5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = playerPath,
                color = Color.White.copy(alpha = 0.75f),
                style = Stroke(width = 1.8f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    // 9. Particles
    for (p in engine.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val pColor = when (p.colorIndex) {
            0 -> NeonCyan
            1 -> NeonMagenta
            else -> NeonAmber
        }
        drawCircle(
            color = pColor.copy(alpha = alpha),
            radius = p.size * scale * alpha,
            center = toScreen(p.x, p.y)
        )
    }

    // 10. Sleek Luminous Origami Chevron Ship + Turn Halo
    val shipCenter = toScreen(engine.shipX, engine.shipY)
    if (engine.phase != RunPhase.CRASHED) {
        if (engine.activeTurnSign != 0f) {
            drawCircle(
                color = NeonMagenta.copy(alpha = 0.25f),
                radius = 24f * scale,
                center = shipCenter,
                style = Stroke(width = 2.2f * scale)
            )
        } else if (engine.controlMode == ControlMode.ALTERNATING) {
            val cueAngle = engine.headingRad + engine.nextTurnSign * 0.65f
            val cueEnd = Offset(
                shipCenter.x + cos(cueAngle) * 26f * scale,
                shipCenter.y + sin(cueAngle) * 26f * scale
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.35f),
                start = shipCenter,
                end = cueEnd,
                strokeWidth = 2f * scale,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        val deg = Math.toDegrees(engine.headingRad.toDouble()).toFloat() + 90f
        rotate(degrees = deg, pivot = shipCenter) {
            val r = 15.5f * scale
            val dart = Path().apply {
                moveTo(shipCenter.x, shipCenter.y - r * 1.28f)
                lineTo(shipCenter.x + r * 0.88f, shipCenter.y + r * 0.86f)
                lineTo(shipCenter.x, shipCenter.y + r * 0.42f)
                lineTo(shipCenter.x - r * 0.88f, shipCenter.y + r * 0.86f)
                close()
            }
            drawPath(dart, color = NeonCyan.copy(alpha = 0.32f), style = Stroke(width = 10f * scale, join = StrokeJoin.Round))
            drawPath(dart, color = NeonCyan)
            drawPath(dart, color = Color.White, style = Stroke(width = 2.2f * scale, join = StrokeJoin.Round))
            drawCircle(color = Color.White, radius = 3.2f * scale, center = shipCenter)
        }
    }

    // 11. Playful Floating Popups in Fredoka
    for (pop in engine.popups) {
        val alpha = (pop.life / pop.maxLife).coerceIn(0f, 1f)
        val pos = toScreen(pop.x, pop.y)
        val pColor = if (pop.colorIndex == 0) NeonCyan else NeonAmber
        drawText(
            textMeasurer = textMeasurer,
            text = pop.text,
            topLeft = Offset(pos.x - 48f, pos.y),
            style = TextStyle(
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = pColor.copy(alpha = alpha)
            )
        )
    }

    // 12. Crash / Clear Chromatic Flash
    if (engine.flashAlpha > 0.01f) {
        val flashColor = if (engine.phase == RunPhase.SECTOR_CLEARED) NeonCyan else NeonMagenta
        drawRect(color = flashColor.copy(alpha = engine.flashAlpha * 0.32f))
    }
}

private fun DrawScope.drawNeonCapsule(topLeft: Offset, widthPx: Float, heightPx: Float, color: Color) {
    val radius = CornerRadius(heightPx * 0.5f, heightPx * 0.5f)
    val glowRadius = CornerRadius(heightPx * 0.85f, heightPx * 0.85f)
    drawRoundRect(
        color = color.copy(alpha = 0.24f),
        topLeft = Offset(topLeft.x - 2f, topLeft.y - heightPx * 0.35f),
        size = Size(widthPx + 4f, heightPx * 1.7f),
        cornerRadius = glowRadius
    )
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(widthPx, heightPx),
        cornerRadius = radius
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.72f),
        topLeft = Offset(topLeft.x + heightPx * 0.2f, topLeft.y + heightPx * 0.3f),
        size = Size((widthPx - heightPx * 0.4f).coerceAtLeast(2f), heightPx * 0.38f),
        cornerRadius = CornerRadius(heightPx * 0.2f, heightPx * 0.2f)
    )
}
