package com.example.estudihambres.data.repository

import com.example.estudihambres.data.model.CouponLocal
import com.example.estudihambres.domain.model.Coupon
import com.example.estudihambres.domain.repository.CouponRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio simulado (mock) para proveer cupones estudiantiles de prueba.
 */
class MockCouponRepository : CouponRepository {

    private val localCoupons = listOf(
        CouponLocal(
            id = "cp-01",
            title = "2x1 en Pizzas Medianas Pan Pizza",
            description = "Válido de lunes a jueves en salón y para llevar con carnet universitario activo.",
            discountPercentage = 50,
            placeId = "pl-02",
            placeName = "Pizza Hut",
            expirationDate = "2026-12-31"
        ),
        CouponLocal(
            id = "cp-02",
            title = "Combo Clásica Universitaria a S/ 14.90",
            description = "Hamburguesa clásica a la parrilla + papas medianas + bebida personal con carnet.",
            discountPercentage = 35,
            placeId = "pl-01",
            placeName = "Bembos",
            expirationDate = "2026-11-30"
        ),
        CouponLocal(
            id = "cp-03",
            title = "2x1 en Frappuccinos y Café del Día",
            description = "2x1 en bebidas seleccionadas de lunes a jueves en tamaño Alto para universitarios.",
            discountPercentage = 50,
            placeId = "cu-02",
            placeName = "Starbucks Coffee",
            expirationDate = "2026-12-31"
        ),
        CouponLocal(
            id = "cp-04",
            title = "2x1 en Entradas 2D de Lunes a Viernes",
            description = "Medio precio o 2x1 en entradas regulares 2D presentando carnet universitario activo.",
            discountPercentage = 50,
            placeId = "cu-04",
            placeName = "Cineplanet",
            expirationDate = "2026-12-31"
        ),
        CouponLocal(
            id = "cp-05",
            title = "Mega Box Estudiantil a S/ 14.90",
            description = "2 piezas de pollo crujiente + papas + puré + ensalada y gaseosa con carnet.",
            discountPercentage = 30,
            placeId = "cu-05",
            placeName = "KFC Perú",
            expirationDate = "2026-12-31"
        )
    )

    override fun getAvailableCoupons(): Flow<List<Coupon>> {
        return flowOf(localCoupons.filter { it.isAvailable }.map { it.toDomain() })
    }

    override fun getCouponsByPlace(placeId: String): Flow<List<Coupon>> {
        return flowOf(localCoupons.filter { it.placeId == placeId && it.isAvailable }.map { it.toDomain() })
    }
}
