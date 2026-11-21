package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.SynthwaveAudioEngine
import com.example.data.GameMode
import com.example.data.GhostLineDatabase
import com.example.data.GhostLineRepository
import com.example.data.UiState
import com.example.ui.GameArenaScreen
import com.example.ui.GhostLineViewModel
import com.example.ui.MainHubScreen
import com.example.ui.isFirebaseAvailable
import com.example.ui.theme.GhostLineTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostLineTheme {
                GhostLineApp()
            }
        }
    }
}

@Composable
fun GhostLineApp() {
    val context = LocalContext.current
    val firebaseAvailable = remember(context) { isFirebaseAvailable(context) }

    if (firebaseAvailable) {
        val auth = remember { Firebase.auth }
        val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
        val uid = currentUser?.uid
        key(uid ?: "local") {
            GhostLineRoot(currentUserId = uid)
        }
    } else {
        // Offline / CI build without google-services.json: run seamlessly in local Room mode
        key("local") {
            GhostLineRoot(currentUserId = null)
        }
    }
}

@Composable
private fun GhostLineRoot(currentUserId: String?) {
    val viewModel: GhostLineViewModel = viewModel(
        key = currentUserId ?: "local",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val firebaseReady = isFirebaseAvailable(app)
                val db = if (firebaseReady) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    FirebaseFirestore.getInstance(databaseId)
                } else {
                    null
                }
                val auth = if (firebaseReady) Firebase.auth else null
                val localDao = GhostLineDatabase.getInstance(app).ghostLineDao()
                GhostLineViewModel(
                    repository = GhostLineRepository(dao = localDao, db = db, auth = auth),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val hubState by viewModel.hubState.collectAsStateWithLifecycle()
    val selectedSector by viewModel.selectedSector.collectAsStateWithLifecycle()
    val activeRunConfig by viewModel.activeRunConfig.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val audioEngine = remember { SynthwaveAudioEngine() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, audioEngine) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                audioEngine.start()
            }

            override fun onPause(owner: LifecycleOwner) {
                audioEngine.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioEngine.stop()
        }
    }

    val successData = (hubState as? UiState.Success)?.data
    val profile = successData?.profile
    LaunchedEffect(profile?.musicOn, profile?.sfxOn) {
        audioEngine.musicEnabled = profile?.musicOn ?: true
        audioEngine.sfxEnabled = profile?.sfxOn ?: true
    }

    val inArena = activeRunConfig != null && profile != null
    AnimatedContent(
        targetState = inArena,
        transitionSpec = {
            (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                .togetherWith(fadeOut(tween(200)))
        },
        label = "hub_arena_transition"
    ) { showArena ->
        val runConfig = activeRunConfig
        if (showArena && runConfig != null && profile != null) {
            val targetSectorKey = if (runConfig.mode == GameMode.ENDLESS) 0L else runConfig.sectorIndex.toLong()
            val sectorGhosts = successData.ghosts.filter { it.sectorIndex == targetSectorKey }

            GameArenaScreen(
                runConfig = runConfig,
                controlMode = profile.controlModeEnum(),
                musicOn = profile.musicOn,
                sfxOn = profile.sfxOn,
                hapticsOn = profile.hapticsOn,
                bestEndlessScore = profile.bestEndlessScore,
                savedGhostsForSector = sectorGhosts,
                audioEngine = audioEngine,
                onToggleMute = {
                    val nextAudio = !(profile.musicOn || profile.sfxOn)
                    viewModel.updatePreferences(
                        controlMode = profile.controlModeEnum(),
                        musicOn = nextAudio,
                        sfxOn = nextAudio,
                        hapticsOn = profile.hapticsOn
                    )
                },
                onSelectControlMode = { newMode ->
                    viewModel.updatePreferences(
                        controlMode = newMode,
                        musicOn = profile.musicOn,
                        sfxOn = profile.sfxOn,
                        hapticsOn = profile.hapticsOn
                    )
                },
                onRunFinished = viewModel::onRunFinished,
                onRetry = { mode, sector -> viewModel.launchRun(mode, sector) },
                onExitArena = { viewModel.closeArena() }
            )
        } else {
            MainHubScreen(
                hubState = hubState,
                selectedSector = selectedSector,
                statusBanner = statusBanner,
                onClearBanner = viewModel::clearBanner,
                onSelectSector = viewModel::selectSector,
                onLaunchRun = viewModel::launchRun,
                onUpdatePreferences = viewModel::updatePreferences,
                onPurgeAllGhosts = viewModel::purgeGhosts
            )
        }
    }
}
