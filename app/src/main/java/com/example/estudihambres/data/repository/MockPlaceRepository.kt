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
            name = "Café D'Wasi / Especialidad Cusco",
            category = "Cafetería & Cowork",
            address = "Calle Santa Catalina Ancha 366 (Cerca a Plaza / Inmediaciones UNSAAC)",
            latitude = -13.5208,
            longitude = -71.9585,
            rating = 4.9,
            discountBadge = "15% DTO",
            discountDescription = "15% de descuento en café de especialidad y postres para estudiantes",
            url = "https://www.dwasi.pe"
        ),
        PlaceLocal(
            id = "cu-07",
            name = "Pizza Hut Real Plaza Cusco",
            category = "Pizzería",
            address = "Real Plaza Cusco Patio de Comidas, Av. Collasuyo",
            latitude = -13.5225,
            longitude = -71.9505,
            rating = 4.8,
            discountBadge = "2x1 PIZZA",
            discountDescription = "2x1 en pizzas medianas tradicionales de lunes a jueves con carnet",
            url = "https://www.pizzahut.com.pe"
        ),
        PlaceLocal(
            id = "cu-08",
            name = "Sándwiches El Chinito Cusco",
            category = "Sandwichería",
            address = "Av. de la Cultura 1150 (Frente a Ciudad Universitaria)",
            latitude = -13.5218,
            longitude = -71.9560,
            rating = 4.7,
            discountBadge = "20% OFF",
            discountDescription = "Pan con chicharrón criollo y jugos naturales con 20% de descuento",
            url = "https://elchinito.com.pe"
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
            discountDescription = "15% en menús universitarios saludables, snacks y café con carnet",
            url = "https://ucontinental.edu.pe"
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
            url = "https://www.peru.travel"
        ),
        PlaceLocal(
            id = "cu-11",
            name = "Librerías Crisol Real Plaza Cusco",
            category = "Librería & Fotocopias",
            address = "Real Plaza Cusco 2do Nivel, Av. Collasuyo 2964",
            latitude = -13.5212,
            longitude = -71.9555,
            rating = 4.9,
            discountBadge = "15% DTO",
            discountDescription = "15% de descuento en libros de ingeniería, humanidades y literatura",
            url = "https://www.crisol.com.pe"
        ),

        // === LOCALES REALES LIMA (Campus UNMSM, PUCP, Av. Universitaria) ===
        PlaceLocal(
            id = "pl-01",
            name = "Bembos Plaza San Miguel",
            category = "Comida Rápida",
            address = "Av. Universitaria con Av. La Marina, San Miguel",
            latitude = -12.0810,
            longitude = -77.0400,
            rating = 4.7,
            discountBadge = "2x1",
            discountDescription = "2x1 en hamburguesas clásicas a la parrilla presentando carnet",
            url = "https://www.bembos.com.pe/promociones"
        ),
        PlaceLocal(
            id = "pl-02",
            name = "Pizza Hut Plaza San Miguel",
            category = "Pizzería",
            address = "Av. La Marina 2050, San Miguel",
            latitude = -12.0790,
            longitude = -77.0510,
            rating = 4.8,
            discountBadge = "2x1",
            discountDescription = "2x1 en pizzas medianas tradicionales de lunes a jueves",
            url = "https://www.pizzahut.com.pe"
        ),
        PlaceLocal(
            id = "pl-03",
            name = "Burger King Plaza San Miguel",
            category = "Comida Rápida",
            address = "Av. Universitaria / Av. La Marina, San Miguel",
            latitude = -12.0870,
            longitude = -77.0490,
            rating = 4.6,
            discountBadge = "25% OFF",
            discountDescription = "25% de descuento en Whopper Doble y combos universitarios",
            url = "https://www.burgerking.com.pe"
        ),
        PlaceLocal(
            id = "pl-04",
            name = "Starbucks Coffee San Miguel / PUCP",
            category = "Cafetería & Cowork",
            address = "Av. Universitaria 1801 (Frente a PUCP)",
            latitude = -12.0910,
            longitude = -77.0380,
            rating = 4.9,
            discountBadge = "2x1 PROMO",
            discountDescription = "2x1 en Frappuccinos y café de filtro de lunes a jueves con carnet",
            url = "https://www.starbucks.pe"
        ),
        PlaceLocal(
            id = "pl-05",
            name = "Cineplanet Plaza San Miguel",
            category = "Entretenimiento",
            address = "Plaza San Miguel 3er Nivel, Av. La Marina 2000",
            latitude = -12.0833,
            longitude = -77.0428,
            rating = 4.7,
            discountBadge = "2x1 ENTRADAS",
            discountDescription = "2x1 en entradas 2D regulares de lunes a viernes con carnet universitario",
            url = "https://www.cineplanet.com.pe/promociones"
        ),
        PlaceLocal(
            id = "pl-06",
            name = "La Lucha Sanguchería San Miguel",
            category = "Sandwichería",
            address = "Av. La Marina 2080, San Miguel",
            latitude = -12.0750,
            longitude = -77.0460,
            rating = 4.5,
            discountBadge = "15% OFF",
            discountDescription = "15% de descuento en sándwiches criollos, pavo y chicharrón con carnet",
            url = "https://lalucha.com.pe"
        ),
        PlaceLocal(
            id = "pl-07",
            name = "Juguería & Frutería San Marcos",
            category = "Jugos & Snacks",
            address = "Av. Universitaria 1320 (Puerta 3 UNMSM)",
            latitude = -12.0860,
            longitude = -77.0350,
            rating = 4.8,
            discountBadge = "S/ 9.00",
            discountDescription = "Jugo surtido de litro + sándwich mixto a tarifa estudiantil",
            url = "https://unmsm.edu.pe"
        ),
        PlaceLocal(
            id = "pl-08",
            name = "KFC Plaza San Miguel",
            category = "Comida Rápida",
            address = "Av. La Marina 2000 / Av. Universitaria",
            latitude = -12.0950,
            longitude = -77.0450,
            rating = 4.6,
            discountBadge = "COMBO",
            discountDescription = "Coronel Box Estudiantil (2 piezas + papas + gaseosa) con carnet",
            url = "https://www.kfc.com.pe/promociones"
        ),
        PlaceLocal(
            id = "pl-09",
            name = "Cafetería Comedor Central UNMSM",
            category = "Cafetería",
            address = "Ciudad Universitaria UNMSM, Av. Venezuela s/n",
            latitude = -12.0845,
            longitude = -77.0415,
            rating = 4.4,
            discountBadge = "15% DTO",
            discountDescription = "Menú estudiantil y snacks saludables para la comunidad universitaria",
            url = "https://unmsm.edu.pe"
        ),
        PlaceLocal(
            id = "pl-11",
            name = "Librerías Crisol Plaza San Miguel",
            category = "Librería & Fotocopias",
            address = "Plaza San Miguel 1er Nivel, Av. La Marina",
            latitude = -12.0825,
            longitude = -77.0410,
            rating = 4.8,
            discountBadge = "15% DTO",
            discountDescription = "15% de descuento en libros académicos, impresiones y útiles con carnet",
            url = "https://www.crisol.com.pe"
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
            url = "https://www.peru.travel"
        )
    )

    override fun getPlaces(): Flow<List<Place>> {
        return flowOf(localPlaces.map { it.toDomain() })
    }

    override suspend fun getPlaceById(id: String): Place? {
        return localPlaces.find { it.id == id }?.toDomain()
    }
}
