package com.example.game

import com.example.data.Vec2
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object GameConstants {
    const val WORLD_HALF_WIDTH = 300f
    const val SHIP_RADIUS = 9f
    const val GHOST_TRAIL_RADIUS = 5.5f
    const val GHOST_CRASH_RADIUS = 12f
    const val NEAR_MISS_RADIUS = 32f
    const val SPAWN_GRACE_DISTANCE = 150f
    const val ORB_MAGNET_RADIUS = 62f
}

sealed interface Obstacle {
    val id: Int
    val y: Float

    data class BarrierGate(
        override val id: Int,
        override val y: Float,
        val gapCenterX: Float,
        val gapHalfWidth: Float,
        val thickness: Float = 16f
    ) : Obstacle

    data class DiamondMonolith(
        override val id: Int,
        override val y: Float,
        val centerX: Float,
        val radius: Float
    ) : Obstacle

    data class MovingSweeper(
        override val id: Int,
        override val y: Float,
        val baseCenterX: Float,
        val amplitude: Float,
        val speedRadPerSec: Float,
        val phaseOffset: Float,
        val halfLength: Float,
        val thickness: Float = 14f
    ) : Obstacle {
        fun currentCenterX(elapsedSec: Float): Float =
            baseCenterX + sin(elapsedSec * speedRadPerSec + phaseOffset) * amplitude
    }
}

data class EnergyOrb(
    val id: Int,
    var x: Float,
    var y: Float,
    val radius: Float = 16f
)

data class GeneratedChunk(
    val obstacles: List<Obstacle>,
    val orbs: List<EnergyOrb>,
    val guidePoint: Vec2
)

data class SpectrePath(
    val name: String,
    val colorIndex: Int,
    val points: List<Vec2>,
    val crashPoint: Vec2?
)

class ProceduralLevelGenerator(seed: Long) {

    private val rng = Random(seed)
    private var nextId = 1
    private var lastGapX = 0f

    fun generateChunk(chunkIndex: Int, yPos: Float, difficulty: Float): GeneratedChunk {
        val obstacles = ArrayList<Obstacle>(2)
        val orbs = ArrayList<EnergyOrb>(2)

        val d = difficulty.coerceIn(0f, 1f)
        val gapHalf = (122f - d * 36f).coerceIn(82f, 124f)
        val maxShift = 155f
        val desiredX = (lastGapX + rng.nextFloat() * 230f - 115f).coerceIn(-maxShift, maxShift)
        lastGapX = desiredX

        when (chunkIndex % 4) {
            0 -> {
                // Welcoming neon laser gate with guiding orb right near the safe corridor
                obstacles.add(
                    Obstacle.BarrierGate(
                        id = nextId++,
                        y = yPos,
                        gapCenterX = desiredX,
                        gapHalfWidth = gapHalf
                    )
                )
                val gentleOffset = (gapHalf * 0.25f) * (if (rng.nextBoolean()) 1f else -1f)
                orbs.add(EnergyOrb(id = nextId++, x = desiredX + gentleOffset, y = yPos - 18f))
            }
            1 -> {
                // Diamond slalom with crystal-clear safe lane orb
                val centerDiamond = rng.nextBoolean()
                if (centerDiamond) {
                    obstacles.add(
                        Obstacle.DiamondMonolith(
                            id = nextId++,
                            y = yPos,
                            centerX = (desiredX * 0.45f).coerceIn(-105f, 105f),
                            radius = 33f + d * 12f
                        )
                    )
                    val side = if (desiredX >= 0f) -138f else 138f
                    orbs.add(EnergyOrb(id = nextId++, x = side, y = yPos))
                } else {
                    obstacles.add(
                        Obstacle.DiamondMonolith(
                            id = nextId++,
                            y = yPos,
                            centerX = -140f,
                            radius = 31f + d * 10f
                        )
                    )
                    obstacles.add(
                        Obstacle.DiamondMonolith(
                            id = nextId++,
                            y = yPos,
                            centerX = 140f,
                            radius = 31f + d * 10f
                        )
                    )
                    orbs.add(EnergyOrb(id = nextId++, x = 0f, y = yPos))
                }
            }
            2 -> {
                // Smooth oscillating sweeper bar
                obstacles.add(
                    Obstacle.MovingSweeper(
                        id = nextId++,
                        y = yPos,
                        baseCenterX = 0f,
                        amplitude = 140f,
                        speedRadPerSec = 1.4f + d * 1.35f,
                        phaseOffset = rng.nextFloat() * (2f * PI.toFloat()),
                        halfLength = 68f + d * 18f
                    )
                )
                orbs.add(EnergyOrb(id = nextId++, x = desiredX, y = yPos - 75f))
            }
            else -> {
                // Offset gate + friendly orb ahead
                obstacles.add(
                    Obstacle.BarrierGate(
                        id = nextId++,
                        y = yPos,
                        gapCenterX = desiredX,
                        gapHalfWidth = gapHalf + 10f
                    )
                )
                val monolithX = if (desiredX > 0) -185f else 185f
                obstacles.add(
                    Obstacle.DiamondMonolith(
                        id = nextId++,
                        y = yPos - 140f,
                        centerX = monolithX * 0.52f,
                        radius = 28f + d * 8f
                    )
                )
                orbs.add(EnergyOrb(id = nextId++, x = desiredX * 0.72f, y = yPos - 110f))
            }
        }

        val guide = Vec2(desiredX, yPos)
        return GeneratedChunk(obstacles = obstacles, orbs = orbs, guidePoint = guide)
    }

    companion object {
        fun sectorTargetDistance(sectorIndex: Int): Float =
            (1800f + (sectorIndex - 1).coerceAtLeast(0) * 420f).coerceAtMost(5600f)

        /**
         * Generates deterministic pre-recorded AI spectre runs for a given seed so even a brand-new
         * sector has luminous spectral trajectories to race and dodge.
         */
        fun buildPreRecordedSpectres(
            seed: Long,
            guidePoints: List<Vec2>,
            maxDistance: Float
        ): List<SpectrePath> {
            val specRng = Random(seed xor 0x5A17L)
            val spectres = ArrayList<SpectrePath>(2)
            val names = listOf("Spectre-Alpha", "Spectre-Vega")
            for (sIdx in 0..1) {
                val pts = ArrayList<Vec2>()
                var x = 0f
                var y = 0f
                pts.add(Vec2(x, y))
                val phase = specRng.nextFloat() * 6.28f
                val crashAt = if (sIdx == 1) maxDistance * (0.58f + specRng.nextFloat() * 0.22f) else maxDistance * 0.92f
                var guideCursor = 0

                while (-y < crashAt) {
                    y -= 28f
                    while (guideCursor < guidePoints.size - 1 && guidePoints[guideCursor].y > y) {
                        guideCursor++
                    }
                    val targetX = if (guidePoints.isNotEmpty()) {
                        val g = guidePoints[guideCursor]
                        val sway = sin((-y * 0.008f) + phase + sIdx * 1.9f) * (42f + sIdx * 24f)
                        (g.x + sway).coerceIn(-255f, 255f)
                    } else {
                        sin((-y * 0.01f) + phase) * 150f
                    }
                    x += (targetX - x) * 0.14f
                    pts.add(Vec2(x, y))
                }
                spectres.add(
                    SpectrePath(
                        name = names[sIdx],
                        colorIndex = if (sIdx == 0) 2 else 3,
                        points = pts,
                        crashPoint = if (sIdx == 1) pts.lastOrNull() else null
                    )
                )
            }
            return spectres
        }
    }
}
