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
            description = "Hamburguesa clásica a la parrilla + papas medianas + gaseosa. Segunda hamburguesa clásica gratis presentando tu carnet universitario en caja o web.",
            category = "Comida",
            discountTag = "2x1",
            partnerName = "Bembos",
            estimatedSavingsSoles = 19.90,
            isFeatured = true,
            url = "https://www.bembos.com.pe/promociones",
            distanceText = "150m · 2 min",
            originalPrice = "S/ 29.80",
            discountedPrice = "S/ 14.90"
        ),
        Promotion(
            id = "p-02",
            title = "Spotify Premium para Estudiantes",
            description = "Música sin anuncios, descargas offline y plan individual con 50% de descuento verificado por SheerID (S/ 10.45/mes) + 1 mes de prueba gratis.",
            category = "Herramientas digitales",
            discountTag = "50% DCTO",
            partnerName = "Spotify",
            estimatedSavingsSoles = 10.45,
            isFeatured = true,
            url = "https://www.spotify.com/pe-es/student/",
            distanceText = "Digital · Sin límites",
            originalPrice = "S/ 20.90",
            discountedPrice = "S/ 10.45/mes"
        ),
        Promotion(
            id = "p-03",
            title = "YouTube Premium Plan Estudiante",
            description = "YouTube y YouTube Music sin anuncios, en segundo plano y con descargas offline con descuento oficial verificado por SheerID.",
            category = "Herramientas digitales",
            discountTag = "40% DCTO",
            partnerName = "YouTube",
            estimatedSavingsSoles = 10.00,
            isFeatured = true,
            url = "https://www.youtube.com/premium/student",
            distanceText = "Digital · Todo el año",
            originalPrice = "S/ 23.90",
            discountedPrice = "S/ 13.90/mes"
        ),
        Promotion(
            id = "p-04",
            title = "Notion Education Plus Pack",
            description = "Plan Plus completo 100% gratis con tu correo institucional (.edu.pe): páginas ilimitadas, historial de 30 días, workspaces de IA y subida ilimitada de apuntes y tesis.",
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
            description = "Acceso gratis a GitHub Copilot, GitHub Pro, dominios gratuitos en Namecheap, créditos en AWS Educate/Azure y acceso a herramientas DevOps profesionales.",
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
            description = "Plan Figma Professional 100% gratis para prototipado UI/UX interactivo, sistemas de diseño colaborativo y pizarras FigJam para universitarios.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Figma",
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
            description = "2x1 en Frappuccinos seleccionados y café del día de lunes a jueves en tamaño Alto presentando carnet universitario activo.",
            category = "Comida",
            discountTag = "2x1 PROMO",
            partnerName = "Starbucks Coffee",
            estimatedSavingsSoles = 16.50,
            isFeatured = true,
            url = "https://www.starbucks.pe",
            distanceText = "200m · 3 min",
            originalPrice = "S/ 33.00",
            discountedPrice = "S/ 16.50"
        ),
        Promotion(
            id = "p-08",
            title = "Porciones Papa John's Pizza",
            description = "2x1 en pizzas medianas y familiares Pepperoni o Americana para almuerzos y grupos de estudio presentando carnet.",
            category = "Comida",
            discountTag = "2x1 SLICE",
            partnerName = "Papa John's",
            estimatedSavingsSoles = 28.00,
            isFeatured = false,
            url = "https://www.papajohns.com.pe",
            distanceText = "350m · 5 min",
            originalPrice = "S/ 56.00",
            discountedPrice = "S/ 28.00"
        ),
        Promotion(
            id = "p-09",
            title = "Medio Pasaje Universitario",
            description = "Tarifa reducida oficial del 50% con carnet universitario SUNEDU vigente en transporte público, Metropolitano (S/ 1.60), Línea 1 (S/ 0.75) y Corredores.",
            category = "Transporte",
            discountTag = "50% DTO",
            partnerName = "Transporte Urbano",
            estimatedSavingsSoles = 25.00,
            isFeatured = false,
            url = "https://portal.atu.gob.pe",
            distanceText = "En todas las rutas",
            originalPrice = "S/ 3.20",
            discountedPrice = "S/ 1.60"
        ),
        Promotion(
            id = "p-10",
            title = "Cineplanet 2x1 Universitario",
            description = "2x1 en entradas 2D regulares de lunes a viernes en boletería física y web acreditando carnet universitario o carnet SUNEDU activo.",
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
            description = "Precios educativos oficiales para estudiantes universitarios peruanos en MacBook Air, MacBook Pro, iPad Air y suscripción Apple Music a S/ 9.90/mes con Apple TV+ gratis.",
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
            description = "Licencia profesional gratuita completa a IntelliJ IDEA Ultimate, PyCharm Pro, WebStorm, CLion, DataGrip y PhpStorm con correo institucional .edu.pe.",
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
            description = "Word, Excel, PowerPoint, OneNote, Teams y 1TB de almacenamiento en la nube OneDrive 100% gratuito con correo institucional universitario.",
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
            description = "2 piezas de pollo crujiente + papas regulares + puré + ensalada regular y gaseosa personal por S/ 14.90 presentando carnet de estudiante en mostrador.",
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
            description = "Entradas 2D a tarifa reducida especial de lunes a jueves en todos los complejos Cinemark de Lima y provincias presentando carnet universitario.",
            category = "Entretenimiento",
            discountTag = "TARIFA EDU",
            partnerName = "Cinemark Perú",
            estimatedSavingsSoles = 10.00,
            isFeatured = false,
            url = "https://www.cinemark-peru.com",
            distanceText = "1.2 km · 15 min",
            originalPrice = "S/ 24.00",
            discountedPrice = "S/ 14.00"
        ),
        Promotion(
            id = "p-17",
            title = "Canva Pro para Estudiantes",
            description = "Suite de diseño gráfico profesional con herramientas premium de IA, eliminación de fondos y miles de plantillas para presentaciones y tesis gratis.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Canva Education",
            estimatedSavingsSoles = 120.00,
            isFeatured = true,
            url = "https://www.canva.com/es_419/educacion/",
            distanceText = "Digital · Sin límites",
            originalPrice = "$119/año",
            discountedPrice = "100% GRATIS"
        ),
        Promotion(
            id = "p-18",
            title = "Autodesk Education: AutoCAD & Revit",
            description = "Licencia educativa de 1 año renovable para AutoCAD, Revit, Civil 3D, Maya y 3ds Max para estudiantes de ingeniería y arquitectura acreditados.",
            category = "Herramientas digitales",
            discountTag = "100% GRATIS",
            partnerName = "Autodesk",
            estimatedSavingsSoles = 450.00,
            isFeatured = false,
            url = "https://www.autodesk.com/education/students",
            distanceText = "Licencia de 1 Año",
            originalPrice = "$1,865/año",
            discountedPrice = "100% GRATIS"
        ),
        Promotion(
            id = "p-19",
            title = "Pizza Hut: 2x1 en Pizzas Medianas",
            description = "2x1 en pizzas medianas tradicionales Pan Pizza (Americana o Pepperoni) para grupos de estudio universitarios de lunes a jueves.",
            category = "Comida",
            discountTag = "2x1 PIZZA",
            partnerName = "Pizza Hut",
            estimatedSavingsSoles = 36.90,
            isFeatured = false,
            url = "https://www.pizzahut.com.pe",
            distanceText = "300m · 4 min",
            originalPrice = "S/ 73.80",
            discountedPrice = "S/ 36.90"
        ),
        Promotion(
            id = "p-20",
            title = "Boleto Turístico y Museos: 50% DCTO",
            description = "Tarifa reducida del 50% en museos nacionales, monumentos arqueológicos y Boleto Turístico del Cusco (S/ 40 vs S/ 70 general) con carnet universitario vigente.",
            category = "Educación",
            discountTag = "50% OFICIAL",
            partnerName = "Ministerio de Cultura",
            estimatedSavingsSoles = 35.00,
            isFeatured = false,
            url = "https://tuboleto.cultura.pe",
            distanceText = "Cusco & Nacional",
            originalPrice = "S/ 70.00",
            discountedPrice = "S/ 35.00"
        )
    )

    override fun getPromotions(): Flow<List<Promotion>> {
        return flowOf(samplePromotions)
    }
}
