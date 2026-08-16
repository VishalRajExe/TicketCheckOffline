package com.ticketcheck.offline.ui.screens.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassOutlineButton
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.GradientIconChip
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.theme.DangerGradient
import com.ticketcheck.offline.ui.theme.ErrorRed
import com.ticketcheck.offline.ui.theme.WarningAmber
import com.ticketcheck.offline.utils.AppSound
import com.ticketcheck.offline.utils.SoundEffects
import com.ticketcheck.offline.utils.BackupManager
import java.io.File

@Composable
fun BackupScreen(viewModel: BackupViewModel, backupManager: BackupManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val event by viewModel.event.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetUsedOnlyDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val tmp = File(context.cacheDir, "restore_import.json")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            pendingRestoreFile = tmp
        }
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is BackupUiEvent.ExportSuccess -> {
                SoundEffects.play(AppSound.ARPEGGIO)
                val uri = backupManager.shareUri(e.file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share backup"))
                snackbarHostState.showSnackbar("Backup saved: ${e.file.name}")
                viewModel.clearEvent()
            }
            is BackupUiEvent.RestoreDone -> {
                SoundEffects.play(AppSound.ARPEGGIO)
                snackbarHostState.showSnackbar("Restored ${e.ticketsRestored} tickets.")
                viewModel.clearEvent()
            }
            is BackupUiEvent.Error -> {
                SoundEffects.play(AppSound.ERROR)
                snackbarHostState.showSnackbar(e.message)
                viewModel.clearEvent()
            }
            null -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { PremiumTopBar("Backup / Restore", onBack) }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ── Export ──────────────────────────────────────────────
                StaggeredAppear(0) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            SectionHeader(
                                Icons.Filled.Download,
                                "Backup Data",
                                "Exports event, tickets, statuses and scan history to a JSON file on this device, ready to share."
                            )
                            Spacer(Modifier.height(16.dp))
                            GradientButton(
                                "BACKUP DATA",
                                onClick = { viewModel.exportBackup() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Restore ─────────────────────────────────────────────
                StaggeredAppear(1) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            SectionHeader(
                                Icons.Filled.Upload,
                                "Restore Backup",
                                "Reads a previously exported JSON file back into the database. Restoring may replace current ticket data."
                            )
                            Spacer(Modifier.height(16.dp))
                            GlassOutlineButton(
                                "RESTORE BACKUP",
                                onClick = { filePicker.launch("application/json") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Danger zone ─────────────────────────────────────────
                StaggeredAppear(2) {
                    GlassCard(
                        Modifier.fillMaxWidth(),
                        fill = ErrorRed.copy(alpha = 0.08f),
                        borderColor = ErrorRed.copy(alpha = 0.30f)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            SectionHeader(
                                Icons.Filled.DeleteForever,
                                "Danger Zone",
                                "Irreversible operations on this device's data.",
                                iconColors = listOf(Color(0xFFDC2626), Color(0xFFEF4444)),
                                titleColor = ErrorRed
                            )
                            Spacer(Modifier.height(16.dp))
                            GlassOutlineButton(
                                "RESET ONLY USED STATUS",
                                icon = Icons.Filled.RestartAlt,
                                onClick = { resetUsedOnlyDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            GradientButton(
                                "RESET EVENT",
                                onClick = { showResetDialog = true },
                                colors = DangerGradient,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    pendingRestoreFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestoreFile = null },
            title = { Text("Restore Backup") },
            text = { Text("Restoring a backup may replace current ticket data.\n\nContinue?") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreBackup(file); pendingRestoreFile = null }) { Text("CONTINUE") }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreFile = null }) { Text("CANCEL") } }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("WARNING: RESET EVENT") },
            text = { Text("This will PERMANENTLY DELETE ALL tickets, scan history, and this event configuration.\n\nAre you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        SoundEffects.play(AppSound.DELETE)
                        viewModel.resetEverything()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE EVERYTHING") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") } }
        )
    }

    if (resetUsedOnlyDialog) {
        AlertDialog(
            onDismissRequest = { resetUsedOnlyDialog = false },
            title = { Text("Reset USED status") },
            text = { Text("This lets tickets be scanned again (useful for testing before the real event). Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    SoundEffects.play(AppSound.TOGGLE)
                    viewModel.resetUsedOnly()
                    resetUsedOnlyDialog = false
                }) { Text("RESET") }
            },
            dismissButton = { TextButton(onClick = { resetUsedOnlyDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColors: List<Color> = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row {
        GradientIconChip(icon, iconColors, size = 40.dp, cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
