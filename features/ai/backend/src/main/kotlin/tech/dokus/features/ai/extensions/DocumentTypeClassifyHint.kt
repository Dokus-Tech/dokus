package tech.dokus.features.ai.extensions

import tech.dokus.domain.enums.DocumentType

/**
 * Short classifier hint for each DocumentType.
 *
 * Purpose: feed into the classification prompt ONLY — one line per type.
 * These are NOT extraction guides. Do NOT use in extraction subgraphs.
 *
 * Design rules:
 * - Max 2 sentences per hint
 * - Lead with the strongest visual/textual signal the model will actually see
 * - Embed disambiguation against the closest confusable type where the risk is real
 * - Include key Belgian/multilingual trigger words inline (not as a separate keywords section)
 */
val DocumentType.classifyHint: String
    get() = when (this) {

        // ═══════════════════════════════════════════════════════════════════
        // FINANCIAL
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Invoice ->
            "Formal A4 document with 'Factuur / Facture / Rechnung / Invoice' header, " +
                    "a line-item table with VAT breakdown (6/12/21%), seller's IBAN and structured payment reference (+++xxx/xxxx/xxxxx+++), " +
                    "requesting future payment — NOT a thermal ticket (→ RECEIPT) and NOT marked 'Creditnota' (→ CREDIT_NOTE)."

        DocumentType.CreditNote ->
            "Explicitly titled 'Creditnota / Note de crédit / Avoir / Gutschrift' and references an original invoice number — " +
                    "amounts cancel or reduce a prior charge. " +
                    "⚠️ A 'REGULARISATIEBERICHT' from a social fund is NOT this — that is SELF_EMPLOYED_CONTRIBUTION."

        DocumentType.ProForma ->
            "Looks like an invoice but is explicitly marked 'Pro Forma' or 'Proforma' — not yet a legal payment request, " +
                    "often used for customs valuation or international bank transfers (may reference Incoterms)."

        DocumentType.Quote ->
            "Headed 'Offerte / Devis / Angebot / Quote/Quotation', states a validity period ('geldig tot / valable jusqu'au'), " +
                    "has NO payment due date, NO IBAN for payment, and NO structured payment reference."

        DocumentType.OrderConfirmation ->
            "Confirms a customer order was accepted — shows order number, expected delivery date, and items ordered, " +
                    "but has no payment due date or IBAN; often from an e-commerce platform and may say 'Factuur volgt'."

        DocumentType.DeliveryNote ->
            "Lists items physically delivered with quantities — titled 'Leveringsbon / Pakbon / Bon de livraison / Lieferschein', " +
                    "has a recipient signature field, and shows NO prices or VAT breakdown."

        DocumentType.Reminder ->
            "References one or more already-issued invoices by number that are overdue — " +
                    "uses urgency language: 'Herinnering / Aanmaning / Rappel / Mahnung', outstanding balance, sometimes threatens interest or legal action."

        DocumentType.StatementOfAccount ->
            "Overview of ALL transactions with one counterparty over a period (multiple invoices, payments, credits) with a running total — " +
                    "'Rekeningoverzicht / Relevé de compte', may show aging buckets (30/60/90 days); NOT a single invoice. " +
                    "NOT a bank or fintech account statement with IBAN and transaction rows (→ BANK_STATEMENT)."

        DocumentType.Receipt ->
            "Small POS/till receipt from a RETAIL or HOSPITALITY issuer proving payment was ALREADY made — " +
                    "narrow thermal-printer format, shows payment method (Bancontact/cash/card), time of purchase, " +
                    "'Betaald / Payé / Paid'; issued by a supermarket, restaurant, petrol station, parking, or shop. " +
                    "NOT an A4 document, NOT issued by a bank or financial institution (→ BANK_STATEMENT or PAYMENT_CONFIRMATION), " +
                    "NOT a formal invoice requesting future payment."

        DocumentType.PurchaseOrder ->
            "YOUR company is the sender placing an order TO a supplier — titled 'Bestelbon / Bon de commande / Purchase Order / PO', " +
                    "has requested delivery date but no payment due yet and no payment reference."

        DocumentType.ExpenseClaim ->
            "Internal employee form requesting reimbursement of personal business expenses — " +
                    "'Onkostennota / Note de frais / Spesenabrechnung', lists multiple small expenses with dates, has a manager approval/signature section."

        // ═══════════════════════════════════════════════════════════════════
        // BANKING
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.BankStatement ->
            "Official account statement from a bank or payment institution " +
                    "(KBC, Belfius, BNP Paribas Fortis, ING, Argenta, Crelan, Triodos, Wise, Revolut, N26, Starling, bunq…) — " +
                    "has the institution's branding, an IBAN or account number, opening and/or closing balance, a covered date range, " +
                    "and one or more debit/credit transaction lines; even a single-transaction daily statement is BANK_STATEMENT. " +
                    "A statement from a FINTECH or PAYMENT INSTITUTION (Wise, Revolut, N26) is still a BANK_STATEMENT. " +
                    "NOT a retail till receipt (→ RECEIPT) and NOT a supplier's invoice/account overview (→ STATEMENT_OF_ACCOUNT)."

        DocumentType.BankFee ->
            "Bank's own document itemising the fees/charges for account services over a period — " +
                    "'Bankkosten / Frais bancaires / Kontoführungsgebühren'; contains fee line items (card, maintenance, transactions) but NOT a full transaction list."

        DocumentType.InterestStatement ->
            "Notice of interest earned on savings or charged on a debt — shows interest rate, calculation period, interest amount, " +
                    "and often roerende voorheffing / précompte mobilier withheld; NOT a full bank statement."

        DocumentType.PaymentConfirmation ->
            "Proof that a SINGLE specific transfer was executed — 'Overschrijving uitgevoerd / Virement effectué', " +
                    "shows sender IBAN, beneficiary IBAN, amount, reference, and execution timestamp; NOT a multi-transaction bank statement."

        // ═══════════════════════════════════════════════════════════════════
        // VAT (Belgium)
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.VatReturn ->
            "Belgian periodic VAT declaration filed BY the taxpayer — distinctive numbered grid boxes " +
                    "(rooster/grille 54, 59, 71, 72, 81, 82) in FOD Financiën / SPF Finances format, shows VAT collected vs. deductible and net position."

        DocumentType.VatListing ->
            "Annual listing of Belgian B2B customers with VAT numbers (BE 0xxx.xxx.xxx) and total sales above €250 — " +
                    "'Jaarlijkse klantenlisting / Liste annuelle des clients assujettis'; filed once per year, lists customers NOT transactions."

        DocumentType.VatAssessment ->
            "Official VAT notice issued BY FOD Financiën / SPF Finances TO the taxpayer — " +
                    "demands additional VAT or correction, references specific VAT periods, has a formal appeal deadline (bezwaartermijn)."

        DocumentType.IcListing ->
            "Quarterly EU intracommunity B2B sales report — 'Intracommunautaire opgave / Relevé intracommunautaire / IC-listing', " +
                    "lists customers with non-Belgian EU VAT numbers (DE/FR/NL/…); NOT Belgian customers (→ VAT_LISTING)."

        DocumentType.OssReturn ->
            "Quarterly One-Stop-Shop VAT return for B2C e-commerce sales to EU consumers — " +
                    "'OSS-aangifte / Déclaration OSS', shows VAT amounts broken down by EU member state at local rates; for B2B see IC_LISTING."

        // ═══════════════════════════════════════════════════════════════════
        // TAX - CORPORATE
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.CorporateTax ->
            "Annual company income tax return — 'Vennootschapsbelasting / VenB / Impôt des sociétés / Isoc', " +
                    "shows taxable profit, 25% (or 20% SME) rate, for BV/NV/BVBA entities; NOT personal tax (→ PERSONAL_TAX) and NOT an advance payment (→ CORPORATE_TAX_ADVANCE)."

        DocumentType.CorporateTaxAdvance ->
            "Quarterly prepayment of estimated corporate tax — 'Voorafbetaling VenB / Versement anticipé Isoc', " +
                    "labelled VA1 / VA2 / VA3 / VA4, mentions avoiding the 'vermeerdering' surcharge; NOT the final annual return."

        DocumentType.TaxAssessment ->
            "Official final tax assessment notice issued BY the Belgian government (FOD Financiën) TO the taxpayer — " +
                    "'Aanslagbiljet / Avertissement-extrait de rôle', has a kohierartikel / article de rôle number and a formal appeal deadline; applies to VenB, PB, or other taxes."

        // ═══════════════════════════════════════════════════════════════════
        // TAX - PERSONAL
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.PersonalTax ->
            "Annual personal income tax return for an individual — 'Personenbelasting / PB / Impôt des personnes physiques / IPP', " +
                    "filed via Tax-on-web; covers all income types including sole-trader (eenmanszaak) business profit."

        DocumentType.WithholdingTax ->
            "Document showing tax withheld at source from employment income — 'Bedrijfsvoorheffing / BV / Précompte professionnel', " +
                    "fiche 281.10 format, shows gross payment, withheld tax, and net; NOT the monthly salary slip itself (→ SALARY_SLIP)."

        // ═══════════════════════════════════════════════════════════════════
        // SOCIAL CONTRIBUTIONS (Belgium)
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.SocialContribution ->
            "Employer's quarterly RSZ/ONSS social security declaration for employees — " +
                    "'Sociale bijdragen werkgever / Cotisations patronales ONSS', based on employee gross wages; NOT for self-employed (→ SELF_EMPLOYED_CONTRIBUTION)."

        DocumentType.SocialFund ->
            "Invoice FOR payroll administration services from a social secretariat (Securex, Acerta, Liantis, Partena, SD Worx, etc.) — " +
                    "this is an INVOICE for a service fee, not a direct social security payment."

        DocumentType.SelfEmployedContribution ->
            "Social security contribution document addressed TO a self-employed person — " +
                    "issued by a Belgian social insurance fund ('sociaal verzekeringsfonds / caisse d'assurances sociales') " +
                    "such as Acerta, Liantis, Zenito, Xerius, Partena, Securex, Group S, UCM, Multipen, or AVIXI. " +
                    "Key signals: 'Sociale bijdragen zelfstandige / Cotisations sociales indépendant', quarterly period reference, " +
                    "income-based contribution amount, IBAN + structured payment reference (+++xxx/xxxx/xxxxx+++), payment deadline. " +
                    "Also covers REGULARISATIEBERICHT (recalculation notice) and provisional/definitive contribution variants — " +
                    "all result in a payment due and are NOT CREDIT_NOTE."

        DocumentType.Vapz ->
            "Premium notice for a self-employed supplementary pension (VAPZ / PLCI) from an insurer (AG Insurance, KBC, NN, Vivium…) — " +
                    "tax-deductible voluntary pension saving; NOT mandatory social security (→ SELF_EMPLOYED_CONTRIBUTION)."

        // ═══════════════════════════════════════════════════════════════════
        // PAYROLL / HR
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.SalarySlip ->
            "Monthly pay statement for ONE employee showing the gross → net calculation — " +
                    "'Loonbrief / Loonstrook / Fiche de paie', deducts RSZ (~13.07%) and bedrijfsvoorheffing, for employees only (NOT self-employed)."

        DocumentType.PayrollSummary ->
            "Employer-level aggregated overview of ALL employees' wages and total labour cost for a period — " +
                    "'Loonoverzicht / Récapitulatif des salaires', usually produced by the social secretariat; NOT a single employee slip (→ SALARY_SLIP)."

        DocumentType.EmploymentContract ->
            "Signed legal agreement defining the terms of employment between employer and employee — " +
                    "'Arbeidsovereenkomst / Contrat de travail', states function, salary, working hours, contract duration, and has two signatures."

        DocumentType.Dimona ->
            "Belgian administrative employment notification submitted to RSZ confirming a hire (IN) or departure (OUT) — " +
                    "shows employee national register number, RSZ employer number, and an official confirmation reference number."

        DocumentType.C4 ->
            "Official end-of-employment certificate issued for unemployment benefits application — " +
                    "official RVA/ONEM 'Formulier C4', states employment period, reason for termination, and last salary details."

        DocumentType.HolidayPay ->
            "Annual vacation pay payout statement — 'Vakantiegeld / Pécule de vacances', shows enkel and/or dubbel vakantiegeld, " +
                    "reference year and days accrued; separate from the regular monthly salary slip."

        // ═══════════════════════════════════════════════════════════════════
        // LEGAL / CONTRACTS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Contract ->
            "General written agreement between two business parties defining rights and obligations — " +
                    "'Overeenkomst / Samenwerkingsovereenkomst / Contrat / Convention', has signatures; NOT employment (→ EMPLOYMENT_CONTRACT), rental (→ LEASE), or loan (→ LOAN)."

        DocumentType.Lease ->
            "Rental or leasing agreement for property or equipment — 'Huurovereenkomst / Bail / Leasing', " +
                    "identifies verhuurder (landlord) and huurder (tenant), states monthly rent, deposit (waarborg), and duration."

        DocumentType.Loan ->
            "Loan or credit agreement showing borrowed capital being repaid — 'Lening / Prêt / Krediet / Darlehen', " +
                    "has interest rate, amortization/repayment schedule, remaining balance; NOT a lease (→ LEASE) and NOT a bank statement (→ BANK_STATEMENT)."

        DocumentType.Insurance ->
            "Insurance policy or periodic premium notice from an insurer (AG Insurance, AXA, Belfius, KBC, Ethias, Baloise, NN, Allianz…) — " +
                    "has policy number, coverage description, and premium amount; NOT a VAPZ pension policy (→ VAPZ)."

        // ═══════════════════════════════════════════════════════════════════
        // CORPORATE DOCUMENTS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Dividend ->
            "Profit distribution statement to a shareholder — 'Dividend / Winstuitkering / Dividenduitkering', " +
                    "shows gross dividend per share, 30% roerende voorheffing / précompte mobilier withheld, and net dividend paid."

        DocumentType.ShareholderRegister ->
            "Official corporate register listing all shareholders, their share counts, and transfer history — " +
                    "'Aandeelhoudersregister / Registre des actionnaires'; a governance record, not a financial transaction."

        DocumentType.CompanyExtract ->
            "Official extract from the Belgian KBO/BCE company register — 'KBO-uittreksel / Extrait BCE', " +
                    "shows ondernemingsnummer (0xxx.xxx.xxx), legal form, registered address, NACE activity codes, and legal representatives."

        DocumentType.AnnualAccounts ->
            "Statutory annual financial statements filed with the National Bank of Belgium (NBB/BNB) — " +
                    "'Jaarrekening / Comptes annuels', standardised balance sheet (activa/passiva) + income statement (resultatenrekening); may include auditor report."

        DocumentType.BoardMinutes ->
            "Written minutes of a board of directors or general assembly meeting — " +
                    "'Notulen / PV / Procès-verbal', lists attendees, agenda items, resolutions voted, and is signed by the chairman/secretary."

        // ═══════════════════════════════════════════════════════════════════
        // GOVERNMENT / REGULATORY
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Subsidy ->
            "Government grant approval or payment notice from an agency (VLAIO, Flanders Innovation, Wallonia, Brussels Region, EU funds) — " +
                    "'Subsidie / Subvention / Prime', references a specific project, states the granted amount and any conditions attached."

        DocumentType.Fine ->
            "Penalty notice from an official authority (police, parket, FOD Financiën, RSZ…) — " +
                    "'Boete / Amende / Bußgeld', describes the infraction, states payment deadline, and mentions an appeal procedure."

        DocumentType.Permit ->
            "Official license or authorisation issued by a regulatory body — " +
                    "'Vergunning / Permis / Licentie / Agrément', has a permit number, specifies what is authorised, and states a validity period."

        // ═══════════════════════════════════════════════════════════════════
        // INTERNATIONAL TRADE
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.CustomsDeclaration ->
            "Official import/export customs form for non-EU trade — " +
                    "'Douaneaangifte / Déclaration en douane', SAD/DAU format, contains HS/tariff codes, country of origin/destination, and calculated duties."

        DocumentType.Intrastat ->
            "Monthly EU goods movement statistical report for intra-EU trade above the threshold — " +
                    "'Intrastat', shows arrivals (aankomst) or dispatches (verzending), CN8 goods codes, net mass, and statistical value; purely statistical, no payment."

        // ═══════════════════════════════════════════════════════════════════
        // ASSETS
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.DepreciationSchedule ->
            "Accounting table of fixed assets being depreciated over time — " +
                    "'Afschrijvingstabel / Tableau d'amortissement', shows each asset's acquisition cost, annual depreciation amount, accumulated depreciation, and net book value."

        DocumentType.Inventory ->
            "Stock list recording goods on hand at a specific point in time with quantities and values — " +
                    "'Inventaris / Inventaire', date of count, locations; a snapshot document, not a transaction or order."

        // ═══════════════════════════════════════════════════════════════════
        // CATCH-ALL
        // ═══════════════════════════════════════════════════════════════════

        DocumentType.Other ->
            "A clearly identifiable business document that does not fit any specific category above — " +
                    "e.g. internal memos, company policies, management reports, presentations, or general business correspondence."

        DocumentType.Unknown ->
            "Document is unreadable (poor scan quality, truncated, corrupt), non-business (personal photo, spam), or completely unidentifiable. " +
                    "Use OTHER if it is recognisably a business document of an unlisted type."
    }