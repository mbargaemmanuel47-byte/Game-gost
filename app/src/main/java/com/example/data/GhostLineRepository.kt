package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GhostLineRepository(
    private val dao: GhostLineDao,
    private val db: FirebaseFirestore? = null,
    private val auth: FirebaseAuth? = null
) {
    private fun activeUidOrNull(): String? = auth?.currentUser?.uid?.takeIf { it.isNotBlank() }

    fun observeUserProfile(userId: String?): Flow<UserProfile?> = flow {
        if (userId.isNullOrBlank() || db == null) {
            emitAll(dao.observeProfile().map { it?.toUserProfile("local") })
        } else {
            emitAll(
                db.collection("users").document(userId)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        } else {
                            dao.getProfileOnce()?.toUserProfile(userId)
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        if (auth?.currentUser?.uid == userId) {
                            handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$userId")
                        }
                        throw error
                    }
            )
        }
    }

    fun observeAllGhosts(userId: String?): Flow<List<GhostRecord>> = flow {
        if (userId.isNullOrBlank() || db == null) {
            emitAll(
                dao.observeAllGhosts().map { list ->
                    list.map { it.toGhostRecord("local") }
                }
            )
        } else {
            emitAll(
                db.collection("users").document(userId)
                    .collection("ghosts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(40)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(GhostRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        if (auth?.currentUser?.uid == userId) {
                            handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$userId/ghosts")
                        }
                        throw error
                    }
            )
        }
    }

    fun observeRecentRuns(userId: String?): Flow<List<RunLog>> = flow {
        if (userId.isNullOrBlank() || db == null) {
            emitAll(
                dao.observeRecentRuns().map { list ->
                    list.map { it.toRunLog("local") }
                }
            )
        } else {
            emitAll(
                db.collection("users").document(userId)
                    .collection("runs")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(RunLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        if (auth?.currentUser?.uid == userId) {
                            handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$userId/runs")
                        }
                        throw error
                    }
            )
        }
    }

    suspend fun ensureUserProfile(userId: String?, rawDisplayName: String?): Result<Unit> {
        return try {
            val local = dao.getProfileOnce()
            if (local == null) {
                dao.upsertProfile(
                    LocalProfileEntity(
                        displayName = rawDisplayName?.trim()?.take(60)?.ifBlank { "Pilote Néon" } ?: "Pilote Néon"
                    )
                )
            }

            if (!userId.isNullOrBlank() && db != null && activeUidOrNull() == userId) {
                val docRef = db.collection("users").document(userId)
                val existing = docRef.get().await()
                if (!existing.exists()) {
                    val base = dao.getProfileOnce() ?: LocalProfileEntity()
                    val cleanName = rawDisplayName?.trim()?.take(60)?.ifBlank { base.displayName } ?: base.displayName
                    val payload = mapOf(
                        "userId" to userId,
                        "displayName" to cleanName,
                        "bestEndlessScore" to base.bestEndlessScore,
                        "highestSector" to base.highestSector.coerceAtLeast(1L),
                        "totalRuns" to base.totalRuns,
                        "totalNearMisses" to base.totalNearMisses,
                        "preferredControlMode" to base.preferredControlMode,
                        "musicOn" to base.musicOn,
                        "sfxOn" to base.sfxOn,
                        "hapticsOn" to base.hapticsOn,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!userId.isNullOrBlank()) {
                handleFirestoreError(e, OperationType.CREATE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String?,
        controlMode: ControlMode,
        musicOn: Boolean,
        sfxOn: Boolean,
        hapticsOn: Boolean
    ): Result<Unit> {
        return try {
            val existingLocal = dao.getProfileOnce() ?: LocalProfileEntity()
            dao.upsertProfile(
                existingLocal.copy(
                    preferredControlMode = controlMode.name,
                    musicOn = musicOn,
                    sfxOn = sfxOn,
                    hapticsOn = hapticsOn
                )
            )

            if (!userId.isNullOrBlank() && db != null && activeUidOrNull() == userId) {
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "preferredControlMode" to controlMode.name,
                            "musicOn" to musicOn,
                            "sfxOn" to sfxOn,
                            "hapticsOn" to hapticsOn,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!userId.isNullOrBlank()) {
                handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    suspend fun recordRunAndGhost(
        userId: String?,
        mode: GameMode,
        sectorIndex: Int,
        score: Long,
        distance: Long,
        nearMisses: Long,
        cleared: Boolean,
        crashPoint: Vec2?,
        trailPoints: List<Vec2>,
        existingProfile: UserProfile?,
        existingSectorGhosts: List<GhostRecord>
    ): Result<Unit> {
        return try {
            val safeScore = score.coerceIn(0L, 100_000_000L)
            val safeDistance = distance.coerceIn(0L, 100_000_000L)
            val safeSector = sectorIndex.toLong().coerceIn(0L, 10_000L)
            val safeNearMisses = nearMisses.coerceIn(0L, 100_000L)
            val sectorKey = if (mode == GameMode.ENDLESS) 0L else safeSector
            val nowMs = System.currentTimeMillis()

            // Always persist locally in Room so playing without an account works seamlessly
            val localProfile = dao.getProfileOnce() ?: LocalProfileEntity()
            val newBest = if (mode == GameMode.ENDLESS) maxOf(localProfile.bestEndlessScore, existingProfile?.bestEndlessScore ?: 0L, safeScore) else maxOf(localProfile.bestEndlessScore, existingProfile?.bestEndlessScore ?: 0L)
            val newHighestSector = if (mode == GameMode.SECTOR && cleared) {
                maxOf(localProfile.highestSector, existingProfile?.highestSector ?: 1L, (safeSector + 1L).coerceAtMost(10_000L))
            } else {
                maxOf(localProfile.highestSector, existingProfile?.highestSector ?: 1L)
            }
            val newTotalRuns = (maxOf(localProfile.totalRuns, existingProfile?.totalRuns ?: 0L) + 1L).coerceAtMost(10_000_000L)
            val newTotalNearMisses = (maxOf(localProfile.totalNearMisses, existingProfile?.totalNearMisses ?: 0L) + safeNearMisses).coerceAtMost(10_000_000L)

            dao.upsertProfile(
                localProfile.copy(
                    bestEndlessScore = newBest,
                    highestSector = newHighestSector,
                    totalRuns = newTotalRuns,
                    totalNearMisses = newTotalNearMisses
                )
            )

            val runId = UUID.randomUUID().toString()
            dao.insertRun(
                LocalRunEntity(
                    id = runId,
                    mode = mode.name,
                    sectorIndex = safeSector,
                    score = safeScore,
                    nearMisses = safeNearMisses,
                    cleared = cleared,
                    createdAtMs = nowMs
                )
            )

            var packedPoints: String? = null
            val colorIdx = ((existingProfile?.totalRuns ?: localProfile.totalRuns) % 4L).coerceIn(0L, 10L)
            if (!cleared && crashPoint != null && trailPoints.size >= 4) {
                val packed = GhostRecord.packPoints(trailPoints)
                packedPoints = packed
                val ghostId = UUID.randomUUID().toString()
                dao.insertGhost(
                    LocalGhostEntity(
                        id = ghostId,
                        sectorIndex = sectorKey,
                        score = safeScore,
                        distance = safeDistance,
                        crashX = crashPoint.x.toDouble().coerceIn(-999999.0, 999999.0),
                        crashY = crashPoint.y.toDouble().coerceIn(-999999.0, 999999.0),
                        packedPoints = packed,
                        colorIndex = colorIdx,
                        createdAtMs = nowMs
                    )
                )
                val localSectorGhosts = dao.getGhostsForSector(sectorKey)
                if (localSectorGhosts.size > 6) {
                    val toDeleteIds = localSectorGhosts.drop(6).map { it.id }
                    dao.deleteGhostsByIds(toDeleteIds)
                }
            }

            // Optional cloud sync if signed in with Google
            if (!userId.isNullOrBlank() && db != null && activeUidOrNull() == userId) {
                val runRef = db.collection("users").document(userId).collection("runs").document()
                val runPayload = mapOf(
                    "userId" to userId,
                    "mode" to mode.name,
                    "sectorIndex" to safeSector,
                    "score" to safeScore,
                    "nearMisses" to safeNearMisses,
                    "cleared" to cleared,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                runRef.set(runPayload).await()

                if (packedPoints != null && crashPoint != null) {
                    val ghostRef = db.collection("users").document(userId).collection("ghosts").document()
                    val ghostPayload = mapOf(
                        "userId" to userId,
                        "sectorIndex" to sectorKey,
                        "score" to safeScore,
                        "distance" to safeDistance,
                        "crashX" to crashPoint.x.toDouble().coerceIn(-999999.0, 999999.0),
                        "crashY" to crashPoint.y.toDouble().coerceIn(-999999.0, 999999.0),
                        "packedPoints" to packedPoints,
                        "colorIndex" to colorIdx,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    ghostRef.set(ghostPayload).await()

                    if (existingSectorGhosts.size >= 6) {
                        val toDelete = existingSectorGhosts.drop(5)
                        for (oldGhost in toDelete) {
                            if (oldGhost.id.isNotBlank()) {
                                db.collection("users").document(userId)
                                    .collection("ghosts").document(oldGhost.id)
                                    .delete()
                                    .await()
                            }
                        }
                    }
                }

                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "bestEndlessScore" to newBest,
                            "highestSector" to newHighestSector,
                            "totalRuns" to newTotalRuns,
                            "totalNearMisses" to newTotalNearMisses,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!userId.isNullOrBlank()) {
                handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            }
            Result.failure(e)
        }
    }

    suspend fun purgeGhosts(userId: String?, ghostIds: List<String>): Result<Unit> {
        if (ghostIds.isEmpty()) return Result.success(Unit)
        return try {
            dao.deleteGhostsByIds(ghostIds)

            if (!userId.isNullOrBlank() && db != null && activeUidOrNull() == userId) {
                for (ghostId in ghostIds) {
                    if (ghostId.isNotBlank()) {
                        db.collection("users").document(userId)
                            .collection("ghosts").document(ghostId)
                            .delete()
                            .await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!userId.isNullOrBlank()) {
                handleFirestoreError(e, OperationType.DELETE, "users/$userId/ghosts")
            }
            Result.failure(e)
        }
    }
}
