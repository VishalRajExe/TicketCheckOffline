package com.ticketcheck.offline.ui.screens.ticketlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCircleButton
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.components.StatusPill
import com.ticketcheck.offline.ui.theme.ErrorRed
import com.ticketcheck.offline.ui.theme.Primary
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.ui.theme.WarningAmber
import com.ticketcheck.offline.ui.navigation.Routes

@Composable
fun TicketListScreen(
    navController: androidx.navigation.NavController, // Pass navController for bulk QR navigation
    viewModel: TicketListViewModel,
    onBack: () -> Unit,
    onTicketClick: (Long) -> Unit
) {
    val tickets by viewModel.visibleTickets.collectAsState()
    val filter by viewModel.currentFilter.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    val isSelectionMode = selectedIds.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isSelectionMode) {
                    PremiumTopBar(
                        title = "${selectedIds.size} selected",
                        onBack = { selectedIds = emptySet() },
                        actions = {
                            GlassCircleButton(
                                Icons.Filled.QrCode2,
                                onClick = {
                                    val selectedCodes = tickets.filter { it.id in selectedIds }.map { it.ticketCode }
                                    navController.navigate(Routes.qrGenerateBulk(selectedCodes))
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            GlassCircleButton(
                                Icons.Filled.Delete,
                                onClick = {
                                    viewModel.deleteTickets(selectedIds)
                                    selectedIds = emptySet()
                                },
                                tint = ErrorRed
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                    )
                } else {
                    PremiumTopBar(title = "Ticket List (${tickets.size})", onBack = onBack)
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                if (!isSelectionMode) {
                    GlassTextField(
                        value = query,
                        onValueChange = { query = it; viewModel.setQuery(it) },
                        label = "Search by code or customer name",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        leadingIcon = Icons.Filled.Search
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumFilterChip("ALL", filter == TicketFilter.ALL) { viewModel.setFilter(TicketFilter.ALL) }
                        PremiumFilterChip("VALID", filter == TicketFilter.VALID) { viewModel.setFilter(TicketFilter.VALID) }
                        PremiumFilterChip("USED", filter == TicketFilter.USED) { viewModel.setFilter(TicketFilter.USED) }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(tickets, key = { _, t -> t.id }) { index, ticket ->
                        val isSelected = selectedIds.contains(ticket.id)
                        StaggeredAppear(index) {
                            TicketRow(
                                ticket = ticket,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - ticket.id else selectedIds + ticket.id
                                    } else {
                                        onTicketClick(ticket.id)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + ticket.id
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PremiumFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) Brush.linearGradient(listOf(Primary, Color(0xFF6366F1)))
                else SolidColor(Color.White.copy(alpha = 0.06f))
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketRow(ticket: TicketEntity, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val statusColor = if (ticket.status == TicketStatus.USED) WarningAmber else Success
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        cornerRadius = 18.dp,
        fill = if (isSelected) Primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
        borderColor = if (isSelected) Primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ticket.ticketCode,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ticket.customerName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusPill(if (ticket.status == TicketStatus.USED) "USED" else "VALID", statusColor)
        }
    }
}
