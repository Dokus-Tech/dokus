package tech.dokus.backend.services.auth

import tech.dokus.domain.enums.Language

private const val GuideUrlPlaceholder = "[GUIDE_URL]"

internal data class WelcomeEmailCopy(
    val workspaceActiveSuffix: String,
    val openingLineTemplate: String,
    val connectedParagraphs: List<String>,
    val notConnectedParagraphs: List<String>,
    val signatureRole: String
) {
    fun openingLine(userName: String, tenantName: String): String = openingLineTemplate
        .replace("{user}", userName)
        .replace("{tenant}", tenantName)
}

internal fun welcomeEmailCopy(language: Language): WelcomeEmailCopy = when (language) {
    Language.En -> WelcomeEmailCopy(
        workspaceActiveSuffix = "workspace active",
        openingLineTemplate = "{user}, the workspace for {tenant} is active.",
        connectedParagraphs = listOf(
            "PEPPOL is connected. Invoices and bills from the network are already being processed — amounts, VAT, contacts, due dates. No manual entry. When Dokus is certain, everything is handled. When something needs your judgment, it will wait for you.",
            "For receipts, contracts, or anything outside PEPPOL — share it from any app on your phone or upload it directly. A short guide with practical tips is at $GuideUrlPlaceholder.",
            "I read every reply to this address. If something is wrong, missing, or unclear — tell me directly."
        ),
        notConnectedParagraphs = listOf(
            "Share a document from any app on your phone, or upload it directly — an invoice, a bill, a receipt. Dokus reads it and handles amounts, VAT, contacts, and due dates. No manual entry. When it is certain, everything is processed. When something needs your judgment, it will wait for you.",
            "When you connect PEPPOL, documents will arrive from the network automatically. A short guide covering uploads, phone integration, and PEPPOL setup is at $GuideUrlPlaceholder.",
            "I read every reply to this address. If something is wrong, missing, or unclear — tell me directly."
        ),
        signatureRole = "Founder, Dokus"
    )

    Language.Fr -> WelcomeEmailCopy(
        workspaceActiveSuffix = "espace de travail actif",
        openingLineTemplate = "{user}, l'espace de travail de {tenant} est actif.",
        connectedParagraphs = listOf(
            "PEPPOL est connecté. Les factures et notes du réseau sont déjà traitées — montants, TVA, contacts, dates d'échéance. Aucune saisie manuelle. Quand Dokus est certain, tout est pris en charge. Quand quelque chose demande votre jugement, cela vous attend.",
            "Pour les reçus, contrats, ou tout ce qui est hors PEPPOL — partagez-le depuis n'importe quelle application sur votre téléphone ou importez-le directement. Un guide court avec des conseils pratiques est disponible sur $GuideUrlPlaceholder.",
            "Je lis chaque réponse à cette adresse. Si quelque chose est incorrect, manquant ou peu clair — dites-le-moi directement."
        ),
        notConnectedParagraphs = listOf(
            "Partagez un document depuis n'importe quelle application sur votre téléphone, ou importez-le directement — une facture, une note, un reçu. Dokus le lit et traite montants, TVA, contacts et dates d'échéance. Aucune saisie manuelle. Quand il est certain, tout est traité. Quand quelque chose demande votre jugement, cela vous attend.",
            "Lorsque vous connectez PEPPOL, les documents arriveront automatiquement depuis le réseau. Un guide court couvrant les imports, l'intégration mobile et la configuration PEPPOL est disponible sur $GuideUrlPlaceholder.",
            "Je lis chaque réponse à cette adresse. Si quelque chose est incorrect, manquant ou peu clair — dites-le-moi directement."
        ),
        signatureRole = "Fondateur, Dokus"
    )

    Language.Nl -> WelcomeEmailCopy(
        workspaceActiveSuffix = "werkruimte actief",
        openingLineTemplate = "{user}, de werkruimte voor {tenant} is actief.",
        connectedParagraphs = listOf(
            "PEPPOL is verbonden. Facturen en rekeningen van het netwerk worden al verwerkt — bedragen, btw, contacten, vervaldata. Geen handmatige invoer. Wanneer Dokus zeker is, wordt alles afgehandeld. Wanneer iets jouw oordeel nodig heeft, wacht het op jou.",
            "Voor bonnetjes, contracten of alles buiten PEPPOL — deel het vanuit eender welke app op je telefoon of upload het direct. Een korte gids met praktische tips staat op $GuideUrlPlaceholder.",
            "Ik lees elk antwoord op dit adres. Als iets fout, ontbrekend of onduidelijk is — laat het me direct weten."
        ),
        notConnectedParagraphs = listOf(
            "Deel een document vanuit eender welke app op je telefoon, of upload het direct — een factuur, een rekening, een bonnetje. Dokus leest het en verwerkt bedragen, btw, contacten en vervaldata. Geen handmatige invoer. Wanneer het zeker is, wordt alles verwerkt. Wanneer iets jouw oordeel nodig heeft, wacht het op jou.",
            "Wanneer je PEPPOL verbindt, komen documenten automatisch binnen vanuit het netwerk. Een korte gids over uploads, telefoonintegratie en PEPPOL-setup staat op $GuideUrlPlaceholder.",
            "Ik lees elk antwoord op dit adres. Als iets fout, ontbrekend of onduidelijk is — laat het me direct weten."
        ),
        signatureRole = "Oprichter, Dokus"
    )

    Language.De -> WelcomeEmailCopy(
        workspaceActiveSuffix = "Arbeitsbereich aktiv",
        openingLineTemplate = "{user}, der Arbeitsbereich für {tenant} ist aktiv.",
        connectedParagraphs = listOf(
            "PEPPOL ist verbunden. Rechnungen und Belege aus dem Netzwerk werden bereits verarbeitet — Beträge, MwSt., Kontakte, Fälligkeitsdaten. Keine manuelle Eingabe. Wenn Dokus sicher ist, wird alles erledigt. Wenn etwas Ihr Urteil braucht, wartet es auf Sie.",
            "Für Belege, Verträge oder alles außerhalb von PEPPOL — teilen Sie es aus jeder App auf Ihrem Telefon oder laden Sie es direkt hoch. Eine kurze Anleitung mit praktischen Tipps finden Sie unter $GuideUrlPlaceholder.",
            "Ich lese jede Antwort an diese Adresse. Wenn etwas falsch, fehlend oder unklar ist — sagen Sie es mir direkt."
        ),
        notConnectedParagraphs = listOf(
            "Teilen Sie ein Dokument aus jeder App auf Ihrem Telefon oder laden Sie es direkt hoch — eine Rechnung, einen Beleg, einen Kassenbon. Dokus liest es und verarbeitet Beträge, MwSt., Kontakte und Fälligkeitsdaten. Keine manuelle Eingabe. Wenn es sicher ist, wird alles verarbeitet. Wenn etwas Ihr Urteil braucht, wartet es auf Sie.",
            "Wenn Sie PEPPOL verbinden, kommen Dokumente automatisch aus dem Netzwerk. Eine kurze Anleitung zu Uploads, Telefonintegration und PEPPOL-Einrichtung finden Sie unter $GuideUrlPlaceholder.",
            "Ich lese jede Antwort an diese Adresse. Wenn etwas falsch, fehlend oder unklar ist — sagen Sie es mir direkt."
        ),
        signatureRole = "Gründer, Dokus"
    )

    Language.Es -> WelcomeEmailCopy(
        workspaceActiveSuffix = "espacio de trabajo activo",
        openingLineTemplate = "{user}, el espacio de trabajo de {tenant} está activo.",
        connectedParagraphs = listOf(
            "PEPPOL está conectado. Las facturas y documentos de la red ya se están procesando — importes, IVA, contactos y fechas de vencimiento. Sin entrada manual. Cuando Dokus tiene certeza, todo queda gestionado. Cuando algo requiere tu criterio, te estará esperando.",
            "Para recibos, contratos o cualquier cosa fuera de PEPPOL — compártelo desde cualquier aplicación de tu teléfono o súbelo directamente. Una guía breve con consejos prácticos está en $GuideUrlPlaceholder.",
            "Leo cada respuesta a esta dirección. Si algo está mal, falta o no está claro — dímelo directamente."
        ),
        notConnectedParagraphs = listOf(
            "Comparte un documento desde cualquier aplicación de tu teléfono o súbelo directamente — una factura, una cuenta o un recibo. Dokus lo lee y gestiona importes, IVA, contactos y fechas de vencimiento. Sin entrada manual. Cuando tiene certeza, todo se procesa. Cuando algo requiere tu criterio, te estará esperando.",
            "Cuando conectes PEPPOL, los documentos llegarán automáticamente desde la red. Una guía breve sobre cargas, integración móvil y configuración de PEPPOL está en $GuideUrlPlaceholder.",
            "Leo cada respuesta a esta dirección. Si algo está mal, falta o no está claro — dímelo directamente."
        ),
        signatureRole = "Fundador, Dokus"
    )

    Language.It -> WelcomeEmailCopy(
        workspaceActiveSuffix = "spazio di lavoro attivo",
        openingLineTemplate = "{user}, lo spazio di lavoro per {tenant} è attivo.",
        connectedParagraphs = listOf(
            "PEPPOL è connesso. Fatture e documenti dalla rete sono già in elaborazione — importi, IVA, contatti e scadenze. Nessun inserimento manuale. Quando Dokus è certo, gestisce tutto. Quando qualcosa richiede il tuo giudizio, resterà in attesa.",
            "Per ricevute, contratti o qualsiasi cosa fuori da PEPPOL — condividila da qualsiasi app sul tuo telefono oppure caricala direttamente. Una guida breve con consigli pratici è disponibile su $GuideUrlPlaceholder.",
            "Leggo ogni risposta a questo indirizzo. Se qualcosa è sbagliato, manca o non è chiaro — dimmelo direttamente."
        ),
        notConnectedParagraphs = listOf(
            "Condividi un documento da qualsiasi app sul tuo telefono, oppure caricalo direttamente — una fattura, una nota, una ricevuta. Dokus lo legge e gestisce importi, IVA, contatti e scadenze. Nessun inserimento manuale. Quando è certo, tutto viene elaborato. Quando qualcosa richiede il tuo giudizio, resterà in attesa.",
            "Quando colleghi PEPPOL, i documenti arriveranno automaticamente dalla rete. Una guida breve su caricamenti, integrazione mobile e configurazione PEPPOL è disponibile su $GuideUrlPlaceholder.",
            "Leggo ogni risposta a questo indirizzo. Se qualcosa è sbagliato, manca o non è chiaro — dimmelo direttamente."
        ),
        signatureRole = "Fondatore, Dokus"
    )
}

internal fun replaceGuidePlaceholder(text: String, guideUrl: String): String =
    text.replace(GuideUrlPlaceholder, guideUrl)

internal fun replaceGuidePlaceholderWithAnchor(
    text: String,
    anchorHtml: String
): String = text.replace(GuideUrlPlaceholder, anchorHtml)
