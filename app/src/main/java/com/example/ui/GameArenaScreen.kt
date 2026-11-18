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
import com.example.ui.theme.JetBrainsMonoFontFamily
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

        // Top Floating Telemetry HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score & Combo pill
                Surface(
                    color = DeepSurface.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = if (runConfig.mode == GameMode.SECTOR) {
                                    "SECTEUR ${runConfig.sectorIndex}"
                                } else {
                                    "SURVIE INFINIE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonMagenta
                            )
                            Text(
                                text = "${engine.score}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonCyan,
                                modifier = Modifier.testTag("hud_score_text")
                            )
                        }
                        if (engine.comboMultiplier > 1) {
                            Surface(
                                color = NeonAmber.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonAmber),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "x${engine.comboMultiplier}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Active Ghosts Badge + Audio + Pause Controls (48dp touch targets)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = DeepSurface.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${engine.activeGhosts.size} FANTÔMES",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonMagenta,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_mute_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = DeepSurface.copy(alpha = 0.82f),
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
                            containerColor = DeepSurface.copy(alpha = 0.82f),
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
                Spacer(modifier = Modifier.height(8.dp))
                val progress = ((-engine.shipY) / engine.targetDistance).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = NeonCyan,
                    trackColor = DeepSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Ready prompt at launch
        AnimatedVisibility(
            visible = currentPhase == RunPhase.READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 52.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                color = DeepSurface.copy(alpha = 0.86f),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOUCHEZ & MAINTENEZ POUR TOURNER",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Relâchez pour filer droit • Esquivez vos propres Trajets Fantômes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Pause Overlay
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
                    .widthIn(max = 420.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightIndigo.copy(alpha = 0.95f)),
                border = BorderStroke(1.5.dp, NeonCyan),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TRAJECTOIRE EN PAUSE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeonCyan
                    )
                    Button(
                        onClick = {
                            engine.resume()
                            currentPhase = engine.phase
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = VoidBlack)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.resume), style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onExitArena,
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

        // Crash or Sector Cleared Overlay
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
                    .widthIn(max = 460.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightIndigo.copy(alpha = 0.93f)),
                border = BorderStroke(1.5.dp, accent),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isCleared) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = if (isCleared) "SECTEUR ${runConfig.sectorIndex} CONQUIS !" else "SIGNAL ROMPU",
                        style = MaterialTheme.typography.headlineLarge,
                        color = accent,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isCleared) {
                            "Portail final franchi avec succès"
                        } else {
                            "${engine.crashReason.labelFr} — Votre fantôme rejoint le cimetière !"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (isNewRecord) {
                        Surface(
                            color = NeonAmber.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NeonAmber),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "NOUVEAU RECORD PERSONNEL !",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge("SCORE", "${engine.score}", NeonCyan)
                        StatBadge("DISTANCE", "${engine.distanceTraveled.toInt()}m", TextPrimary)
                        StatBadge("FRÔLEMENTS", "${engine.nearMissCount}", NeonAmber)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isCleared) {
                        Button(
                            onClick = { onRetry(GameMode.SECTOR, runConfig.sectorIndex + 1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
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
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
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

    // 1. Deep Synthwave Background + Distant Horizon Glow
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(VoidBlack, MidnightIndigo, VoidBlack)
        )
    )

    val leftWallX = originX - GameConstants.WORLD_HALF_WIDTH * scale
    val rightWallX = originX + GameConstants.WORLD_HALF_WIDTH * scale

    // Corridor dark floor
    drawRect(
        color = Color(0xFF0A0C1F),
        topLeft = Offset(leftWallX, 0f),
        size = Size(rightWallX - leftWallX, h)
    )

    // 2. Scrolling Neon Grid inside the corridor
    val gridSpacing = 120f
    val topWorldY = engine.shipY - (shipScreenY / scale) - 120f
    val botWorldY = engine.shipY + ((h - shipScreenY) / scale) + 120f
    val firstGridY = (topWorldY / gridSpacing).toInt() * gridSpacing
    var gy = firstGridY
    while (gy <= botWorldY) {
        val sy = shipScreenY + (gy - engine.shipY) * scale
        drawLine(
            color = NeonViolet.copy(alpha = 0.14f),
            start = Offset(leftWallX, sy),
            end = Offset(rightWallX, sy),
            strokeWidth = 1.2f
        )
        gy += gridSpacing
    }
    for (gx in -200..200 step 100) {
        val sx = originX + gx.toFloat() * scale
        drawLine(
            color = NeonCyan.copy(alpha = 0.08f),
            start = Offset(sx, 0f),
            end = Offset(sx, h),
            strokeWidth = 1f
        )
    }

    // 3. Corridor Laser Boundary Rails
    drawLine(
        color = NeonMagenta.copy(alpha = 0.28f),
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 10f * scale
    )
    drawLine(
        color = NeonMagenta,
        start = Offset(leftWallX, 0f),
        end = Offset(leftWallX, h),
        strokeWidth = 2.6f * scale
    )
    drawLine(
        color = NeonMagenta.copy(alpha = 0.28f),
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 10f * scale
    )
    drawLine(
        color = NeonMagenta,
        start = Offset(rightWallX, 0f),
        end = Offset(rightWallX, h),
        strokeWidth = 2.6f * scale
    )

    // 4. Sector Finish Portal (in SECTOR mode)
    if (engine.mode == GameMode.SECTOR) {
        val portalY = -engine.targetDistance
        if (portalY in topWorldY..botWorldY) {
            val pScreenY = shipScreenY + (portalY - engine.shipY) * scale
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, NeonLime.copy(alpha = 0.35f), Color.Transparent),
                    startY = pScreenY - 26f * scale,
                    endY = pScreenY + 26f * scale
                ),
                topLeft = Offset(leftWallX, pScreenY - 26f * scale),
                size = Size(rightWallX - leftWallX, 52f * scale)
            )
            drawLine(
                color = NeonLime,
                start = Offset(leftWallX, pScreenY),
                end = Offset(rightWallX, pScreenY),
                strokeWidth = 4f * scale
            )
        }
    }

    // 5. Ghost Lines & Crash Markers ("Cimetière d'erreurs")
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
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.10f else 0.16f),
                    style = Stroke(width = 13f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = gColor.copy(alpha = if (ghost.isSpectre) 0.42f else 0.62f),
                    style = Stroke(
                        width = 3.2f * scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (ghost.isSpectre) PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) else null
                    )
                )
            }
        }

        // Animated spectral cursor racing along the ghost path
        val cursor = ghost.cursorAt(engine.distanceTraveled)
        if (cursor != null && cursor.y in topWorldY..botWorldY) {
            val sc = toScreen(cursor.x, cursor.y)
            drawCircle(
                color = gColor.copy(alpha = 0.25f),
                radius = 12f * scale,
                center = sc
            )
            drawCircle(
                color = gColor.copy(alpha = 0.85f),
                radius = 5f * scale,
                center = sc
            )
        }

        // Crash X marker at ghost death spot
        val cp = ghost.crashPoint
        if (cp != null && cp.y in topWorldY..botWorldY) {
            val sc = toScreen(cp.x, cp.y)
            val r = 10f * scale
            drawCircle(color = gColor.copy(alpha = 0.20f), radius = 15f * scale, center = sc)
            drawLine(gColor, Offset(sc.x - r, sc.y - r), Offset(sc.x + r, sc.y + r), strokeWidth = 3f * scale, cap = StrokeCap.Round)
            drawLine(gColor, Offset(sc.x + r, sc.y - r), Offset(sc.x - r, sc.y + r), strokeWidth = 3f * scale, cap = StrokeCap.Round)
        }
    }

    // 6. Procedural Obstacles
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
                    drawNeonBar(pLeft, wPx, hPx, NeonCoral)
                }
                if (rightStart < GameConstants.WORLD_HALF_WIDTH) {
                    val pRight = toScreen(rightStart, topY)
                    val wPx = (GameConstants.WORLD_HALF_WIDTH - rightStart) * scale
                    drawNeonBar(pRight, wPx, hPx, NeonCoral)
                }
            }
            is Obstacle.MovingSweeper -> {
                val cx = obs.currentCenterX(engine.elapsedSec)
                val pStart = toScreen(cx - obs.halfLength, obs.y - obs.thickness * 0.5f)
                drawNeonBar(pStart, obs.halfLength * 2f * scale, obs.thickness * scale, NeonAmber)
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
                drawPath(diamond, color = NeonViolet.copy(alpha = 0.25f))
                drawPath(diamond, color = NeonCoral.copy(alpha = 0.35f), style = Stroke(width = 10f * scale))
                drawPath(diamond, color = NeonCoral, style = Stroke(width = 3f * scale))
            }
        }
    }

    // 7. Collectible Energy Orbs
    val pulse = 1f + sin(engine.elapsedSec * 7f) * 0.15f
    for (orb in engine.orbs) {
        if (engine.collectedOrbIds.contains(orb.id)) continue
        if (orb.y < topWorldY || orb.y > botWorldY) continue
        val center = toScreen(orb.x, orb.y)
        drawCircle(
            color = NeonCyan.copy(alpha = 0.24f),
            radius = orb.radius * 1.45f * pulse * scale,
            center = center
        )
        drawCircle(
            color = NeonCyan,
            radius = orb.radius * 0.72f * pulse * scale,
            center = center,
            style = Stroke(width = 2.8f * scale)
        )
        drawCircle(
            color = Color.White,
            radius = orb.radius * 0.32f * scale,
            center = center
        )
    }

    // 8. Player Trail
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
                color = NeonCyan.copy(alpha = 0.28f),
                style = Stroke(width = 16f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = playerPath,
                color = NeonCyan,
                style = Stroke(width = 4.6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
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

    // 10. Player Geometric Neon Dart + Directional Preview Arc
    val shipCenter = toScreen(engine.shipX, engine.shipY)
    if (engine.phase != RunPhase.CRASHED) {
        // Next turn cue arc in ALTERNATING mode
        if (engine.controlMode == ControlMode.ALTERNATING && engine.activeTurnSign == 0f) {
            val cueAngle = engine.headingRad + engine.nextTurnSign * 0.65f
            val cueEnd = Offset(
                shipCenter.x + cos(cueAngle) * 28f * scale,
                shipCenter.y + sin(cueAngle) * 28f * scale
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.45f),
                start = shipCenter,
                end = cueEnd,
                strokeWidth = 2f * scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }

        val deg = Math.toDegrees(engine.headingRad.toDouble()).toFloat() + 90f
        rotate(degrees = deg, pivot = shipCenter) {
            val r = 15f * scale
            val dart = Path().apply {
                moveTo(shipCenter.x, shipCenter.y - r * 1.25f)
                lineTo(shipCenter.x + r * 0.85f, shipCenter.y + r * 0.9f)
                lineTo(shipCenter.x, shipCenter.y + r * 0.45f)
                lineTo(shipCenter.x - r * 0.85f, shipCenter.y + r * 0.9f)
                close()
            }
            drawPath(dart, color = NeonCyan.copy(alpha = 0.35f), style = Stroke(width = 8f * scale))
            drawPath(dart, color = NeonCyan)
            drawPath(dart, color = Color.White, style = Stroke(width = 2f * scale))
        }
    }

    // 11. Floating Near-Miss / Orb Popups
    for (pop in engine.popups) {
        val alpha = (pop.life / pop.maxLife).coerceIn(0f, 1f)
        val pos = toScreen(pop.x, pop.y)
        val pColor = if (pop.colorIndex == 0) NeonCyan else NeonAmber
        drawText(
            textMeasurer = textMeasurer,
            text = pop.text,
            topLeft = Offset(pos.x - 48f, pos.y),
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = pColor.copy(alpha = alpha)
            )
        )
    }

    // 12. Crash / Clear Chromatic Flash
    if (engine.flashAlpha > 0.01f) {
        val flashColor = if (engine.phase == RunPhase.SECTOR_CLEARED) NeonCyan else NeonMagenta
        drawRect(color = flashColor.copy(alpha = engine.flashAlpha * 0.35f))
    }
}

private fun DrawScope.drawNeonBar(topLeft: Offset, widthPx: Float, heightPx: Float, color: Color) {
    drawRect(
        color = color.copy(alpha = 0.25f),
        topLeft = Offset(topLeft.x, topLeft.y - heightPx * 0.35f),
        size = Size(widthPx, heightPx * 1.7f)
    )
    drawRect(
        color = color,
        topLeft = topLeft,
        size = Size(widthPx, heightPx)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.7f),
        topLeft = Offset(topLeft.x, topLeft.y + heightPx * 0.3f),
        size = Size(widthPx, heightPx * 0.4f)
    )
}
