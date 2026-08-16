package com.ticketcheck.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ticketcheck.offline.ui.navigation.TicketCheckNavGraph
import com.ticketcheck.offline.ui.theme.TicketCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TicketCheckApp

        setContent {
            var darkTheme by remember { mutableStateOf(app.settings.darkTheme) }
            TicketCheckTheme(darkTheme = darkTheme) {
                // Transparent so each screen's animated aurora background
                // shows through edge to edge.
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    TicketCheckNavGraph(app = app)
                }
            }
        }
    }
}
