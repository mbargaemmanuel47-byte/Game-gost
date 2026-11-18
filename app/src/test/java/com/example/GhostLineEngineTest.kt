package com.example

import com.example.data.ControlMode
import com.example.data.GameMode
import com.example.data.GhostRecord
import com.example.data.Vec2
import com.example.game.EngineEvent
import com.example.game.GameConstants
import com.example.game.GameEngine
import com.example.game.ProceduralLevelGenerator
import com.example.game.RunPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class GhostLineEngineTest {

    @Test
    fun `ghost polyline pack and decode preserves coordinates within tolerance`() {
        val original = listOf(
            Vec2(0f, 0f),
            Vec2(45.67f, -180.23f),
            Vec2(-112.4f, -450.8f),
            Vec2(88.1f, -920.5f)
        )
        val packed = GhostRecord.packPoints(original)
        val decoded = GhostRecord(packedPoints = packed).decodePoints()
        assertTrue(decoded.size >= 4)
        assertEquals(0f, decoded.first().x, 0.2f)
        assertEquals(88.1f, decoded.last().x, 0.2f)
        assertEquals(-920.5f, decoded.last().y, 0.2f)
    }

    @Test
    fun `alternating control mode curves opposite directions on consecutive presses and straight on release`() {
        val engine = GameEngine(
            mode = GameMode.ENDLESS,
            sectorIndex = 0,
            controlMode = ControlMode.ALTERNATING,
            savedGhosts = emptyList()
        )
        val initialHeading = engine.headingRad
        assertEquals((-PI / 2.0).toFloat(), initialHeading, 0.001f)

        // Press 1: turns right (positive angle delta)
        engine.onTouchChanged(true, 0.5f)
        engine.step(0.03f)
        assertTrue(engine.headingRad > initialHeading)
        val afterFirstTurn = engine.headingRad

        // Release: stays straight
        engine.onTouchChanged(false, 0.5f)
        engine.step(0.03f)
        assertEquals(afterFirstTurn, engine.headingRad, 0.0001f)

        // Press 2: alternates left (negative angle delta)
        engine.onTouchChanged(true, 0.5f)
        engine.step(0.03f)
        assertTrue(engine.headingRad < afterFirstTurn)
    }

    @Test
    fun `colliding with a saved ghost line after grace zone triggers crash`() {
        // Straight ghost path directly ahead on x = 0 from y = 0 to y = -600
        val ghost = GhostRecord(
            id = "g_test",
            sectorIndex = 1L,
            crashX = 0.0,
            crashY = -600.0,
            packedPoints = "0.0,0.0;0.0,-200.0;0.0,-400.0;0.0,-600.0"
        )
        val engine = GameEngine(
            mode = GameMode.SECTOR,
            sectorIndex = 1,
            controlMode = ControlMode.ALTERNATING,
            savedGhosts = listOf(ghost)
        )
        engine.startIfReady()

        // Step straight ahead past spawn grace distance (>150f)
        var crashed = false
        for (i in 0 until 80) {
            val events = engine.step(0.02f)
            if (events.contains(EngineEvent.Crashed)) {
                crashed = true
                break
            }
        }
        assertTrue("Expected ship flying straight on x=0 to collide with ghost on x=0", crashed)
        assertEquals(RunPhase.CRASHED, engine.phase)
    }

    @Test
    fun `procedural sector target distance scales monotonically`() {
        val d1 = ProceduralLevelGenerator.sectorTargetDistance(1)
        val d2 = ProceduralLevelGenerator.sectorTargetDistance(2)
        val d5 = ProceduralLevelGenerator.sectorTargetDistance(5)
        assertTrue(d2 > d1)
        assertTrue(d5 > d2)
        assertTrue(GameConstants.WORLD_HALF_WIDTH == 300f)
    }
}
