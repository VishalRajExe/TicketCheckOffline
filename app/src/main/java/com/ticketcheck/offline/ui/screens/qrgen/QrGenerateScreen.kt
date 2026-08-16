package com.ticketcheck.offline.ui.screens.qrgen

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassOutlineButton
import com.ticketcheck.offline.ui.components.GlassTextField
import com.ticketcheck.offline.ui.components.GradientButton
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.theme.Primary
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrGenerateScreen(
    viewModel: QrGenerateViewModel,
    repository: TicketRepository,
    initialTicketCode: String?,
    initialTicketCodes: List<String>? = null, // Support multiple
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    var codeInput by remember { mutableStateOf(initialTicketCode ?: "") }

    LaunchedEffect(initialTicketCode, initialTicketCodes) {
        if (!initialTicketCodes.isNullOrEmpty()) {
            viewModel.loadMultiple(initialTicketCodes)
        } else if (!initialTicketCode.isNullOrBlank()) {
            viewModel.load(initialTicketCode)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = if (state.items.size > 1) "QR Codes (${state.items.size})" else "Generate QR",
                    onBack = onBack
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
                if (state.items.size <= 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase() },
                            label = "Ticket Code",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(12.dp))
                        TextButton(onClick = { viewModel.load(codeInput) }) {
                            Text("LOAD", color = Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                when {
                    state.notFound -> Text(
                        "Ticket not found. Check the code and try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    state.items.isNotEmpty() -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(state.items.size) { index ->
                                val item = state.items[index]
                                StaggeredAppear(index) {
                                    QrItemView(item, context, scope)
                                }
                            }
                        }
                    }
                    else -> Text(
                        "Enter a ticket code above and tap LOAD to generate its QR.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun QrItemView(item: QrItem, context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
    val ticket = item.ticket
    val bmp = item.bitmap
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            ticket.ticketCode,
            style = MaterialTheme.typography.headlineMedium,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(16.dp))

        // The QR sits on a white plate inside a glass card so it scans
        // reliably against the dark background.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            shadowElevation = 12.dp
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "QR code for ${ticket.ticketCode}",
                modifier = Modifier
                    .size(248.dp)
                    .padding(18.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ticket.customerName?.let {
                Text("Customer: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ticket.ticketType?.let {
                Text("Type: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ticket.price?.let {
                Text("Price: ₹%.2f".format(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientButton(
                "SAVE QR",
                icon = Icons.Filled.Save,
                onClick = { scope.launch { saveQrToFile(context, ticket.ticketCode, bmp) } },
                height = 50.dp
            )
            GlassOutlineButton(
                "SHARE QR",
                icon = Icons.Filled.Share,
                onClick = { scope.launch { shareQr(context, ticket.ticketCode, bmp) } },
                height = 50.dp
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

private suspend fun saveQrToFile(context: android.content.Context, code: String, bmp: Bitmap) {
    val dir = File(context.getExternalFilesDir(null), "qrcodes").apply { mkdirs() }
    val file = File(dir, "$code.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

private suspend fun shareQr(context: android.content.Context, code: String, bmp: Bitmap) {
    val dir = File(context.getExternalFilesDir(null), "qrcodes").apply { mkdirs() }
    val file = File(dir, "$code.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR for $code"))
}
