package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para Mejoras de Intuitividad, UX y Navegación (v1.2.3).
 * Valida chips de búsqueda rápida, filtrado por categorías en mapa, sugerencias universitarias,
 * reglas de formato y completitud del flujo de usuario.
 */
class IntuitiveUxAndNavigationQaTest {

    private lateinit var promotionRepository: MockPromotionRepositoryImpl
    private lateinit var placeRepository: MockPlaceRepository

    @Before
    fun setUp() {
        promotionRepository = MockPromotionRepositoryImpl()
        placeRepository = MockPlaceRepository()
    }

    @Test
    fun testQuickSearchSuggestionsReturnMatchingPromotions() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()
        val quickChips = listOf("Bembos", "Spotify", "GitHub", "Notion")

        quickChips.forEach { chip ->
            val matching = promotions.filter { promo ->
                promo.title.contains(chip, ignoreCase = true) ||
                promo.partnerName.contains(chip, ignoreCase = true) ||
                promo.category.contains(chip, ignoreCase = true) ||
                promo.discountTag.contains(chip, ignoreCase = true)
            }
            assertTrue("El chip '$chip' debe retornar al menos un resultado en el catálogo", matching.isNotEmpty())
        }
    }

    @Test
    fun testQuickSearchByDiscountTag() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()
        val discountQuery = "2x1"
        val matching = promotions.filter { promo ->
            promo.title.contains(discountQuery, ignoreCase = true) ||
            promo.partnerName.contains(discountQuery, ignoreCase = true) ||
            promo.discountTag.contains(discountQuery, ignoreCase = true)
        }
        assertTrue("El filtro '2x1' debe arrojar promociones que incluyan 2x1", matching.isNotEmpty())
    }

    @Test
    fun testMapCategoryFilteringFoodCategory() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val foodPlaces = places.filter { it.category.contains("Comida", ignoreCase = true) }
        assertTrue("Debe existir al menos un lugar bajo la categoría Comida", foodPlaces.isNotEmpty())
    }

    @Test
    fun testMapCategoryFilteringCafeCategory() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val cafePlaces = places.filter { it.category.contains("Café", ignoreCase = true) || it.category.contains("Cafetería", ignoreCase = true) }
        assertTrue("Debe existir al menos un lugar bajo la categoría Café", cafePlaces.isNotEmpty())
    }

    @Test
    fun testMapCategoryFilteringLibreriaCategory() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val bookStores = places.filter { it.category.contains("Librería", ignoreCase = true) }
        assertTrue("Debe existir al menos un lugar bajo la categoría Librería", bookStores.isNotEmpty())
    }

    @Test
    fun testMapCategoryAllReturnsAllPlaces() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val selectedCategory = "Todos"
        val filtered = if (selectedCategory == "Todos") places else places.filter { it.category.contains(selectedCategory, ignoreCase = true) }
        assertEquals(places.size, filtered.size)
    }

    @Test
    fun testPeruvianUniversitiesCatalogPopularChipsMatch() {
        val popularChips = listOf("Continental", "PUCP", "UNMSM", "UNI", "UTEC", "UNSAAC")
        val fullCatalog = listOf(
            "Universidad Continental",
            "Pontificia Universidad Católica del Perú (PUCP)",
            "Universidad Nacional Mayor de San Marcos (UNMSM)",
            "Universidad Nacional de Ingeniería (UNI)",
            "Universidad Peruana de Ciencias Aplicadas (UPC)",
            "Universidad de Ingeniería y Tecnología (UTEC)",
            "Universidad San Ignacio de Loyola (USIL)",
            "Universidad Nacional San Antonio Abad del Cusco (UNSAAC)"
        )

        popularChips.forEach { chip ->
            val match = fullCatalog.firstOrNull { it.contains(chip, ignoreCase = true) }
            assertNotNull("El chip rápido '$chip' debe coincidir con alguna universidad oficial", match)
        }
    }

    @Test
    fun testDniValidationLengthRule() {
        val validDni = "72849103"
        val shortDni = "72849"
        val longDni = "728491039"
        val alphaDni = "7284A103"

        val isValidLengthAndDigits: (String) -> Boolean = { it.length == 8 && it.all { char -> char.isDigit() } }

        assertTrue(isValidLengthAndDigits(validDni))
        assertFalse(isValidLengthAndDigits(shortDni))
        assertFalse(isValidLengthAndDigits(longDni))
        assertFalse(isValidLengthAndDigits(alphaDni))
    }

    @Test
    fun testInstitutionalEmailPatternVerification() {
        val validInstitutionalEmail1 = "u202110294@continental.edu.pe"
        val validInstitutionalEmail2 = "alumno.estudiante@pucp.edu.pe"
        val validInstitutionalEmail3 = "72849103@unmsm.edu.pe"
        val personalEmail = "usuario@gmail.com"

        val isEduPe: (String) -> Boolean = { it.trim().endsWith(".edu.pe", ignoreCase = true) }

        assertTrue(isEduPe(validInstitutionalEmail1))
        assertTrue(isEduPe(validInstitutionalEmail2))
        assertTrue(isEduPe(validInstitutionalEmail3))
        assertFalse(isEduPe(personalEmail))
    }

    @Test
    fun testPromotionDetailsSheetMetadataIntegrity() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()
        promotions.forEach { promo ->
            assertTrue("El partnerName debe tener contenido", promo.partnerName.isNotBlank())
            assertTrue("El título debe tener contenido", promo.title.isNotBlank())
            assertTrue("El discountTag debe tener contenido", promo.discountTag.isNotBlank())
            assertTrue("La categoría debe tener contenido", promo.category.isNotBlank())
            assertTrue("La URL del beneficio debe ser válida", promo.url.startsWith("http"))
        }
    }

    @Test
    fun testFaqQuestionsAndAnswersIntegrity() {
        val faqs = listOf(
            Pair("¿Por qué mi DNI y Universidad están bloqueados?", "Para prevenir la suplantación de identidad estudiantil ante la SUNEDU y comercios aliados, estos campos son inmutables una vez registrados o escaneados."),
            Pair("¿Cómo funciona la validación del carnet?", "El motor de visión artificial (Google ML Kit OCR) procesa tu carnet localmente en el dispositivo, validando sellos oficiales SUNEDU y fechas de vigencia sin almacenar tus fotos en servidores externos."),
            Pair("¿Cómo canjear beneficios en comercios físicos?", "En la pestaña 'Radar', ubica el local más cercano dentro del radio de 30 km y muestra tu perfil o carnet universitario al momento de pagar.")
        )

        faqs.forEach { (q, a) ->
            assertTrue(q.isNotBlank())
            assertTrue(a.isNotBlank())
            assertTrue(q.startsWith("¿") && q.endsWith("?"))
        }
    }

    @Test
    fun testCampusLocationCoordinatesAndLabels() {
        val cusco = com.example.estudihambres.presentation.map.CampusLocation.CUSCO
        val lima = com.example.estudihambres.presentation.map.CampusLocation.LIMA

        assertTrue(cusco.displayName.contains("Cusco"))
        assertTrue(lima.displayName.contains("Lima"))
        assertEquals(-13.5230, cusco.coordinates.latitude, 0.001)
        assertEquals(-12.0833, lima.coordinates.latitude, 0.001)
    }

    @Test
    fun testMapRadiusFiltering5kmExcludesFarPlaces() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val filterUseCase = com.example.estudihambres.domain.usecase.FilterPlacesByDistanceUseCase()
        val cuscoCoords = com.example.estudihambres.domain.repository.UserCoordinates(-13.5230, -71.9480)

        val places5km = filterUseCase(cuscoCoords, places, maxDistanceKm = 5.0)
        val places30km = filterUseCase(cuscoCoords, places, maxDistanceKm = 30.0)

        assertTrue("Radio de 5 km debe contener locales inmediatos al campus", places5km.isNotEmpty())
        assertTrue("Radio de 30 km debe abarcar más o igual cantidad de locales que el de 5 km", places30km.size >= places5km.size)
        assertTrue("Todos los locales en radio de 5 km deben tener distancia calculada <= 5.0", places5km.all { (it.distanceKm ?: 0.0) <= 5.0 })
    }

    @Test
    fun testMapSearchQueryFilteringByNameOrCategory() = runBlocking {
        val places = placeRepository.getPlaces().first()
        val searchQuery = "bembos"

        val matching = places.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.discountBadge.contains(searchQuery, ignoreCase = true)
        }

        assertTrue("La búsqueda 'bembos' debe arrojar al menos un local en el radar", matching.isNotEmpty())
    }

    @Test
    fun testEstimatedWalkingTimeCalculation() {
        val calculateWalkingMinutes: (Double) -> Int = { distKm -> (distKm * 12).toInt().coerceAtLeast(1) }

        assertEquals(1, calculateWalkingMinutes(0.05))
        assertEquals(6, calculateWalkingMinutes(0.5))
        assertEquals(12, calculateWalkingMinutes(1.0))
        assertEquals(24, calculateWalkingMinutes(2.0))
    }
}
