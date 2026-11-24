package com.example.estudihambres.data.repository

import com.example.estudihambres.data.remote.scraper.CuponidadOfferParser
import com.example.estudihambres.domain.model.WebOffer
import com.example.estudihambres.domain.model.WebOfferCategory
import com.example.estudihambres.domain.repository.WebOffersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * Ofertas de restaurantes obtenidas por scraping de las páginas de categoría de Cuponidad.pe.
 *
 * - Sólo consulta rutas de categoría, permitidas por el robots.txt del sitio
 *   (no toca búsqueda, carrito ni áreas privadas).
 * - Caché en memoria por categoría ([CACHE_TTL_MILLIS]) para no repetir descargas de ~0.5-1 MB
 *   en cada cambio de chip o de pestaña.
 * - Requiere internet: sin conexión devuelve un error legible (la app además se bloquea a nivel raíz).
 *
 * @param fetchHtml Descarga el HTML de una URL; inyectable para pruebas sin red.
 */
class CuponidadWebOffersRepository(
    private val fetchHtml: suspend (String) -> String = ::downloadHtml,
    private val clock: () -> Long = System::currentTimeMillis
) : WebOffersRepository {

    override val sourceName: String = CuponidadOfferParser.SOURCE_NAME

    override val categories: List<WebOfferCategory> = listOf(
        WebOfferCategory("Todas", "/restaurantes-5"),
        WebOfferCategory("Pollerías", "/restaurantes/pollerias-53"),
        WebOfferCategory("Sanguches y salchipapas", "/restaurantes/sanguches-salchipapas-51"),
        WebOfferCategory("Pizzas y pastas", "/restaurantes/pizzas-y-pastas-69"),
        WebOfferCategory("Chifas", "/restaurantes/chifas-129"),
        WebOfferCategory("Comida peruana", "/restaurantes/comida-peruana-68"),
        WebOfferCategory("Café y postres", "/restaurantes/coffee-postres-52"),
        WebOfferCategory("Sushi y makis", "/restaurantes/sushi-makis-y-open-roll-48"),
        WebOfferCategory("Buffets", "/restaurantes/buffets-45")
    )

    private data class CacheEntry(val offers: List<WebOffer>, val fetchedAt: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun getOffers(category: WebOfferCategory, forceRefresh: Boolean): Result<List<WebOffer>> {
        val now = clock()
        if (!forceRefresh) {
            cache[category.path]?.takeIf { now - it.fetchedAt < CACHE_TTL_MILLIS }?.let {
                return Result.success(it.offers)
            }
        }
        return try {
            val url = CuponidadOfferParser.BASE_URL + category.path
            val html = fetchHtml(url)
            val offers = withContext(Dispatchers.Default) { CuponidadOfferParser.parse(html, url) }
            if (offers.isEmpty()) {
                Result.failure(IOException("No encontramos ofertas en ${sourceName} por ahora. Intenta otra categoría."))
            } else {
                cache[category.path] = CacheEntry(offers, now)
                Result.success(offers)
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Sin conexión a internet. Conéctate para ver las ofertas.", e))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("${sourceName} tardó demasiado en responder. Desliza para reintentar.", e))
        } catch (e: IOException) {
            Result.failure(IOException("No se pudieron cargar las ofertas de ${sourceName}. Desliza para reintentar.", e))
        }
    }

    companion object {
        const val CACHE_TTL_MILLIS = 10 * 60 * 1000L
        private const val TIMEOUT_MILLIS = 12_000
        private const val MAX_BODY_BYTES = 3 * 1024 * 1024
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) EstudiHambres/1.6 (app estudiantil)"

        private suspend fun downloadHtml(url: String): String = withContext(Dispatchers.IO) {
            Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MILLIS)
                .maxBodySize(MAX_BODY_BYTES)
                .followRedirects(true)
                .execute()
                .body() // cuerpo crudo: se parsea una sola vez en CuponidadOfferParser
        }
    }
}
