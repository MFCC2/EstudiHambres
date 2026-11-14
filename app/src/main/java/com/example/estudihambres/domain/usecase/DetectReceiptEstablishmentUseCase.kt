package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.service.SunatCpeValidatorBot
import java.util.Locale

/**
 * Caso de uso inteligente para la detección automática del establecimiento comercial
 * directamente a partir del texto OCR del comprobante y/o del RUC emisor.
 *
 * Cumple con el requerimiento del usuario:
 * "Al escanear la boleta debería detectar el establecimiento... quita comercio aliado,
 * todos esos que pusiste, debería detectarse en la boleta".
 *
 * Elimina la necesidad de seleccionar comercios manualmente en menús desplegables o chips de prueba.
 */
class DetectReceiptEstablishmentUseCase {

    companion object {
        fun detectFromRuc(ruc: String): String? {
            return SunatCpeValidatorBot.TRUSTED_RUC_DIRECTORY[ruc.trim()]
        }

        fun detectFromOcrText(text: String): String {
            return DetectReceiptEstablishmentUseCase()(text, null)
        }

        // Directorio de marcas comerciales populares y cadenas
        private val BRAND_PATTERNS = listOf(
            Regex("""\bSTARBUCKS\b""", RegexOption.IGNORE_CASE) to "Starbucks",
            Regex("""\bBEMBOS\b""", RegexOption.IGNORE_CASE) to "Bembos",
            Regex("""\bKFC\b|\bKENTUCKY\b""", RegexOption.IGNORE_CASE) to "KFC",
            Regex("""\bBURGER\s*KING\b""", RegexOption.IGNORE_CASE) to "Burger King",
            Regex("""\bPIZZA\s*HUT\b""", RegexOption.IGNORE_CASE) to "Pizza Hut",
            Regex("""\bCINEPLANET\b|\bCINEPLEX\b""", RegexOption.IGNORE_CASE) to "Cineplanet",
            Regex("""\bCINEMARK\b""", RegexOption.IGNORE_CASE) to "Cinemark",
            Regex("""\bTAMBO\b""", RegexOption.IGNORE_CASE) to "Tiendas Tambo",
            Regex("""\bOXXO\b""", RegexOption.IGNORE_CASE) to "Tiendas Oxxo",
            Regex("""\bPLAZA\s*VEA\b""", RegexOption.IGNORE_CASE) to "Plaza Vea",
            Regex("""\bMETRO\b""", RegexOption.IGNORE_CASE) to "Supermercados Metro",
            Regex("""\bWONG\b""", RegexOption.IGNORE_CASE) to "Supermercados Wong",
            Regex("""\bTOTTUS\b""", RegexOption.IGNORE_CASE) to "Hipermercados Tottus",
            Regex("""\bVIVANDA\b""", RegexOption.IGNORE_CASE) to "Vivanda",
            Regex("""\bINKAFARMA\b""", RegexOption.IGNORE_CASE) to "Inkafarma",
            Regex("""\bMIFARMA\b""", RegexOption.IGNORE_CASE) to "Mifarma",
            Regex("""\bCRISOL\b""", RegexOption.IGNORE_CASE) to "Librerías Crisol",
            Regex("""\bLA\s*LUCHA\b""", RegexOption.IGNORE_CASE) to "La Lucha Sanguchería",
            Regex("""\bSIETE\s*SOPAS\b""", RegexOption.IGNORE_CASE) to "Siete Sopas",
            Regex("""\bPARDOS\s*CHICKEN\b|\bPARDOS\b""", RegexOption.IGNORE_CASE) to "Pardos Chicken",
            Regex("""\bROKY'?S\b""", RegexOption.IGNORE_CASE) to "Roki's",
            Regex("""\bNORKY'?S\b""", RegexOption.IGNORE_CASE) to "Norky's",
            Regex("""\bRUSTICA\b""", RegexOption.IGNORE_CASE) to "Rústica",
            Regex("""\bCHILI'?S\b""", RegexOption.IGNORE_CASE) to "Chili's",
            Regex("""\bTGI\s*FRIDAYS\b|\bFRIDAYS\b""", RegexOption.IGNORE_CASE) to "TGI Fridays",
            Regex("""\bMC\s*DONALD'?S\b|\bMCDONALD'?S\b""", RegexOption.IGNORE_CASE) to "McDonald's",
            Regex("""\bDUNKIN\b""", RegexOption.IGNORE_CASE) to "Dunkin' Donuts",
            Regex("""\bPAPA\s*JOHN'?S\b""", RegexOption.IGNORE_CASE) to "Papa John's",
            Regex("""\bCHINA\s*WOK\b""", RegexOption.IGNORE_CASE) to "China Wok",
            Regex("""\bSUBWAY\b""", RegexOption.IGNORE_CASE) to "Subway",
            Regex("""\bTAI\s*LOY\b|\bTAY\s*LOY\b""", RegexOption.IGNORE_CASE) to "Tai Loy",
            Regex("""\bCRUZ\s*DEL\s*SUR\b""", RegexOption.IGNORE_CASE) to "Cruz del Sur Transportes",
            Regex("""\bCIVA\b""", RegexOption.IGNORE_CASE) to "Turismo Civa",
            Regex("""\bOLVA\s*COURIER\b|\bOLVA\b""", RegexOption.IGNORE_CASE) to "Olva Courier"
        )

        // Líneas o palabras tributarias comunes que deben excluirse al buscar razón social
        private val EXCLUDED_HEADER_WORDS = setOf(
            "BOLETA", "VENTA", "FACTURA", "ELECTRONICA", "ELECTRÓNICA", "RUC", "R.U.C.",
            "TICKET", "COMPROBANTE", "PAGO", "SERIE", "NUMERO", "NRO", "CORRELATIVO",
            "FECHA", "HORA", "CAJA", "CAJERO", "TERMINAL", "DIRECCION", "DIRECCIÓN",
            "LIMA", "PERU", "PERÚ", "TELEFONO", "TELÉFONO", "SENORES", "SEÑORES", "CLIENTE"
        )
    }

    /**
     * Deduce y extrae el nombre comercial o razón social del establecimiento a partir
     * del texto escaneado en el ticket y/o su RUC.
     */
    operator fun invoke(ocrText: String?, ruc: String? = null): String {
        val cleanRuc = ruc?.trim().orEmpty()
        val text = ocrText?.trim().orEmpty()

        // 1. Detección por patrones de marcas conocidas en el texto del comprobante
        if (text.isNotBlank()) {
            for ((regex, brandName) in BRAND_PATTERNS) {
                if (regex.containsMatchIn(text)) {
                    return brandName
                }
            }
        }

        // 2. Extracción de encabezado de ticket (las primeras líneas donde suele figurar la Razón Social)
        if (text.isNotBlank()) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines.take(6)) {
                val upperLine = line.uppercase(Locale.ROOT)
                val words = upperLine.split(Regex("""[\s,:;#\-–/]+""")).filter { it.isNotBlank() }

                // Descartar líneas que consistan únicamente en términos fiscales o números
                val isOnlyTaxBoilerplate = words.all { EXCLUDED_HEADER_WORDS.contains(it) || it.all { c -> c.isDigit() } }
                if (!isOnlyTaxBoilerplate && words.isNotEmpty() && line.length in 4..60) {
                    // Si contiene indicativos de empresa peruana (S.A.C., S.A., E.I.R.L., S.R.L.)
                    if (upperLine.contains("S.A.C.") || upperLine.contains("S.A.") ||
                        upperLine.contains("E.I.R.L.") || upperLine.contains("S.R.L.") ||
                        upperLine.contains("COMERCIAL") || upperLine.contains("SERVICIOS") ||
                        upperLine.contains("RESTAURANTE") || upperLine.contains("TIENDA")
                    ) {
                        return cleanEstablishmentName(line)
                    }
                    // Si no es un patrón conocido pero parece un nombre comercial limpio
                    if (!upperLine.startsWith("RUC") && !upperLine.startsWith("R.U.C") && !upperLine.startsWith("NRO") && !upperLine.startsWith("BOLETA")) {
                        return cleanEstablishmentName(line)
                    }
                }
            }
        }

        // 3. Fallback a directorio oficial por RUC
        if (cleanRuc.isNotBlank()) {
            val fromDirectory = SunatCpeValidatorBot.TRUSTED_RUC_DIRECTORY[cleanRuc]
            if (fromDirectory != null) {
                return fromDirectory
            }
            return "Establecimiento RUC: $cleanRuc"
        }

        return "Comercio no identificado"
    }

    private fun cleanEstablishmentName(raw: String): String {
        return raw.replace(Regex("""^[^\p{L}\p{N}]+"""), "")
            .replace(Regex("""[^\p{L}\p{N}.)]+$"""), "")
            .take(45)
    }
}
