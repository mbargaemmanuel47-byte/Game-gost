package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameMode
import com.example.data.GhostRecord
import com.example.data.RunLog
import com.example.ui.theme.GlassSurface
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

private fun graveyardColor(idx: Long): Color = when ((idx % 4L).toInt()) {
    0 -> NeonMagenta
    1 -> NeonViolet
    2 -> NeonAmber
    else -> NeonCoral
}

@Composable
fun GraveyardTabContent(
    ghosts: List<GhostRecord>,
    recentRuns: List<RunLog>,
    onPurgeAllGhosts: () -> Unit,
    onReplaySector: (GameMode, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CIMETIÈRE D'ERREURS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = NeonMagenta
                        )
                        Text(
                            text = "${ghosts.size} trajectoires fantômes actives",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (ghosts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onPurgeAllGhosts,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("purge_ghosts_button"),
                            border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.purge_ghosts), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (ghosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = BorderStroke(1.dp, OutlineGlow.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Aucun fantôme enregistré",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Chaque chute laisse votre trajet en spectre lumineux sur le terrain. Lancez une partie pour peupler le cimetière.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(ghosts, key = { it.id }) { ghost ->
                    GhostTrajectoryCard(
                        ghost = ghost,
                        onReplay = {
                            if (ghost.sectorIndex <= 0L) {
                                onReplaySector(GameMode.ENDLESS, 0)
                            } else {
                                onReplaySector(GameMode.SECTOR, ghost.sectorIndex.toInt())
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = "DERNIERS VOLS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }
            }

            if (recentRuns.isEmpty()) {
                item {
                    Text(
                        text = "Aucun vol enregistré pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(recentRuns, key = { it.id }) { run ->
                    RunHistoryCard(run = run)
                }
            }
        }
    }
}

@Composable
private fun GhostTrajectoryCard(
    ghost: GhostRecord,
    onReplay: () -> Unit
) {
    val points = remember(ghost.packedPoints) { ghost.decodePoints() }
    val trailColor = graveyardColor(ghost.colorIndex)

    Card(
        onClick = onReplay,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ghost_card_${ghost.id}"),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, trailColor.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Vector mini-map preview of the ghost trajectory
            Surface(
                color = VoidBlack,
                border = BorderStroke(1.dp, OutlineGlow),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(width = 64.dp, height = 76.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    if (points.size >= 2) {
                        val minY = points.minOf { it.y }
                        val maxY = points.maxOf { it.y }
                        val spanY = (maxY - minY).coerceAtLeast(100f)
                        val path = Path()
                        points.forEachIndexed { idx, p ->
                            val nx = ((p.x + 300f) / 600f).coerceIn(0f, 1f) * size.width
                            val ny = ((p.y - minY) / spanY).coerceIn(0f, 1f) * size.height
                            if (idx == 0) path.moveTo(nx, ny) else path.lineTo(nx, ny)
                        }
                        drawPath(
                            path = path,
                            color = trailColor,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        val last = points.last()
                        val lx = ((last.x + 300f) / 600f).coerceIn(0f, 1f) * size.width
                        val ly = ((last.y - minY) / spanY).coerceIn(0f, 1f) * size.height
                        drawCircle(color = NeonCoral, radius = 4f, center = Offset(lx, ly))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (ghost.sectorIndex <= 0L) "SURVIE INFINIE" else "SECTEUR ${ghost.sectorIndex}",
                    style = MaterialTheme.typography.labelLarge,
                    color = trailColor
                )
                Text(
                    text = "Score ${ghost.score} • Distance ${ghost.distance}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Appuyez pour affronter ce fantôme",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RunHistoryCard(run: RunLog) {
    val accent = if (run.cleared) NeonLime else NeonMagenta
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, OutlineGlow.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (run.cleared) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = NeonLime, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = if (run.mode == GameMode.ENDLESS.name) "Survie Infinie" else "Secteur ${run.sectorIndex}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${run.nearMisses} frôlements",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${run.score}",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (run.cleared) "CONQUIS" else "CRASH",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}
