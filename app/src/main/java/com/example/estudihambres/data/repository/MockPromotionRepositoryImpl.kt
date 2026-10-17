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
        ),
        Promotion(
            id = "p-10",
            title = "Cineplanet 2x1 Universitario",
            description = "2x1 en entradas 2D regulares de lunes a viernes en boletería física y web mostrando carnet universitario o Sunedu vigente.",
            category = "Entretenimiento",
            discountTag = "2x1 ENTRADAS",
            partnerName = "Cineplanet",
            estimatedSavingsSoles = 18.00,
            isFeatured = true,
            url = "https://www.cineplanet.com.pe/promociones",
            distanceText = "300m · 4 min",
            originalPrice = "S/ 36.00",
            discountedPrice = "S/ 18.00"
        ),
        Promotion(
            id = "p-11",
            title = "Apple Education Store: Mac & iPad",
            description = "Precios educativos preferenciales para estudiantes universitarios en MacBook Air, MacBook Pro y iPad, con Apple Music a S/ 9.90.",
            category = "Herramientas digitales",
            discountTag = "DCTO EDU",
            partnerName = "Apple Education",
            estimatedSavingsSoles = 350.00,
            isFeatured = true,
            url = "https://www.apple.com/pe/shop/education-pricing",
            distanceText = "Tienda Oficial Online",
            originalPrice = "S/ 4,299.00",
            discountedPrice = "S/ 3,949.00"
        ),
        Promotion(
            id = "p-12",
            title = "JetBrains All Products Student Pack",
            description = "Suscripción gratuita completa para estudiantes universitarios a IntelliJ IDEA Ultimate, PyCharm Pro, WebStorm, CLion y DataGrip con correo .edu.pe.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "JetBrains",
            estimatedSavingsSoles = 249.00,
            isFeatured = true,
            url = "https://www.jetbrains.com/community/education/#students",
            distanceText = "Licencia Anual Renovable",
            originalPrice = "$289/año",
            discountedPrice = "GRATIS"
        ),
        Promotion(
            id = "p-13",
            title = "Microsoft 365 Educación & 1TB OneDrive",
            description = "Word, Excel, PowerPoint, OneNote, Teams y 1TB de almacenamiento en la nube OneDrive gratis con tu correo institucional universitario.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Microsoft Education",
            estimatedSavingsSoles = 70.00,
            isFeatured = false,
            url = "https://www.microsoft.com/es-pe/education/products/office",
            distanceText = "Nube Institucional",
            originalPrice = "S/ 269/año",
            discountedPrice = "GRATIS"
        ),
        Promotion(
            id = "p-14",
            title = "Mega Box Estudiantil KFC",
            description = "2 piezas de pollo crujiente + papas medianas + puré + ensalada regular y gaseosa presentando carnet de estudiante en mostrador.",
            category = "Comida",
            discountTag = "PROMO S/ 14.90",
            partnerName = "KFC Perú",
            estimatedSavingsSoles = 8.50,
            isFeatured = false,
            url = "https://www.kfc.com.pe/promociones",
            distanceText = "250m · 3 min",
            originalPrice = "S/ 23.40",
            discountedPrice = "S/ 14.90"
        ),
        Promotion(
            id = "p-15",
            title = "Librerías SBS: 15% DCTO en Textos Académicos",
            description = "15% de descuento en libros de ingeniería, medicina, derecho, humanidades y diccionarios presentando carnet universitario activo.",
            category = "Educación",
            discountTag = "15% DCTO",
            partnerName = "Librerías SBS",
            estimatedSavingsSoles = 18.00,
            isFeatured = false,
            url = "https://www.sbs.com.pe",
            distanceText = "500m · 7 min",
            originalPrice = "S/ 120.00",
            discountedPrice = "S/ 102.00"
        ),
        Promotion(
            id = "p-16",
            title = "Tarifa Universitaria Cinemark",
            description = "Entrada 2D general a tarifa reducida de lunes a jueves en todos los cines Cinemark de Lima y provincias presentando carnet.",
            category = "Entretenimiento",
            discountTag = "TARIFA EDU",
            partnerName = "Cinemark Perú",
            estimatedSavingsSoles = 10.00,
            isFeatured = false,
            url = "https://www.cinemark-peru.com",
            distanceText = "1.2 km · 15 min",
            originalPrice = "S/ 24.00",
            discountedPrice = "S/ 14.00"
        )
    )

    override fun getPromotions(): Flow<List<Promotion>> {
        return flowOf(samplePromotions)
    }
}
