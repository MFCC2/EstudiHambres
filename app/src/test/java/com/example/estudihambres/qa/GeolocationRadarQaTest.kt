package com.example.estudihambres.qa

import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.repository.UserCoordinates
import com.example.estudihambres.domain.usecase.FilterPlacesByDistanceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para el Radar GPS y Cálculo de Distancias Haversine (Agente 4: Geolocalización / Agente 5: QA).
 * Valida 12 situaciones de geolocalización, coordenadas en Cusco y Lima, límites de 30 km y simetría esférica.
 */
class GeolocationRadarQaTest {

    private lateinit var useCase: FilterPlacesByDistanceUseCase

    @Before
    fun setUp() {
        useCase = FilterPlacesByDistanceUseCase()
    }

    @Test
    fun testZeroDistanceForSameCoordinates() {
        val distance = useCase.calculateDistanceKm(-13.5230, -71.9480, -13.5230, -71.9480)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testCuscoRealPlazaToUNSAACDistanceWithinRadar() {
        // Real Plaza Cusco: -13.5230, -71.9480
        // UNSAAC Av. de la Cultura: -13.5208, -71.9585
        val distance = useCase.calculateDistanceKm(-13.5230, -71.9480, -13.5208, -71.9585)
        assertTrue(distance > 0.8 && distance < 1.5)
        assertTrue(distance <= 30.0)
    }

    @Test
    fun testCuscoAvCulturaToContinentalDistanceWithinRadar() {
        // Av. de la Cultura: -13.5222, -71.9520
        // Campus Continental Cusco: -13.5240, -71.9440
        val distance = useCase.calculateDistanceKm(-13.5222, -71.9520, -13.5240, -71.9440)
        assertTrue(distance > 0.5 && distance < 1.2)
        assertTrue(distance <= 30.0)
    }

    @Test
    fun testLimaToCuscoDistanceExceeds30KmRadar() {
        // Lima (-12.0464, -77.0428) a Cusco (-13.5230, -71.9480) ~580 km
        val distance = useCase.calculateDistanceKm(-12.0464, -77.0428, -13.5230, -71.9480)
        assertTrue(distance > 500.0)
    }

    @Test
    fun testExactBoundaryDistanceAt29KmIsIncluded() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        // Punto aproximadamente a ~25 km al norte
        val place25Km = Place(
            id = "p-25",
            name = "Local a 25 km",
            category = "Comida",
            address = "Valle Sagrado",
            latitude = -13.3000,
            longitude = -71.9480,
            rating = 4.5,
            discountBadge = "10%",
            discountDescription = "Descuento"
        )

        val result = useCase(userLocation, listOf(place25Km), maxDistanceKm = 30.0)
        assertEquals(1, result.size)
    }

    @Test
    fun testExactBoundaryDistanceAt35KmIsExcluded() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        // Punto a más de 35 km
        val place40Km = Place(
            id = "p-40",
            name = "Local Lejano",
            category = "Comida",
            address = "Ollantaytambo",
            latitude = -13.1500,
            longitude = -72.2600,
            rating = 4.0,
            discountBadge = "10%",
            discountDescription = "Descuento"
        )

        val result = useCase(userLocation, listOf(place40Km), maxDistanceKm = 30.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testNegativeCoordinatesCalculationSouthernWesternHemisphere() {
        val distance = useCase.calculateDistanceKm(-12.00, -77.00, -12.10, -77.10)
        assertTrue(distance > 0.0)
        assertTrue(distance < 25.0)
    }

    @Test
    fun testHaversineSymmetryDistanceABEqualsDistanceBA() {
        val d1 = useCase.calculateDistanceKm(-13.5230, -71.9480, -12.0464, -77.0428)
        val d2 = useCase.calculateDistanceKm(-12.0464, -77.0428, -13.5230, -71.9480)
        assertEquals(d1, d2, 0.0001)
    }

    @Test
    fun testFilterEmptyPlacesListReturnsEmpty() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        val result = useCase(userLocation, emptyList(), maxDistanceKm = 30.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFilterAllNearbyPlacesKeepsAll() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        val places = listOf(
            Place(
                id = "1",
                name = "Local 1",
                category = "Comida",
                address = "Av. Cultura",
                latitude = -13.5235,
                longitude = -71.9485,
                rating = 4.5,
                discountBadge = "2x1",
                discountDescription = "Desc"
            ),
            Place(
                id = "2",
                name = "Local 2",
                category = "Comida",
                address = "Real Plaza",
                latitude = -13.5225,
                longitude = -71.9475,
                rating = 4.8,
                discountBadge = "30%",
                discountDescription = "Desc"
            )
        )
        val result = useCase(userLocation, places, maxDistanceKm = 30.0)
        assertEquals(2, result.size)
    }

    @Test
    fun testFilterAllFarPlacesRemovesAll() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        val places = listOf(
            Place(
                id = "1",
                name = "Local Lima 1",
                category = "Comida",
                address = "Javier Prado",
                latitude = -12.0800,
                longitude = -77.0200,
                rating = 4.5,
                discountBadge = "2x1",
                discountDescription = "Desc"
            ),
            Place(
                id = "2",
                name = "Local Lima 2",
                category = "Comida",
                address = "Miraflores",
                latitude = -12.1200,
                longitude = -77.0300,
                rating = 4.8,
                discountBadge = "30%",
                discountDescription = "Desc"
            )
        )
        val result = useCase(userLocation, places, maxDistanceKm = 30.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testCustomMaxDistanceKmFilter() {
        val userLocation = UserCoordinates(-13.5230, -71.9480)
        // Local a ~1.2 km
        val place1_2Km = Place(
            id = "1",
            name = "Local",
            category = "Comida",
            address = "UNSAAC",
            latitude = -13.5208,
            longitude = -71.9585,
            rating = 4.5,
            discountBadge = "2x1",
            discountDescription = "Desc"
        )

        // Con radio estricto de 0.5 km debe excluirse
        val result500m = useCase(userLocation, listOf(place1_2Km), maxDistanceKm = 0.5)
        assertTrue(result500m.isEmpty())

        // Con radio de 5.0 km debe incluirse
        val result5km = useCase(userLocation, listOf(place1_2Km), maxDistanceKm = 5.0)
        assertEquals(1, result5km.size)
    }
}
