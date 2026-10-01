package com.example.estudihambres.data.repository

import com.example.estudihambres.domain.model.Promotion
import com.example.estudihambres.domain.repository.PromotionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Implementación mock del repositorio de promociones con el catálogo de beneficios universitarios
 * requeridos por la Fase 4 (Bembos 2x1, Spotify/YouTube, Notion Pack, etc.).
 */
class MockPromotionRepositoryImpl : PromotionRepository {

    private val samplePromotions = listOf(
        Promotion(
            id = "p-01",
            title = "Bembos 2x1 Clásica Mediana",
            description = "Compra una hamburguesa clásica a lo pobre y llévate la segunda gratis mostrando tu carnet.",
            category = "Comida",
            discountTag = "2x1",
            partnerName = "Bembos",
            estimatedSavingsSoles = 19.90,
            isFeatured = true
        ),
        Promotion(
            id = "p-02",
            title = "Spotify Premium para Estudiantes",
            description = "Música sin anuncios, descargas offline y plan mensual a mitad de precio verificado con SheerID/CampusPass.",
            category = "Herramientas digitales",
            discountTag = "50% OFF",
            partnerName = "Spotify",
            estimatedSavingsSoles = 12.00,
            isFeatured = true
        ),
        Promotion(
            id = "p-03",
            title = "YouTube Premium Plan Universitario",
            description = "YouTube y YouTube Music sin cortes comerciales en tu smartphone y laptop durante todo el semestre.",
            category = "Herramientas digitales",
            discountTag = "40% OFF",
            partnerName = "YouTube",
            estimatedSavingsSoles = 14.50,
            isFeatured = false
        ),
        Promotion(
            id = "p-04",
            title = "Notion Education Plus Pack",
            description = "Almacenamiento de archivos ilimitado, historial de versiones de 30 días y workspaces colaborativos gratis.",
            category = "Herramientas digitales",
            discountTag = "GRATIS",
            partnerName = "Notion",
            estimatedSavingsSoles = 45.00,
            isFeatured = true
        ),
        Promotion(
            id = "p-05",
            title = "Menú Ejecutivo Universitario",
            description = "Entrada, segundo, refresco ilimitado y postre en locales aledaños al campus.",
            category = "Comida",
            discountTag = "S/ 12.90",
            partnerName = "El Buen Menú",
            estimatedSavingsSoles = 7.00,
            isFeatured = false
        ),
        Promotion(
            id = "p-06",
            title = "Corredor Rojo / Metropolitano Medio Pasaje",
            description = "Tarifa universitaria oficial del 50% con carnet universitario vigente en todas las troncales.",
            category = "Transporte",
            discountTag = "50% DTO",
            partnerName = "ATU",
            estimatedSavingsSoles = 25.00,
            isFeatured = false
        )
    )

    override fun getPromotions(): Flow<List<Promotion>> {
        return flowOf(samplePromotions)
    }
}
