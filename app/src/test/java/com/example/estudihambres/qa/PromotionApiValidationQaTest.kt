package com.example.estudihambres.qa

import com.example.estudihambres.data.remote.api.DefaultPromotionApiServiceImpl
import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para la API de Promociones y Validación de Enlaces Oficiales.
 */
class PromotionApiValidationQaTest {

    private lateinit var apiService: DefaultPromotionApiServiceImpl
    private lateinit var repository: MockPromotionRepositoryImpl

    @Before
    fun setUp() {
        apiService = DefaultPromotionApiServiceImpl()
        repository = MockPromotionRepositoryImpl(apiService)
    }

    @Test
    fun testAllPromotionsFromApiHaveHttpsUrlsAndValidDomains() = runBlocking {
        val dtos = apiService.getVerifiedPromotions()
        assertTrue("La API debe retornar al menos 15 promociones universitarias", dtos.size >= 15)

        dtos.forEach { dto ->
            assertTrue("La oferta ${dto.title} debe usar HTTPS", dto.officialUrl.startsWith("https://"))
            assertTrue(
                "La oferta ${dto.title} debe pertenecer a un dominio confiable",
                apiService.isOfferUrlSecureAndVerified(dto.officialUrl)
            )
            assertTrue("La oferta debe estar marcada como validada", dto.isValidated)
            assertTrue("El origen de verificación no debe estar vacío", dto.verificationSource.isNotBlank())
        }
    }

    @Test
    fun testApiRejectsInsecureHttpOrUntrustedDomains() {
        assertFalse("Debe rechazar enlaces HTTP no cifrados", apiService.isOfferUrlSecureAndVerified("http://cineplanet.com.pe"))
        assertFalse("Debe rechazar dominios no autorizados", apiService.isOfferUrlSecureAndVerified("https://sitio-malicioso-phishing.com/promo"))
        assertFalse("Debe rechazar cadenas inválidas", apiService.isOfferUrlSecureAndVerified("no-url"))
        assertTrue("Debe aceptar subdominios de confianza", apiService.isOfferUrlSecureAndVerified("https://education.github.com/pack"))
        assertTrue("Debe aceptar dominio principal de confianza", apiService.isOfferUrlSecureAndVerified("https://www.starbucks.pe"))
    }

    @Test
    fun testDomainMappingPreservesValidationStatus() = runBlocking {
        val promotions = repository.getPromotions().first()
        assertTrue(promotions.isNotEmpty())
        assertTrue("Todas las promociones emitidas deben estar validadas", promotions.all { it.isValidated })
        assertTrue("Todas las URLs de ofertas deben coincidir con officialOfferUrl", promotions.all { it.url == it.officialOfferUrl })
    }
}
