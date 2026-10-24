package com.example.estudihambres.core.legal

/**
 * Constantes legales, normativas y regulatorias de EstudiHambres conforme a la legislación peruana.
 *
 * Marco normativo aplicable:
 * 1. [Ley N° 29733] - Ley de Protección de Datos Personales (MINJUSDH / ANPPD).
 * 2. [Ley N° 29571] - Código de Protección y Defensa del Consumidor (INDECOPI).
 * 3. [Ley N° 29985] - Ley que regula las características básicas del Dinero Electrónico (SBS).
 * 4. [Ley N° 30096] - Ley de Delitos Informáticos del Perú.
 * 5. [Res. SUNAT N° 097-2012] - Normativa de Comprobantes de Pago Electrónicos (BVE).
 */
object LegalConstants {

    /**
     * Descargo de responsabilidad obligatorio auditado ante la SBS (Ley N° 29985).
     * Establece la naturaleza no fiduciaria de los StudiPuntos como programa cerrado de beneficios.
     */
    const val SBS_LEGAL_DISCLAIMER =
        "Los StudiPuntos son unidades de fidelización comercial de circuito cerrado sin valor monetario de curso legal. " +
        "No son convertibles a dinero en efectivo (Soles PEN, transferencias bancarias ni billeteras digitales Yape/Plin), " +
        "no son transferibles entre estudiantes y su uso es exclusivo para la red de beneficios universitarios acreditados, " +
        "conforme a las disposiciones de la Superintendencia de Banca, Seguros y AFP (SBS)."

    /**
     * Política de Privacidad y Tratamiento de Datos Personales (Ley N° 29733 y D.S. 003-2013-JUS).
     */
    const val DATA_PROTECTION_POLICY =
        "En cumplimiento de la Ley N° 29733 (Ley de Protección de Datos Personales del Perú), EstudiHambres garantiza " +
        "que el procesamiento de comprobantes de pago y carnets universitarios se realiza 100% de manera local en el " +
        "dispositivo mediante reconocimiento óptico seguro (Google ML Kit on-device). La aplicación NO almacena imágenes " +
        "fotográficas en servidores permanentes. Los datos fiscales se reducen a huellas criptográficas anonimizadas (SHA-256) " +
        "con ventana de conservación máxima de 72 horas para prevención de fraude."

    /**
     * Derechos ARCO (Acceso, Rectificación, Cancelación y Oposición).
     */
    const val ARCO_RIGHTS_INFO =
        "Como titular de tus datos personales, tienes derecho a solicitar en cualquier momento el Acceso, Rectificación, " +
        "Cancelación (Supresión) u Oposición de tus datos registrados, mediante la opción 'Eliminar Mi Cuenta y Datos' " +
        "en la sección de Perfil o escribiendo a: privacidad@estudihambres.pe."

    /**
     * Términos y Condiciones del Programa de Recompensas (INDECOPI - Ley N° 29571).
     */
    const val TERMS_AND_CONDITIONS_SUMMARY =
        "1. Los cupones y vales de la Tienda de StudiPuntos están sujetos a disponibilidad y stock de los comercios aliados.\n" +
        "2. Solo se admite el registro de Boletas de Venta Electrónicas (BVE) emitidas dentro de las últimas 72 horas y con un importe mínimo de S/ 5.00.\n" +
        "3. Cada comprobante es de uso único intransferible (First-Come First-Served). El intento de re-escanear boletas ajenas o falsificadas constituye fraude sancionable con la suspensión definitiva de la cuenta (Ley N° 30096).\n" +
        "4. Los puntos acumulados no vencen mientras la cuenta registre al menos un canje o verificación cada 12 meses."

    /**
     * Correo oficial del Delegado de Protección de Datos (DPO).
     */
    const val DPO_CONTACT_EMAIL = "privacidad@estudihambres.pe"
}
