package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.WebOffer
import com.example.estudihambres.domain.model.WebOfferCategory

/**
 * Fuente de ofertas gastronómicas publicadas en páginas web externas.
 */
interface WebOffersRepository {

    /** Nombre del sitio de origen, para mostrar la atribución en la interfaz. */
    val sourceName: String

    /** Categorías disponibles, la primera es la vista general. */
    val categories: List<WebOfferCategory>

    /**
     * Obtiene las ofertas de una categoría. Usa una caché en memoria de corta duración
     * salvo que [forceRefresh] sea verdadero. Requiere conexión a internet.
     */
    suspend fun getOffers(category: WebOfferCategory, forceRefresh: Boolean = false): Result<List<WebOffer>>
}
