package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_profile")
data class LocalProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Pilote Néon",
    val bestEndlessScore: Long = 0L,
    val highestSector: Long = 1L,
    val totalRuns: Long = 0L,
    val totalNearMisses: Long = 0L,
    val preferredControlMode: String = ControlMode.SPLIT.name,
    val musicOn: Boolean = true,
    val sfxOn: Boolean = true,
    val hapticsOn: Boolean = true
) {
    fun toUserProfile(uid: String): UserProfile = UserProfile(
        id = uid,
        userId = uid,
        displayName = displayName,
        bestEndlessScore = bestEndlessScore,
        highestSector = highestSector,
        totalRuns = totalRuns,
        totalNearMisses = totalNearMisses,
        preferredControlMode = preferredControlMode,
        musicOn = musicOn,
        sfxOn = sfxOn,
        hapticsOn = hapticsOn
    )
}

@Entity(tableName = "local_ghosts")
data class LocalGhostEntity(
    @PrimaryKey val id: String,
    val sectorIndex: Long,
    val score: Long,
    val distance: Long,
    val crashX: Double,
    val crashY: Double,
    val packedPoints: String,
    val colorIndex: Long,
    val createdAtMs: Long
) {
    fun toGhostRecord(uid: String): GhostRecord = GhostRecord(
        id = id,
        userId = uid,
        sectorIndex = sectorIndex,
        score = score,
        distance = distance,
        crashX = crashX,
        crashY = crashY,
        packedPoints = packedPoints,
        colorIndex = colorIndex
    )
}

@Entity(tableName = "local_runs")
data class LocalRunEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val sectorIndex: Long,
    val score: Long,
    val nearMisses: Long,
    val cleared: Boolean,
    val createdAtMs: Long
) {
    fun toRunLog(uid: String): RunLog = RunLog(
        id = id,
        userId = uid,
        mode = mode,
        sectorIndex = sectorIndex,
        score = score,
        nearMisses = nearMisses,
        cleared = cleared
    )
}

@Dao
interface GhostLineDao {
    @Query("SELECT * FROM local_profile WHERE id = 1")
    fun observeProfile(): Flow<LocalProfileEntity?>

    @Query("SELECT * FROM local_profile WHERE id = 1")
    suspend fun getProfileOnce(): LocalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: LocalProfileEntity)

    @Query("SELECT * FROM local_ghosts ORDER BY createdAtMs DESC LIMIT 40")
    fun observeAllGhosts(): Flow<List<LocalGhostEntity>>

    @Query("SELECT * FROM local_ghosts WHERE sectorIndex = :sectorIndex ORDER BY createdAtMs DESC")
    suspend fun getGhostsForSector(sectorIndex: Long): List<LocalGhostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGhost(ghost: LocalGhostEntity)

    @Query("DELETE FROM local_ghosts WHERE id IN (:ids)")
    suspend fun deleteGhostsByIds(ids: List<String>)

    @Query("SELECT * FROM local_runs ORDER BY createdAtMs DESC LIMIT 20")
    fun observeRecentRuns(): Flow<List<LocalRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: LocalRunEntity)
}

@Database(
    entities = [LocalProfileEntity::class, LocalGhostEntity::class, LocalRunEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GhostLineDatabase : RoomDatabase() {
    abstract fun ghostLineDao(): GhostLineDao

    companion object {
        @Volatile
        private var INSTANCE: GhostLineDatabase? = null

        fun getInstance(context: Context): GhostLineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GhostLineDatabase::class.java,
                    "ghost_line.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
