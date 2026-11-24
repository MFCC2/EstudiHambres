package com.example.estudihambres.presentation.rewards.receipt

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.estudihambres.domain.model.ReceiptDraft
import com.example.estudihambres.domain.service.SunatCheckStatus
import com.example.estudihambres.domain.service.SunatCpeValidatorBot
import com.example.estudihambres.domain.service.SunatResultClassifier
import kotlinx.coroutines.delay
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resultado final de la corroboración en SUNAT. */
enum class SunatOutcomeKind { VALID, NOT_VALID, CANCELLED, DATA_REJECTED, UNCONFIRMED }

data class SunatOutcome(val kind: SunatOutcomeKind, val detail: String)

private enum class SunatPhase { OPENING, WAITING_USER, READING, DONE }

private const val OPEN_TIMEOUT_MS = 25_000L
private const val READ_TIMEOUT_MS = 45_000L
private const val SUBMIT_DETECTION_SCRIPT =
    "(function(){var f=document.lista;return !!(f&&f.wacepta&&f.wacepta.disabled);})();"
private const val READ_ATTEMPTS = 12
private const val READ_INTERVAL_MS = 750L

/**
 * Consulta la validez del comprobante en la web oficial de SUNAT (Consulta de Validez del CPE).
 *
 * La app abre la consulta con los datos ya escritos y el estudiante toca "Buscar": el firewall
 * de SUNAT rechaza los envíos automáticos ("The requested URL was rejected"), que era la razón
 * por la que la verificación anterior se quedaba colgada. La respuesta se lee y se interpreta
 * automáticamente con [SunatResultClassifier].
 *
 * @param attempt Cambia en cada reintento para reiniciar la consulta.
 * @param onOutcome Se invoca una sola vez por intento con el resultado.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SunatVerificationPanel(
    draft: ReceiptDraft,
    attempt: Int,
    onOutcome: (SunatOutcome) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnOutcome by rememberUpdatedState(onOutcome)
    val bot = remember { SunatCpeValidatorBot() }

    var phase by remember(attempt) { mutableStateOf(SunatPhase.OPENING) }
    var showPage by remember(attempt) { mutableStateOf(true) }

    // Estado mutable compartido con los callbacks del WebView (siempre en el hilo principal)
    val session = remember(attempt) { SunatSession() }

    fun finish(outcome: SunatOutcome) {
        if (session.done) return
        session.done = true
        phase = SunatPhase.DONE
        showPage = false
        currentOnOutcome(outcome)
    }

    val webView = remember(attempt) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(false)
            // La página de SUNAT es de escritorio: se ajusta al ancho del teléfono
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
        }
    }

    DisposableEffect(webView) {
        onDispose {
            session.done = true
            webView.stopLoading()
            // Un WebView debe quitarse de su contenedor antes de destruirse
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    // Tiempos máximos sólo mientras la app espera a SUNAT (no mientras espera al estudiante)
    LaunchedEffect(attempt, phase) {
        when (phase) {
            SunatPhase.OPENING -> {
                delay(OPEN_TIMEOUT_MS)
                finish(SunatOutcome(SunatOutcomeKind.UNCONFIRMED, "No pudimos abrir la consulta de SUNAT. Inténtalo de nuevo en unos segundos."))
            }
            SunatPhase.WAITING_USER -> {
                // Al tocar "Buscar", opcGrabar() deshabilita el botón: se detecta para iniciar el
                // tiempo máximo aunque SUNAT tarde en empezar a responder (antes podía colgarse).
                while (!session.done && phase == SunatPhase.WAITING_USER) {
                    delay(1_000)
                    webView.evaluateJavascript(SUBMIT_DETECTION_SCRIPT) { raw ->
                        if (raw == "true" && !session.done && phase == SunatPhase.WAITING_USER) {
                            phase = SunatPhase.READING
                        }
                    }
                }
            }
            SunatPhase.READING -> {
                delay(READ_TIMEOUT_MS)
                finish(SunatOutcome(SunatOutcomeKind.UNCONFIRMED, "SUNAT no respondió a tiempo. Inténtalo de nuevo en unos segundos."))
            }
            else -> Unit
        }
    }

    LaunchedEffect(attempt) {
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(draft.issueDateMillis))
        val prefillScript = bot.generateAutoFillScript(
            ruc = draft.ruc,
            tipoCpe = draft.tipo,
            serie = draft.serie,
            correlativo = draft.numero,
            fechaEmision = fecha,
            monto = draft.amount ?: 0.0,
            autoSubmit = false
        )

        fun readResult(view: WebView, remaining: Int) {
            if (session.done) return
            view.evaluateJavascript(bot.generatePageTextScript()) { raw ->
                if (session.done) return@evaluateJavascript
                val text = decodeJsString(raw)
                when (SunatResultClassifier.classify(text, draft.serie, draft.numero)) {
                    SunatCheckStatus.VALID -> finish(SunatOutcome(SunatOutcomeKind.VALID, extractSentence(text)))
                    SunatCheckStatus.NOT_VALID -> finish(SunatOutcome(SunatOutcomeKind.NOT_VALID, extractSentence(text)))
                    SunatCheckStatus.CANCELLED -> finish(SunatOutcome(SunatOutcomeKind.CANCELLED, extractSentence(text)))
                    null -> if (remaining > 0) {
                        view.postDelayed({ readResult(view, remaining - 1) }, READ_INTERVAL_MS)
                    } else {
                        val rejected = SunatResultClassifier.normalize(text).contains("URL WAS REJECTED")
                        finish(
                            SunatOutcome(
                                SunatOutcomeKind.UNCONFIRMED,
                                if (rejected) {
                                    "SUNAT rechazó la consulta. Reinténtalo y toca «Buscar» en la página."
                                } else {
                                    "SUNAT respondió, pero no pudimos leer el resultado. Puedes revisarlo en «Ver página de SUNAT»."
                                }
                            )
                        )
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // SUNAT valida los campos con alert(): antes esos mensajes se perdían en silencio
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                result?.confirm()
                finish(
                    SunatOutcome(
                        SunatOutcomeKind.DATA_REJECTED,
                        message?.trim().orEmpty().ifBlank { "SUNAT no aceptó los datos ingresados." }
                    )
                )
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                // Una navegación después de rellenar el formulario = el estudiante tocó "Buscar"
                if (session.prefilled && !session.done) {
                    session.resultNavigationStarted = true
                    phase = SunatPhase.READING
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (session.done) return
                if (!session.prefilled) {
                    // body onload="inicializar()" limpia el formulario: se rellena después de cargar
                    view.evaluateJavascript(prefillScript) { status ->
                        if (session.done) return@evaluateJavascript
                        if (decodeJsString(status) == "PREFILLED") {
                            session.prefilled = true
                            phase = SunatPhase.WAITING_USER
                        } else {
                            finish(SunatOutcome(SunatOutcomeKind.UNCONFIRMED, "La página de SUNAT cambió y no pudimos preparar la consulta."))
                        }
                    }
                } else if (session.resultNavigationStarted && !session.readingStarted) {
                    // Sólo se lee la página cargada DESPUÉS de "Buscar": WebView a veces dispara
                    // onPageFinished dos veces para el mismo formulario.
                    session.readingStarted = true
                    readResult(view, READ_ATTEMPTS)
                }
            }

            // Sólo un fallo de la página principal es un error: la de SUNAT pide scripts a un CDN
            // inexistente (oss.maxcdn.com), lo que antes se reportaba como "sin conexión".
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    finish(SunatOutcome(SunatOutcomeKind.UNCONFIRMED, "No pudimos abrir la consulta de SUNAT. Revisa tu conexión."))
                }
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                if (request?.isForMainFrame == true) {
                    finish(SunatOutcome(SunatOutcomeKind.UNCONFIRMED, "SUNAT no está disponible en este momento (${response?.statusCode})."))
                }
            }

            // El WebView no navega fuera de la consulta oficial
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                request?.url?.host?.endsWith("sunat.gob.pe") != true
        }

        webView.loadUrl(SunatCpeValidatorBot.SUNAT_CONSVALI_URL)
    }

    Column(modifier = modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (phase != SunatPhase.DONE) {
            StepRow("Abriendo la consulta oficial de SUNAT", stepState(phase, SunatPhase.OPENING))
            StepRow("Toca «Buscar» en la página de SUNAT", stepState(phase, SunatPhase.WAITING_USER))
            StepRow("Leyendo la respuesta de SUNAT", stepState(phase, SunatPhase.READING))
        }

        if (phase == SunatPhase.WAITING_USER) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ya escribimos tus datos en el formulario oficial. Sólo toca «Buscar»: el resultado aparecerá aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (phase == SunatPhase.DONE) {
            TextButton(onClick = { showPage = !showPage }, modifier = Modifier.align(Alignment.Start)) {
                Text(if (showPage) "Ocultar página de SUNAT" else "Ver página de SUNAT")
            }
        }

        // key(attempt): cada reintento monta su propio WebView (el anterior se destruye).
        // Oculto se mantiene con 1 dp e invisible para no interrumpir la carga.
        key(attempt) {
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (showPage) 460.dp else 1.dp)
                    .alpha(if (showPage) 1f else 0f)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

/** Estado mutable de una consulta (accedido sólo desde el hilo principal). */
private class SunatSession {
    var prefilled = false
    var resultNavigationStarted = false
    var readingStarted = false
    var done = false
}

private enum class StepState { PENDING, RUNNING, DONE }

private fun stepState(current: SunatPhase, step: SunatPhase): StepState = when {
    current.ordinal > step.ordinal -> StepState.DONE
    current == step -> StepState.RUNNING
    else -> StepState.PENDING
}

@Composable
private fun StepRow(label: String, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StepState.RUNNING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                StepState.DONE -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(3.dp).size(14.dp)
                    )
                }
                StepState.PENDING -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (state == StepState.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
            color = if (state == StepState.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** evaluateJavascript devuelve un literal JSON ("texto" con escapes); se decodifica a texto plano. */
private fun decodeJsString(raw: String?): String {
    if (raw.isNullOrEmpty() || raw == "null") return ""
    return runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull() ?: raw.trim('"')
}

/** Frase de resultado de SUNAT ("La Boleta ... es un comprobante de pago válido.") para mostrarla tal cual. */
private fun extractSentence(text: String): String {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return lines.firstOrNull { line ->
        // Ej.: "La Boleta de Venta Electrónica B001-00009999 no existe en los registros de SUNAT."
        val n = SunatResultClassifier.normalize(line)
        listOf("VALIDO", "EXISTE", "BAJA", "INFORMAD", "ANULAD", "RECHAZAD").any { it in n } && "VALIDEZ" !in n
    }?.take(240) ?: ""
}
