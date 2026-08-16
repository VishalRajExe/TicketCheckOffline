package com.ticketcheck.offline.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.TicketCheckApp
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.GradientIconChip
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.theme.BrandGradient
import com.ticketcheck.offline.utils.AppSound
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    app: TicketCheckApp,
    currentEvent: EventEntity? = null,
    onEventCreated: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(currentEvent?.name ?: "") }
    var date by remember { mutableStateOf(currentEvent?.eventDate ?: "") }
    var venue by remember { mutableStateOf(currentEvent?.venue ?: "") }
    var prefix by remember { mutableStateOf(currentEvent?.ticketPrefix ?: "") }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            StaggeredAppear(0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GradientIconChip(
                        icon = Icons.Filled.QrCodeScanner,
                        colors = BrandGradient,
                        size = 76.dp,
                        cornerRadius = 24.dp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (currentEvent == null) "Welcome" else "Edit Event",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (currentEvent == null) "Create your first event to get started."
                        else "Update your event details below.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    StaggeredAppear(1) {
                        GlassTextField(name, { name = it }, "Event Name", Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    StaggeredAppear(2) {
                        GlassTextField(date, { date = it }, "Event Date (e.g. 16 August 2026)", Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    StaggeredAppear(3) {
                        GlassTextField(venue, { venue = it }, "Venue", Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    StaggeredAppear(4) {
                        GlassTextField(prefix, { prefix = it.uppercase() }, "Ticket Prefix (e.g. SISH)", Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            StaggeredAppear(5) {
                GradientButton(
                    text = if (currentEvent == null) "CREATE EVENT" else "SAVE CHANGES",
                    onClick = {
                        scope.launch {
                            app.repository.createOrUpdateEvent(
                                EventEntity(
                                    id = currentEvent?.id ?: 0,
                                    name = name.ifBlank { "My Event" },
                                    eventDate = date,
                                    venue = venue,
                                    ticketPrefix = prefix.ifBlank { "TKT" }
                                )
                            )
                            onEventCreated()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    sound = AppSound.ARPEGGIO
                )
            }

            onCancel?.let {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
