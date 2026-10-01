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
            title = "2x1 en Pizzas Medianas",
            description = "Válido de lunes a jueves presentando credencial activa de CampusPass.",
            discountPercentage = 50,
            placeId = "pl-01",
            placeName = "Pizzería La Previa",
            expirationDate = "2026-12-31"
        ),
        CouponLocal(
            id = "cp-02",
            title = "25% off en Combo Doble",
            description = "Incluye papas rústicas y bebida universitaria.",
            discountPercentage = 25,
            placeId = "pl-02",
            placeName = "Burger Campus",
            expirationDate = "2026-11-30"
        ),
        CouponLocal(
            id = "cp-03",
            title = "Café Americano Gratis",
            description = "Por compras mayores a S/ 15 en repostería durante época de parciales.",
            discountPercentage = 100,
            placeId = "pl-03",
            placeName = "Café & Tesis",
            expirationDate = "2026-10-31"
        )
    )

    override fun getAvailableCoupons(): Flow<List<Coupon>> {
        return flowOf(localCoupons.filter { it.isAvailable }.map { it.toDomain() })
    }

    override fun getCouponsByPlace(placeId: String): Flow<List<Coupon>> {
        return flowOf(localCoupons.filter { it.placeId == placeId && it.isAvailable }.map { it.toDomain() })
    }
}
