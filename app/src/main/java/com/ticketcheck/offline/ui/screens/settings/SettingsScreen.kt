package com.ticketcheck.offline.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassOutlineButton
import com.ticketcheck.offline.ui.components.GradientIconChip
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.theme.Gold
import com.ticketcheck.offline.ui.theme.Primary
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.ui.theme.WarningAmber
import com.ticketcheck.offline.utils.AppSound
import com.ticketcheck.offline.utils.SettingsStore
import com.ticketcheck.offline.utils.SoundEffects
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settings: SettingsStore, repository: TicketRepository, onBack: () -> Unit) {
    var soundOn by remember { mutableStateOf(settings.soundEnabled) }
    var vibrationOn by remember { mutableStateOf(settings.vibrationEnabled) }
    var darkTheme by remember { mutableStateOf(settings.darkTheme) }
    var showDemoDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PremiumTopBar("Settings", onBack) }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ── Feedback ───────────────────────────────────────────
                StaggeredAppear(0) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            SettingRow(Icons.AutoMirrored.Filled.VolumeUp, "Sound", "Scan results and UI tap sounds", soundOn) {
                                soundOn = it
                                settings.soundEnabled = it
                                SoundEffects.enabled = it
                            }
                            Spacer(Modifier.height(8.dp))
                            SettingRow(Icons.Filled.Vibration, "Vibration", "Haptic pulse on scan results", vibrationOn) {
                                vibrationOn = it
                                settings.vibrationEnabled = it
                            }
                            Spacer(Modifier.height(8.dp))
                            SettingRow(Icons.Filled.DarkMode, "Dark Theme", "Premium dark look (applies on restart)", darkTheme) {
                                darkTheme = it
                                settings.darkTheme = it
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Demo mode ──────────────────────────────────────────
                StaggeredAppear(1) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Row {
                                GradientIconChip(
                                    Icons.Filled.Science,
                                    listOf(WarningAmber, Color(0xFFF97316)),
                                    size = 40.dp,
                                    cornerRadius = 12.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Test / Demo Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Generates 10 sample tickets (SISH01-SISH10) so you can test scanning before the real event.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            GlassOutlineButton(
                                "GENERATE 10 TEST TICKETS",
                                onClick = { showDemoDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── About ──────────────────────────────────────────────
                StaggeredAppear(2) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.height(28.dp).fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("TicketCheck Offline v1.0", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "100% offline - no server, no cloud, no login.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDemoDialog) {
        AlertDialog(
            onDismissRequest = { showDemoDialog = false },
            title = { Text("Generate test tickets?") },
            text = { Text("This creates SISH01 through SISH10 for testing. Clearly marked as demo data - delete via Reset Event when done testing.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.generateSequentialTickets("SISH", 1, 10, 2) }
                    SoundEffects.play(AppSound.ARPEGGIO)
                    showDemoDialog = false
                }) { Text("GENERATE") }
            },
            dismissButton = { TextButton(onClick = { showDemoDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            GradientIconChip(
                icon,
                if (checked) listOf(Primary, Color(0xFF6366F1)) else listOf(Color(0xFF64748B), Color(0xFF475569)),
                size = 38.dp,
                cornerRadius = 11.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Primary,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.14f),
                uncheckedThumbColor = Color(0xFF94A3B8)
            )
        )
    }
}
