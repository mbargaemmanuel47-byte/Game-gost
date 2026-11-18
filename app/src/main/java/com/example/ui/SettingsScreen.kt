package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.ControlMode
import com.example.data.UserProfile
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
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "CONFIGURATION PILOTE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonCyan
                )
            }

            // Tactile Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, OutlineGlow),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = NeonCyan)
                            Text(
                                text = "MODE DE CONTRÔLE TACTILE",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonCyan
                            )
                        }

                        ControlMode.entries.forEach { mode ->
                            val selected = mode == currentMode
                            Card(
                                onClick = {
                                    onUpdatePreferences(mode, profile.musicOn, profile.sfxOn, profile.hapticsOn)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("control_mode_${mode.name}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) NeonCyan.copy(alpha = 0.14f) else VoidBlack.copy(alpha = 0.55f)
                                ),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) NeonCyan else OutlineGlow
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
                                        Text(
                                            text = mode.labelFr,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) NeonCyan else TextPrimary
                                        )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, OutlineGlow),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "AUDIO SYNTHWAVE & RETOUR",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonMagenta
                        )

                        SettingToggleRow(
                            icon = Icons.Filled.MusicNote,
                            title = "Bande-son Synthwave",
                            subtitle = "Synthétiseur temps réel 124 BPM",
                            checked = profile.musicOn,
                            tag = "toggle_music",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, checked, profile.sfxOn, profile.hapticsOn)
                            }
                        )
                        SettingToggleRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "Effets Sonores Néon",
                            subtitle = "Frôlements, orbes et collisions",
                            checked = profile.sfxOn,
                            tag = "toggle_sfx",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, profile.musicOn, checked, profile.hapticsOn)
                            }
                        )
                        SettingToggleRow(
                            icon = Icons.Filled.Vibration,
                            title = "Vibrations Tactiles",
                            subtitle = "Impulsions haptiques de précision",
                            checked = profile.hapticsOn,
                            tag = "toggle_haptics",
                            onCheckedChange = { checked ->
                                onUpdatePreferences(currentMode, profile.musicOn, profile.sfxOn, checked)
                            }
                        )
                    }
                }
            }

            // Optional Cloud Sync Card (No account required!)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, OutlineGlow),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
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
                                        "Sauvegarde Cloud active (${profile.displayName})"
                                    } else {
                                        "Mode Local (sans compte)"
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
                                text = "Vos scores et fantômes sont enregistrés sur cet appareil. Vous pouvez connecter Google à tout moment pour les synchroniser dans le cloud (optionnel).",
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
                            Button(
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("optional_google_sign_in_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.16f),
                                    contentColor = NeonCyan
                                ),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
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
                                    Text("SYNCHRONISER AVEC GOOGLE (OPTIONNEL)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { signOut(credentialManager, scope) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button"),
                                border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCoral)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("PASSER EN MODE LOCAL", style = MaterialTheme.typography.labelLarge)
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
                uncheckedTrackColor = VoidBlack
            )
        )
    }
}
