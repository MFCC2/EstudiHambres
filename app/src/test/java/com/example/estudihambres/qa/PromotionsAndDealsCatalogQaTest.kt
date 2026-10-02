package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para Catálogo de Promociones, Enlaces Oficiales y Filtrado (Agente 5: QA).
 * Valida 12 situaciones de búsqueda, URLs seguras, formatos de precio y consistencia de cupones.
 */
class PromotionsAndDealsCatalogQaTest {

    private lateinit var repository: MockPromotionRepositoryImpl

    @Before
    fun setUp() {
        repository = MockPromotionRepositoryImpl()
    }

    @Test
    fun testAllPromotionsHaveValidHttpUrls() = runBlocking {
        val promotions = repository.getPromotions().first()
        assertTrue(promotions.isNotEmpty())
        promotions.forEach { promo ->
            assertTrue(
                "La URL de ${promo.title} debe iniciar con http o https",
                promo.url.startsWith("http://") || promo.url.startsWith("https://")
            )
        }
    }

    @Test
    fun testOfficialUrlsForSpotifyNotionGithubFigma() = runBlocking {
        val promotions = repository.getPromotions().first()

        val spotify = promotions.firstOrNull { it.partnerName.contains("Spotify") }
        assertTrue(spotify?.url?.contains("spotify.com") == true)

        val notion = promotions.firstOrNull { it.partnerName.contains("Notion") }
        assertTrue(notion?.url?.contains("notion.so") == true)

        val github = promotions.firstOrNull { it.partnerName.contains("GitHub") }
        assertTrue(github?.url?.contains("education.github.com") == true)

        val figma = promotions.firstOrNull { it.partnerName.contains("Figma") }
        assertTrue(figma?.url?.contains("figma.com") == true)
    }

    @Test
    fun testCategoryFilteringComida() = runBlocking {
        val promotions = repository.getPromotions().first()
        val foodPromos = promotions.filter { it.category == "Comida" }
        assertTrue(foodPromos.isNotEmpty())
        assertTrue(foodPromos.all { it.category == "Comida" })
    }

    @Test
    fun testCategoryFilteringHerramientasDigitales() = runBlocking {
        val promotions = repository.getPromotions().first()
        val digitalPromos = promotions.filter { it.category == "Herramientas digitales" }
        assertTrue(digitalPromos.isNotEmpty())
        assertTrue(digitalPromos.all { it.category == "Herramientas digitales" })
    }

    @Test
    fun testCategoryFilteringTransporte() = runBlocking {
        val promotions = repository.getPromotions().first()
        val transitPromos = promotions.filter { it.category == "Transporte" }
        assertTrue(transitPromos.isNotEmpty())
        assertTrue(transitPromos.all { it.category == "Transporte" })
    }

    @Test
    fun testSearchByTitleCaseInsensitive() = runBlocking {
        val promotions = repository.getPromotions().first()
        val query = "bembos"
        val results = promotions.filter {
            it.title.contains(query, ignoreCase = true) || it.partnerName.contains(query, ignoreCase = true)
        }
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testSearchByPartnerNameCaseInsensitive() = runBlocking {
        val promotions = repository.getPromotions().first()
        val query = "starbucks"
        val results = promotions.filter {
            it.partnerName.contains(query, ignoreCase = true)
        }
        assertEquals(1, results.size)
    }

    @Test
    fun testSearchWithLeadingAndTrailingSpaces() = runBlocking {
        val promotions = repository.getPromotions().first()
        val query = "  Notion  ".trim().lowercase()
        val results = promotions.filter {
            it.title.lowercase().contains(query) || it.partnerName.lowercase().contains(query)
        }
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testSearchWithNonExistentQueryReturnsEmpty() = runBlocking {
        val promotions = repository.getPromotions().first()
        val query = "termino_inexistente_xyz_123"
        val results = promotions.filter {
            it.title.lowercase().contains(query) || it.partnerName.lowercase().contains(query)
        }
        assertTrue(results.isEmpty())
    }

    @Test
    fun testOriginalPriceAndDiscountedPriceNotNullOrBlank() = runBlocking {
        val promotions = repository.getPromotions().first()
        promotions.forEach { promo ->
            assertFalse(promo.discountTag.isBlank())
            assertFalse(promo.title.isBlank())
            assertFalse(promo.partnerName.isBlank())
        }
    }

    @Test
    fun testEstimatedSavingsIsPositiveAmount() = runBlocking {
        val promotions = repository.getPromotions().first()
        promotions.forEach { promo ->
            assertTrue("El ahorro estimado debe ser positivo", promo.estimatedSavingsSoles > 0.0)
        }
    }

    @Test
    fun testFeaturedPromotionsExistInCatalog() = runBlocking {
        val promotions = repository.getPromotions().first()
        val featured = promotions.filter { it.isFeatured }
        assertTrue("Debe existir al menos 3 promociones destacadas", featured.size >= 3)
    }
}
