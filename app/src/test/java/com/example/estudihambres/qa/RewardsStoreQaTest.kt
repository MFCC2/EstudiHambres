package com.example.estudihambres.qa

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.RewardItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para la Tienda de StudiPuntos (Premios) y Billetera de Lealtad Universitaria.
 */
class RewardsStoreQaTest {

    private lateinit var repository: MockRewardsRepositoryImpl

    @Before
    fun setUp() {
        repository = MockRewardsRepositoryImpl()
    }

    @Test
    fun testRewardsCatalogContainsOfficialGiftCards() = runBlocking {
        val rewards = repository.getAvailableRewards().first()
        assertTrue("El catálogo debe contener al menos 6 premios", rewards.size >= 6)

        assertTrue(rewards.any { it.title.contains("Roblox") && it.pointsCost == 1800 })
        assertTrue(rewards.any { it.title.contains("Xbox") && it.pointsCost == 2200 })
        assertTrue(rewards.any { it.title.contains("Spotify") && it.pointsCost == 1200 })
        assertTrue(rewards.any { it.title.contains("Mega Bajón") && it.pointsCost == 950 })
        assertTrue(rewards.any { it.title.contains("Steam") && it.pointsCost == 2000 })
    }

    @Test
    fun testRedeemRewardSucceedsWithSufficientPoints() = runBlocking {
        val reward = RewardItem(
            id = "test-rew-01",
            title = "Spotify Premium Estudiante",
            category = "Música",
            pointsCost = 1200,
            description = "1 mes de música sin interrupciones",
            partnerName = "Spotify",
            iconEmoji = "🎧",
            sampleCode = "SPOT-UNIV-4411"
        )

        val result = repository.redeemReward(reward)
        assertTrue("El canje debe ser exitoso con saldo suficiente", result is Resource.Success)
        val redeemed = (result as Resource.Success).data
        assertNotNull(redeemed)
        assertEquals("Spotify Premium Estudiante", redeemed?.rewardTitle)
        assertEquals(1200, redeemed?.pointsCost)
        assertTrue("El código generado debe contener el prefijo del aliado", redeemed?.redemptionCode?.startsWith("SPOT") == true)
        assertNotNull(redeemed?.redeemedAtFormatted)
    }

    @Test
    fun testRedeemRewardFailsWhenInsufficientPoints() = runBlocking {
        val expensiveReward = RewardItem(
            id = "test-rew-exp",
            title = "Viaje de Estudios Internacional",
            category = "Educación",
            pointsCost = 999999,
            description = "Premio exclusivo fuera del alcance actual",
            partnerName = "Global Education",
            iconEmoji = "✈️",
            sampleCode = "TRIP-MAX-9999"
        )

        val result = repository.redeemReward(expensiveReward)
        assertTrue("El canje debe fallar por saldo insuficiente", result is Resource.Error)
        assertTrue("El mensaje debe advertir sobre saldo insuficiente", result.message?.contains("insuficiente") == true)
    }

    @Test
    fun testEarnPointsFromReceiptAdds80Points() = runBlocking {
        val initialPoints = repository.getUserStudiPuntos().first()
        val result = repository.earnPointsFromReceipt()
        assertTrue(result is Resource.Success)
        val newPoints = (result as Resource.Success).data
        assertEquals(initialPoints + 80, newPoints)
    }

    @Test
    fun testConversionRateCalculation() {
        val points = 2450
        val solesValue = points / 100.0
        assertEquals(24.50, solesValue, 0.01)

        val points500 = 500
        val soles500 = points500 / 100.0
        assertEquals(5.00, soles500, 0.01)
    }
}
