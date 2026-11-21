package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.SoftGlassCard
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
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Cimetière d'erreurs",
                            style = MaterialTheme.typography.headlineLarge,
                            color = NeonMagenta
                        )
                        Text(
                            text = "${ghosts.size} tracés fantômes enregistrés",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (ghosts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onPurgeAllGhosts,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("purge_ghosts_button"),
                            border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.purge_ghosts), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (ghosts.isEmpty()) {
                item {
                    SoftGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = NeonMagenta.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = "Aucun fantôme pour le moment",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Chaque envol laisse un écho lumineux de votre trajectoire. Jouez une partie pour les voir apparaître ici !",
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
                        text = "Derniers vols",
                        style = MaterialTheme.typography.titleLarge,
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

    SoftGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onReplay)
            .testTag("ghost_card_${ghost.id}"),
        shape = MaterialTheme.shapes.medium,
        accentBorder = trailColor.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = VoidBlack.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, OutlineGlow),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(width = 64.dp, height = 76.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
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
                            color = trailColor.copy(alpha = 0.3f),
                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = trailColor,
                            style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        val last = points.last()
                        val lx = ((last.x + 300f) / 600f).coerceIn(0f, 1f) * size.width
                        val ly = ((last.y - minY) / spanY).coerceIn(0f, 1f) * size.height
                        drawCircle(color = NeonCoral, radius = 4.2f, center = Offset(lx, ly))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (ghost.sectorIndex <= 0L) "Survie Infinie" else "Secteur ${ghost.sectorIndex}",
                    style = MaterialTheme.typography.titleMedium,
                    color = trailColor
                )
                Text(
                    text = "Score ${ghost.score} • ${ghost.distance} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Toucher pour défier ce fantôme",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RunHistoryCard(run: RunLog) {
    val accent = if (run.cleared) NeonLime else NeonMagenta
    SoftGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        accentBorder = OutlineGlow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (run.cleared) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = NeonLime, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = if (run.mode == GameMode.ENDLESS.name) "Survie Infinie" else "Secteur ${run.sectorIndex}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${run.nearMisses} frôlements",
                        style = MaterialTheme.typography.labelMedium,
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
                    text = if (run.cleared) "Conquis" else "Terminé",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
    }
}
