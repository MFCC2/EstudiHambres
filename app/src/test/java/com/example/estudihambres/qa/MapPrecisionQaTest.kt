package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.domain.usecase.FilterPlacesByDistanceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para Validar la Precisión Geográfica del Mapa y Evitar Superposiciones Erradas.
 */
class MapPrecisionQaTest {

    private lateinit var repository: MockPlaceRepository
    private lateinit var filterUseCase: FilterPlacesByDistanceUseCase

    // Coordenadas conocidas del Hospital Regional del Cusco (Av. de la Cultura / Wanchaq)
    private val hospitalRegionalCuscoLat = -13.5238
    private val hospitalRegionalCuscoLng = -71.9500

    // Coordenadas conocidas del Hospital Edgardo Rebagliati Martins en Lima (Jesús María)
    private val hospitalRebagliatiLimaLat = -12.0792
    private val hospitalRebagliatiLimaLng = -77.0392

    @Before
    fun setUp() {
        repository = MockPlaceRepository()
        filterUseCase = FilterPlacesByDistanceUseCase()
    }

    @Test
    fun testStarbucksRealPlazaCuscoCoordinatesAreAccurateAndNotInHospital() = runBlocking {
        val places = repository.getPlaces().first()
        val starbucksCusco = places.find { it.id == "cu-02" }
        assertNotNull("Starbucks Real Plaza Cusco debe estar en el catálogo", starbucksCusco)

        // Starbucks Real Plaza Cusco está en Av. Collasuyo 2964 (~ -13.5232, -71.9442)
        val distanceToHospitalKm = filterUseCase.calculateDistanceKm(
            starbucksCusco!!.latitude,
            starbucksCusco.longitude,
            hospitalRegionalCuscoLat,
            hospitalRegionalCuscoLng
        )

        // Debe haber una separación física clara (> 500 metros) respecto al hospital regional
        assertTrue(
            "Starbucks Cusco ($distanceToHospitalKm km) NO debe estar dentro del Hospital Regional",
            distanceToHospitalKm > 0.5
        )

        // Debe coincidir con la ubicación de Real Plaza Cusco (longitud ~ -71.944)
        assertTrue(starbucksCusco.longitude > -71.946 && starbucksCusco.longitude < -71.943)
    }

    @Test
    fun testStarbucksSanMiguelLimaCoordinatesAreAccurateAndNotInHospital() = runBlocking {
        val places = repository.getPlaces().first()
        val starbucksLima = places.find { it.id == "pl-04" }
        assertNotNull("Starbucks Lima debe estar en el catálogo", starbucksLima)

        // Starbucks San Miguel / PUCP debe estar en Plaza San Miguel (~ -12.0772, -77.0802)
        val distanceToRebagliatiKm = filterUseCase.calculateDistanceKm(
            starbucksLima!!.latitude,
            starbucksLima.longitude,
            hospitalRebagliatiLimaLat,
            hospitalRebagliatiLimaLng
        )

        // Debe estar a más de 4 km del Hospital Rebagliati (ubicado en Jesús María)
        assertTrue(
            "Starbucks Lima ($distanceToRebagliatiKm km) NO debe estar en la zona hospitalaria de Jesús María",
            distanceToRebagliatiKm > 4.0
        )

        // Debe estar en San Miguel (longitud ~ -77.080)
        assertTrue(starbucksLima.longitude < -77.075 && starbucksLima.longitude > -77.085)
    }

    @Test
    fun testAllNearbyPlacesHaveValidPeruvianCoordinates() = runBlocking {
        val places = repository.getPlaces().first()
        places.filter { !it.name.contains("Campestre") }.forEach { place ->
            // Perú se encuentra en latitud negativa (-18 a 0) y longitud negativa (-81 a -68)
            assertTrue("Latitud debe ser válida para Perú", place.latitude < 0.0 && place.latitude > -19.0)
            assertTrue("Longitud debe ser válida para Perú", place.longitude < -68.0 && place.longitude > -82.0)
            assertFalse("URL oficial no debe estar en blanco", place.url.isBlank())
            assertTrue("URL oficial debe ser HTTPS", place.url.startsWith("https://"))
        }
    }
}
