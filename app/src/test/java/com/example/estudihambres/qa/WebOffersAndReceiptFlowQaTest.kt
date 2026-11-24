package com.example.estudihambres.qa

import com.example.estudihambres.data.remote.scraper.CuponidadOfferParser
import com.example.estudihambres.data.repository.CuponidadWebOffersRepository
import com.example.estudihambres.domain.model.ReceiptSource
import com.example.estudihambres.domain.model.WebOffer
import com.example.estudihambres.domain.service.SunatCheckStatus
import com.example.estudihambres.domain.service.SunatResultClassifier
import com.example.estudihambres.domain.usecase.ReceiptScanParser
import com.example.estudihambres.presentation.offers.OfferSort
import com.example.estudihambres.presentation.offers.OffersUiState
import com.example.estudihambres.presentation.offers.OffersViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.UnknownHostException

/**
 * Ofertas web por scraping (Cuponidad.pe) y flujo de boletas (parser de cámara + clasificador SUNAT).
 */
class WebOffersAndReceiptFlowQaTest {

    // ------------------------------------------------------------------
    // Scraping de ofertas
    // ------------------------------------------------------------------

    private val listHtml = """
        <html><body><ul>
          <li class="cd2-loccard" data-offer-id="101">
            <a href="/pollo-a-la-brasa-para-2-101" title="Pollo a la brasa para 2">
              <img class="cd2-loccard__img" src="https://cdn.cuponidad.pe/images/Deals/350x210/pollo.webp">
              <span class="cd2-loccard__body">
                <span class="cd2-loccard__merchant">la granja   del pollo</span>
                <span class="cd2-loccard__title">Pollo a la brasa para 2 + papas</span>
                <span class="cd2-loccard__place">San Miguel<span class="cd2-loccard__dist"></span></span>
                <span class="cd2-loccard__price">S/ 39.90</span>
              </span>
            </a>
          </li>
          <li class="cd2-loccard" data-offer-id="102">
            <a href="/salchipapa-clasica-102"><span class="cd2-loccard__title">Salchipapa clásica</span>
              <span class="cd2-loccard__price">S/ 1,250.00</span></a>
          </li>
          <li class="cd2-loccard" data-offer-id="103">
            <a href="https://sitio-malicioso.example/robo"><span class="cd2-loccard__title">Enlace externo</span></a>
          </li>
          <li class="cd2-loccard" data-offer-id="101">
            <a href="/pollo-a-la-brasa-para-2-101"><span class="cd2-loccard__title">Duplicado</span></a>
          </li>
        </ul></body></html>
    """.trimIndent()

    @Test
    fun parser_readsListCards_withAbsoluteUrlsPriceAndDistrict() {
        val offers = CuponidadOfferParser.parse(listHtml, "https://cuponidad.pe/restaurantes-5")

        assertThat(offers.map { it.id }).containsExactly("101", "102").inOrder()
        val pollo = offers.first()
        assertThat(pollo.title).isEqualTo("Pollo a la brasa para 2 + papas")
        assertThat(pollo.merchant).isEqualTo("La granja del pollo")
        assertThat(pollo.district).isEqualTo("San Miguel")
        assertThat(pollo.priceSoles).isWithin(0.001).of(39.90)
        assertThat(pollo.url).isEqualTo("https://cuponidad.pe/pollo-a-la-brasa-para-2-101")
        assertThat(pollo.imageUrl).startsWith("https://cdn.cuponidad.pe/")
        assertThat(offers[1].priceSoles).isWithin(0.001).of(1250.0)
    }

    @Test
    fun parser_rejectsLinksOutsideTheSourceDomain() {
        val offers = CuponidadOfferParser.parse(listHtml, "https://cuponidad.pe/restaurantes-5")
        assertThat(offers.none { it.url.contains("sitio-malicioso") }).isTrue()
    }

    @Test
    fun parser_fallsBackToThumbnailCards() {
        val html = """
            <div class="container__thumbnail district__1" data-offer-id="7">
              <a href="/menu-del-dia-7" title="Menú del día">
                <img src="https://cdn.cuponidad.pe/images/Deals/menu.webp">
                <h3 class="thumbnail__provider">comedor central</h3>
                <h2 class="thumbnail__title">Menú del día con refresco</h2>
                <h3 class="thumbnail__place"> lince </h3>
                <h5 class="thumbnail__price"><div class="cd2-offer__amount">S/ 12.00</div></h5>
              </a>
            </div>
        """.trimIndent()
        val offers = CuponidadOfferParser.parse(html, "https://cuponidad.pe/restaurantes-5")
        assertThat(offers).hasSize(1)
        assertThat(offers[0].merchant).isEqualTo("Comedor central")
        assertThat(offers[0].district).isEqualTo("Lince")
        assertThat(offers[0].priceSoles).isWithin(0.001).of(12.0)
    }

    @Test
    fun parser_fallsBackToJsonLdItemList() {
        val html = """
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[{"@type":"ItemList","itemListElement":[
              {"@type":"ListItem","position":1,"url":"https://cuponidad.pe/ceviche-para-2-55","name":"Ceviche para 2"}
            ]}]}
            </script>
        """.trimIndent()
        val offers = CuponidadOfferParser.parse(html)
        assertThat(offers).hasSize(1)
        assertThat(offers[0].title).isEqualTo("Ceviche para 2")
        assertThat(offers[0].id).isEqualTo("55")
        assertThat(offers[0].priceSoles).isNull()
    }

    @Test
    fun parser_priceParsing() {
        assertThat(CuponidadOfferParser.parsePrice("S/ 9.90")).isWithin(0.001).of(9.9)
        assertThat(CuponidadOfferParser.parsePrice("S/ 2,349.00")).isWithin(0.001).of(2349.0)
        assertThat(CuponidadOfferParser.parsePrice("Gratis")).isNull()
        assertThat(CuponidadOfferParser.parsePrice(null)).isNull()
    }

    @Test
    fun repository_cachesPerCategory_andForceRefreshDownloadsAgain() = runTest {
        var downloads = 0
        val repo = CuponidadWebOffersRepository(fetchHtml = { downloads++; listHtml }, clock = { 1_000L })
        val category = repo.categories.first()

        assertThat(repo.getOffers(category).getOrThrow()).hasSize(2)
        repo.getOffers(category)
        assertThat(downloads).isEqualTo(1)

        repo.getOffers(category, forceRefresh = true)
        assertThat(downloads).isEqualTo(2)
    }

    @Test
    fun repository_cacheExpires() = runTest {
        var now = 0L
        var downloads = 0
        val repo = CuponidadWebOffersRepository(fetchHtml = { downloads++; listHtml }, clock = { now })
        val category = repo.categories.first()
        repo.getOffers(category)
        now += CuponidadWebOffersRepository.CACHE_TTL_MILLIS + 1
        repo.getOffers(category)
        assertThat(downloads).isEqualTo(2)
    }

    @Test
    fun repository_offlineReturnsReadableError() = runTest {
        val repo = CuponidadWebOffersRepository(fetchHtml = { throw UnknownHostException("cuponidad.pe") })
        val result = repo.getOffers(repo.categories.first())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Sin conexión")
    }

    @Test
    fun repository_onlyUsesPathsAllowedByRobots() {
        val repo = CuponidadWebOffersRepository(fetchHtml = { "" })
        val blocked = listOf("/buscar", "/cart", "/myaccount", "/coupons", "/payment")
        assertThat(repo.categories.none { c -> blocked.any { c.path.startsWith(it) } }).isTrue()
    }

    @Test
    fun offersFilters_searchBudgetAndSort() {
        fun offer(id: String, title: String, price: Double?) =
            WebOffer(id, title, "Local $id", "Lince", price, null, "https://cuponidad.pe/x-$id", "Cuponidad.pe")
        val base = OffersUiState(offers = listOf(offer("1", "Pollo broaster", 25.0), offer("2", "Pizza familiar", 45.0), offer("3", "Pollo a la brasa", 18.0)))

        val searched = OffersViewModel.applyFilters(base.copy(query = "pollo"))
        assertThat(searched.visibleOffers.map { it.id }).containsExactly("1", "3").inOrder()

        val budget = OffersViewModel.applyFilters(base.copy(onlyBudget = true))
        assertThat(budget.visibleOffers.map { it.id }).containsExactly("1", "3")

        val sorted = OffersViewModel.applyFilters(base.copy(sort = OfferSort.PRICE_ASC))
        assertThat(sorted.visibleOffers.map { it.id }).containsExactly("3", "1", "2").inOrder()
    }

    // ------------------------------------------------------------------
    // Lectura de comprobantes por cámara
    // ------------------------------------------------------------------

    @Test
    fun scanParser_readsSunatQrWithDate() {
        val scan = ReceiptScanParser.parseQr("20100107214|03|B001|00049281|2.83|18.50|2026-11-23|1|72945602|")
        assertThat(scan).isNotNull()
        scan!!
        assertThat(scan.source).isEqualTo(ReceiptSource.QR)
        assertThat(scan.serie).isEqualTo("B001")
        assertThat(scan.numero).isEqualTo("49281")
        assertThat(scan.amount).isWithin(0.001).of(18.50)
        assertThat(scan.issueDateMillis).isEqualTo(ReceiptScanParser.parseDate("23/11/2026"))
    }

    @Test
    fun scanParser_rejectsNonReceiptQr() {
        assertThat(ReceiptScanParser.parseQr("https://example.com")).isNull()
        assertThat(ReceiptScanParser.parseQr("20100107214|07|G001|123|0|10.00|2026-11-23")).isNull()
    }

    @Test
    fun scanParser_readsPrintedTicket() {
        val text = """
            DELOSI S.A.
            RUC: 20100107214
            TELF: 9841234567
            BOLETA DE VENTA ELECTRONICA
            B001-00012345
            FECHA: 23/11/2026 13:40
            SUBTOTAL 15.68
            TOTAL S/ 18.50
        """.trimIndent()
        val scan = ReceiptScanParser.parseOcr(text)
        assertThat(scan).isNotNull()
        scan!!
        assertThat(scan.ruc).isEqualTo("20100107214")
        assertThat(scan.serie).isEqualTo("B001")
        assertThat(scan.numero).isEqualTo("12345")
        assertThat(scan.amount).isWithin(0.001).of(18.50)
        assertThat(scan.issueDateMillis).isNotNull()
    }

    @Test
    fun scanParser_ignoresFourLetterWordsAsSeries() {
        val text = "RUC 20100107214\nFONO: 1234567\nNOTA DE VENTA"
        assertThat(ReceiptScanParser.parseOcr(text)).isNull()
    }

    @Test
    fun scanParser_strictDates() {
        assertThat(ReceiptScanParser.parseDate("32/13/2026")).isNull()
        assertThat(ReceiptScanParser.parseDate("")).isNull()
        assertThat(ReceiptScanParser.parseDate("2026-11-23")).isNotNull()
    }

    // ------------------------------------------------------------------
    // Interpretación de la respuesta de SUNAT
    // ------------------------------------------------------------------

    @Test
    fun sunatClassifier_validSentence() {
        val page = "Resultado de la Consulta\nLa Boleta de Venta Electrónica B001-49281 es un comprobante de pago válido."
        assertThat(SunatResultClassifier.classify(page, "B001", "00049281")).isEqualTo(SunatCheckStatus.VALID)
    }

    @Test
    fun sunatClassifier_negativeSentenceWinsOverPositivePhrase() {
        val page = "La Boleta B001-49281 no es un comprobante de pago válido."
        assertThat(SunatResultClassifier.classify(page, "B001", "49281")).isEqualTo(SunatCheckStatus.NOT_VALID)
    }

    @Test
    fun sunatClassifier_realNotFoundResponse() {
        // Texto real devuelto por ConsValiCpe.htm para un comprobante inexistente
        val page = "Consulta Validez del Comprobante de Pago Electrónico Resultado de la Consulta " +
            "La Boleta de Venta Electrónica B001-00009999 no existe en los registros de SUNAT. Imprimir"
        assertThat(SunatResultClassifier.classify(page, "B001", "9999")).isEqualTo(SunatCheckStatus.NOT_VALID)
    }

    @Test
    fun sunatClassifier_cancelledReceipt() {
        val page = "La Factura Electrónica F001-77 ha sido comunicada de baja."
        assertThat(SunatResultClassifier.classify(page, "F001", "77")).isEqualTo(SunatCheckStatus.CANCELLED)
    }

    @Test
    fun sunatClassifier_formPageOrOtherReceiptIsNotAResult() {
        val form = "Consulta de Validez del Comprobante de Pago Electrónico. Ingrese los siguientes datos."
        assertThat(SunatResultClassifier.classify(form, "B001", "1")).isNull()
        val otherReceipt = "La Boleta B002-555 es un comprobante de pago válido."
        assertThat(SunatResultClassifier.classify(otherReceipt, "B001", "555")).isNull()
    }
}
