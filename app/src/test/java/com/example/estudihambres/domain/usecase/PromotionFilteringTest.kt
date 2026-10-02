package com.example.estudihambres.domain.usecase

import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para el catálogo de promociones y filtrado por categorías (Agente 5: QA).
 */
class PromotionFilteringTest {

    private lateinit var repository: MockPromotionRepositoryImpl

    @Before
    fun setUp() {
        repository = MockPromotionRepositoryImpl()
    }

    @Test
    fun `promotions catalog contains requested perks like Bembos, Spotify, Notion`() = runBlocking {
        val promotions = repository.getPromotions().first()

        assertTrue(promotions.any { it.partnerName.contains("Bembos") && (it.discountTag.contains("2x1") || it.title.contains("2x1")) })
        assertTrue(promotions.any { it.partnerName.contains("Spotify") })
        assertTrue(promotions.any { it.partnerName.contains("Notion") })
    }

    @Test
    fun `filtering by category Comida returns food perks`() = runBlocking {
        val promotions = repository.getPromotions().first()
        val foodPromos = promotions.filter { it.category == "Comida" }

        assertTrue(foodPromos.isNotEmpty())
        assertTrue(foodPromos.all { it.category == "Comida" })
    }
}
