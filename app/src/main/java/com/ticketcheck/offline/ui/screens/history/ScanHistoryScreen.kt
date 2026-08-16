package com.ticketcheck.offline.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.ScanResult
import com.ticketcheck.offline.ui.components.AuroraBackground
import com.ticketcheck.offline.ui.components.GlassCard
import com.ticketcheck.offline.ui.components.PremiumTopBar
import com.ticketcheck.offline.ui.components.StaggeredAppear
import com.ticketcheck.offline.ui.components.StatusPill
import com.ticketcheck.offline.ui.theme.ErrorRed
import com.ticketcheck.offline.ui.theme.Primary
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanHistoryScreen(viewModel: ScanHistoryViewModel, onBack: () -> Unit) {
    val entries by viewModel.visible.collectAsState()
    val filter by viewModel.currentFilter.collectAsState()
    val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PremiumTopBar("Scan History", onBack) }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryFilterChip("ALL", filter == HistoryFilter.ALL) { viewModel.setFilter(HistoryFilter.ALL) }
                    HistoryFilterChip("VALID", filter == HistoryFilter.VALID) { viewModel.setFilter(HistoryFilter.VALID) }
                    HistoryFilterChip("USED", filter == HistoryFilter.ALREADY_USED) { viewModel.setFilter(HistoryFilter.ALREADY_USED) }
                    HistoryFilterChip("INVALID", filter == HistoryFilter.INVALID) { viewModel.setFilter(HistoryFilter.INVALID) }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                        StaggeredAppear(index) { HistoryRow(entry, fmt) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Primary else Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun HistoryRow(entry: ScanHistoryEntity, fmt: SimpleDateFormat) {
    val color = when (entry.result) {
        ScanResult.VALID -> Success
        ScanResult.ALREADY_USED -> WarningAmber
        ScanResult.INVALID -> ErrorRed
    }
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.06f)
            ) {
                Text(
                    fmt.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Text(
                entry.scannedValue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatusPill(entry.result.name.replace("_", " "), color, fontSize = 10.sp)
        }
    }
}
