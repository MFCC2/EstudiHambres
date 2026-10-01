package com.example.estudihambres.data.repository

import com.example.estudihambres.domain.model.Commerce
import com.example.estudihambres.domain.repository.CommerceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Implementación de prueba / en memoria de [CommerceRepository] para proveer datos
 * de comercios aliados a estudiantes universitarios.
 */
class MockCommerceRepositoryImpl : CommerceRepository {

    private val sampleCommerces = listOf(
        Commerce(
            id = "c1",
            name = "El Rincón Universitario",
            description = "Menú estudiantil casero, opciones vegetarianas y jugos naturales.",
            category = "Menú Ejecutivo",
            discountDescription = "20% de descuento mostrando CampusPass",
            latitude = -12.0833,
            longitude = -77.0428,
            address = "Av. Universitaria 1234"
        ),
        Commerce(
            id = "c2",
            name = "Burger Campus",
            description = "Hamburguesas artesanales, papas rústicas y combos para grupos de estudio.",
            category = "Comida Rápida",
            discountDescription = "Combo Estudiante: 25% off en burgers dobles",
            latitude = -12.0870,
            longitude = -77.0490,
            address = "Calle Los Estudiantes 456"
        ),
        Commerce(
            id = "c3",
            name = "Café & Tesis",
            description = "Espacio de coworking con café de especialidad, wifi veloz y repostería.",
            category = "Cafetería",
            discountDescription = "Café americano gratis con consumo mínimo de S/ 15",
            latitude = -12.0910,
            longitude = -77.0380,
            address = "Jr. Las Letras 789"
        ),
        Commerce(
            id = "c4",
            name = "Pizzería La Previa",
            description = "Pizzas medianas y familiares ideales para reuniones y celebraciones de exámenes.",
            category = "Pizzería",
            discountDescription = "2x1 en pizzas familiares de lunes a jueves",
            latitude = -12.0790,
            longitude = -77.0510,
            address = "Av. Colonial 980"
        ),
        Commerce(
            id = "c5",
            name = "Comercio Foráneo Lejano",
            description = "Comercio fuera del radio de 30 km para verificar pruebas de filtro.",
            category = "Varios",
            discountDescription = "10% off",
            latitude = -11.5000,
            longitude = -76.5000,
            address = "Carretera Central Km 65"
        )
    )

    override fun getCommerces(): Flow<List<Commerce>> {
        return flowOf(sampleCommerces)
    }

    override suspend fun getCommerceById(id: String): Commerce? {
        return sampleCommerces.find { it.id == id }
    }
}
