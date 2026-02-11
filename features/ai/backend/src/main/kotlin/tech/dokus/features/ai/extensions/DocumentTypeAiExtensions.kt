package tech.dokus.features.ai.extensions

import tech.dokus.domain.enums.DocumentType

val DocumentType.description: String
    get() = when (this) {

        // ═══════════════════════════════════════════════════════════════════
        // SALES (money coming in)
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Invoice ->
            """
            SALES INVOICE — A document issued BY this business TO a customer requesting payment.
            
            KEYWORDS (look for these exact terms):
            - NL: "Factuur", "Factuurnummer", "Factuur nr", "BTW-nummer", "Te betalen", "Totaal te betalen", "Vervaldatum", "Betalingstermijn"
            - FR: "Facture", "Numéro de facture", "N° TVA", "Total à payer", "Date d'échéance", "Échéance"
            - DE: "Rechnung", "Rechnungsnummer", "MwSt-Nummer", "Zahlbar bis", "Gesamtbetrag"
            - EN: "Invoice", "Invoice number", "VAT number", "Amount due", "Due date", "Payment terms"
            
            VISUAL STRUCTURE:
            - Header with seller's company name, logo, address, VAT number
            - "FACTUUR" / "FACTURE" / "INVOICE" prominently displayed
            - Customer (buyer) details section
            - Table with columns: description, quantity, unit price, VAT %, line total
            - VAT breakdown section (6%, 12%, 21% rates in Belgium)
            - Total excl. VAT, VAT amount, Total incl. VAT
            - Payment details: bank account (IBAN), structured communication (+++xxx/xxxx/xxxxx+++)
            - Invoice number and date clearly visible
            
            DISTINGUISHING FEATURES:
            - The SELLER's VAT number and bank account are shown (not the buyer's)
            - Contains structured Belgian payment reference (OGM/VCS): +++xxx/xxxx/xxxxx+++
            - Requests future payment (not proof of payment already made)
            - Has line items with quantities and prices
            
            COMMON ISSUERS: Any business selling goods/services
            
            NOT THIS TYPE IF:
            - Document shows "Creditnota" / "Note de crédit" → CREDIT_NOTE
            - Document shows "Offerte" / "Devis" → QUOTE
            - Document shows "Bestelbon" / "Bon de commande" → ORDER_CONFIRMATION
            - It's a ticket/receipt from a shop → RECEIPT
            - You RECEIVED this from a supplier for goods YOU bought → INVOICE
            """.trimIndent()

        DocumentType.CreditNote ->
            """
            CREDIT NOTE — A document that reduces or cancels a previous invoice amount.
            
            KEYWORDS (look for these exact terms):
            - NL: "Creditnota", "Creditfactuur", "Tegoedbon", "Credit nota nr", "Terugbetaling", "Correctie op factuur", "Ref. factuur"
            - FR: "Note de crédit", "Avoir", "Facture d'avoir", "Remboursement", "Correction facture"
            - DE: "Gutschrift", "Stornorechnung", "Rechnungskorrektur"
            - EN: "Credit note", "Credit memo", "Refund", "Adjustment"
            
            VISUAL STRUCTURE:
            - Similar layout to invoice BUT clearly marked as credit/correction
            - "CREDITNOTA" / "NOTE DE CRÉDIT" prominently displayed (not "Factuur")
            - Reference to original invoice number
            - Amounts may be negative or marked as credit
            - Reason for credit often stated (return, discount, error correction)
            
            DISTINGUISHING FEATURES:
            - Explicitly references an original invoice number
            - Amounts reduce what was owed, not add to it
            - Often shows reason: "retour", "korting", "correctie", "annulatie"
            - May have negative amounts or "credit" terminology
            
            DIRECTION: Can be issued BY the business (sales credit) or received FROM supplier (purchase credit)
            
            NOT THIS TYPE IF:
            - No reference to previous invoice → probably INVOICE
            - It's a discount offer for future purchase → QUOTE
            - It's a refund receipt from a store → RECEIPT
            
            ⚠️ NOT A CREDIT NOTE:
            - "REGULARISATIEBERICHT" from social funds → SELF_EMPLOYED_CONTRIBUTION (recalculation, not credit)
            - Adjustment notices that result in MORE to pay → original document type
            - Bank statements showing credits → BANK_STATEMENT
            """.trimIndent()

        DocumentType.ProForma ->
            """
            PRO FORMA INVOICE — A preliminary/formal quote before actual delivery, often for international trade.
            
            KEYWORDS (look for these exact terms):
            - NL: "Pro forma factuur", "Pro-forma", "Proforma factuur", "Voorlopige factuur"
            - FR: "Facture pro forma", "Pro-forma", "Facture préliminaire"
            - DE: "Proforma-Rechnung", "Vorausrechnung"
            - EN: "Pro forma invoice", "Proforma", "Preliminary invoice"
            
            VISUAL STRUCTURE:
            - Looks like an invoice BUT marked "PRO FORMA" or "PROFORMA"
            - Full details of goods/services with prices
            - May include shipping terms (Incoterms), customs info
            - Often used for customs valuation or bank transfers
            
            DISTINGUISHING FEATURES:
            - Clearly states "Pro Forma" — not just "Factuur"
            - Often for international/export transactions
            - Not a legal request for payment yet
            - May mention customs, import/export, bank transfer requirements
            
            NOT THIS TYPE IF:
            - Just says "Factuur" without "Pro Forma" → INVOICE
            - Says "Offerte" / "Devis" → QUOTE
            - Informal price estimate → QUOTE
            """.trimIndent()

        DocumentType.Quote ->
            """
            QUOTE / ESTIMATE — An informal price offer for goods or services, not binding.
            
            KEYWORDS (look for these exact terms):
            - NL: "Offerte", "Prijsopgave", "Kostenraming", "Voorstel", "Aanbieding", "Geldig tot", "Vrijblijvend"
            - FR: "Devis", "Offre de prix", "Estimation", "Proposition", "Valable jusqu'au"
            - DE: "Angebot", "Kostenvoranschlag", "Preisangebot", "Gültig bis"
            - EN: "Quote", "Quotation", "Estimate", "Proposal", "Valid until"
            
            VISUAL STRUCTURE:
            - Header with company details
            - "OFFERTE" / "DEVIS" / "QUOTE" as title
            - Description of proposed goods/services
            - Prices (may be indicative)
            - Validity period ("geldig tot", "valable jusqu'au")
            - Terms and conditions
            - NO payment request, NO bank account for payment
            
            DISTINGUISHING FEATURES:
            - States validity period (not due date for payment)
            - Words like "vrijblijvend" (non-binding), "onder voorbehoud"
            - No structured payment reference (OGM)
            - No "te betalen" / "à payer" / "amount due"
            
            NOT THIS TYPE IF:
            - Has "Te betalen binnen X dagen" → INVOICE
            - Has bank account + payment reference → INVOICE
            - Says "Pro Forma" → PRO_FORMA
            - Confirms an order was placed → ORDER_CONFIRMATION
            """.trimIndent()

        DocumentType.OrderConfirmation ->
            """
            ORDER CONFIRMATION — Confirms that a customer's order has been accepted.
            
            KEYWORDS (look for these exact terms):
            - NL: "Orderbevestiging", "Bestelbevestiging", "Bevestiging van uw bestelling", "Order nr", "Bestelling", "Uw order"
            - FR: "Confirmation de commande", "Accusé de réception", "Votre commande", "N° de commande"
            - DE: "Auftragsbestätigung", "Bestellbestätigung", "Ihre Bestellung"
            - EN: "Order confirmation", "Your order", "Order number", "Order received"
            
            VISUAL STRUCTURE:
            - Lists ordered items with quantities and prices
            - Order number and date
            - Expected delivery date/time
            - Customer shipping address
            - May show order status
            - Often from e-commerce (webshops)
            
            DISTINGUISHING FEATURES:
            - Confirms what was ORDERED, not what to PAY now
            - Shows expected delivery, not payment due date
            - Often has order tracking info
            - May say "Factuur volgt" (invoice follows)
            
            NOT THIS TYPE IF:
            - Requests payment with bank details → INVOICE
            - Accompanies physical goods delivery → DELIVERY_NOTE
            - Is the order YOU are placing to a supplier → PURCHASE_ORDER
            """.trimIndent()

        DocumentType.DeliveryNote ->
            """
            DELIVERY NOTE — Accompanies shipped goods, proving what was delivered.
            
            KEYWORDS (look for these exact terms):
            - NL: "Leveringsbon", "Pakbon", "Vrachtbrief", "Afleverbon", "Leveringsnota", "Ontvangen door", "Handtekening"
            - FR: "Bon de livraison", "Bordereau de livraison", "Note de livraison", "Reçu par", "Signature"
            - DE: "Lieferschein", "Versandschein", "Frachtbrief"
            - EN: "Delivery note", "Packing slip", "Dispatch note", "Received by"
            
            VISUAL STRUCTURE:
            - List of items delivered with quantities
            - Delivery date and address
            - Space for recipient signature
            - Reference to order number
            - NO prices (or prices hidden/not prominent)
            - Carrier/transport info may be included
            
            DISTINGUISHING FEATURES:
            - Focuses on WHAT was delivered, not WHAT to pay
            - Often has signature line for recipient
            - Usually no VAT breakdown or payment details
            - May have "Aantal colli" (number of packages)
            
            NOT THIS TYPE IF:
            - Shows prices and requests payment → INVOICE
            - Is a transport company's invoice → INVOICE
            - Is from a postal service with tracking → different document
            """.trimIndent()

        DocumentType.Reminder ->
            """
            PAYMENT REMINDER / DUNNING — Notice for overdue unpaid invoices.
            
            KEYWORDS (look for these exact terms):
            - NL: "Herinnering", "Aanmaning", "Betalingsherinnering", "Openstaand saldo", "Vervallen factuur", "Eerste/Tweede/Laatste aanmaning", "Ingebrekestelling"
            - FR: "Rappel", "Rappel de paiement", "Mise en demeure", "Facture impayée", "Solde dû", "Relance"
            - DE: "Mahnung", "Zahlungserinnerung", "Zahlungsaufforderung", "Überfällig"
            - EN: "Reminder", "Payment reminder", "Overdue notice", "Outstanding balance", "Past due"
            
            VISUAL STRUCTURE:
            - References one or more unpaid invoices with numbers and dates
            - Shows outstanding amount
            - May list multiple overdue invoices
            - Urgency indicators (first reminder, final notice)
            - May mention interest, collection fees, legal action
            
            DISTINGUISHING FEATURES:
            - References PREVIOUS invoices that are unpaid
            - Uses urgency language: "vervallen", "onbetaald", "laatste aanmaning"
            - May threaten consequences (interest, incasso, rechtbank)
            - Often has aging info (30 days, 60 days overdue)
            
            NOT THIS TYPE IF:
            - It's the original invoice (no "reminder" language) → INVOICE
            - It's a statement showing all activity → STATEMENT_OF_ACCOUNT
            """.trimIndent()

        DocumentType.StatementOfAccount ->
            """
            STATEMENT OF ACCOUNT — Overview of all transactions with a customer/supplier.
            
            KEYWORDS (look for these exact terms):
            - NL: "Rekeningoverzicht", "Klantenrekening", "Openstaande posten", "Saldo", "Overzicht facturen", "Ouderdomsanalyse"
            - FR: "Relevé de compte", "État de compte", "Balance client", "Solde", "Récapitulatif"
            - DE: "Kontoauszug", "Kontouebersicht", "Saldenliste", "Offene Posten"
            - EN: "Statement of account", "Account statement", "Aging report", "Balance due"
            
            VISUAL STRUCTURE:
            - List of multiple invoices, payments, credit notes
            - Running balance or total outstanding
            - Date range (period covered)
            - Aging buckets (current, 30 days, 60 days, 90+ days)
            - Summary totals
            
            DISTINGUISHING FEATURES:
            - Shows HISTORY of transactions, not single invoice
            - Multiple document references in one overview
            - Often shows what was paid AND what's still open
            - May be sent periodically (monthly, quarterly)
            
            NOT THIS TYPE IF:
            - Single invoice only → INVOICE
            - Specifically about overdue amounts → REMINDER
            - Bank account transactions → BANK_STATEMENT
            """.trimIndent()

        DocumentType.Receipt ->
            """
            RECEIPT / TICKET — Proof of payment already made, typically small retail purchases.
            
            KEYWORDS (look for these exact terms):
            - NL: "Kassabon", "Kasticket", "Ticket", "Betaald", "Ontvangen", "Contant", "Bancontact", "Bedankt voor uw aankoop"
            - FR: "Ticket de caisse", "Reçu", "Payé", "Merci de votre visite", "Montant payé"
            - DE: "Kassenbon", "Quittung", "Beleg", "Bezahlt", "Danke für Ihren Einkauf"
            - EN: "Receipt", "Till receipt", "Paid", "Thank you for your purchase", "Amount paid"
            
            VISUAL STRUCTURE:
            - Narrow format (thermal printer width)
            - Store name and address at top
            - List of items purchased
            - VAT number of store
            - Payment method (cash, card, Bancontact)
            - Date and time of purchase
            - Total amount PAID (not due)
            - May have transaction/receipt number
            
            DISTINGUISHING FEATURES:
            - Shows payment was COMPLETED (not requested)
            - Usually thermal paper, narrow format
            - Time of transaction shown
            - Payment method indicated
            - "Betaald" / "Payé" / "Paid" terminology
            
            COMMON SOURCES:
            - Supermarkets (Colruyt, Delhaize, Carrefour, Aldi, Lidl)
            - Gas stations, restaurants, parking
            - Retail stores, electronics, office supplies
            
            NOT THIS TYPE IF:
            - Requests future payment → INVOICE
            - Formal A4 document with company letterhead → INVOICE
            - Employee expense form → EXPENSE_CLAIM
            """.trimIndent()

        DocumentType.PurchaseOrder ->
            """
            PURCHASE ORDER — An order placed BY your business TO a supplier.
            
            KEYWORDS (look for these exact terms):
            - NL: "Bestelbon", "Bestelling", "Inkooporder", "PO nummer", "Aankooporder", "Wij bestellen hierbij"
            - FR: "Bon de commande", "Commande", "N° de commande", "Ordre d'achat", "Nous commandons"
            - DE: "Bestellung", "Bestellnummer", "Einkaufsauftrag"
            - EN: "Purchase order", "PO", "Order number", "We hereby order"
            
            VISUAL STRUCTURE:
            - YOUR company header (you're the buyer)
            - "BESTELBON" / "BON DE COMMANDE" / "PURCHASE ORDER" as title
            - Supplier (vendor) details
            - List of items being ordered with quantities
            - Requested delivery date
            - Terms and conditions
            - Authorized signature
            
            DISTINGUISHING FEATURES:
            - YOU are placing the order (your company is sender)
            - No payment due yet — it's an order, not invoice
            - Has "besteld door" / "ordered by" / your reference
            - May have approval signatures
            
            NOT THIS TYPE IF:
            - Supplier confirming YOUR order → that's their ORDER_CONFIRMATION to you
            - Supplier invoicing you → INVOICE
            - Your customer ordering from you → their PURCHASE_ORDER (your incoming order)
            """.trimIndent()

        DocumentType.ExpenseClaim ->
            """
            EXPENSE CLAIM — Employee request for reimbursement of personal business expenses.
            
            KEYWORDS (look for these exact terms):
            - NL: "Onkostennota", "Onkostendeclaratie", "Terugbetaling kosten", "Kilometervergoeding", "Verplaatsingskosten"
            - FR: "Note de frais", "Déclaration de frais", "Remboursement", "Indemnité kilométrique", "Frais de déplacement"
            - DE: "Spesenabrechnung", "Reisekostenabrechnung", "Auslagenerstattung"
            - EN: "Expense claim", "Expense report", "Reimbursement request", "Mileage claim"
            
            VISUAL STRUCTURE:
            - Employee name and details
            - List of expenses with dates
            - Categories (travel, meals, supplies)
            - Attached receipts referenced
            - Total reimbursement requested
            - Manager approval signature/section
            - Often internal company form
            
            DISTINGUISHING FEATURES:
            - INTERNAL document (not from external supplier)
            - Employee requesting money back
            - Lists multiple small expenses
            - Has approval workflow (manager signature)
            - References attached receipts as proof
            
            NOT THIS TYPE IF:
            - External supplier invoice → INVOICE
            - Single receipt from store → RECEIPT
            - Company credit card statement → different type
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // BANKING
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.BankStatement ->
            """
            BANK STATEMENT — Official account statement showing transactions over a period.
            
            KEYWORDS (look for these exact terms):
            - NL: "Rekeninguittreksels", "Dagafschrift", "Bankafschrift", "Saldo", "Rekeningnummer", "IBAN", "Valutadatum"
            - FR: "Extrait de compte", "Relevé bancaire", "Solde", "Numéro de compte", "Date valeur"
            - DE: "Kontoauszug", "Bankauszug", "Saldo", "Kontonummer", "Wertstellung"
            - EN: "Bank statement", "Account statement", "Balance", "Account number", "Value date"
            
            FORMATS:
            - CODA: Belgian electronic bank statement format (coded file, not human-readable PDF)
            - PDF statement: Human-readable bank statement
            
            VISUAL STRUCTURE:
            - Bank logo and name (KBC, BNP Paribas Fortis, Belfius, ING, Argenta)
            - Account holder name and address
            - Account number (IBAN)
            - Statement period (from-to dates)
            - Opening and closing balance
            - List of transactions with:
              - Date (booking date, value date)
              - Description / counterparty
              - Reference / communication
              - Amount (+/-)
              - Running balance
            
            DISTINGUISHING FEATURES:
            - Official bank branding
            - Shows IN and OUT transactions
            - Has running balance
            - IBAN account number prominent
            - Period covered clearly stated
            
            BELGIAN BANKS: KBC, Belfius, BNP Paribas Fortis, ING, Argenta, Crelan, AXA Bank, Keytrade, Triodos
            
            NOT THIS TYPE IF:
            - Single transaction confirmation → PAYMENT_CONFIRMATION
            - Bank fee breakdown → BANK_FEE
            - Loan statement → LOAN
            - Interest notice → INTEREST_STATEMENT
            """.trimIndent()

        DocumentType.BankFee ->
            """
            BANK FEE NOTICE — Breakdown of banking costs and charges.
            
            KEYWORDS (look for these exact terms):
            - NL: "Bankkosten", "Kosten", "Tarieven", "Rekeningkosten", "Beheerskosten", "Overzicht kosten"
            - FR: "Frais bancaires", "Coûts", "Tarifs", "Frais de gestion", "Frais de tenue de compte"
            - DE: "Bankgebühren", "Kontoführungsgebühren", "Gebührenübersicht"
            - EN: "Bank charges", "Account fees", "Service charges", "Fee statement"
            
            VISUAL STRUCTURE:
            - Bank branding
            - Itemized list of fees charged
            - Period covered
            - Fee descriptions (card fees, transaction fees, maintenance)
            - Total fees for period
            
            DISTINGUISHING FEATURES:
            - Focus on COSTS/FEES charged by bank
            - Itemized breakdown of different fee types
            - Often quarterly or annual summary
            
            NOT THIS TYPE IF:
            - Full transaction list → BANK_STATEMENT
            - Single transaction receipt → PAYMENT_CONFIRMATION
            """.trimIndent()

        DocumentType.InterestStatement ->
            """
            INTEREST STATEMENT — Notice of interest earned or paid.
            
            KEYWORDS (look for these exact terms):
            - NL: "Rente", "Rentebericht", "Interestafrekening", "Spaarrente", "Debetrente", "Creditrente"
            - FR: "Intérêts", "Avis d'intérêts", "Décompte d'intérêts", "Intérêts créditeurs/débiteurs"
            - DE: "Zinsen", "Zinsabrechnung", "Habenzinsen", "Sollzinsen"
            - EN: "Interest", "Interest statement", "Interest earned", "Interest charged"
            
            VISUAL STRUCTURE:
            - Bank branding
            - Interest rate applied
            - Period for calculation
            - Principal amount
            - Interest amount earned/charged
            - Withholding tax on interest (roerende voorheffing / précompte mobilier)
            
            DISTINGUISHING FEATURES:
            - Specifically about INTEREST (not transactions)
            - Shows rate and calculation period
            - For savings: interest earned
            - For loans/overdraft: interest charged
            - Often shows withholding tax deducted
            
            NOT THIS TYPE IF:
            - Loan repayment schedule → LOAN
            - Full bank statement → BANK_STATEMENT
            """.trimIndent()

        DocumentType.PaymentConfirmation ->
            """
            PAYMENT CONFIRMATION — Proof that a specific payment was executed.
            
            KEYWORDS (look for these exact terms):
            - NL: "Betalingsbewijs", "Overschrijving uitgevoerd", "Betaling bevestigd", "Transactiebewijs", "Storting"
            - FR: "Confirmation de paiement", "Virement effectué", "Preuve de paiement", "Ordre exécuté"
            - DE: "Zahlungsbestätigung", "Überweisung ausgeführt", "Zahlungsbeleg"
            - EN: "Payment confirmation", "Transfer completed", "Payment receipt", "Transaction confirmation"
            
            VISUAL STRUCTURE:
            - Single transaction details
            - From account (payer)
            - To account (beneficiary)
            - Amount transferred
            - Date and time of execution
            - Reference / communication
            - Confirmation or reference number
            
            DISTINGUISHING FEATURES:
            - Single payment focus (not list of transactions)
            - Confirms payment was MADE/EXECUTED
            - Has beneficiary details
            - Has payment reference
            - Often from online banking
            
            NOT THIS TYPE IF:
            - Multiple transactions over period → BANK_STATEMENT
            - Request to pay → INVOICE
            - Receipt from purchase → RECEIPT
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // VAT (Belgium)
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.VatReturn ->
            """
            VAT RETURN — Periodic VAT declaration filed with Belgian tax authority.
            
            KEYWORDS (look for these exact terms):
            - NL: "BTW-aangifte", "BTW-listing", "Periodieke aangifte", "Rooster 71", "Rooster 82", "Rooster 54", "Teruggave BTW"
            - FR: "Déclaration TVA", "Déclaration périodique", "Grille 71", "Grille 82", "Grille 54", "Remboursement TVA"
            - DE: "MwSt-Erklärung", "Umsatzsteuererklärung"
            - EN: "VAT return", "VAT declaration", "Periodic VAT filing"
            
            VISUAL STRUCTURE:
            - Belgian tax authority format (FOD Financiën / SPF Finances)
            - Grid/box numbers (Belgian VAT return uses numbered boxes)
            - Key boxes: 01-03 (sales), 81-83 (purchases), 54/59 (VAT due/deductible), 71/72 (balance)
            - Period (month or quarter)
            - Net amount payable or refundable
            
            DISTINGUISHING FEATURES:
            - Official Belgian VAT return format
            - Grid numbers (rooster/grille) like 54, 59, 71
            - Filing period clearly stated
            - Shows VAT collected vs. VAT paid
            - Net position (te betalen / terug te vorderen)
            
            NOT THIS TYPE IF:
            - Assessment from tax authority → VAT_ASSESSMENT
            - Annual client listing → VAT_LISTING
            - EU sales report → IC_LISTING
            """.trimIndent()

        DocumentType.VatListing ->
            """
            VAT LISTING — Annual listing of Belgian VAT-registered customers.
            
            KEYWORDS (look for these exact terms):
            - NL: "Jaarlijkse klantenlisting", "Listing BTW-plichtigen", "Jaarlijkse BTW-listing", "Klanten met BTW-nummer"
            - FR: "Liste annuelle des clients assujettis", "Listing TVA annuel"
            - EN: "Annual VAT customer listing", "VAT client listing"
            
            VISUAL STRUCTURE:
            - List of customers with Belgian VAT numbers
            - Total sales per customer (> €250)
            - Customer VAT number and name
            - Total VAT charged per customer
            - Year covered
            
            DISTINGUISHING FEATURES:
            - Annual document (not monthly/quarterly)
            - Lists CUSTOMERS, not transactions
            - Belgian VAT numbers (BE 0xxx.xxx.xxx)
            - Only B2B sales > €250/year
            
            NOT THIS TYPE IF:
            - Regular VAT return → VAT_RETURN
            - EU customer listing → IC_LISTING
            - Customer account overview → STATEMENT_OF_ACCOUNT
            """.trimIndent()

        DocumentType.VatAssessment ->
            """
            VAT ASSESSMENT — Official VAT notice from Belgian tax authority.
            
            KEYWORDS (look for these exact terms):
            - NL: "BTW-aanslag", "Aanslagbiljet BTW", "Correctie BTW", "Navordering BTW", "Controle BTW"
            - FR: "Avis d'imposition TVA", "Cotisation TVA", "Redressement TVA", "Contrôle TVA"
            - DE: "MwSt-Bescheid", "Steuerbescheid"
            - EN: "VAT assessment", "VAT notice", "VAT audit result"
            
            VISUAL STRUCTURE:
            - Official FOD Financiën / SPF Finances letterhead
            - Reference to specific VAT periods
            - Assessment amount
            - Deadline for payment or appeal
            - Legal references
            
            DISTINGUISHING FEATURES:
            - FROM tax authority TO taxpayer
            - Official government document
            - Shows additional VAT owed or adjustment
            - May result from audit or automatic check
            - Has appeal deadline
            
            NOT THIS TYPE IF:
            - Your filed return → VAT_RETURN
            - General tax assessment → TAX_ASSESSMENT
            """.trimIndent()

        DocumentType.IcListing ->
            """
            IC LISTING — Intracommunity listing for EU B2B sales.
            
            KEYWORDS (look for these exact terms):
            - NL: "Intracommunautaire opgave", "IC-listing", "ICL", "Intracommunautaire leveringen", "EU-verkopen"
            - FR: "Relevé intracommunautaire", "Déclaration IC", "Livraisons intracommunautaires"
            - DE: "Zusammenfassende Meldung", "Innergemeinschaftliche Lieferungen"
            - EN: "EC Sales List", "Intracommunity listing", "IC listing", "EU sales report"
            
            VISUAL STRUCTURE:
            - Quarterly report
            - List of EU (non-Belgian) customers
            - VAT numbers from other EU countries (DE, FR, NL, etc.)
            - Sales amounts per customer per quarter
            - Country codes
            
            DISTINGUISHING FEATURES:
            - Only EU cross-border B2B sales
            - VAT numbers from OTHER EU countries (not BE)
            - Quarterly filing
            - No Belgian customers included
            
            NOT THIS TYPE IF:
            - Belgian customers → VAT_LISTING
            - Non-EU customers → no listing required
            - VAT return itself → VAT_RETURN
            """.trimIndent()

        DocumentType.OssReturn ->
            """
            OSS RETURN — One-Stop-Shop VAT return for EU B2C e-commerce.
            
            KEYWORDS (look for these exact terms):
            - NL: "OSS-aangifte", "One-Stop-Shop", "EU B2C verkopen", "Afstandsverkopen"
            - FR: "Déclaration OSS", "Guichet unique", "Ventes à distance"
            - DE: "OSS-Meldung", "One-Stop-Shop"
            - EN: "OSS return", "One-Stop-Shop declaration", "EU B2C distance sales"
            
            VISUAL STRUCTURE:
            - Quarterly declaration
            - Sales per EU member state
            - VAT rates per country
            - VAT due per country
            - Total VAT to pay to Belgian authority
            
            DISTINGUISHING FEATURES:
            - Only for B2C sales to EU CONSUMERS (not businesses)
            - Shows VAT by COUNTRY (not customer)
            - Different VAT rates per country
            - Filed via Belgian OSS portal
            
            NOT THIS TYPE IF:
            - B2B sales → IC_LISTING
            - Domestic VAT → VAT_RETURN
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // TAX - CORPORATE
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.CorporateTax ->
            """
            CORPORATE TAX — Company income tax return or assessment.
            
            KEYWORDS (look for these exact terms):
            - NL: "Vennootschapsbelasting", "VenB", "Aangifte vennootschapsbelasting", "Isoc", "Belastbaar resultaat"
            - FR: "Impôt des sociétés", "Isoc", "Déclaration Isoc", "Résultat imposable"
            - DE: "Körperschaftsteuer", "Gesellschaftssteuer"
            - EN: "Corporate income tax", "Company tax", "Corporation tax"
            
            VISUAL STRUCTURE:
            - Annual filing
            - Financial year covered
            - Taxable profit calculation
            - Tax rate applied (usually 25% or 20% SME rate in Belgium)
            - Tax due
            - May include breakdown of deductions
            
            DISTINGUISHING FEATURES:
            - Tax on company PROFITS (not sales/VAT)
            - Annual filing linked to financial year
            - For BV, NV, BVBA, etc. (not eenmanszaak)
            
            NOT THIS TYPE IF:
            - Personal income tax → PERSONAL_TAX
            - VAT → VAT_RETURN
            - Advance payment → CORPORATE_TAX_ADVANCE
            - Tax assessment notice → TAX_ASSESSMENT
            """.trimIndent()

        DocumentType.CorporateTaxAdvance ->
            """
            CORPORATE TAX ADVANCE — Prepayment of estimated corporate tax.
            
            KEYWORDS (look for these exact terms):
            - NL: "Voorafbetaling", "VA vennootschapsbelasting", "Voorafbetalingen VenB", "Kwartaalvoorschot"
            - FR: "Versement anticipé", "VA Isoc", "Versements anticipés"
            - DE: "Vorauszahlung Körperschaftsteuer"
            - EN: "Advance corporate tax", "Estimated tax payment", "Quarterly prepayment"
            
            VISUAL STRUCTURE:
            - Quarterly payment (VA1, VA2, VA3, VA4)
            - Reference period (tax year)
            - Payment amount
            - Payment deadline (typically 10th of Jan, Apr, Jul, Oct)
            - Bank account for payment
            - Tax benefit mentioned (avoid surcharge)
            
            DISTINGUISHING FEATURES:
            - PREPAYMENT (not final assessment)
            - Quarterly timing
            - Mentions avoiding "vermeerdering" (surcharge)
            - May show calculation of optimal advance payment
            
            NOT THIS TYPE IF:
            - Final tax return → CORPORATE_TAX
            - Tax assessment → TAX_ASSESSMENT
            - VAT advance → different thing
            """.trimIndent()

        DocumentType.TaxAssessment ->
            """
            TAX ASSESSMENT — Official tax assessment notice from Belgian tax authority.
            
            KEYWORDS (look for these exact terms):
            - NL: "Aanslagbiljet", "Kohierartikel", "Aanslag", "Inkohiering", "Te betalen aan de ontvanger"
            - FR: "Avertissement-extrait de rôle", "Rôle", "Imposition", "Cotisation", "À payer au receveur"
            - DE: "Steuerbescheid", "Festsetzungsbescheid"
            - EN: "Tax assessment", "Tax notice", "Assessment notice"
            
            VISUAL STRUCTURE:
            - Official FOD Financiën / SPF Finances document
            - Kohierartikel/Article de rôle number
            - Tax type (VenB, PB, BTW)
            - Period/year assessed
            - Taxable base
            - Tax calculated
            - Payments already made
            - Amount still due or to be refunded
            - Payment deadline
            - Appeal deadline (bezwaartermijn / délai de réclamation)
            
            DISTINGUISHING FEATURES:
            - FROM government TO taxpayer
            - Official assessment (not self-filed return)
            - Has kohierartikel number
            - Has formal appeal period
            - Final/official tax determination
            
            NOT THIS TYPE IF:
            - Your filed tax return → CORPORATE_TAX or PERSONAL_TAX
            - Advance payment notice → CORPORATE_TAX_ADVANCE
            - VAT specific assessment → VAT_ASSESSMENT
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // TAX - PERSONAL
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.PersonalTax ->
            """
            PERSONAL TAX — Personal income tax return or assessment.
            
            KEYWORDS (look for these exact terms):
            - NL: "Personenbelasting", "PB", "Aangifte personenbelasting", "Tax-on-web", "Belastingaangifte"
            - FR: "Impôt des personnes physiques", "IPP", "Déclaration IPP", "Tax-on-web"
            - DE: "Einkommensteuer", "Personensteuer"
            - EN: "Personal income tax", "Individual tax return"
            
            VISUAL STRUCTURE:
            - Annual tax return
            - Income categories (employment, self-employed, real estate, investments)
            - Deductions and reductions
            - Tax calculation
            - For eenmanszaak: business income included in personal tax
            
            DISTINGUISHING FEATURES:
            - Individual person (not company)
            - Includes ALL income types
            - For sole proprietors (eenmanszaak): business profit here
            - Annual filing (income year)
            
            NOT THIS TYPE IF:
            - Company tax → CORPORATE_TAX
            - Withholding tax slip → WITHHOLDING_TAX
            - Tax assessment notice → TAX_ASSESSMENT
            """.trimIndent()

        DocumentType.WithholdingTax ->
            """
            WITHHOLDING TAX — Tax withheld at source from payments.
            
            KEYWORDS (look for these exact terms):
            - NL: "Bedrijfsvoorheffing", "BV", "Voorheffing", "Ingehouden belasting", "Fiche 281.10"
            - FR: "Précompte professionnel", "Pr.P", "Retenue à la source", "Fiche 281.10"
            - DE: "Lohnsteuer", "Quellensteuer"
            - EN: "Withholding tax", "Tax withheld", "PAYE"
            
            VISUAL STRUCTURE:
            - Shows gross payment
            - Tax withheld
            - Net payment
            - Period covered
            - Often linked to salary slips (fiche 281)
            
            DISTINGUISHING FEATURES:
            - Tax DEDUCTED before payment
            - Employer responsibility to remit
            - Monthly declaration by employer
            - Links to salary administration
            
            NOT THIS TYPE IF:
            - Salary slip showing deduction → SALARY_SLIP
            - Company's withholding tax filing → different administrative doc
            - Dividend withholding → DIVIDEND
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // SOCIAL CONTRIBUTIONS (Belgium)
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.SocialContribution ->
            """
            SOCIAL CONTRIBUTION — Employer social security payment (RSZ/ONSS).
            
            KEYWORDS (look for these exact terms):
            - NL: "RSZ", "Rijksdienst voor Sociale Zekerheid", "RSZ-bijdrage", "Sociale bijdragen werkgever", "Kwartaalaangifte RSZ"
            - FR: "ONSS", "Office National de Sécurité Sociale", "Cotisations sociales", "Cotisations patronales"
            - DE: "Sozialversicherungsbeiträge", "Arbeitgeberbeiträge"
            - EN: "Social security contribution", "Employer contribution", "NSSO"
            
            VISUAL STRUCTURE:
            - Quarterly declaration
            - Number of employees
            - Gross wages basis
            - Employer contribution rate (~25%)
            - Employee contribution (withheld)
            - Total due to RSZ/ONSS
            - Payment deadline
            
            DISTINGUISHING FEATURES:
            - EMPLOYER paying for EMPLOYEES
            - Quarterly basis
            - RSZ/ONSS as recipient
            - Based on employee wages
            
            NOT THIS TYPE IF:
            - Self-employed contribution → SELF_EMPLOYED_CONTRIBUTION
            - Social secretariat fees → SOCIAL_FUND
            - Individual salary slip → SALARY_SLIP
            """.trimIndent()

        DocumentType.SocialFund ->
            """
            SOCIAL FUND — Social secretariat or sector fund fees.
            
            KEYWORDS (look for these exact terms):
            - NL: "Sociaal secretariaat", "Groep S", "Securex", "Acerta", "Liantis", "Partena", "Sectorfonds", "Fonds voor bestaanszekerheid"
            - FR: "Secrétariat social", "Fonds sectoriel", "Fonds de sécurité d'existence"
            - EN: "Social secretariat", "Payroll provider", "Sector fund"
            
            COMMON PROVIDERS: Securex, Acerta, Liantis (Zenito), Partena, Attentia, Sodalis, Group S, SD Worx
            
            VISUAL STRUCTURE:
            - Monthly/quarterly invoice
            - Service fees for payroll administration
            - May include sector fund contributions
            - Often itemized services
            
            DISTINGUISHING FEATURES:
            - INVOICE for payroll services
            - From social secretariat provider
            - Fees for administration, not direct social security
            - May include mandatory sector fund contributions
            
            NOT THIS TYPE IF:
            - Direct RSZ/ONSS payment → SOCIAL_CONTRIBUTION
            - Self-employed social contribution → SELF_EMPLOYED_CONTRIBUTION
            - Salary slip → SALARY_SLIP
            """.trimIndent()

        DocumentType.SelfEmployedContribution ->
            """
            SELF-EMPLOYED CONTRIBUTION — Quarterly social security for independents (zelfstandigen).
            
            KEYWORDS (look for these EXACT terms):
            - NL: "Sociale bijdragen zelfstandige", "Sociale bijdragen als zelfstandige", "Sociaal verzekeringsfonds", 
                  "Kwartaalbijdrage", "REGULARISATIEBERICHT", "Regularisatie", "Voorlopige bijdrage", "Definitieve bijdrage"
            - FR: "Cotisations sociales indépendant", "Caisse d'assurances sociales", "Cotisations trimestrielles", 
                  "Régularisation", "Cotisations provisoires", "Cotisations définitives"
            - EN: "Self-employed social contribution", "Independent social security"
            
            COMMON FUNDS (Belgian): Acerta, Liantis (Zenito), Xerius, Partena, Securex, Group S, UCM, Multipen, AVIXI
            
            VISUAL STRUCTURE:
            - Social insurance fund logo and letterhead
            - Recipient is the self-employed person (you)
            - Shows quarterly breakdown (kwartaal 2024/1, 2024/2, etc.)
            - Contribution amount based on income (bijdrage)
            - Payment deadline and bank account (IBAN)
            - Structured communication (+++xxx/xxxx/xxxxx+++)
            - May show "vorige toestand" vs "nieuwe toestand" for regularizations
            
            DOCUMENT SUBTYPES (all are SelfEmployedContribution):
            - Regular quarterly contribution request
            - REGULARISATIEBERICHT: Recalculation based on updated income — still a PAYMENT REQUEST
            - Provisional contribution (voorlopige bijdrage)
            - Definitive contribution (definitieve bijdrage)
            
            DISTINGUISHING FEATURES:
            - FROM: Social insurance fund (sociaal verzekeringsfonds / caisse d'assurances sociales)
            - TO: Self-employed individual
            - Shows income-based calculation
            - Quarterly periods referenced
            - Amount TO PAY (not a refund)
            
            ⚠️ CRITICAL: REGULARISATIEBERICHT is NOT a credit note!
            - It's a recalculation when income data changes
            - Shows old vs new contribution amounts
            - Results in payment DUE (te betalen), not a refund
            - The word "regularisatie" means ADJUSTMENT, not CREDIT
            
            NOT THIS TYPE IF:
            - Employer RSZ/ONSS payment → SOCIAL_CONTRIBUTION
            - VAPZ pension contribution → VAPZ
            - Social secretariat invoice → SOCIAL_FUND
            - Actual credit/refund from fund → could be CREDIT_NOTE (rare)
            """.trimIndent()

        DocumentType.Vapz ->
            """
            VAPZ — Supplementary pension for self-employed.
            
            KEYWORDS (look for these exact terms):
            - NL: "VAPZ", "Vrij Aanvullend Pensioen Zelfstandigen", "Aanvullend pensioen zelfstandige", "Pensioensparen zelfstandige"
            - FR: "PLCI", "Pension Libre Complémentaire Indépendant", "Pension complémentaire indépendant"
            - EN: "Supplementary pension self-employed", "VAPZ pension"
            
            COMMON PROVIDERS: AG Insurance, KBC, Belfius, NN, Vivium, insurance companies, pension funds
            
            VISUAL STRUCTURE:
            - Annual or periodic premium notice
            - Premium amount
            - Tax deductibility mentioned
            - Pension fund/insurer details
            - Coverage details
            
            DISTINGUISHING FEATURES:
            - PENSION savings (not social security)
            - Tax-deductible contribution
            - From insurance company or pension fund
            - Voluntary (not mandatory like social contributions)
            
            NOT THIS TYPE IF:
            - Mandatory social contribution → SELF_EMPLOYED_CONTRIBUTION
            - Insurance policy (non-pension) → INSURANCE
            - Company pension plan → different type
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // PAYROLL / HR
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.SalarySlip ->
            """
            SALARY SLIP — Employee monthly pay statement.
            
            KEYWORDS (look for these exact terms):
            - NL: "Loonfiche", "Loonbrief", "Loonstrook", "Bezoldiging", "Brutoloon", "Nettoloon", "RSZ-bijdrage werknemer"
            - FR: "Fiche de paie", "Bulletin de salaire", "Salaire brut", "Salaire net", "Cotisations ONSS"
            - DE: "Gehaltsabrechnung", "Lohnzettel", "Bruttolohn", "Nettolohn"
            - EN: "Pay slip", "Salary statement", "Paycheck", "Gross salary", "Net salary"
            
            VISUAL STRUCTURE:
            - Employee name and details
            - Pay period (month)
            - Gross salary
            - Deductions:
              - RSZ/ONSS employee share (~13.07%)
              - Withholding tax (bedrijfsvoorheffing)
              - Other deductions (meal vouchers, car, etc.)
            - Allowances and benefits
            - Net salary
            - Employer contact details
            - Often from social secretariat
            
            DISTINGUISHING FEATURES:
            - Monthly pay breakdown
            - Shows gross → net calculation
            - Employee social security deducted
            - Withholding tax deducted
            - For EMPLOYEE (not self-employed)
            
            NOT THIS TYPE IF:
            - Self-employed income → different (no salary slip)
            - Employer's RSZ payment → SOCIAL_CONTRIBUTION
            - Annual tax fiche 281.10 → WITHHOLDING_TAX
            - Holiday pay statement → HOLIDAY_PAY
            """.trimIndent()

        DocumentType.PayrollSummary ->
            """
            PAYROLL SUMMARY — Aggregated payroll overview for employer.
            
            KEYWORDS (look for these exact terms):
            - NL: "Loonoverzicht", "Maandelijkse loonkost", "Loonkostenoverzicht", "Totaal loonkosten", "Personeelskosten"
            - FR: "Récapitulatif des salaires", "Coût salarial", "Charges de personnel"
            - EN: "Payroll summary", "Wage overview", "Personnel costs"
            
            VISUAL STRUCTURE:
            - All employees listed
            - Per-employee: gross, deductions, net
            - Totals: gross wages, employer contributions, total cost
            - Period (month/quarter/year)
            - From social secretariat
            
            DISTINGUISHING FEATURES:
            - SUMMARY for employer (not individual slip)
            - Multiple employees aggregated
            - Shows total employer cost
            - Management/accounting document
            
            NOT THIS TYPE IF:
            - Individual employee slip → SALARY_SLIP
            - RSZ payment document → SOCIAL_CONTRIBUTION
            """.trimIndent()

        DocumentType.EmploymentContract ->
            """
            EMPLOYMENT CONTRACT — Legal agreement between employer and employee.
            
            KEYWORDS (look for these exact terms):
            - NL: "Arbeidsovereenkomst", "Arbeidscontract", "Werkgever", "Werknemer", "Functie", "Loon", "Arbeidsduur", "Proefperiode"
            - FR: "Contrat de travail", "Employeur", "Travailleur", "Fonction", "Rémunération", "Durée du travail"
            - DE: "Arbeitsvertrag", "Arbeitgeber", "Arbeitnehmer"
            - EN: "Employment contract", "Work agreement", "Job contract"
            
            VISUAL STRUCTURE:
            - Legal document format
            - Parties identified (employer, employee)
            - Job function/title
            - Salary and benefits
            - Working hours
            - Start date
            - Contract type (bepaalde/onbepaalde duur)
            - Signatures
            
            DISTINGUISHING FEATURES:
            - Legal/formal document
            - Two parties signing
            - Terms of employment
            - Not a payment document
            
            NOT THIS TYPE IF:
            - Monthly pay → SALARY_SLIP
            - Service contract with freelancer → CONTRACT
            - Termination document → C4
            """.trimIndent()

        DocumentType.Dimona ->
            """
            DIMONA — Belgian employment declaration to social security.
            
            KEYWORDS (look for these exact terms):
            - NL: "Dimona", "Déclaration Immédiate", "Onmiddellijke Aangifte", "Dimona IN", "Dimona OUT", "RSZ-nummer"
            - FR: "Dimona", "Déclaration Immédiate", "Entrée en service", "Sortie de service"
            - EN: "Dimona declaration", "Employment notification"
            
            VISUAL STRUCTURE:
            - RSZ/ONSS reference
            - Employee national number (rijksregisternummer)
            - Employer RSZ number
            - Start date (IN) or end date (OUT)
            - Declaration type
            - Confirmation number
            
            DISTINGUISHING FEATURES:
            - ADMINISTRATIVE declaration (not contract)
            - Mandatory notification to RSZ/ONSS
            - For hiring (IN) or termination (OUT)
            - Has confirmation number
            
            NOT THIS TYPE IF:
            - Employment contract → EMPLOYMENT_CONTRACT
            - Termination letter → different
            - C4 for unemployment → C4
            """.trimIndent()

        DocumentType.C4 ->
            """
            C4 — Belgian unemployment document at end of employment.
            
            KEYWORDS (look for these exact terms):
            - NL: "C4", "Formulier C4", "Bewijs van werkloosheid", "Einde arbeidsovereenkomst", "Reden beëindiging", "RVA", "VDAB"
            - FR: "C4", "Formulaire C4", "Attestation de chômage", "Fin de contrat", "ONEM", "Forem", "Actiris"
            - EN: "C4 form", "Unemployment certificate"
            
            VISUAL STRUCTURE:
            - Official RVA/ONEM form
            - Employee details
            - Employer details
            - Employment period
            - Reason for termination
            - Last salary information
            - Signatures
            
            DISTINGUISHING FEATURES:
            - End of employment specific
            - For unemployment benefits application
            - Official Belgian form format
            - Reason for leaving indicated
            
            NOT THIS TYPE IF:
            - Employment contract → EMPLOYMENT_CONTRACT
            - Dimona OUT → DIMONA
            - Resignation letter → different
            """.trimIndent()

        DocumentType.HolidayPay ->
            """
            HOLIDAY PAY — Annual vacation pay statement.
            
            KEYWORDS (look for these exact terms):
            - NL: "Vakantiegeld", "Vakantiebon", "Enkel/Dubbel vakantiegeld", "Vertrekvakantiegeld", "Vakantiekas", "Jaarlijkse vakantie"
            - FR: "Pécule de vacances", "Simple/Double pécule", "Caisse de vacances", "Congés annuels"
            - DE: "Urlaubsgeld", "Feriengeld"
            - EN: "Holiday pay", "Vacation pay", "Annual leave payment"
            
            VISUAL STRUCTURE:
            - Employee details
            - Reference year (vakantiejaar)
            - Days accrued
            - Single holiday pay (enkel)
            - Double holiday pay (dubbel)
            - Deductions (social security, tax)
            - Net amount
            - Often from holiday pay fund (arbeiders) or employer (bedienden)
            
            DISTINGUISHING FEATURES:
            - Annual payment (usually May/June)
            - Separate from regular salary
            - Shows days and amounts
            - For employees only
            
            NOT THIS TYPE IF:
            - Regular monthly salary → SALARY_SLIP
            - Bonus payment → different
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // LEGAL / CONTRACTS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Contract ->
            """
            CONTRACT — General business contract or service agreement.
            
            KEYWORDS (look for these exact terms):
            - NL: "Overeenkomst", "Contract", "Samenwerkingsovereenkomst", "Dienstenovereenkomst", "Partijen", "Voorwaarden", "Handtekening"
            - FR: "Contrat", "Convention", "Accord", "Parties", "Conditions", "Signature"
            - DE: "Vertrag", "Vereinbarung", "Vertragspartner"
            - EN: "Contract", "Agreement", "Terms and conditions", "Parties"
            
            VISUAL STRUCTURE:
            - Legal document format
            - Parties identified
            - Subject/scope of agreement
            - Terms and conditions
            - Duration
            - Payment terms (if applicable)
            - Signatures
            - Date
            
            DISTINGUISHING FEATURES:
            - Binding legal agreement
            - Two or more parties
            - Rights and obligations defined
            - Not an invoice or payment request
            
            NOT THIS TYPE IF:
            - Employment related → EMPLOYMENT_CONTRACT
            - Rental/lease → LEASE
            - Loan → LOAN
            - Insurance → INSURANCE
            """.trimIndent()

        DocumentType.Lease ->
            """
            LEASE — Rental or lease agreement for property/equipment.
            
            KEYWORDS (look for these exact terms):
            - NL: "Huurovereenkomst", "Huurcontract", "Leasing", "Verhuurder", "Huurder", "Huurprijs", "Waarborg", "Huurtermijn"
            - FR: "Bail", "Contrat de location", "Leasing", "Bailleur", "Locataire", "Loyer", "Garantie locative"
            - DE: "Mietvertrag", "Leasingvertrag", "Vermieter", "Mieter"
            - EN: "Lease", "Rental agreement", "Landlord", "Tenant", "Rent"
            
            VISUAL STRUCTURE:
            - Property/asset description
            - Landlord (verhuurder) and tenant (huurder) details
            - Monthly rent amount
            - Duration
            - Deposit (waarborg)
            - Payment terms
            - Special conditions
            - Signatures
            
            DISTINGUISHING FEATURES:
            - For property OR equipment rental
            - Recurring payment obligation
            - Has start/end date or duration
            - Deposit mentioned
            
            TYPES: Property lease, car lease, equipment lease
            
            NOT THIS TYPE IF:
            - Monthly rent invoice → INVOICE
            - Loan agreement → LOAN
            - Service contract → CONTRACT
            """.trimIndent()

        DocumentType.Loan ->
            """
            LOAN — Loan agreement or periodic loan statement.
            
            KEYWORDS (look for these exact terms):
            - NL: "Lening", "Krediet", "Kredietovereenkomst", "Aflossing", "Hoofdsom", "Rente", "Looptijd", "Aflossingstabel"
            - FR: "Prêt", "Crédit", "Emprunt", "Amortissement", "Capital", "Intérêts", "Durée", "Tableau d'amortissement"
            - DE: "Darlehen", "Kredit", "Tilgung", "Zinsen", "Laufzeit"
            - EN: "Loan", "Credit", "Principal", "Interest", "Repayment", "Amortization"
            
            VISUAL STRUCTURE:
            - Lender and borrower details
            - Principal amount
            - Interest rate
            - Duration/term
            - Repayment schedule
            - Monthly payment amount
            - Remaining balance
            - Collateral (if applicable)
            
            DISTINGUISHING FEATURES:
            - Borrowed money being repaid
            - Shows principal and interest
            - Has repayment schedule
            - From bank or financial institution
            
            NOT THIS TYPE IF:
            - Bank account statement → BANK_STATEMENT
            - Lease/rental → LEASE
            - Interest notice only → INTEREST_STATEMENT
            """.trimIndent()

        DocumentType.Insurance ->
            """
            INSURANCE — Insurance policy or premium notice.
            
            KEYWORDS (look for these exact terms):
            - NL: "Verzekering", "Polis", "Polisnummer", "Premie", "Dekking", "Verzekerde", "Verzekeringnemer", "AG", "AXA", "Belfius", "KBC"
            - FR: "Assurance", "Police", "Prime", "Couverture", "Assuré", "Preneur d'assurance"
            - DE: "Versicherung", "Police", "Prämie", "Deckung"
            - EN: "Insurance", "Policy", "Premium", "Coverage", "Insured"
            
            COMMON INSURERS: AG Insurance, AXA, Belfius, KBC, Ethias, Baloise, NN, Allianz, Vivium
            
            INSURANCE TYPES:
            - Liability (aansprakelijkheid/responsabilité)
            - Property (brand/incendie)
            - Vehicle (auto/voiture)
            - Professional (beroepsaansprakelijkheid)
            - Health (hospitalisatie)
            - Life (levensverzekering)
            
            VISUAL STRUCTURE:
            - Insurance company branding
            - Policy number
            - Policyholder details
            - Coverage description
            - Premium amount
            - Payment terms
            - Coverage period
            
            DISTINGUISHING FEATURES:
            - Insurance company as issuer
            - Has policy number
            - Describes risks covered
            - Premium (not regular invoice)
            
            NOT THIS TYPE IF:
            - Pension insurance → VAPZ
            - Claim payment → different
            - Health reimbursement → different
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // CORPORATE DOCUMENTS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Dividend ->
            """
            DIVIDEND — Profit distribution to shareholders.
            
            KEYWORDS (look for these exact terms):
            - NL: "Dividend", "Dividenduitkering", "Winstuitkering", "Roerende voorheffing", "Bruto dividend", "Netto dividend", "Aandeelhouder"
            - FR: "Dividende", "Distribution de dividende", "Précompte mobilier", "Actionnaire"
            - DE: "Dividende", "Gewinnausschüttung", "Kapitalertragssteuer"
            - EN: "Dividend", "Dividend distribution", "Withholding tax", "Shareholder"
            
            VISUAL STRUCTURE:
            - Company distributing
            - Shareholder name
            - Number of shares
            - Gross dividend per share
            - Total gross dividend
            - Withholding tax (30% roerende voorheffing)
            - Net dividend
            - Payment date
            
            DISTINGUISHING FEATURES:
            - Profit distribution (not salary)
            - Withholding tax applied (30%)
            - Per-share calculation
            - From company to shareholder
            
            NOT THIS TYPE IF:
            - Salary → SALARY_SLIP
            - Interest payment → INTEREST_STATEMENT
            - General payment → different
            """.trimIndent()

        DocumentType.ShareholderRegister ->
            """
            SHAREHOLDER REGISTER — Official record of company ownership.
            
            KEYWORDS (look for these exact terms):
            - NL: "Aandeelhoudersregister", "Aandelenregister", "Register van aandelen", "Overdracht van aandelen", "Aandeelhouders"
            - FR: "Registre des actionnaires", "Registre des actions", "Transfert d'actions"
            - EN: "Shareholder register", "Share register", "Stock ledger"
            
            VISUAL STRUCTURE:
            - Company name
            - List of shareholders
            - Number of shares per holder
            - Share transfers (date, from, to)
            - Share classes (if applicable)
            - Ownership percentages
            
            DISTINGUISHING FEATURES:
            - Ownership record (not transaction)
            - Lists all shareholders
            - Shows share transfers
            - Legal corporate record
            
            NOT THIS TYPE IF:
            - Dividend payment → DIVIDEND
            - Share purchase invoice → different
            """.trimIndent()

        DocumentType.CompanyExtract ->
            """
            COMPANY EXTRACT — Official registration extract from Crossroads Bank.
            
            KEYWORDS (look for these exact terms):
            - NL: "KBO-uittreksel", "Uittreksel KBO", "Kruispuntbank van Ondernemingen", "Ondernemingsnummer", "BTW-nummer", "Activiteiten", "Vestigingseenheid"
            - FR: "Extrait BCE", "Banque-Carrefour des Entreprises", "Numéro d'entreprise", "Unité d'établissement"
            - EN: "CBE extract", "Company registration extract"
            
            VISUAL STRUCTURE:
            - Official government document
            - Company number (0xxx.xxx.xxx)
            - Company name and legal form
            - Registered address
            - Activity codes (NACE)
            - Start date
            - Legal representatives
            - Establishment units
            
            DISTINGUISHING FEATURES:
            - From official Belgian register
            - Has ondernemingsnummer
            - Shows legal status
            - Official government source
            
            NOT THIS TYPE IF:
            - Annual accounts → ANNUAL_ACCOUNTS
            - Tax registration → different
            """.trimIndent()

        DocumentType.AnnualAccounts ->
            """
            ANNUAL ACCOUNTS — Yearly financial statements filed with National Bank.
            
            KEYWORDS (look for these exact terms):
            - NL: "Jaarrekening", "Balans", "Resultatenrekening", "Winst- en verliesrekening", "Toelichting", "Nationale Bank", "NBB"
            - FR: "Comptes annuels", "Bilan", "Compte de résultats", "Annexes", "Banque Nationale", "BNB"
            - DE: "Jahresabschluss", "Bilanz", "Gewinn- und Verlustrechnung"
            - EN: "Annual accounts", "Financial statements", "Balance sheet", "Income statement"
            
            VISUAL STRUCTURE:
            - Balance sheet (activa/passiva)
            - Income statement
            - Notes/appendix
            - Accounting period
            - Filing with NBB
            - Full or abbreviated format
            - Auditor report (if applicable)
            
            DISTINGUISHING FEATURES:
            - Annual financial report
            - Standardized Belgian format
            - Filed with National Bank
            - Balance sheet + income statement
            
            NOT THIS TYPE IF:
            - Corporate tax return → CORPORATE_TAX
            - Bank statement → BANK_STATEMENT
            - Internal management accounts → different
            """.trimIndent()

        DocumentType.BoardMinutes ->
            """
            BOARD MINUTES — Minutes of board or shareholder meeting.
            
            KEYWORDS (look for these exact terms):
            - NL: "Notulen", "PV", "Proces-verbaal", "Algemene vergadering", "Raad van bestuur", "Bestuurdersverslag", "Beslissingen"
            - FR: "Procès-verbal", "PV", "Assemblée générale", "Conseil d'administration", "Décisions"
            - DE: "Protokoll", "Hauptversammlung", "Vorstandssitzung"
            - EN: "Minutes", "Board minutes", "Shareholder meeting", "Resolutions"
            
            VISUAL STRUCTURE:
            - Date and location
            - Attendees listed
            - Agenda items
            - Decisions/resolutions
            - Voting results
            - Signatures
            - Legal required format
            
            DISTINGUISHING FEATURES:
            - Record of MEETING decisions
            - Legal corporate document
            - Has date, attendees, votes
            - Not financial transaction
            
            NOT THIS TYPE IF:
            - Company extract → COMPANY_EXTRACT
            - Contract → CONTRACT
            - Financial statement → ANNUAL_ACCOUNTS
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // GOVERNMENT / REGULATORY
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Subsidy ->
            """
            SUBSIDY — Government grant or support payment notice.
            
            KEYWORDS (look for these exact terms):
            - NL: "Subsidie", "Premie", "Steunmaatregel", "Toekenning", "VLAIO", "Innovatiesteun", "KMO-portefeuille", "Werkingssubsidie"
            - FR: "Subside", "Aide", "Prime", "Subvention", "Région wallonne", "Bruxelles Économie"
            - EN: "Subsidy", "Grant", "Support measure", "Funding"
            
            COMMON SOURCES: VLAIO, Flanders Innovation, Wallonia, Brussels Region, Federal, EU funds
            
            VISUAL STRUCTURE:
            - Government agency letterhead
            - Grant/subsidy name
            - Approval decision
            - Amount granted
            - Conditions
            - Project reference
            - Payment schedule
            
            DISTINGUISHING FEATURES:
            - FROM government/agency
            - Approval or payment notice
            - Has conditions/requirements
            - Project or purpose specific
            
            NOT THIS TYPE IF:
            - Tax refund → TAX_ASSESSMENT
            - Invoice to government → INVOICE
            """.trimIndent()

        DocumentType.Fine ->
            """
            FINE — Penalty or fine notice from government or authority.
            
            KEYWORDS (look for these exact terms):
            - NL: "Boete", "Verkeersboete", "Administratieve geldboete", "Sanctie", "Te betalen", "Betaaltermijn", "Beroep"
            - FR: "Amende", "Contravention", "Sanction", "PV", "Délai de paiement"
            - DE: "Bußgeld", "Strafe", "Strafzettel"
            - EN: "Fine", "Penalty", "Traffic fine", "Sanction"
            
            COMMON SOURCES:
            - Traffic fines (politie, parket)
            - Tax penalties (FOD Financiën)
            - Social security penalties (RSZ/ONSS)
            - Environmental fines
            - Regulatory fines
            
            VISUAL STRUCTURE:
            - Issuing authority
            - Violation/infraction description
            - Fine amount
            - Payment deadline
            - Payment options
            - Appeal procedure
            
            DISTINGUISHING FEATURES:
            - PENALTY for violation
            - From official authority
            - Has violation details
            - Appeal option mentioned
            
            NOT THIS TYPE IF:
            - Late payment interest on invoice → different
            - Tax assessment → TAX_ASSESSMENT
            """.trimIndent()

        DocumentType.Permit ->
            """
            PERMIT — License, permit, or registration document.
            
            KEYWORDS (look for these exact terms):
            - NL: "Vergunning", "Erkenning", "Licentie", "Registratie", "Machtiging", "Attest", "Certificaat", "Toelating"
            - FR: "Permis", "Autorisation", "Licence", "Agrément", "Enregistrement", "Certificat"
            - DE: "Genehmigung", "Lizenz", "Zulassung", "Erlaubnis"
            - EN: "Permit", "License", "Authorization", "Registration", "Certificate"
            
            TYPES:
            - Environmental permits
            - Building permits
            - Professional licenses
            - Operating permits
            - Food safety registration
            - Transport licenses
            
            VISUAL STRUCTURE:
            - Issuing authority
            - Permit/license number
            - Holder details
            - Scope/activities covered
            - Validity period
            - Conditions
            - Signatures/stamps
            
            DISTINGUISHING FEATURES:
            - AUTHORIZATION to do something
            - From regulatory body
            - Has validity period
            - Conditions specified
            
            NOT THIS TYPE IF:
            - Company registration → COMPANY_EXTRACT
            - Professional certification (training) → different
            - Contract → CONTRACT
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // INTERNATIONAL TRADE
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.CustomsDeclaration ->
            """
            CUSTOMS DECLARATION — Import/export customs documentation.
            
            KEYWORDS (look for these exact terms):
            - NL: "Douaneaangifte", "Invoerrechten", "Uitvoerrechten", "SAD", "Enig Document", "Douane", "Customs"
            - FR: "Déclaration en douane", "Droits d'importation", "Droits de douane", "DAU", "Document Administratif Unique"
            - EN: "Customs declaration", "Import duties", "Export declaration", "SAD", "Single Administrative Document"
            
            VISUAL STRUCTURE:
            - Official customs form (SAD/DAU)
            - Import/export indicator
            - Goods description
            - HS codes (tariff classification)
            - Value of goods
            - Duties and taxes calculated
            - Country of origin/destination
            - Customs office stamp
            
            DISTINGUISHING FEATURES:
            - For non-EU trade (import/export)
            - Official customs form
            - HS/tariff codes
            - Duties calculated
            
            NOT THIS TYPE IF:
            - EU goods movement → INTRASTAT
            - Regular invoice from foreign supplier → INVOICE
            - Shipping document → DELIVERY_NOTE
            """.trimIndent()

        DocumentType.Intrastat ->
            """
            INTRASTAT — EU goods movement statistical declaration.
            
            KEYWORDS (look for these exact terms):
            - NL: "Intrastat", "Intracommunautair goederenverkeer", "Aankomst", "Verzending", "Goederencode"
            - FR: "Intrastat", "Échanges intracommunautaires", "Arrivées", "Expéditions"
            - EN: "Intrastat", "EU trade statistics", "Arrivals", "Dispatches"
            
            VISUAL STRUCTURE:
            - Monthly declaration
            - Flow direction (arrival/dispatch)
            - Partner EU country
            - Goods codes (CN8)
            - Statistical value
            - Net mass
            - Supplementary units
            
            DISTINGUISHING FEATURES:
            - STATISTICAL report (not payment)
            - EU internal trade only
            - Monthly filing
            - Above threshold reporting
            
            NOT THIS TYPE IF:
            - IC Listing (VAT) → IC_LISTING
            - Customs (non-EU) → CUSTOMS_DECLARATION
            - Invoice → INVOICE
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // ASSETS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.DepreciationSchedule ->
            """
            DEPRECIATION SCHEDULE — Asset depreciation overview.
            
            KEYWORDS (look for these exact terms):
            - NL: "Afschrijvingstabel", "Afschrijvingen", "Vaste activa", "Boekwaarde", "Restwaarde", "Afschrijvingspercentage"
            - FR: "Tableau d'amortissement", "Amortissements", "Immobilisations", "Valeur comptable", "Valeur résiduelle"
            - DE: "Abschreibungstabelle", "Anlagevermögen", "Buchwert"
            - EN: "Depreciation schedule", "Fixed assets", "Book value", "Residual value"
            
            VISUAL STRUCTURE:
            - List of fixed assets
            - Purchase date and value
            - Depreciation method
            - Annual depreciation amount
            - Accumulated depreciation
            - Net book value
            - Useful life
            
            DISTINGUISHING FEATURES:
            - Accounting document
            - Lists assets over time
            - Shows value decrease
            - For tax/accounting purposes
            
            NOT THIS TYPE IF:
            - Purchase invoice for asset → INVOICE
            - Annual accounts → ANNUAL_ACCOUNTS
            - Inventory list → INVENTORY
            """.trimIndent()

        DocumentType.Inventory ->
            """
            INVENTORY — Stock or inventory list/valuation.
            
            KEYWORDS (look for these exact terms):
            - NL: "Inventaris", "Voorraad", "Voorraadlijst", "Stocktelling", "Goederenvoorraad", "Inventarisatie"
            - FR: "Inventaire", "Stock", "Liste des stocks", "Comptage de stock"
            - DE: "Inventar", "Lagerbestand", "Bestandsliste"
            - EN: "Inventory", "Stock list", "Stock count", "Goods on hand"
            
            VISUAL STRUCTURE:
            - List of items/products
            - Quantities on hand
            - Unit values
            - Total value
            - Date of count
            - Location (if multiple warehouses)
            - Categories
            
            DISTINGUISHING FEATURES:
            - Physical goods list
            - Quantities and values
            - Point-in-time snapshot
            - For stock management/accounting
            
            NOT THIS TYPE IF:
            - Fixed assets list → DEPRECIATION_SCHEDULE
            - Purchase invoice → INVOICE
            - Delivery note → DELIVERY_NOTE
            """.trimIndent()

        // ═══════════════════════════════════════════════════════════════════
        // CATCH-ALL
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Other ->
            """
            OTHER — A business document that doesn't fit other categories.
            
            USE THIS WHEN:
            - Document is clearly business-related
            - Document type is identifiable but not in the list
            - Examples: internal memos, policies, reports, presentations, correspondence
            
            DO NOT USE WHEN:
            - Document fits another category (even partially)
            - Document is unreadable or unclear → use UNKNOWN
            - Document is personal/non-business → use UNKNOWN
            """.trimIndent()

        DocumentType.Unknown ->
            """
            UNKNOWN — Cannot determine document type.
            
            USE THIS WHEN:
            - Document is unreadable (poor quality, corrupt, cut off)
            - Document is not business-related (personal photos, spam, etc.)
            - Content is insufficient to classify
            - Language/format is completely unrecognizable
            - Empty or nearly empty document
            
            DO NOT USE WHEN:
            - Document is business-related but just unfamiliar → use OTHER
            - Document type is recognizable but unsupported → use specific type
            """.trimIndent()
    }
