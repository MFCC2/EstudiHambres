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
        // === LOCALES REALES CUSCO (Real Plaza Cusco, Av. de la Cultura, Campus Continental, UNSAAC) ===
        PlaceLocal(
            id = "cu-01",
            name = "Bembos Real Plaza Cusco",
            category = "Comida Rápida",
            address = "Real Plaza Cusco, Av. Collasuyo 2964, Wanchaq",
            latitude = -13.5233,
            longitude = -71.9482,
            rating = 4.8,
            discountBadge = "40% DCTO",
            discountDescription = "Combo Universitario 2x1 en hamburguesas clásicas con carnet",
            url = "https://www.bembos.com.pe/promociones"
        ),
        PlaceLocal(
            id = "cu-02",
            name = "Starbucks Real Plaza Cusco",
            category = "Cafetería & Cowork",
            address = "Real Plaza Cusco 1er Nivel, Av. Collasuyo",
            latitude = -13.5231,
            longitude = -71.9478,
            rating = 4.9,
            discountBadge = "2x1 PROMO",
            discountDescription = "2x1 en Frappuccinos y café del día de lunes a jueves",
            url = "https://www.starbucks.pe"
        ),
        PlaceLocal(
            id = "cu-03",
            name = "Papa John's Pizza Av. de la Cultura",
            category = "Pizzería",
            address = "Av. de la Cultura 720, Wanchaq",
            latitude = -13.5222,
            longitude = -71.9520,
            rating = 4.7,
            discountBadge = "2x1 SLICE",
            discountDescription = "2x1 en porciones dobles y pizzas familiares para estudiantes",
            url = "https://www.papajohns.com.pe"
        ),
        PlaceLocal(
            id = "cu-04",
            name = "Cineplanet Real Plaza Cusco",
            category = "Entretenimiento",
            address = "Real Plaza Cusco 3er Nivel, Av. Collasuyo",
            latitude = -13.5235,
            longitude = -71.9485,
            rating = 4.8,
            discountBadge = "2x1 ENTRADAS",
            discountDescription = "Medio precio en entradas 2D de lunes a viernes con carnet vigente",
            url = "https://www.cineplanet.com.pe"
        ),
        PlaceLocal(
            id = "cu-05",
            name = "KFC / Pizza Hut Av. de la Cultura",
            category = "Comida Rápida",
            address = "Av. de la Cultura 1024 (Frente a UNSAAC)",
            latitude = -13.5215,
            longitude = -71.9540,
            rating = 4.6,
            discountBadge = "30% OFF",
            discountDescription = "30% de descuento en Mega Box Universitario",
            url = "https://www.kfc.com.pe"
        ),
        PlaceLocal(
            id = "cu-06",
            name = "Café & Cowork Cultural UNSAAC",
            category = "Cafetería & Cowork",
            address = "Av. de la Cultura 733 (Inmediaciones UNSAAC)",
            latitude = -13.5208,
            longitude = -71.9585,
            rating = 4.9,
            discountBadge = "GRATIS",
            discountDescription = "Café americano gratis con consumo mínimo de S/ 12",
            url = "https://campuspass.pe"
        ),
        PlaceLocal(
            id = "cu-07",
            name = "Pizzería La Previa Cusco",
            category = "Pizzería",
            address = "Av. de la Cultura 840, Wanchaq",
            latitude = -13.5225,
            longitude = -71.9505,
            rating = 4.8,
            discountBadge = "S/ 11.90",
            discountDescription = "Pizza personal + bebida helada a precio de estudiante",
            url = "https://campuspass.pe"
        ),
        PlaceLocal(
            id = "cu-08",
            name = "Sandwichería El Bajón Universitario",
            category = "Sandwichería",
            address = "Av. Universitaria / Av. de la Cultura 1150",
            latitude = -13.5218,
            longitude = -71.9560,
            rating = 4.7,
            discountBadge = "20% OFF",
            discountDescription = "Pan con chicharrón y jugos naturales con 20% de descuento",
            url = "https://campuspass.pe"
        ),
        PlaceLocal(
            id = "cu-09",
            name = "Cafetería Central Campus Continental",
            category = "Cafetería",
            address = "Campus Continental, Calle Manuel Prado / Los Sauces",
            latitude = -13.5240,
            longitude = -71.9440,
            rating = 4.9,
            discountBadge = "15% DTO",
            discountDescription = "15% en snacks saludables, almuerzos ejecutivos y café",
            url = "https://campuspass.pe"
        ),
        PlaceLocal(
            id = "cu-10",
            name = "Restaurante Campestre Valle Sagrado",
            category = "Comida Campestre",
            address = "Carretera Urubamba - Ollantaytambo Km 48 (Fuera de 30 km)",
            latitude = -13.3000,
            longitude = -72.1100,
            rating = 4.2,
            discountBadge = "10% DTO",
            discountDescription = "Comercio foráneo alejado para verificar exclusión del radar de 30 km",
            url = "https://campuspass.pe"
        ),

        // === LOCALES REALES LIMA (Campus UNMSM, PUCP, Av. Universitaria) ===
        PlaceLocal(
            id = "pl-01",
            name = "Bembos Campus",
            category = "Comida Rápida",
            address = "Av. Universitaria 1420",
            latitude = -12.0810,
            longitude = -77.0400,
            rating = 4.7,
            discountBadge = "2x1",
            discountDescription = "2x1 en hamburguesas clásicas presentando CampusPass",
            url = "https://www.bembos.com.pe/promociones"
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
            discountDescription = "2x1 en pizzas familiares de lunes a jueves",
            url = "https://campuspass.pe"
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
            discountDescription = "25% de descuento en combos dobles universitarios",
            url = "https://campuspass.pe"
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
            discountDescription = "Café americano gratis con consumo mínimo de S/ 15",
            url = "https://campuspass.pe"
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
            discountDescription = "Menú casero completo (entrada, segundo y refresco)",
            url = "https://campuspass.pe"
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
            discountDescription = "Pan con chicharrón y jugos naturales con 20% de descuento",
            url = "https://campuspass.pe"
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
            discountDescription = "Jugo surtido de litro + sándwich mixto a precio universitario",
            url = "https://campuspass.pe"
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
            discountDescription = "2 burritos grandes + bebida helada por S/ 15",
            url = "https://campuspass.pe"
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
            discountDescription = "15% en snacks saludables y bebidas frías",
            url = "https://campuspass.pe"
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
            discountDescription = "Comercio foráneo alejado para verificar exclusión del radar",
            url = "https://campuspass.pe"
        )
    )

    override fun getPlaces(): Flow<List<Place>> {
        return flowOf(localPlaces.map { it.toDomain() })
    }

    override suspend fun getPlaceById(id: String): Place? {
        return localPlaces.find { it.id == id }?.toDomain()
    }
}
