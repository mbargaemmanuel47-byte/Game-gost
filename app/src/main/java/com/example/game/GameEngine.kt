package com.example.game

import com.example.data.ControlMode
import com.example.data.GameMode
import com.example.data.GhostRecord
import com.example.data.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val colorIndex: Int
)

data class FloatingPopup(
    val text: String,
    var x: Float,
    var y: Float,
    var life: Float,
    val maxLife: Float,
    val colorIndex: Int
)

data class ActiveGhost(
    val label: String,
    val colorIndex: Int,
    val points: List<Vec2>,
    val crashPoint: Vec2?,
    val isSpectre: Boolean
) {
    /**
     * Returns interpolated cursor position along the ghost's path at the given progress index.
     */
    fun cursorAt(distanceProgress: Float): Vec2? {
        if (points.size < 2) return points.firstOrNull()
        val approxIdx = (distanceProgress / 24f).toInt().coerceIn(0, points.size - 1)
        return points[approxIdx]
    }
}

enum class RunPhase {
    READY,
    RUNNING,
    PAUSED,
    CRASHED,
    SECTOR_CLEARED
}

enum class CrashReason(val labelFr: String) {
    NONE(""),
    WALL("Mur laser latéral percuté"),
    OBSTACLE("Obstacle néon percuté"),
    GHOST("Collision avec un Trajet Fantôme !")
}

sealed interface EngineEvent {
    data object OrbCollected : EngineEvent
    data object NearMiss : EngineEvent
    data object Crashed : EngineEvent
    data object SectorCleared : EngineEvent
}

class GameEngine(
    val mode: GameMode,
    val sectorIndex: Int,
    var controlMode: ControlMode,
    savedGhosts: List<GhostRecord>
) {
    var phase: RunPhase = RunPhase.READY
        private set
    var crashReason: CrashReason = CrashReason.NONE
        private set

    // Ship kinematics
    var shipX: Float = 0f
        private set
    var shipY: Float = 0f
        private set
    var headingRad: Float = (-PI / 2.0).toFloat()
        private set
    var currentSpeed: Float = 335f
        private set

    // Next turn direction for ALTERNATING mode (+1 clockwise/right, -1 counter-clockwise/left)
    var nextTurnSign: Float = 1f
        private set
    var activeTurnSign: Float = 0f
        private set

    // Run metrics
    var elapsedSec: Float = 0f
        private set
    var distanceTraveled: Float = 0f
        private set
    var score: Long = 0L
        private set
    var bonusScore: Long = 0L
        private set
    var nearMissCount: Long = 0L
        private set
    var comboMultiplier: Int = 1
        private set
    private var comboDecayTimer: Float = 0f

    var screenShake: Float = 0f
        private set
    var flashAlpha: Float = 0f
        private set

    val targetDistance: Float = if (mode == GameMode.SECTOR) {
        ProceduralLevelGenerator.sectorTargetDistance(sectorIndex)
    } else {
        Float.MAX_VALUE
    }

    val playerTrail = ArrayList<Vec2>(512)
    val obstacles = ArrayList<Obstacle>(32)
    val orbs = ArrayList<EnergyOrb>(24)
    val collectedOrbIds = HashSet<Int>()
    val particles = ArrayList<Particle>(128)
    val popups = ArrayList<FloatingPopup>(16)
    val activeGhosts = ArrayList<ActiveGhost>(10)

    private val levelSeed: Long = if (mode == GameMode.SECTOR) {
        sectorIndex.toLong() * 7919L + 1337L
    } else {
        System.currentTimeMillis()
    }
    private val generator = ProceduralLevelGenerator(levelSeed)
    private val guidePoints = ArrayList<Vec2>(32)
    private var nextChunkIndex = 0
    private var nextChunkY = -360f
    private var lastTrailSamplePos = Vec2(0f, 0f)
    private val nearMissCooldowns = HashMap<Int, Float>()
    private val ghostNearMissCooldowns = HashMap<Int, Float>()
    private val fxRng = Random(4242)

    init {
        playerTrail.add(Vec2(0f, 0f))
        // Pre-generate initial horizon chunks
        val initialLimit = if (mode == GameMode.SECTOR) -(targetDistance - 240f) else -3200f
        while (nextChunkY > initialLimit) {
            spawnNextChunk()
        }

        // Load player's saved ghosts for this mode/sector
        for ((idx, record) in savedGhosts.take(6).withIndex()) {
            val pts = record.decodePoints()
            if (pts.size >= 2) {
                activeGhosts.add(
                    ActiveGhost(
                        label = "Essai #${idx + 1}",
                        colorIndex = record.colorIndex.toInt().coerceIn(0, 3),
                        points = pts,
                        crashPoint = Vec2(record.crashX.toFloat(), record.crashY.toFloat()),
                        isSpectre = false
                    )
                )
            }
        }

        // Add deterministic pre-recorded spectres
        val spectreDistance = if (mode == GameMode.SECTOR) targetDistance else 3000f
        val spectres = ProceduralLevelGenerator.buildPreRecordedSpectres(
            seed = levelSeed,
            guidePoints = guidePoints,
            maxDistance = spectreDistance
        )
        for (spec in spectres) {
            activeGhosts.add(
                ActiveGhost(
                    label = spec.name,
                    colorIndex = spec.colorIndex,
                    points = spec.points,
                    crashPoint = spec.crashPoint,
                    isSpectre = true
                )
            )
        }
    }

    private fun spawnNextChunk() {
        val diff = if (mode == GameMode.SECTOR) {
            ((sectorIndex - 1) * 0.11f + (-nextChunkY / targetDistance) * 0.35f).coerceIn(0.08f, 0.95f)
        } else {
            (-nextChunkY / 8500f).coerceIn(0.10f, 0.98f)
        }
        val chunk = generator.generateChunk(nextChunkIndex, nextChunkY, diff)
        obstacles.addAll(chunk.obstacles)
        orbs.addAll(chunk.orbs)
        guidePoints.add(chunk.guidePoint)
        nextChunkIndex++
        val spacing = (310f - diff * 55f).coerceIn(245f, 315f)
        nextChunkY -= spacing
    }

    fun startIfReady() {
        if (phase == RunPhase.READY) {
            phase = RunPhase.RUNNING
        }
    }

    fun pause() {
        if (phase == RunPhase.RUNNING) {
            phase = RunPhase.PAUSED
        }
    }

    fun resume() {
        if (phase == RunPhase.PAUSED) {
            phase = RunPhase.RUNNING
        }
    }

    fun onTouchChanged(isDown: Boolean, normalizedX: Float) {
        if (phase == RunPhase.READY && isDown) {
            phase = RunPhase.RUNNING
        }
        if (phase != RunPhase.RUNNING) {
            activeTurnSign = 0f
            return
        }

        if (!isDown) {
            activeTurnSign = 0f
            return
        }

        activeTurnSign = when (controlMode) {
            ControlMode.ALTERNATING -> {
                if (activeTurnSign == 0f) {
                    val chosen = nextTurnSign
                    nextTurnSign = -nextTurnSign
                    chosen
                } else {
                    activeTurnSign
                }
            }
            ControlMode.CONTINUOUS -> 1f
            ControlMode.SPLIT -> if (normalizedX < 0.5f) -1f else 1f
        }
    }

    fun step(rawDt: Float): List<EngineEvent> {
        val dt = rawDt.coerceIn(0.001f, 0.033f)
        val events = ArrayList<EngineEvent>(2)

        updateParticlesAndPopups(dt)
        if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 28f)
        if (flashAlpha > 0f) flashAlpha = max(0f, flashAlpha - dt * 2.4f)

        if (phase != RunPhase.RUNNING) return events

        elapsedSec += dt

        // Speed progression
        val sectorBase = 330f + (sectorIndex - 1).coerceAtLeast(0) * 14f
        val ramp = if (mode == GameMode.ENDLESS) (distanceTraveled * 0.015f).coerceAtMost(210f) else (distanceTraveled * 0.01f).coerceAtMost(70f)
        currentSpeed = (sectorBase + ramp).coerceAtMost(560f)

        // Turning physics
        if (activeTurnSign != 0f) {
            val turnRate = 3.25f + (currentSpeed - 330f) * 0.002f
            headingRad += activeTurnSign * turnRate * dt
            if (controlMode != ControlMode.CONTINUOUS) {
                val minAngle = (-PI * 0.87).toFloat()
                val maxAngle = (-PI * 0.13).toFloat()
                headingRad = headingRad.coerceIn(minAngle, maxAngle)
            }
        }

        val prevX = shipX
        val prevY = shipY
        shipX += cos(headingRad) * currentSpeed * dt
        shipY += sin(headingRad) * currentSpeed * dt
        distanceTraveled += hypot(shipX - prevX, shipY - prevY)

        // Combo decay
        if (comboDecayTimer > 0f) {
            comboDecayTimer -= dt
            if (comboDecayTimer <= 0f) comboMultiplier = 1
        }

        // Sample player trail
        if (hypot(shipX - lastTrailSamplePos.x, shipY - lastTrailSamplePos.y) >= 16f) {
            val pt = Vec2(shipX, shipY)
            playerTrail.add(pt)
            lastTrailSamplePos = pt
        }

        // Emit subtle engine neon sparks
        if (fxRng.nextFloat() < 0.65f) {
            particles.add(
                Particle(
                    x = shipX + fxRng.nextFloat() * 8f - 4f,
                    y = shipY + fxRng.nextFloat() * 8f - 4f,
                    vx = -cos(headingRad) * 65f + (fxRng.nextFloat() - 0.5f) * 45f,
                    vy = -sin(headingRad) * 65f + (fxRng.nextFloat() - 0.5f) * 45f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    size = 3.2f,
                    colorIndex = if (activeTurnSign != 0f) 1 else 0
                )
            )
        }

        // Endless procedural streaming + pruning behind camera
        if (mode == GameMode.ENDLESS) {
            while (nextChunkY > shipY - 1900f) {
                spawnNextChunk()
            }
            obstacles.removeAll { it.y > shipY + 750f }
            orbs.removeAll { it.y > shipY + 750f }
        }

        // Score calculation
        score = (-shipY / 5f).toLong().coerceAtLeast(0L) + bonusScore

        // Check Sector finish portal
        if (mode == GameMode.SECTOR && shipY <= -targetDistance) {
            phase = RunPhase.SECTOR_CLEARED
            score += 500L
            flashAlpha = 0.65f
            spawnBurst(shipX, shipY, count = 42, colorIndex = 0)
            events.add(EngineEvent.SectorCleared)
            return events
        }

        // Check Orb pickups
        for (orb in orbs) {
            if (!collectedOrbIds.contains(orb.id)) {
                val dist = hypot(shipX - orb.x, shipY - orb.y)
                if (dist <= GameConstants.SHIP_RADIUS + orb.radius) {
                    collectedOrbIds.add(orb.id)
                    val pts = 50L * comboMultiplier
                    bonusScore += pts
                    comboDecayTimer = 3.2f
                    popups.add(FloatingPopup("+$pts ORBE", orb.x, orb.y - 16f, 0.9f, 0.9f, 0))
                    spawnBurst(orb.x, orb.y, count = 14, colorIndex = 0)
                    events.add(EngineEvent.OrbCollected)
                }
            }
        }

        // Check corridor boundary walls
        if (abs(shipX) + GameConstants.SHIP_RADIUS >= GameConstants.WORLD_HALF_WIDTH) {
            triggerCrash(CrashReason.WALL)
            events.add(EngineEvent.Crashed)
            return events
        }

        // Decrement near-miss cooldowns
        val cooldownIter = nearMissCooldowns.entries.iterator()
        while (cooldownIter.hasNext()) {
            val e = cooldownIter.next()
            e.setValue(e.value - dt)
            if (e.value <= 0f) cooldownIter.remove()
        }
        val ghostCdIter = ghostNearMissCooldowns.entries.iterator()
        while (ghostCdIter.hasNext()) {
            val e = ghostCdIter.next()
            e.setValue(e.value - dt)
            if (e.value <= 0f) ghostCdIter.remove()
        }

        // Check Obstacles
        for (obs in obstacles) {
            if (abs(obs.y - shipY) > 180f) continue
            val minDist = distanceToObstacle(shipX, shipY, obs)
            if (minDist <= GameConstants.SHIP_RADIUS) {
                triggerCrash(CrashReason.OBSTACLE)
                events.add(EngineEvent.Crashed)
                return events
            } else if (minDist <= GameConstants.NEAR_MISS_RADIUS && !nearMissCooldowns.containsKey(obs.id)) {
                nearMissCooldowns[obs.id] = 0.75f
                awardNearMiss(events)
            }
        }

        // Check Ghost Lines & Crash Cores (after spawn grace zone)
        if (-shipY > GameConstants.SPAWN_GRACE_DISTANCE) {
            for ((gIdx, ghost) in activeGhosts.withIndex()) {
                // Crash core check
                val cp = ghost.crashPoint
                if (cp != null && -cp.y > GameConstants.SPAWN_GRACE_DISTANCE) {
                    val dCrash = hypot(shipX - cp.x, shipY - cp.y)
                    if (dCrash <= GameConstants.SHIP_RADIUS + GameConstants.GHOST_CRASH_RADIUS) {
                        triggerCrash(CrashReason.GHOST)
                        events.add(EngineEvent.Crashed)
                        return events
                    }
                }

                // Polyline segments check (quick bounding Y precheck)
                val pts = ghost.points
                var closestGhostDist = Float.MAX_VALUE
                for (i in 0 until pts.size - 1) {
                    val a = pts[i]
                    val b = pts[i + 1]
                    if (-a.y < GameConstants.SPAWN_GRACE_DISTANCE && -b.y < GameConstants.SPAWN_GRACE_DISTANCE) continue
                    val minY = min(a.y, b.y) - 35f
                    val maxY = max(a.y, b.y) + 35f
                    if (shipY < minY || shipY > maxY) continue

                    val dSeg = pointToSegmentDistance(shipX, shipY, a.x, a.y, b.x, b.y)
                    if (dSeg < closestGhostDist) closestGhostDist = dSeg
                    if (dSeg <= GameConstants.SHIP_RADIUS + GameConstants.GHOST_TRAIL_RADIUS) {
                        triggerCrash(CrashReason.GHOST)
                        events.add(EngineEvent.Crashed)
                        return events
                    }
                }
                if (closestGhostDist <= GameConstants.NEAR_MISS_RADIUS + 4f && !ghostNearMissCooldowns.containsKey(gIdx)) {
                    ghostNearMissCooldowns[gIdx] = 0.85f
                    awardNearMiss(events)
                }
            }
        }

        return events
    }

    private fun awardNearMiss(events: ArrayList<EngineEvent>) {
        nearMissCount++
        comboMultiplier = (comboMultiplier + 1).coerceAtMost(5)
        comboDecayTimer = 3.2f
        val pts = 25L * comboMultiplier
        bonusScore += pts
        popups.add(FloatingPopup("FRÔLEMENT +$pts", shipX, shipY - 26f, 0.95f, 0.95f, 1))
        spawnBurst(shipX, shipY, count = 8, colorIndex = 2)
        events.add(EngineEvent.NearMiss)
    }

    private fun triggerCrash(reason: CrashReason) {
        phase = RunPhase.CRASHED
        crashReason = reason
        activeTurnSign = 0f
        screenShake = 18f
        flashAlpha = 0.8f
        playerTrail.add(Vec2(shipX, shipY))
        spawnBurst(shipX, shipY, count = 38, colorIndex = 1)
    }

    private fun spawnBurst(x: Float, y: Float, count: Int, colorIndex: Int) {
        for (i in 0 until count) {
            val angle = fxRng.nextFloat() * (2f * PI.toFloat())
            val spd = 70f + fxRng.nextFloat() * 280f
            val life = 0.4f + fxRng.nextFloat() * 0.45f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    life = life,
                    maxLife = life,
                    size = 3f + fxRng.nextFloat() * 4.5f,
                    colorIndex = colorIndex
                )
            )
        }
    }

    private fun updateParticlesAndPopups(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.95f
            p.vy *= 0.95f
            p.life -= dt
            if (p.life <= 0f) pIter.remove()
        }
        val popIter = popups.iterator()
        while (popIter.hasNext()) {
            val pop = popIter.next()
            pop.y -= 48f * dt
            pop.life -= dt
            if (pop.life <= 0f) popIter.remove()
        }
    }

    private fun distanceToObstacle(px: Float, py: Float, obs: Obstacle): Float = when (obs) {
        is Obstacle.BarrierGate -> {
            val leftEnd = obs.gapCenterX - obs.gapHalfWidth
            val rightStart = obs.gapCenterX + obs.gapHalfWidth
            val halfT = obs.thickness * 0.5f
            val dLeft = pointToRectDistance(px, py, -GameConstants.WORLD_HALF_WIDTH, obs.y - halfT, leftEnd, obs.y + halfT)
            val dRight = pointToRectDistance(px, py, rightStart, obs.y - halfT, GameConstants.WORLD_HALF_WIDTH, obs.y + halfT)
            min(dLeft, dRight)
        }
        is Obstacle.MovingSweeper -> {
            val cx = obs.currentCenterX(elapsedSec)
            val halfT = obs.thickness * 0.5f
            pointToRectDistance(px, py, cx - obs.halfLength, obs.y - halfT, cx + obs.halfLength, obs.y + halfT)
        }
        is Obstacle.DiamondMonolith -> {
            // L1 diamond distance scaled by 1/sqrt(2) gives exact perpendicular edge distance
            val l1 = abs(px - obs.centerX) + abs(py - obs.y)
            ((l1 - obs.radius) * 0.7071f).coerceAtLeast(0f)
        }
    }

    private fun pointToRectDistance(
        px: Float,
        py: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Float {
        if (left >= right) return Float.MAX_VALUE
        val dx = max(max(left - px, 0f), px - right)
        val dy = max(max(top - py, 0f), py - bottom)
        return hypot(dx, dy)
    }

    private fun pointToSegmentDistance(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        if (lenSq <= 0.0001f) return hypot(px - ax, py - ay)
        val t = (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0f, 1f)
        val projX = ax + t * abx
        val projY = ay + t * aby
        return hypot(px - projX, py - projY)
    }
}
