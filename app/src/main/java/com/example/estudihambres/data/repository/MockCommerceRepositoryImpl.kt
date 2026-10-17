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
            name = "Bembos Real Plaza / Campus",
            description = "Hamburguesas a la parrilla, combos estudiantiles y papas crocantes con carnet.",
            category = "Comida Rápida",
            discountDescription = "Combo Universitario a S/ 14.90 mostrando carnet activo",
            latitude = -12.0833,
            longitude = -77.0428,
            address = "Av. Universitaria 1420 (Cerca a Campus)"
        ),
        Commerce(
            id = "c2",
            name = "Starbucks Coffee",
            description = "Café de especialidad, wifi de alta velocidad, frappuccinos y espacios de estudio.",
            category = "Cafetería & Cowork",
            discountDescription = "2x1 en Frappuccinos tamaño Alto de lunes a jueves",
            latitude = -12.0870,
            longitude = -77.0490,
            address = "Av. Universitaria 1801, San Miguel"
        ),
        Commerce(
            id = "c3",
            name = "Cineplanet Multicines",
            description = "Salas de cine 2D/3D con cartelera de estreno y confitería universitaria.",
            category = "Entretenimiento",
            discountDescription = "2x1 en entradas 2D regulares de lunes a viernes con carnet",
            latitude = -12.0910,
            longitude = -77.0380,
            address = "Centro Comercial Plaza San Miguel / Real Plaza"
        ),
        Commerce(
            id = "c4",
            name = "Pizza Hut",
            description = "Pizzas Pan Pizza, combos personales y familiares ideales para grupos de estudio.",
            category = "Pizzería",
            discountDescription = "2x1 en pizzas medianas tradicionales de lunes a jueves",
            latitude = -12.0790,
            longitude = -77.0510,
            address = "Av. La Marina 2050 / Av. Colonial"
        ),
        Commerce(
            id = "c5",
            name = "Comercio Foráneo Huachipa (Control 30km)",
            description = "Comercio fuera del radio de 30 km para verificar pruebas de filtro esférico.",
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
