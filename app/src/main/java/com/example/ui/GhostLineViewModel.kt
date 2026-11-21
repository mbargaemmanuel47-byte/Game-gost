package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ControlMode
import com.example.data.GameMode
import com.example.data.GhostLineRepository
import com.example.data.GhostRecord
import com.example.data.RunLog
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.Vec2
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HubUiData(
    val profile: UserProfile,
    val ghosts: List<GhostRecord>,
    val recentRuns: List<RunLog>,
    val isCloudLinked: Boolean
)

data class ActiveRunConfig(
    val mode: GameMode,
    val sectorIndex: Int,
    val runInstanceId: Long = System.nanoTime()
)

class GhostLineViewModel(
    private val repository: GhostLineRepository,
    private val currentUserId: String?
) : ViewModel() {

    private val _selectedSector = MutableStateFlow(1)
    val selectedSector: StateFlow<Int> = _selectedSector.asStateFlow()

    private val _activeRunConfig = MutableStateFlow<ActiveRunConfig?>(null)
    val activeRunConfig: StateFlow<ActiveRunConfig?> = _activeRunConfig.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val isCloudLinked: Boolean = !currentUserId.isNullOrBlank()

    init {
        viewModelScope.launch {
            val displayName = if (isCloudLinked) {
                runCatching { Firebase.auth.currentUser?.displayName }.getOrNull() ?: "Pilote Néon"
            } else {
                "Pilote Néon"
            }
            repository.ensureUserProfile(currentUserId, displayName)
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Impossible d'initialiser le profil."
                }
        }
    }

    val hubState: StateFlow<UiState<HubUiData>> = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeAllGhosts(currentUserId),
        repository.observeRecentRuns(currentUserId)
    ) { profile, ghosts, runs ->
        val safeProfile = profile ?: UserProfile(
            userId = currentUserId ?: "local",
            displayName = if (isCloudLinked) {
                runCatching { Firebase.auth.currentUser?.displayName }.getOrNull() ?: "Pilote Néon"
            } else {
                "Pilote Néon"
            }
        )
        UiState.Success(
            HubUiData(
                profile = safeProfile,
                ghosts = ghosts,
                recentRuns = runs,
                isCloudLinked = isCloudLinked
            )
        ) as UiState<HubUiData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing GhostLine state", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Erreur de chargement des données."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun selectSector(sector: Int) {
        _selectedSector.value = sector.coerceAtLeast(1)
    }

    fun launchRun(mode: GameMode, sectorIndex: Int = _selectedSector.value) {
        val cleanSector = if (mode == GameMode.ENDLESS) 0 else sectorIndex.coerceAtLeast(1)
        if (mode == GameMode.SECTOR) {
            _selectedSector.value = cleanSector
        }
        _activeRunConfig.value = ActiveRunConfig(
            mode = mode,
            sectorIndex = cleanSector,
            runInstanceId = System.nanoTime()
        )
    }

    fun closeArena() {
        _activeRunConfig.value = null
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun updatePreferences(
        controlMode: ControlMode,
        musicOn: Boolean,
        sfxOn: Boolean,
        hapticsOn: Boolean
    ) {
        viewModelScope.launch {
            repository.updatePreferences(
                userId = currentUserId,
                controlMode = controlMode,
                musicOn = musicOn,
                sfxOn = sfxOn,
                hapticsOn = hapticsOn
            ).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Échec de sauvegarde des préférences."
            }
        }
    }

    fun onRunFinished(
        mode: GameMode,
        sectorIndex: Int,
        score: Long,
        distance: Long,
        nearMisses: Long,
        cleared: Boolean,
        crashPoint: Vec2?,
        trailPoints: List<Vec2>
    ) {
        val currentData = (hubState.value as? UiState.Success)?.data
        val targetSectorKey = if (mode == GameMode.ENDLESS) 0L else sectorIndex.toLong()
        val sectorGhosts = currentData?.ghosts
            ?.filter { it.sectorIndex == targetSectorKey }
            ?: emptyList()

        viewModelScope.launch {
            repository.recordRunAndGhost(
                userId = currentUserId,
                mode = mode,
                sectorIndex = sectorIndex,
                score = score,
                distance = distance,
                nearMisses = nearMisses,
                cleared = cleared,
                crashPoint = crashPoint,
                trailPoints = trailPoints,
                existingProfile = currentData?.profile,
                existingSectorGhosts = sectorGhosts
            ).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Erreur lors de l'enregistrement de la partie."
            }
        }
    }

    fun purgeGhosts(ghostIds: List<String>) {
        if (ghostIds.isEmpty()) return
        viewModelScope.launch {
            repository.purgeGhosts(currentUserId, ghostIds)
                .onSuccess {
                    _statusBanner.value = "Cimetière fantôme purgé."
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Impossible de purger les fantômes."
                }
        }
    }

    private companion object {
        const val TAG = "GhostLineVM"
    }
}
