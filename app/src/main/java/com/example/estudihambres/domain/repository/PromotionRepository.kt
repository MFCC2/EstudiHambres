package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.Promotion
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para el catálogo de promociones y beneficios estudiantiles.
 */
interface PromotionRepository {
    /**
     * Emite el listado de promociones activas.
     */
    fun getPromotions(): Flow<List<Promotion>>
}
