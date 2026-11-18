package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale
import kotlin.math.roundToInt

enum class ControlMode(val labelFr: String, val subtitleFr: String) {
    ALTERNATING(
        "1 Doigt Alterné",
        "Appui long pour tourner, relâcher pour filer droit. Alterne gauche/droite à chaque appui."
    ),
    CONTINUOUS(
        "360° Continu",
        "Appui long pour tourner en boucle horaire, relâcher pour filer droit."
    ),
    SPLIT(
        "Gauche / Droite",
        "Maintenir à gauche ou à droite de l'écran pour braquer, relâcher pour aller tout droit."
    );

    companion object {
        fun fromRaw(raw: String?): ControlMode =
            entries.firstOrNull { it.name == raw } ?: ALTERNATING
    }
}

enum class GameMode {
    ENDLESS,
    SECTOR;

    companion object {
        fun fromRaw(raw: String?): GameMode =
            entries.firstOrNull { it.name == raw } ?: SECTOR
    }
}

data class Vec2(val x: Float, val y: Float)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Pilote Néon",
    val bestEndlessScore: Long = 0L,
    val highestSector: Long = 1L,
    val totalRuns: Long = 0L,
    val totalNearMisses: Long = 0L,
    val preferredControlMode: String = ControlMode.ALTERNATING.name,
    val musicOn: Boolean = true,
    val sfxOn: Boolean = true,
    val hapticsOn: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun controlModeEnum(): ControlMode = ControlMode.fromRaw(preferredControlMode)
}

@Keep
@IgnoreExtraProperties
data class GhostRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sectorIndex: Long = 0L,
    val score: Long = 0L,
    val distance: Long = 0L,
    val crashX: Double = 0.0,
    val crashY: Double = 0.0,
    val packedPoints: String = "",
    val colorIndex: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun decodePoints(): List<Vec2> {
        if (packedPoints.isBlank()) return emptyList()
        val result = ArrayList<Vec2>()
        val parts = packedPoints.split(';')
        for (part in parts) {
            val comma = part.indexOf(',')
            if (comma <= 0 || comma >= part.length - 1) continue
            val x = part.substring(0, comma).toFloatOrNull()
            val y = part.substring(comma + 1).toFloatOrNull()
            if (x != null && y != null) {
                result.add(Vec2(x, y))
            }
        }
        return result
    }

    companion object {
        fun packPoints(points: List<Vec2>, maxSamples: Int = 360): String {
            if (points.isEmpty()) return "0.0,0.0"
            val step = if (points.size <= maxSamples) 1 else (points.size / maxSamples).coerceAtLeast(1)
            val sb = StringBuilder()
            var count = 0
            for (i in points.indices step step) {
                if (count > 0) sb.append(';')
                val p = points[i]
                val rx = (p.x * 10f).roundToInt() / 10f
                val ry = (p.y * 10f).roundToInt() / 10f
                sb.append(String.format(Locale.US, "%.1f,%.1f", rx, ry))
                count++
            }
            val last = points.last()
            val lastStr = String.format(
                Locale.US,
                "%.1f,%.1f",
                (last.x * 10f).roundToInt() / 10f,
                (last.y * 10f).roundToInt() / 10f
            )
            if (sb.length + lastStr.length + 1 <= 19500) {
                sb.append(';').append(lastStr)
            }
            return sb.toString().take(19800).ifBlank { "0.0,0.0" }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class RunLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = GameMode.SECTOR.name,
    val sectorIndex: Long = 1L,
    val score: Long = 0L,
    val nearMisses: Long = 0L,
    val cleared: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
