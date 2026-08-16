package com.ticketcheck.offline.ui.screens.ticketdetail

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassCircleButton
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.components.StatusPill
import com.ticketcheck.offline.ui.theme.ErrorRed
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.ui.theme.WarningAmber
import com.ticketcheck.offline.utils.AppSound
import com.ticketcheck.offline.utils.SoundEffects
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TicketDetailScreen(viewModel: TicketDetailViewModel, ticketId: Long, onBack: () -> Unit, onGenerateQr: (String) -> Unit) {
    LaunchedEffect(ticketId) { viewModel.load(ticketId) }
    val ticket by viewModel.ticket.collectAsState()
    val fmt = remember { SimpleDateFormat("d MMM yyyy  h:mm a", Locale.getDefault()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Ticket Details",
                    onBack = onBack,
                    actions = {
                        GlassCircleButton(Icons.Filled.Edit, onClick = { showEditDialog = true })
                        Spacer(Modifier.width(10.dp))
                        GlassCircleButton(Icons.Filled.Delete, onClick = { showDeleteDialog = true }, tint = ErrorRed)
                        Spacer(Modifier.width(6.dp))
                    }
                )
            }
        ) { padding ->
            val t = ticket
            if (t == null) {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Scaffold
            }

            val statusColor = if (t.status == TicketStatus.USED) WarningAmber else Success

            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                StaggeredAppear(0) {
                    GlassCard(
                        Modifier.fillMaxWidth(),
                        glowColor = statusColor,
                        fill = statusColor.copy(alpha = 0.08f),
                        borderColor = statusColor.copy(alpha = 0.30f)
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    t.ticketCode,
                                    style = MaterialTheme.typography.headlineMedium,
                                    letterSpacing = 1.sp
                                )
                                StatusPill(
                                    if (t.status == TicketStatus.USED) "USED" else "VALID",
                                    statusColor
                                )
                            }
                            t.customerName?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                StaggeredAppear(1) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp)) {
                            DetailRow("TICKET TYPE", t.ticketType ?: "—")
                            DetailRow("PRICE", t.price?.let { "₹%.2f".format(it) } ?: "—")
                            DetailRow("CREATED", fmt.format(Date(t.createdAt)))
                            DetailRow(
                                "USED",
                                if (t.status == TicketStatus.USED && t.usedAt != null) fmt.format(Date(t.usedAt)) else "Never"
                            )
                            DetailRow("SCAN COUNT", t.scanCount.toString(), last = true)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                StaggeredAppear(2) {
                    GradientButton(
                        "VIEW / SHARE QR CODE",
                        icon = Icons.Filled.QrCode2,
                        onClick = { onGenerateQr(t.ticketCode) },
                        modifier = Modifier.fillMaxWidth(),
                        height = 58.dp
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showEditDialog && ticket != null) {
        EditTicketDialog(
            ticket = ticket!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, type, price ->
                viewModel.updateTicket(name, type, price)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && ticket != null) {
        val t = ticket!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ticket") },
            text = { Text("Are you sure you want to delete ticket ${t.ticketCode}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        SoundEffects.play(AppSound.DELETE)
                        viewModel.deleteTicket(onDone = onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun EditTicketDialog(
    ticket: TicketEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double?) -> Unit
) {
    var name by remember { mutableStateOf(ticket.customerName ?: "") }
    var type by remember { mutableStateOf(ticket.ticketType ?: "") }
    var priceStr by remember { mutableStateOf(ticket.price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Ticket") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Ticket Type") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull()
                onConfirm(name, type, price)
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String, last: Boolean = false) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    if (!last) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(vertical = 0.dp)
        )
    }
}
