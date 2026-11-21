package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.ControlMode
import com.example.data.UserProfile
import com.example.ui.components.BouncyPillButton
import com.example.ui.components.SoftGlassCard
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OutlineGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun SettingsTabContent(
    profile: UserProfile,
    isCloudLinked: Boolean,
    onUpdatePreferences: (ControlMode, Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val canUseGoogleCloud = remember(context) {
        isFirebaseAvailable(context) && resolveDefaultWebClientId(context) != null
    }
    val currentMode = profile.controlModeEnum()
    var isAuthLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Mon Pilote",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "Personnalisez le pilotage, la musique et vos sauvegardes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Tactile Controls Card
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = NeonCyan.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = NeonCyan)
                            Text(
                                text = "Contrôle tactile",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }

                        ControlMode.entries.forEach { mode ->
                            val selected = mode == currentMode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onUpdatePreferences(mode, profile.musicOn, profile.sfxOn, profile.hapticsOn)
                                    }
                                    .testTag("control_mode_${mode.name}"),
                                color = if (selected) NeonCyan.copy(alpha = 0.14f) else DeepSurface.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) NeonCyan.copy(alpha = 0.85f) else OutlineGlow
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (selected) NeonCyan else TextSecondary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = mode.labelFr,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (selected) NeonCyan else TextPrimary
                                            )
                                            if (mode == ControlMode.SPLIT) {
                                                Surface(
                                                    color = NeonLime.copy(alpha = 0.18f),
                                                    shape = CircleShape
                                                ) {
                                                    Text(
                                                        text = "Recommandé",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = NeonLime,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = mode.subtitleFr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Synthwave Audio & Haptics Card
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = NeonMagenta.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Son & sensations",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonMagenta
                        )

                        SettingToggleRow(
                            icon = Icons.Filled.MusicNote,
                            title = "Musique Synthwave",
                            subtitle = "Nappe mélodique douce en temps réel",
                            checked = profile.musicOn,
                            tag = "toggle_music",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, checked, profile.sfxOn, profile.hapticsOn)
                            }
                        )
                        SettingToggleRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "Effets sonores",
                            subtitle = "Orbes, frôlements et portails",
                            checked = profile.sfxOn,
                            tag = "toggle_sfx",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, profile.musicOn, checked, profile.hapticsOn)
                            }
                        )
                        SettingToggleRow(
                            icon = Icons.Filled.Vibration,
                            title = "Vibrations douces",
                            subtitle = "Retours tactiles légers",
                            checked = profile.hapticsOn,
                            tag = "toggle_haptics",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, profile.musicOn, profile.sfxOn, checked)
                            }
                        )
                    }
                }
            }

            // Optional Cloud Sync Card
            item {
                SoftGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = OutlineGlow
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isCloudLinked) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (isCloudLinked) NeonLime else TextSecondary
                            )
                            Column {
                                Text(
                                    text = if (isCloudLinked) {
                                        "Cloud actif (${profile.displayName})"
                                    } else {
                                        "Sauvegarde locale active"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${profile.totalRuns} vols • ${profile.totalNearMisses} frôlements",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (!isCloudLinked) {
                            Text(
                                text = if (canUseGoogleCloud) {
                                    "Tout est sauvegardé instantanément sur cet appareil. Connectez Google seulement si vous souhaitez retrouver vos scores ailleurs."
                                } else {
                                    "Tout est sauvegardé instantanément sur cet appareil. Jouez librement hors-ligne sans aucun compte."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (authError != null) {
                                Text(
                                    text = authError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonCoral
                                )
                            }
                            if (canUseGoogleCloud) {
                                BouncyPillButton(
                                    onClick = {
                                        authError = null
                                        isAuthLoading = true
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = { isAuthLoading = false },
                                            onAuthError = { msg ->
                                                isAuthLoading = false
                                                authError = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = { isAuthLoading = false }
                                        )
                                    },
                                    enabled = !isAuthLoading,
                                    containerColor = NeonCyan.copy(alpha = 0.18f),
                                    contentColor = NeonCyan,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("optional_google_sign_in_button")
                                ) {
                                    if (isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sauvegarder avec Google (optionnel)", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { signOut(credentialManager, scope) },
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button"),
                                border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Passer en mode local", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (checked) NeonCyan else TextSecondary)
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBlack,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DeepSurface,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
