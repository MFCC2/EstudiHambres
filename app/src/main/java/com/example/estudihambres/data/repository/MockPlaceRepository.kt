package com.example.estudihambres.data.repository

import com.example.estudihambres.data.model.PlaceLocal
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio simulado (mock) de lugares y restaurantes con beneficios para estudiantes.
 */
class MockPlaceRepository : PlaceRepository {

    private val localPlaces = listOf(
        PlaceLocal(
            id = "pl-01",
            name = "Pizzería La Previa",
            category = "Pizzería",
            address = "Av. Colonial 980",
            latitude = -12.0790,
            longitude = -77.0510,
            rating = 4.8
        ),
        PlaceLocal(
            id = "pl-02",
            name = "Burger Campus",
            category = "Comida Rápida",
            address = "Calle Los Estudiantes 456",
            latitude = -12.0870,
            longitude = -77.0490,
            rating = 4.6
        ),
        PlaceLocal(
            id = "pl-03",
            name = "Café & Tesis",
            category = "Cafetería & Cowork",
            address = "Jr. Las Letras 789",
            latitude = -12.0910,
            longitude = -77.0380,
            rating = 4.9
        ),
        PlaceLocal(
            id = "pl-04",
            name = "El Rincón Universitario",
            category = "Menú Ejecutivo",
            address = "Av. Universitaria 1234",
            latitude = -12.0833,
            longitude = -77.0428,
            rating = 4.7
        )
    )

    override fun getPlaces(): Flow<List<Place>> {
        return flowOf(localPlaces.map { it.toDomain() })
    }

    override suspend fun getPlaceById(id: String): Place? {
        return localPlaces.find { it.id == id }?.toDomain()
    }
}
