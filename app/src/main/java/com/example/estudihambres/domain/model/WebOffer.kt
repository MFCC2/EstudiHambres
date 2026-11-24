package com.example.estudihambres.domain.model

/**
 * Oferta gastronómica publicada en una página web externa y obtenida mediante scraping.
 *
 * @property id Identificador de la oferta en el sitio de origen.
 * @property title Título comercial de la oferta.
 * @property merchant Nombre del comercio que la ofrece.
 * @property district Distrito o zona donde se consume (puede venir vacío).
 * @property priceSoles Precio de la oferta en soles; null si la página no lo publicó.
 * @property imageUrl Imagen de la oferta (https) o null.
 * @property url Enlace https a la oferta original para completar la compra en el sitio.
 * @property sourceName Sitio del que proviene la oferta (se muestra como atribución).
 */
data class WebOffer(
    val id: String,
    val title: String,
    val merchant: String,
    val district: String,
    val priceSoles: Double?,
    val imageUrl: String?,
    val url: String,
    val sourceName: String
)

/**
 * Categoría de ofertas disponible en el sitio de origen.
 *
 * @property label Texto mostrado en el chip de filtro.
 * @property path Ruta relativa de la página de categoría (permitida por robots.txt).
 */
data class WebOfferCategory(
    val label: String,
    val path: String
)
