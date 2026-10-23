package com.example.estudihambres.domain.repository

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para el catálogo de recompensas de StudiPuntos (Tienda de Premios).
 */
interface RewardsRepository {

    /**
     * Emite el listado de tarjetas de regalo y premios disponibles para canje.
     */
    fun getAvailableRewards(): Flow<List<RewardItem>>

    /**
     * Emite la lista de premios que el estudiante ha canjeado activamente.
     */
    fun getRedeemedRewards(): Flow<List<RedeemedReward>>

    /**
     * Emite el saldo en tiempo real de StudiPuntos del estudiante.
     */
    fun getUserStudiPuntos(): Flow<Int>

    /**
     * Procesa el canje de un premio descontando los puntos correspondientes y generando el vale digital.
     *
     * @param reward Recompensa a canjear.
     * @return [Resource.Success] con el premio canjeado o [Resource.Error] si los puntos son insuficientes.
     */
    suspend fun redeemReward(reward: RewardItem): Resource<RedeemedReward>

    /**
     * Acredita +80 StudiPuntos al registrar o escanear una boleta de compra en comercio asociado.
     *
     * @return [Resource.Success] con el nuevo saldo de puntos.
     */
    suspend fun earnPointsFromReceipt(): Resource<Int>
}
