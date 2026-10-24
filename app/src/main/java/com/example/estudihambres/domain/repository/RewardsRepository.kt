package com.example.estudihambres.domain.repository

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.ReceiptValidationResult
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para el catálogo de recompensas de StudiPuntos (Tienda de Premios),
 * billetera de lealtad, libro mayor criptográfico y validación fiscal de boletas electrónicas SUNAT.
 */
interface RewardsRepository {

    /**
     * Emite el listado de tarjetas de regalo y beneficios verificados disponibles para canje.
     */
    fun getAvailableRewards(): Flow<List<RewardItem>>

    /**
     * Emite la lista permanente de premios que el estudiante ha canjeado activamente.
     */
    fun getRedeemedRewards(): Flow<List<RedeemedReward>>

    /**
     * Emite el saldo en tiempo real de StudiPuntos calculado a partir del libro mayor.
     */
    fun getUserStudiPuntos(): Flow<Int>

    /**
     * Emite el historial cronológico inmutable de transacciones (+pts y -pts) con encadenamiento SHA-256.
     */
    fun getPointTransactions(): Flow<List<PointTransaction>>

    /**
     * Procesa el canje de un premio descontando los puntos correspondientes y generando el vale digital.
     *
     * @param reward Recompensa a canjear.
     * @return [Resource.Success] con el premio canjeado o [Resource.Error] si los puntos son insuficientes.
     */
    suspend fun redeemReward(reward: RewardItem): Resource<RedeemedReward>

    /**
     * Registra y audita una boleta electrónica SUNAT mediante sus datos fiscales.
     */
    suspend fun registerReceipt(
        ruc: String,
        serie: String,
        correlativo: String,
        amount: Double,
        dateMillis: Long
    ): Resource<ReceiptValidationResult>

    /**
     * Registra y valida una boleta escaneada a través de su código QR oficial de SUNAT.
     */
    suspend fun registerReceiptQr(qrRawText: String): Resource<ReceiptValidationResult>

    /**
     * Marca un vale canjeado como utilizado en la caja del establecimiento o web del aliado.
     */
    suspend fun markRewardAsUsed(rewardId: String): Boolean

    /**
     * Método de compatibilidad rápida para acreditar puntos por boleta (+80).
     */
    suspend fun earnPointsFromReceipt(): Resource<Int>
}
