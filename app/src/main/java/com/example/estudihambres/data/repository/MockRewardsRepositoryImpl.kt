package com.example.estudihambres.data.repository

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.repository.RewardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Implementación en memoria y con respaldo en [SessionManager] del catálogo de recompensas de StudiPuntos.
 *
 * @param sessionManager Gestor de persistencia de sesión y puntos.
 */
class MockRewardsRepositoryImpl(
    private val sessionManager: SessionManager? = SessionManager.getInstanceOrNull()
) : RewardsRepository {

    private val pointsFlow = MutableStateFlow(sessionManager?.getStudiPuntos() ?: 2450)

    private val catalogRewards = listOf(
        RewardItem(
            id = "rew-01",
            title = "Roblox 400 Robux Card",
            category = "Gaming",
            pointsCost = 1800,
            description = "Canjeable en la app oficial de Roblox para avatar items y pases.",
            partnerName = "Roblox",
            iconEmoji = "🟥",
            sampleCode = "RBLX-9942-XQ88"
        ),
        RewardItem(
            id = "rew-02",
            title = "Xbox Game Pass PC (1 Mes)",
            category = "Gaming",
            pointsCost = 2200,
            description = "Acceso ilimitado a cientos de juegos para PC, EA Play y títulos en la nube.",
            partnerName = "Microsoft Xbox",
            iconEmoji = "🎮",
            sampleCode = "XBOX-MC99-7721"
        ),
        RewardItem(
            id = "rew-03",
            title = "Spotify Premium Estudiante",
            category = "Música",
            pointsCost = 1200,
            description = "1 mes de música sin interrupciones ni anuncios en alta fidelidad.",
            partnerName = "Spotify",
            iconEmoji = "🎧",
            sampleCode = "SPOT-UNIV-4411"
        ),
        RewardItem(
            id = "rew-04",
            title = "Combo Mega Bajón Estudiantil",
            category = "Comida",
            pointsCost = 950,
            description = "Pollo crispy + papas familiares + gaseosa en patios de comida aliados.",
            partnerName = "Mega Bajón",
            iconEmoji = "🍗",
            sampleCode = "VALE-POLLO-2026"
        ),
        RewardItem(
            id = "rew-05",
            title = "Steam Wallet Card S/ 20",
            category = "Gaming",
            pointsCost = 2000,
            description = "Saldo directo en Soles para comprar juegos y DLCs en Steam Store.",
            partnerName = "Valve / Steam",
            iconEmoji = "🕹️",
            sampleCode = "STEM-20PEN-8831"
        ),
        RewardItem(
            id = "rew-06",
            title = "Cineplanet Entrada 2D Regular",
            category = "Cine",
            pointsCost = 1100,
            description = "Boleto válido de lunes a domingo en salas 2D a nivel nacional.",
            partnerName = "Cineplanet",
            iconEmoji = "🎬",
            sampleCode = "CINE-2D-9182"
        ),
        RewardItem(
            id = "rew-07",
            title = "Starbucks Frappuccino Grande",
            category = "Comida",
            pointsCost = 850,
            description = "Bebida fría Frappuccino tamaño Grande a elección en cualquier tienda.",
            partnerName = "Starbucks",
            iconEmoji = "☕",
            sampleCode = "SBUX-FRAP-3321"
        ),
        RewardItem(
            id = "rew-08",
            title = "Vale Librerías Crisol S/ 25",
            category = "Educación",
            pointsCost = 2500,
            description = "Vale de consumo en libros de texto académico, novelas y útiles.",
            partnerName = "Crisol",
            iconEmoji = "📚",
            sampleCode = "CRIS-BOOK-5520"
        )
    )

    override fun getAvailableRewards(): Flow<List<RewardItem>> {
        return flowOf(catalogRewards)
    }

    override fun getRedeemedRewards(): Flow<List<RedeemedReward>> {
        val list = sessionManager?.getRedeemedRewards() ?: emptyList()
        return flowOf(list)
    }

    override fun getUserStudiPuntos(): Flow<Int> {
        return pointsFlow.asStateFlow()
    }

    override suspend fun redeemReward(reward: RewardItem): Resource<RedeemedReward> {
        val current = sessionManager?.getStudiPuntos() ?: pointsFlow.value
        if (current < reward.pointsCost) {
            return Resource.Error("⚠️ Saldo insuficiente de StudiPuntos (${current} pts). Necesitas ${reward.pointsCost} pts.")
        }

        val successDeduct = sessionManager?.deductStudiPuntos(reward.pointsCost) ?: true
        if (!successDeduct) {
            return Resource.Error("No se pudo completar el canje.")
        }

        val newBalance = current - reward.pointsCost
        pointsFlow.value = newBalance

        val prefix = reward.title.take(4).uppercase().replace(" ", "X")
        val randomSuffix = (1000..9999).random()
        val randomLetters = ('A'..'Z').random().toString() + ('A'..'Z').random().toString()
        val generatedCode = "$prefix-$randomSuffix-$randomLetters"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())

        val redeemed = RedeemedReward(
            id = UUID.randomUUID().toString(),
            rewardTitle = reward.title,
            pointsCost = reward.pointsCost,
            redemptionCode = generatedCode,
            redeemedAtFormatted = formattedDate,
            partnerName = reward.partnerName,
            iconEmoji = reward.iconEmoji
        )

        sessionManager?.saveRedeemedReward(redeemed)

        return Resource.Success(redeemed)
    }

    override suspend fun earnPointsFromReceipt(): Resource<Int> {
        val bonus = 80
        val updated = sessionManager?.addStudiPuntos(bonus) ?: (pointsFlow.value + bonus)
        pointsFlow.value = updated
        return Resource.Success(updated)
    }
}
