package com.ticketcheck.offline.ui.screens.manage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.GlassOutlineButton
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.GradientIconChip
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.theme.Accent
import com.ticketcheck.offline.ui.theme.BrandGradient
import com.ticketcheck.offline.ui.theme.Primary
import com.ticketcheck.offline.utils.AppSound

private enum class ManageTab { GENERATE, ADD, IMPORT }

@Composable
fun ManageTicketsScreen(viewModel: ManageTicketsViewModel, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(ManageTab.GENERATE) }
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { PremiumTopBar("Manage Tickets", onBack) }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                TabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[tab.ordinal])
                                .padding(horizontal = 28.dp)
                                .height(3.dp)
                                .background(
                                    Brush.linearGradient(BrandGradient),
                                    RoundedCornerShape(50)
                                )
                        )
                    },
                    divider = {}
                ) {
                    ManageTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = {
                                Text(
                                    when (t) {
                                        ManageTab.GENERATE -> "Generate"
                                        ManageTab.ADD -> "Add Ticket"
                                        ManageTab.IMPORT -> "Import"
                                    },
                                    fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(260)) + slideInVertically(
                            tween(300, easing = FastOutSlowInEasing)
                        ) { it / 16 }) togetherWith fadeOut(tween(140))
                    },
                    label = "tabContent"
                ) { currentTab ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        when (currentTab) {
                            ManageTab.GENERATE -> GenerateTab(viewModel)
                            ManageTab.ADD -> AddTicketTab(viewModel)
                            ManageTab.IMPORT -> ImportTab(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GradientIconChip(icon, BrandGradient, size = 40.dp, cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GenerateTab(viewModel: ManageTicketsViewModel) {
    var prefix by remember { mutableStateOf("SISH") }
    var count by remember { mutableStateOf("100") }
    var start by remember { mutableStateOf("1") }
    var padding by remember { mutableStateOf("2") }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            TabHeader(Icons.Filled.AutoAwesome, "Generate ticket codes", "Sequential codes with zero padding")

            Spacer(Modifier.height(18.dp))
            GlassTextField(prefix, { prefix = it.uppercase() }, "Prefix", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            GlassTextField(start, { start = it.filter(Char::isDigit) }, "Starting number", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            GlassTextField(count, { count = it.filter(Char::isDigit) }, "Number of tickets", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            GlassTextField(padding, { padding = it.filter(Char::isDigit) }, "Zero padding (digits)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(16.dp))

            val previewCount = count.toIntOrNull() ?: 0
            val previewStart = start.toIntOrNull() ?: 1
            val previewPad = padding.toIntOrNull() ?: 2
            Text(
                "Preview:  ${prefix}${previewStart.toString().padStart(previewPad, '0')}  →  ${prefix}${(previewStart + maxOf(previewCount - 1, 0)).toString().padStart(previewPad, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = Accent
            )
            Spacer(Modifier.height(18.dp))
            GradientButton(
                "GENERATE TICKETS",
                onClick = { viewModel.generateSequential(prefix, previewStart, previewCount, previewPad) },
                modifier = Modifier.fillMaxWidth(),
                sound = AppSound.ARPEGGIO
            )
        }
    }
}

@Composable
private fun AddTicketTab(viewModel: ManageTicketsViewModel) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            TabHeader(Icons.Filled.Add, "Add a single ticket", "Manually register one ticket code")

            Spacer(Modifier.height(18.dp))
            GlassTextField(code, { code = it.uppercase() }, "Ticket Code", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            GlassTextField(name, { name = it }, "Customer Name (optional)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            GlassTextField(type, { type = it }, "Ticket Type (optional)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            GlassTextField(price, { price = it.filter { c -> c.isDigit() || c == '.' } }, "Price (optional)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(18.dp))
            GradientButton(
                "+ ADD TICKET",
                onClick = {
                    viewModel.addManualTicket(code, name, type, price.toDoubleOrNull())
                    code = ""; name = ""; type = ""; price = ""
                },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                sound = AppSound.ARPEGGIO
            )
        }
    }
}

@Composable
private fun ImportTab(viewModel: ManageTicketsViewModel) {
    val context = LocalContext.current
    var pastedContent by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                viewModel.importFromText(it.readText())
            }
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            TabHeader(Icons.Filled.UploadFile, "Bulk import", "From a CSV or TXT file on this device")

            Spacer(Modifier.height(14.dp))
            Text(
                "Supported: one code per line, or ticketCode,customerName,ticketType,price",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            GradientButton("CHOOSE FILE", onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(22.dp))
            Text("Or paste ticket codes directly:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            GlassTextField(
                pastedContent,
                { pastedContent = it },
                "SISH01\nSISH02\nSISH03 ...",
                Modifier.fillMaxWidth().height(160.dp),
                singleLine = false
            )
            Spacer(Modifier.height(14.dp))
            GlassOutlineButton(
                "IMPORT PASTED CODES",
                onClick = { viewModel.importFromText(pastedContent); pastedContent = "" },
                modifier = Modifier.fillMaxWidth(),
                enabled = pastedContent.isNotBlank()
            )
        }
    }
}
