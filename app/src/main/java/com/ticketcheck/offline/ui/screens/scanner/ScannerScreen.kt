package com.ticketcheck.offline.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ticketcheck.offline.domain.models.ScanOutcome
import com.ticketcheck.offline.scanner.QrAnalyzer
import com.ticketcheck.offline.ui.components.GlassCircleButton
import com.ticketcheck.offline.ui.components.GlassOutlineButton
import com.ticketcheck.offline.ui.components.PulseRings
import com.ticketcheck.offline.ui.components.SpringInIcon
import com.ticketcheck.offline.ui.components.StaggeredAppear
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

@Composable
fun ScannerScreen(viewModel: ScannerViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // The result overlay animates OUT while the state already flipped back to
    // Scanning - keep the last outcome around so the exit animation can render it.
    var lastResult by remember { mutableStateOf<ScanOutcome?>(null) }
    (uiState as? ScannerUiState.Result)?.let { lastResult = it.outcome }
    val showResult = uiState is ScannerUiState.Result

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraPermission) {
            CameraPermissionDenied(onExit)
            return@Box
        }

        CameraPreview(onQrDetected = { viewModel.onQrDetected(it) })

        AnimatedVisibility(
            visible = !showResult,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            ScanFrameOverlay()
        }

        AnimatedVisibility(
            visible = showResult,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 1.04f, animationSpec = tween(220)),
            exit = fadeOut(tween(240)),
            modifier = Modifier.fillMaxSize()
        ) {
            lastResult?.let { ResultOverlay(it, onScanNext = { viewModel.resumeScanning() }) }
        }

        // Floating top bar
        AnimatedVisibility(
            visible = !showResult,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(Icons.Filled.Cancel, onClick = onExit)
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        "SCANNER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val analyzer = remember { QrAnalyzer(onQrDetected) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer) }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                    // Camera init failure - screen simply stays blank rather than crashing.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScanFrameOverlay() {
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "laser"
    )

    Box(Modifier.fillMaxSize()) {
        // Soft vignette around the edges so the frame reads clearly.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    radius = size.minDimension * 1.1f
                )
            )
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(272.dp)) {
                val inset = 6.dp.toPx()
                val frame = size.width - inset * 2
                val cornerLen = frame * 0.22f
                val stroke = 5.dp.toPx()
                val brush = Brush.linearGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6)),
                    start = Offset(inset, inset),
                    end = Offset(inset + frame, inset + frame)
                )

                // Four corner brackets
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(brush, Offset(x, y), Offset(x + dx * cornerLen, y), stroke, StrokeCap.Round)
                    drawLine(brush, Offset(x, y), Offset(x, y + dy * cornerLen), stroke, StrokeCap.Round)
                }
                corner(inset, inset, 1f, 1f)
                corner(inset + frame, inset, -1f, 1f)
                corner(inset, inset + frame, 1f, -1f)
                corner(inset + frame, inset + frame, -1f, -1f)

                // Sweeping laser - triangle wave so it ping-pongs.
                val p = abs(sweep * 2f - 1f) // 1 -> 0 -> 1
                val y = inset + frame * (1f - p)
                val laser = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFF38BDF8).copy(alpha = 0.95f), Color.Transparent),
                    startX = inset,
                    endX = inset + frame
                )
                drawLine(laser, Offset(inset, y), Offset(inset + frame, y), 3.dp.toPx(), StrokeCap.Round)
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF38BDF8).copy(alpha = 0.25f), Color.Transparent),
                        startX = inset,
                        endX = inset + frame
                    ),
                    Offset(inset, y - 6.dp.toPx()),
                    Offset(inset + frame, y - 6.dp.toPx()),
                    9.dp.toPx()
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Text(
                "Point the camera at the ticket's QR code",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

private data class ResultVisual(
    val colorDeep: Color,
    val colorMid: Color,
    val icon: ImageVector,
    val title: String,
    val code: String,
    val subtitle: String,
    val detail: String?
)

@Composable
private fun ResultOverlay(outcome: ScanOutcome, onScanNext: () -> Unit) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy  h:mm a", Locale.getDefault()) }
    val visual = when (outcome) {
        is ScanOutcome.Valid -> ResultVisual(
            Color(0xFF07361F), Color(0xFF10B981), Icons.Filled.Verified,
            "VALID TICKET", outcome.ticket.ticketCode,
            "ENTRY ALLOWED", outcome.ticket.ticketType ?: "General Admission"
        )
        is ScanOutcome.AlreadyUsed -> ResultVisual(
            Color(0xFF3D2405), Color(0xFFF59E0B), Icons.Filled.Cancel,
            "ALREADY USED", outcome.ticket.ticketCode,
            "This ticket has already been scanned.",
            "Used at: ${outcome.ticket.usedAt?.let { fmt.format(Date(it)) } ?: "Unknown"}"
        )
        is ScanOutcome.Invalid -> ResultVisual(
            Color(0xFF3B0D0D), Color(0xFFEF4444), Icons.Filled.Cancel,
            "INVALID TICKET", outcome.scannedCode,
            "This ticket is not registered.", null
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(visual.colorDeep, visual.colorMid, visual.colorDeep)
                )
            )
    ) {
        // Center glow behind the icon
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width / 2, size.height * 0.42f),
                radius = size.minDimension * 0.65f
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                PulseRings(color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(220.dp))
                SpringInIcon(icon = visual.icon, tint = Color.White, iconSize = 108.dp)
            }

            Spacer(Modifier.height(22.dp))

            StaggeredAppear(1) {
                Text(
                    visual.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            StaggeredAppear(2) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Text(
                        visual.code,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            StaggeredAppear(3) {
                Text(
                    visual.subtitle,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            visual.detail?.let {
                Spacer(Modifier.height(6.dp))
                StaggeredAppear(4) {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(38.dp))

            StaggeredAppear(5) {
                GlassOutlineButton(
                    text = "SCAN NEXT",
                    icon = Icons.Filled.QrCodeScanner,
                    onClick = onScanNext
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onExit: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera permission is required to scan tickets.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(24.dp))
        GlassOutlineButton(
            text = "OPEN SETTINGS",
            onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        Spacer(Modifier.height(12.dp))
        GlassOutlineButton(text = "Go back", onClick = onExit)
    }
}
