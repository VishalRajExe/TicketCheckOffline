package com.ticketcheck.offline.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.domain.models.DashboardStats
import com.ticketcheck.offline.ui.components.AnimatedCounter
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassCircleButton
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.GradientIconChip
import com.ticketcheck.offline.ui.components.GradientProgressBar
import com.ticketcheck.offline.ui.components.PulsingDot
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.components.StatusPill
import com.ticketcheck.offline.ui.theme.Accent
import com.ticketcheck.offline.ui.theme.BrandGradient
import com.ticketcheck.offline.ui.theme.ErrorRed
import com.ticketcheck.offline.ui.theme.Gold
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.ui.theme.WarningAmber
import com.ticketcheck.offline.utils.AppSound
import com.ticketcheck.offline.utils.SoundEffects

@Composable
fun HomeScreen(
    app: com.ticketcheck.offline.TicketCheckApp,
    viewModel: HomeViewModel,
    onScan: () -> Unit,
    onManage: () -> Unit,
    onTicketList: () -> Unit,
    onGenerateQr: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val event by viewModel.event.collectAsState()
    var showEditEvent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showEditEvent && event != null) {
        com.ticketcheck.offline.ui.screens.onboarding.OnboardingScreen(
            app = app,
            currentEvent = event,
            onEventCreated = { showEditEvent = false },
            onCancel = { showEditEvent = false }
        )
    } else {
        Box(Modifier.fillMaxSize()) {
            AuroraBackground()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // ── Header ──────────────────────────────────────────────
                StaggeredAppear(0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(Success)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "LIVE EVENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                event?.name ?: "TicketCheck Offline",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            event?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${it.venue}  •  ${it.eventDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        GlassCircleButton(Icons.Filled.Edit, onClick = { showEditEvent = true })
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Dashboard ───────────────────────────────────────────
                StaggeredAppear(1) {
                    StatsCard(stats)
                }

                Spacer(Modifier.height(26.dp))

                // ── Primary CTA ────────────────────────────────────────
                StaggeredAppear(2) {
                    ScanCta(onScan)
                }

                Spacer(Modifier.height(26.dp))

                // ── Action tiles ───────────────────────────────────────
                val actions = listOf(
                    ActionTile("Manage Tickets", Icons.Filled.ConfirmationNumber, listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)), onManage),
                    ActionTile("Ticket List", Icons.AutoMirrored.Filled.ListAlt, listOf(Color(0xFF38BDF8), Color(0xFF2563EB)), onTicketList),
                    ActionTile("Generate QR", Icons.Filled.QrCode2, listOf(Color(0xFF34D399), Color(0xFF0EA5E9)), onGenerateQr),
                    ActionTile("Scan History", Icons.Filled.History, listOf(Color(0xFFFBBF24), Color(0xFFF97316)), onHistory),
                    ActionTile("Backup", Icons.Filled.Backup, listOf(Color(0xFFF472B6), Color(0xFF8B5CF6)), onBackup),
                    ActionTile("Settings", Icons.Filled.Settings, listOf(Color(0xFF94A3B8), Color(0xFF475569)), onSettings)
                )

                actions.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEachIndexed { colIndex, tile ->
                            StaggeredAppear(3 + rowIndex * 2 + colIndex, modifier = Modifier.weight(1f)) {
                                ActionTileCard(tile)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Offline badge ───────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "100% OFFLINE — NO DATA LEAVES THIS DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanCta(onScan: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ctaGlow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = glow * 0.30f }
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(BrandGradient))
        )
        GradientButton(
            text = "SCAN TICKET",
            icon = Icons.Filled.QrCodeScanner,
            onClick = onScan,
            height = 64.dp,
            cornerRadius = 22.dp,
            sound = AppSound.OPEN,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class ActionTile(
    val label: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun ActionTileCard(tile: ActionTile) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tilePress"
    )
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null) {
                SoundEffects.play(AppSound.CLICK)
                tile.onClick()
            },
        cornerRadius = 22.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            GradientIconChip(tile.icon, tile.colors)
            Spacer(Modifier.height(14.dp))
            Text(
                tile.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatsCard(stats: DashboardStats) {
    val checkInPct = if (stats.totalTickets > 0) stats.usedTickets.toFloat() / stats.totalTickets else 0f

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBlock("TOTAL", stats.totalTickets, MaterialTheme.colorScheme.onSurface)
                StatBlock("USED", stats.usedTickets, Success)
                StatBlock("REMAINING", stats.remainingTickets, Accent)
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "CHECK-IN PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${(checkInPct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Success
                )
            }
            Spacer(Modifier.height(8.dp))
            GradientProgressBar(checkInPct)

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("VALID SCANS", stats.validScans, Success)
                MiniStat("ALREADY USED", stats.alreadyUsedScans, WarningAmber)
                MiniStat("INVALID", stats.invalidScans, ErrorRed)
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCounter(value = value, color = color)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            AnimatedCounter(value = value, color = color, fontSize = 18.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
