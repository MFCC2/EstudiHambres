package com.example.estudihambres.data.remote.scraper

import com.example.estudihambres.domain.model.WebOffer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Extrae ofertas del HTML de las páginas de categoría de Cuponidad.pe.
 *
 * Estrategia en cascada para tolerar cambios de diseño del sitio:
 * 1. Tarjetas de listado `li.cd2-loccard` (comercio, título, distrito, precio, imagen).
 * 2. Tarjetas de catálogo `div.container__thumbnail[data-offer-id]`.
 * 3. Datos estructurados JSON-LD `ItemList` (solo título y enlace).
 *
 * Sólo se aceptan enlaces e imágenes https del propio dominio y su CDN, de modo que una
 * página alterada no pueda inyectar enlaces a otros sitios en la app.
 */
object CuponidadOfferParser {

    const val SOURCE_NAME = "Cuponidad.pe"
    const val BASE_URL = "https://cuponidad.pe"

    private val ALLOWED_HOSTS = setOf("cuponidad.pe", "www.cuponidad.pe", "cdn.cuponidad.pe")
    private val PRICE_REGEX = Regex("""(\d{1,5}(?:[.,]\d{1,2})?)""")
    private val LD_LIST_ITEM_REGEX = Regex("""\{[^{}]*"ListItem"[^{}]*\}""")
    private val LD_URL_REGEX = Regex("\"url\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val LD_NAME_REGEX = Regex("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
    private const val MAX_TEXT = 160

    fun parse(html: String, pageUrl: String = BASE_URL, limit: Int = 60): List<WebOffer> {
        val doc = Jsoup.parse(html, pageUrl)
        val offers = parseListCards(doc)
            .ifEmpty { parseThumbnailCards(doc) }
            .ifEmpty { parseJsonLd(doc) }
        return offers.distinctBy { it.id }.take(limit)
    }

    private fun parseListCards(doc: Document): List<WebOffer> =
        doc.select("li.cd2-loccard").mapNotNull { card ->
            val link = card.selectFirst("a[href]") ?: return@mapNotNull null
            buildOffer(
                id = card.attr("data-offer-id"),
                href = link.absUrl("href"),
                title = card.selectFirst(".cd2-loccard__title")?.text() ?: link.attr("title"),
                merchant = card.selectFirst(".cd2-loccard__merchant")?.text().orEmpty(),
                district = card.selectFirst(".cd2-loccard__place")?.ownText().orEmpty(),
                priceText = card.selectFirst(".cd2-loccard__price")?.text(),
                imageUrl = card.selectFirst("img")?.absUrl("src")
            )
        }

    private fun parseThumbnailCards(doc: Document): List<WebOffer> =
        doc.select("div.container__thumbnail[data-offer-id]").mapNotNull { card ->
            val link = card.selectFirst("a[href]") ?: return@mapNotNull null
            buildOffer(
                id = card.attr("data-offer-id"),
                href = link.absUrl("href"),
                title = card.selectFirst(".thumbnail__title")?.text() ?: link.attr("title"),
                merchant = card.selectFirst(".thumbnail__provider")?.text().orEmpty(),
                district = card.selectFirst(".thumbnail__place")?.text().orEmpty(),
                priceText = card.selectFirst(".cd2-offer__amount, .thumbnail__price")?.text(),
                imageUrl = card.selectFirst("img")?.absUrl("src")
            )
        }

    /**
     * Respaldo con datos estructurados: cada `ListItem` del JSON-LD trae `url` y `name`.
     * Se extrae con expresiones regulares para no depender de org.json (stub en pruebas JVM).
     */
    private fun parseJsonLd(doc: Document): List<WebOffer> =
        doc.select("script[type=application/ld+json]")
            .flatMap { script -> LD_LIST_ITEM_REGEX.findAll(script.data()).toList() }
            .mapNotNull { match ->
                val body = match.value
                val url = LD_URL_REGEX.find(body)?.groupValues?.get(1)?.unescapeJson() ?: return@mapNotNull null
                val name = LD_NAME_REGEX.find(body)?.groupValues?.get(1)?.unescapeJson() ?: return@mapNotNull null
                buildOffer(
                    id = url.substringAfterLast('-'),
                    href = url,
                    title = name,
                    merchant = "",
                    district = "",
                    priceText = null,
                    imageUrl = null
                )
            }

    private fun buildOffer(
        id: String,
        href: String,
        title: String,
        merchant: String,
        district: String,
        priceText: String?,
        imageUrl: String?
    ): WebOffer? {
        val cleanTitle = title.clean()
        if (cleanTitle.isBlank() || !isAllowedUrl(href)) return null
        return WebOffer(
            id = id.trim().ifBlank { href },
            title = cleanTitle,
            merchant = merchant.clean().replaceFirstChar { it.titlecase() },
            district = district.clean().replaceFirstChar { it.titlecase() },
            priceSoles = parsePrice(priceText),
            imageUrl = imageUrl?.takeIf { isAllowedUrl(it) },
            url = href,
            sourceName = SOURCE_NAME
        )
    }

    /** Convierte "S/ 1,234.90" o "S/ 49.90" en soles. Devuelve null si no hay un precio válido. */
    fun parsePrice(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val compact = text.replace(Regex("""(\d),(\d{3})"""), "$1$2")
        val value = PRICE_REGEX.find(compact)?.value?.replace(',', '.')?.toDoubleOrNull()
        return value?.takeIf { it > 0.0 && it < 100_000.0 }
    }

    private fun isAllowedUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host in ALLOWED_HOSTS
    }

    private fun String.clean(): String = replace(Regex("""\s+"""), " ").trim().take(MAX_TEXT)

    private fun String.unescapeJson(): String =
        replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\")
}
