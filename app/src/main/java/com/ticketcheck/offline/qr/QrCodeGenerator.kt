package com.ticketcheck.offline.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Turns a ticket code into a QR bitmap, fully offline (ZXing does not
 * need Google Play Services or network access).
 *
 * The QR payload abstraction (TicketQrPayload) is kept separate from the
 * raw ticket code on purpose: today the payload is just the plain code,
 * but this can later be swapped for a signed/random token format without
 * touching the scanner or generator call sites.
 */
interface TicketQrPayload {
    fun encode(ticketCode: String): String
    fun decode(scannedValue: String): String
}

/** Version 1 payload: the QR literally contains the ticket code, nothing else. */
object PlainCodePayload : TicketQrPayload {
    override fun encode(ticketCode: String): String = ticketCode
    override fun decode(scannedValue: String): String = scannedValue.trim()
}

object QrCodeGenerator {

    private val payload: TicketQrPayload = PlainCodePayload

    fun generate(ticketCode: String, sizePx: Int = 512): Bitmap {
        val content = payload.encode(ticketCode)
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    fun decodeScannedValue(scannedValue: String): String = payload.decode(scannedValue)
}
