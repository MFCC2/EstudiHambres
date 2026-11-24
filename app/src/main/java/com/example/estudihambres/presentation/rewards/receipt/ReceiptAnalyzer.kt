package com.example.estudihambres.presentation.rewards.receipt

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.estudihambres.domain.model.ScannedReceipt
import com.example.estudihambres.domain.usecase.ReceiptScanParser
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analizador de frames para comprobantes SUNAT.
 *
 * 1. Busca primero el código QR oficial (sólo formato QR: más rápido que escanear todos).
 * 2. Si no hay QR, usa OCR y exige que el mismo comprobante se lea en [OCR_CONFIRM_FRAMES]
 *    frames seguidos antes de aceptarlo, para no disparar con lecturas parciales o erróneas.
 * 3. Tras una detección se pausa solo; [resume] lo reactiva.
 *
 * El [ImageProxy] se cierra únicamente cuando ambos motores terminaron de leer el frame.
 *
 * @param workExecutor Hilo del analizador; también ejecuta el parseo de resultados.
 * @param onDetected Se invoca en el hilo principal con el comprobante detectado.
 */
@OptIn(ExperimentalGetImage::class)
class ReceiptAnalyzer(
    private val workExecutor: Executor,
    private val onDetected: (ScannedReceipt) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val active = AtomicBoolean(true)
    private val closed = AtomicBoolean(false)

    // Sólo se accede desde workExecutor (hilo único)
    private var lastAnalyzedAt = 0L
    private var lastOcrKey: String? = null
    private var ocrHits = 0

    fun resume() {
        workExecutor.execute {
            lastOcrKey = null
            ocrHits = 0
        }
        active.set(true)
    }

    fun pause() = active.set(false)

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val mediaImage = image.image
        if (!active.get() || closed.get() || mediaImage == null || now - lastAnalyzedAt < ANALYSIS_INTERVAL_MS) {
            image.close()
            return
        }
        lastAnalyzedAt = now
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

        barcodeScanner.process(input)
            .continueWithTask(workExecutor) { qrTask ->
                val qr = if (qrTask.isSuccessful) {
                    qrTask.result.orEmpty().firstNotNullOfOrNull { it.rawValue?.let(ReceiptScanParser::parseQr) }
                } else {
                    null
                }
                if (qr != null) {
                    deliver(qr)
                    Tasks.forResult(Unit)
                } else {
                    textRecognizer.process(input).continueWith(workExecutor) { ocrTask ->
                        if (ocrTask.isSuccessful) handleOcr(ocrTask.result.text)
                    }
                }
            }
            .addOnCompleteListener(workExecutor) { image.close() }
    }

    private fun handleOcr(text: String) {
        val parsed = ReceiptScanParser.parseOcr(text)
        if (parsed == null) {
            lastOcrKey = null
            ocrHits = 0
            return
        }
        val key = "${parsed.ruc}|${parsed.serie}|${parsed.numero}"
        ocrHits = if (key == lastOcrKey) ocrHits + 1 else 1
        lastOcrKey = key
        if (ocrHits >= OCR_CONFIRM_FRAMES) deliver(parsed)
    }

    private fun deliver(receipt: ScannedReceipt) {
        // compareAndSet: una sola entrega aunque lleguen dos frames casi a la vez
        if (active.compareAndSet(true, false) && !closed.get()) {
            mainHandler.post { if (!closed.get()) onDetected(receipt) }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            active.set(false)
            runCatching { barcodeScanner.close() }
            runCatching { textRecognizer.close() }
        }
    }

    private companion object {
        const val ANALYSIS_INTERVAL_MS = 350L
        const val OCR_CONFIRM_FRAMES = 2
    }
}
