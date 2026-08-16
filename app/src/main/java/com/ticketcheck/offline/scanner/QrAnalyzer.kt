package com.ticketcheck.offline.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis.Analyzer backed by on-device ML Kit barcode
 * scanning. ML Kit's barcode-scanning model ships bundled with the app
 * (no download, no network) as long as the "unbundled" model dependency
 * is NOT used - this project depends on the bundled artifact.
 *
 * [paused] lets the caller stop feeding new frames to the callback while
 * a result is being shown, which combined with the ViewModel-side
 * "already processing this code" guard prevents the same physical scan
 * from being registered multiple times because the camera keeps
 * re-detecting the same QR across several frames.
 */
class QrAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val paused = AtomicBoolean(false)
    private val processing = AtomicBoolean(false)

    fun pause() { paused.set(true) }
    fun resume() { paused.set(false) }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || paused.get() || !processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes: List<Barcode> ->
                if (!paused.get()) {
                    val value = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (value != null) {
                        onQrDetected(value)
                    }
                }
            }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }
}
