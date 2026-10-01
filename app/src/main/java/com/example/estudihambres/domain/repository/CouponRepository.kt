package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para la gestión y consulta de cupones universitarios.
 */
interface CouponRepository {
    /**
     * Obtiene todos los cupones activos disponibles para estudiantes.
     */
    fun getAvailableCoupons(): Flow<List<Coupon>>

    /**
     * Obtiene cupones filtrados por el identificador del lugar asociado.
     *
     * @param placeId Identificador único del comercio.
     */
    fun getCouponsByPlace(placeId: String): Flow<List<Coupon>>
}
