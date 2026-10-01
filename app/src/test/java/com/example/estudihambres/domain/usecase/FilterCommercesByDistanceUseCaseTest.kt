package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.model.Commerce
import com.example.estudihambres.domain.repository.UserCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [FilterCommercesByDistanceUseCase] (Agente 5: QA).
 * Valida que los comercios a más de 30 km sean filtrados y que se calculen
 * correctamente las distancias usando la fórmula del Haversine.
 */
class FilterCommercesByDistanceUseCaseTest {

    private lateinit var useCase: FilterCommercesByDistanceUseCase

    @Before
    fun setUp() {
        useCase = FilterCommercesByDistanceUseCase()
    }

    @Test
    fun `invoke should exclude commerces farther than 30 km and include nearby ones`() {
        // Coordenadas de referencia (ej. Lima Centro: -12.0464, -77.0428)
        val userLocation = UserCoordinates(latitude = -12.0464, longitude = -77.0428)

        val nearbyCommerce = Commerce(
            id = "c1",
            name = "Cafetería Universitaria",
            description = "Café y snacks cerca al campus",
            category = "Cafetería",
            discountDescription = "15% dto",
            latitude = -12.0500, // ~0.5 km
            longitude = -77.0450
        )

        val farCommerce = Commerce(
            id = "c2",
            name = "Restaurante de Playa",
            description = "Comercio a más de 50 km",
            category = "Mariscos",
            discountDescription = "10% dto",
            latitude = -11.5000, // ~60 km
            longitude = -76.8000
        )

        val result = useCase(userLocation, listOf(nearbyCommerce, farCommerce), maxDistanceKm = 30.0)

        assertEquals(1, result.size)
        assertEquals("c1", result.first().id)
        assertNotNull(result.first().distanceKm)
        assertTrue(result.first().distanceKm!! <= 30.0)
    }

    @Test
    fun `calculateHaversineDistance returns zero when coordinates are identical`() {
        val distance = useCase.calculateHaversineDistance(-12.0464, -77.0428, -12.0464, -77.0428)
        assertEquals(0.0, distance, 0.001)
    }
}
