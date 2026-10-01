package com.example.estudihambres.data.repository

import com.example.estudihambres.data.model.PlaceLocal
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio de lugares y comercios aliados en un radio de hasta 30 km del campus universitario.
 * Incluye más de 8 comercios cercanos y un local foráneo lejano para probar el filtro de 30 km.
 */
class MockPlaceRepository : PlaceRepository {

    private val localPlaces = listOf(
        PlaceLocal(
            id = "pl-01",
            name = "Bembos Campus",
            category = "Comida Rápida",
            address = "Av. Universitaria 1420",
            latitude = -12.0810,
            longitude = -77.0400,
            rating = 4.7,
            discountBadge = "2x1",
            discountDescription = "2x1 en hamburguesas clásicas presentando CampusPass"
        ),
        PlaceLocal(
            id = "pl-02",
            name = "Pizzería La Previa",
            category = "Pizzería",
            address = "Av. Colonial 980",
            latitude = -12.0790,
            longitude = -77.0510,
            rating = 4.8,
            discountBadge = "2x1",
            discountDescription = "2x1 en pizzas familiares de lunes a jueves"
        ),
        PlaceLocal(
            id = "pl-03",
            name = "Burger Campus",
            category = "Comida Rápida",
            address = "Calle Los Estudiantes 456",
            latitude = -12.0870,
            longitude = -77.0490,
            rating = 4.6,
            discountBadge = "25% OFF",
            discountDescription = "25% de descuento en combos dobles universitarios"
        ),
        PlaceLocal(
            id = "pl-04",
            name = "Café & Tesis",
            category = "Cafetería & Cowork",
            address = "Jr. Las Letras 789",
            latitude = -12.0910,
            longitude = -77.0380,
            rating = 4.9,
            discountBadge = "GRATIS",
            discountDescription = "Café americano gratis con consumo mínimo de S/ 15"
        ),
        PlaceLocal(
            id = "pl-05",
            name = "El Rincón Universitario",
            category = "Menú Ejecutivo",
            address = "Av. Universitaria 1234",
            latitude = -12.0833,
            longitude = -77.0428,
            rating = 4.7,
            discountBadge = "S/ 11.50",
            discountDescription = "Menú casero completo (entrada, segundo y refresco)"
        ),
        PlaceLocal(
            id = "pl-06",
            name = "Sandwichería El Bajón",
            category = "Sandwichería",
            address = "Av. Venezuela 820",
            latitude = -12.0750,
            longitude = -77.0460,
            rating = 4.5,
            discountBadge = "20% OFF",
            discountDescription = "Pan con chicharrón y jugos naturales con 20% de descuento"
        ),
        PlaceLocal(
            id = "pl-07",
            name = "Juguería San Marcos",
            category = "Jugos & Snacks",
            address = "Jr. Los Álamos 210",
            latitude = -12.0860,
            longitude = -77.0350,
            rating = 4.8,
            discountBadge = "S/ 9.00",
            discountDescription = "Jugo surtido de litro + sándwich mixto a precio universitario"
        ),
        PlaceLocal(
            id = "pl-08",
            name = "Tacos & Burritos Uni",
            category = "Comida Mexicana",
            address = "Av. Bolívar 650",
            latitude = -12.0950,
            longitude = -77.0450,
            rating = 4.6,
            discountBadge = "COMBO",
            discountDescription = "2 burritos grandes + bebida helada por S/ 15"
        ),
        PlaceLocal(
            id = "pl-09",
            name = "Cafetería Central Campus",
            category = "Cafetería",
            address = "Campus Universitario Puerta 3",
            latitude = -12.0845,
            longitude = -77.0415,
            rating = 4.4,
            discountBadge = "15% DTO",
            discountDescription = "15% en snacks saludables y bebidas frías"
        ),
        PlaceLocal(
            id = "pl-10",
            name = "Restaurante Campestre Huachipa",
            category = "Comida Campestre",
            address = "Carretera Central Km 65 (Fuera de 30 km)",
            latitude = -11.5000,
            longitude = -76.5000,
            rating = 4.0,
            discountBadge = "10% DTO",
            discountDescription = "Comercio foráneo alejado para verificar exclusión del radar"
        )
    )

    override fun getPlaces(): Flow<List<Place>> {
        return flowOf(localPlaces.map { it.toDomain() })
    }

    override suspend fun getPlaceById(id: String): Place? {
        return localPlaces.find { it.id == id }?.toDomain()
    }
}
