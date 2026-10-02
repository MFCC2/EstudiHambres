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
            title = "Combo Universitario 2x1",
            description = "Hamburguesa clásica + papas medianas + gaseosa. Segunda hamburguesa gratis mostrando tu carnet.",
            category = "Comida",
            discountTag = "2x1",
            partnerName = "Bembos",
            estimatedSavingsSoles = 19.90,
            isFeatured = true,
            url = "https://www.bembos.com.pe/promociones",
            distanceText = "150m · 2 min",
            originalPrice = "S/ 21.90",
            discountedPrice = "S/ 12.90"
        ),
        Promotion(
            id = "p-02",
            title = "Spotify Premium para Estudiantes",
            description = "Música sin anuncios, descargas offline y plan individual con 50% de descuento verificado.",
            category = "Herramientas digitales",
            discountTag = "50% DCTO",
            partnerName = "Spotify",
            estimatedSavingsSoles = 12.00,
            isFeatured = true,
            url = "https://www.spotify.com/pe-es/student/",
            distanceText = "Digital · Sin límites",
            originalPrice = "S/ 20.90",
            discountedPrice = "S/ 10.45/mes"
        ),
        Promotion(
            id = "p-03",
            title = "YouTube Premium Plan Estudiante",
            description = "YouTube y YouTube Music sin cortes comerciales en tu smartphone y laptop durante todo el semestre.",
            category = "Herramientas digitales",
            discountTag = "40% DCTO",
            partnerName = "YouTube",
            estimatedSavingsSoles = 14.50,
            isFeatured = true,
            url = "https://www.youtube.com/premium/student",
            distanceText = "Digital · Todo el año",
            originalPrice = "S/ 23.90",
            discountedPrice = "S/ 14.30/mes"
        ),
        Promotion(
            id = "p-04",
            title = "Notion Education Plus Pack",
            description = "Almacenamiento ilimitado, historial de versiones de 30 días y workspaces de IA gratis para apuntes y tesis.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Notion",
            estimatedSavingsSoles = 45.00,
            isFeatured = true,
            url = "https://www.notion.so/product/notion-for-education",
            distanceText = "Licencia Anual",
            originalPrice = "$120/año",
            discountedPrice = "GRATIS"
        ),
        Promotion(
            id = "p-05",
            title = "GitHub Student Developer Pack",
            description = "Acceso gratis a GitHub Copilot, dominios gratis de Namecheap, créditos en la nube y terminales Pro.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "GitHub & Copilot",
            estimatedSavingsSoles = 180.00,
            isFeatured = true,
            url = "https://education.github.com/pack",
            distanceText = "Licencia Estudiante",
            originalPrice = "$200+ USD",
            discountedPrice = "GRATIS"
        ),
        Promotion(
            id = "p-06",
            title = "Figma for Education",
            description = "Cuentas profesionales completas para prototipado interactivo de interfaces UI/UX y pizarras FigJam.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Figma Design",
            estimatedSavingsSoles = 60.00,
            isFeatured = true,
            url = "https://www.figma.com/education/",
            distanceText = "Nube · 2 Años",
            originalPrice = "$144/año",
            discountedPrice = "GRATIS"
        ),
        Promotion(
            id = "p-07",
            title = "2x1 en Frappuccinos Universitarios",
            description = "Válido de lunes a viernes en bebidas seleccionadas presentando carnet universitario activo.",
            category = "Comida",
            discountTag = "2x1 PROMO",
            partnerName = "Starbucks Coffee",
            estimatedSavingsSoles = 16.50,
            isFeatured = true,
            url = "https://www.starbucks.pe",
            distanceText = "200m · 3 min",
            originalPrice = "S/ 32.00",
            discountedPrice = "S/ 16.00"
        ),
        Promotion(
            id = "p-08",
            title = "Porciones Papa John's Pizza",
            description = "Porciones dobles familiares de Pepperoni o Suprema para almuerzos entre clases.",
            category = "Comida",
            discountTag = "2x1 SLICE",
            partnerName = "Papa John's",
            estimatedSavingsSoles = 9.00,
            isFeatured = false,
            url = "https://www.papajohns.com.pe",
            distanceText = "350m · 5 min",
            originalPrice = "S/ 18.00",
            discountedPrice = "S/ 9.90"
        ),
        Promotion(
            id = "p-09",
            title = "Medio Pasaje Universitario",
            description = "Tarifa oficial reducida del 50% con carnet universitario vigente en todas las troncales y rutas urbanas.",
            category = "Transporte",
            discountTag = "50% DTO",
            partnerName = "Transporte Urbano",
            estimatedSavingsSoles = 25.00,
            isFeatured = false,
            url = "https://www.gob.pe/atu",
            distanceText = "En todas las rutas",
            originalPrice = "S/ 2.50",
            discountedPrice = "S/ 1.20"
        )
    )

    override fun getPromotions(): Flow<List<Promotion>> {
        return flowOf(samplePromotions)
    }
}
