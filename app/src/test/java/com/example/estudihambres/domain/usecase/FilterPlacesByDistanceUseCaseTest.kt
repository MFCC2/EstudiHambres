package com.example.estudihambres.domain.usecase

import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.domain.repository.UserCoordinates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el Radar de 30 km sobre lugares con convenios (Agente 4 & Agente 5).
 */
class FilterPlacesByDistanceUseCaseTest {

    private lateinit var useCase: FilterPlacesByDistanceUseCase
    private lateinit var repository: MockPlaceRepository

    @Before
    fun setUp() {
        useCase = FilterPlacesByDistanceUseCase()
        repository = MockPlaceRepository()
    }

    @Test
    fun `radar filters out places beyond 30 km and retains at least 8 nearby perk places`() = runBlocking {
        // Coordenadas campus Lima: -12.0833, -77.0428
        val campusCoords = UserCoordinates(latitude = -12.0833, longitude = -77.0428)
        val allPlaces = repository.getPlaces().first()

        val filtered = useCase(campusCoords, allPlaces, maxDistanceKm = 30.0)

        // Se deben conservar al menos 8 locales dentro del radio
        assertTrue("Debe contener al menos 8 locales cercanos", filtered.size >= 8)

        // Todos los locales filtrados deben tener distanceKm <= 30.0
        assertTrue(filtered.all { it.distanceKm != null && it.distanceKm!! <= 30.0 })

        // El local lejano (a 65 km) debe ser excluido
        assertFalse(filtered.any { it.id == "pl-10" })
    }

    @Test
    fun `nearby places are ordered by closest distance`() = runBlocking {
        val campusCoords = UserCoordinates(latitude = -12.0833, longitude = -77.0428)
        val allPlaces = repository.getPlaces().first()

        val filtered = useCase(campusCoords, allPlaces, maxDistanceKm = 30.0)

        for (i in 0 until filtered.size - 1) {
            assertTrue(filtered[i].distanceKm!! <= filtered[i + 1].distanceKm!!)
        }
    }
}
