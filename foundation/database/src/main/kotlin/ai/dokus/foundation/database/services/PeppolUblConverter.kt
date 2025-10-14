package ai.dokus.foundation.database.services

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import kotlinx.datetime.toJavaLocalDate
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

/**
 * Peppol UBL 2.1 Invoice Converter
 * Converts Dokus invoices to UBL 2.1 XML format for Peppol e-invoicing
 * Complies with Peppol BIS Billing 3.0 specification (Belgium 2026 requirement)
 *
 * Reference: https://docs.peppol.eu/poacc/billing/3.0/
 */
class PeppolUblConverter {
    private val logger = LoggerFactory.getLogger(PeppolUblConverter::class.java)

    companion object {
        const val UBL_VERSION = "2.1"
        const val CUSTOMIZATION_ID = "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0"
        const val PROFILE_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"

        // Belgium-specific codes
        const val COUNTRY_CODE_BELGIUM = "BE"
        const val CURRENCY_EUR = "EUR"
        const val TAX_SCHEME_VAT = "VAT"

        // Invoice type codes
        const val INVOICE_TYPE_COMMERCIAL = "380"  // Commercial invoice
        const val INVOICE_TYPE_CREDIT_NOTE = "381" // Credit note
    }

    /**
     * Convert a Dokus invoice to Peppol UBL 2.1 XML format
     */
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    fun convertToUbl(
        invoice: Invoice,
        supplier: TenantInfo,
        customer: Client,
        lineItems: List<InvoiceItem>
    ): String {
        logger.info("Converting invoice ${invoice.invoiceNumber.value} to UBL 2.1 format")

        val outputFactory = XMLOutputFactory.newInstance()
        val stringWriter = StringWriter()
        val writer = outputFactory.createXMLStreamWriter(stringWriter)

        writer.writeStartDocument("UTF-8", "1.0")

        // Root element with namespaces
        writer.writeStartElement("Invoice")
        writer.writeNamespace("", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2")
        writer.writeNamespace("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2")
        writer.writeNamespace("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2")

        // Header information
        writeElement(writer, "cbc:CustomizationID", CUSTOMIZATION_ID)
        writeElement(writer, "cbc:ProfileID", PROFILE_ID)
        writeElement(writer, "cbc:ID", invoice.invoiceNumber.value)
        writeElement(writer, "cbc:IssueDate", invoice.issueDate.toJavaLocalDate().format(DateTimeFormatter.ISO_DATE))
        writeElement(writer, "cbc:DueDate", invoice.dueDate.toJavaLocalDate().format(DateTimeFormatter.ISO_DATE))
        writeElement(writer, "cbc:InvoiceTypeCode", INVOICE_TYPE_COMMERCIAL)
        invoice.notes?.let { writeElement(writer, "cbc:Note", it) }
        writeElement(writer, "cbc:DocumentCurrencyCode", CURRENCY_EUR)

        // Invoice period (optional - representing billing period)
        // Using issue date and due date as invoice period
        writer.writeStartElement("cac:InvoicePeriod")
        writeElement(writer, "cbc:StartDate", invoice.issueDate.toJavaLocalDate().format(DateTimeFormatter.ISO_DATE))
        writeElement(writer, "cbc:EndDate", invoice.dueDate.toJavaLocalDate().format(DateTimeFormatter.ISO_DATE))
        writer.writeEndElement() // InvoicePeriod

        // Supplier (AccountingSupplierParty)
        writeSupplierParty(writer, supplier)

        // Customer (AccountingCustomerParty)
        writeCustomerParty(writer, customer)

        // Payment means (if specified)
        writePaymentMeans(writer, invoice)

        // Tax total
        writeTaxTotal(writer, invoice, lineItems)

        // Legal monetary total
        writeLegalMonetaryTotal(writer, invoice)

        // Invoice lines
        lineItems.forEachIndexed { index, lineItem ->
            writeInvoiceLine(writer, lineItem, index + 1)
        }

        writer.writeEndElement() // Invoice
        writer.writeEndDocument()
        writer.flush()
        writer.close()

        val xml = stringWriter.toString()
        logger.info("Successfully converted invoice ${invoice.invoiceNumber.value} to UBL format (${xml.length} bytes)")

        return xml
    }

    private fun writeSupplierParty(writer: XMLStreamWriter, supplier: TenantInfo) {
        writer.writeStartElement("cac:AccountingSupplierParty")
        writer.writeStartElement("cac:Party")

        // Peppol endpoint ID
        val peppolIdValue = supplier.peppolId
        if (peppolIdValue != null) {
            writer.writeStartElement("cbc:EndpointID")
            writer.writeAttribute("schemeID", "0208") // Belgian business number scheme
            writer.writeCharacters(peppolIdValue)
            writer.writeEndElement()
        }

        // Party identification (VAT number)
        val vatNumberValue = supplier.vatNumber
        if (vatNumberValue != null) {
            writer.writeStartElement("cac:PartyIdentification")
            writeElement(writer, "cbc:ID", vatNumberValue)
            writer.writeEndElement()
        }

        // Party name
        writer.writeStartElement("cac:PartyName")
        writeElement(writer, "cbc:Name", supplier.companyName)
        writer.writeEndElement()

        // Postal address
        val addressValue = supplier.address
        if (addressValue != null) {
            writePostalAddress(writer, addressValue)
        }

        // Party tax scheme
        if (vatNumberValue != null) {
            writer.writeStartElement("cac:PartyTaxScheme")
            writeElement(writer, "cbc:CompanyID", vatNumberValue)
            writer.writeStartElement("cac:TaxScheme")
            writeElement(writer, "cbc:ID", TAX_SCHEME_VAT)
            writer.writeEndElement() // TaxScheme
            writer.writeEndElement() // PartyTaxScheme
        }

        // Party legal entity
        writer.writeStartElement("cac:PartyLegalEntity")
        writeElement(writer, "cbc:RegistrationName", supplier.companyName)
        val companyNumberValue = supplier.companyNumber
        if (companyNumberValue != null) {
            writeElement(writer, "cbc:CompanyID", companyNumberValue)
        }
        writer.writeEndElement() // PartyLegalEntity

        // Contact
        val emailValue = supplier.email
        val phoneValue = supplier.phone
        if (emailValue != null || phoneValue != null) {
            writer.writeStartElement("cac:Contact")
            emailValue?.let { writeElement(writer, "cbc:ElectronicMail", it) }
            phoneValue?.let { writeElement(writer, "cbc:Telephone", it) }
            writer.writeEndElement() // Contact
        }

        writer.writeEndElement() // Party
        writer.writeEndElement() // AccountingSupplierParty
    }

    private fun writeCustomerParty(writer: XMLStreamWriter, customer: Client) {
        writer.writeStartElement("cac:AccountingCustomerParty")
        writer.writeStartElement("cac:Party")

        // Peppol endpoint ID
        val peppolIdValue = customer.peppolId
        if (peppolIdValue != null) {
            writer.writeStartElement("cbc:EndpointID")
            writer.writeAttribute("schemeID", "0208")
            writer.writeCharacters(peppolIdValue)
            writer.writeEndElement()
        }

        // Party identification
        val vatNumberValue = customer.vatNumber
        if (vatNumberValue != null) {
            writer.writeStartElement("cac:PartyIdentification")
            writeElement(writer, "cbc:ID", vatNumberValue.value)
            writer.writeEndElement()
        }

        // Party name
        writer.writeStartElement("cac:PartyName")
        writeElement(writer, "cbc:Name", customer.name)
        writer.writeEndElement()

        // Postal address
        writePostalAddress(
            writer,
            Address(
                streetName = customer.addressLine1 ?: "",
                cityName = customer.city ?: "",
                postalZone = customer.postalCode ?: "",
                countryCode = customer.country ?: COUNTRY_CODE_BELGIUM
            )
        )

        // Party tax scheme
        if (vatNumberValue != null) {
            writer.writeStartElement("cac:PartyTaxScheme")
            writeElement(writer, "cbc:CompanyID", vatNumberValue.value)
            writer.writeStartElement("cac:TaxScheme")
            writeElement(writer, "cbc:ID", TAX_SCHEME_VAT)
            writer.writeEndElement()
            writer.writeEndElement()
        }

        // Party legal entity
        writer.writeStartElement("cac:PartyLegalEntity")
        writeElement(writer, "cbc:RegistrationName", customer.name)
        val companyNumberValue = customer.companyNumber
        if (companyNumberValue != null) {
            writeElement(writer, "cbc:CompanyID", companyNumberValue)
        }
        writer.writeEndElement()

        writer.writeEndElement() // Party
        writer.writeEndElement() // AccountingCustomerParty
    }

    private fun writePostalAddress(writer: XMLStreamWriter, address: Address) {
        writer.writeStartElement("cac:PostalAddress")
        if (address.streetName.isNotEmpty()) {
            writeElement(writer, "cbc:StreetName", address.streetName)
        }
        if (address.cityName.isNotEmpty()) {
            writeElement(writer, "cbc:CityName", address.cityName)
        }
        if (address.postalZone.isNotEmpty()) {
            writeElement(writer, "cbc:PostalZone", address.postalZone)
        }
        writer.writeStartElement("cac:Country")
        writeElement(writer, "cbc:IdentificationCode", address.countryCode)
        writer.writeEndElement() // Country
        writer.writeEndElement() // PostalAddress
    }

    private fun writePaymentMeans(writer: XMLStreamWriter, invoice: Invoice) {
        writer.writeStartElement("cac:PaymentMeans")
        writeElement(writer, "cbc:PaymentMeansCode", "30") // Credit transfer

        // Payment instructions (using terms and conditions if available)
        invoice.termsAndConditions?.let {
            writeElement(writer, "cbc:InstructionNote", it)
        }

        writer.writeEndElement() // PaymentMeans
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun writeTaxTotal(writer: XMLStreamWriter, invoice: Invoice, lineItems: List<InvoiceItem>) {
        writer.writeStartElement("cac:TaxTotal")

        // Total tax amount
        writeElement(writer, "cbc:TaxAmount", invoice.vatAmount.value, "currencyID", CURRENCY_EUR)

        // Group by VAT rate
        val taxByRate = lineItems.groupBy { it.vatRate }

        taxByRate.forEach { (vatRate, items) ->
            val taxableAmount = items.sumOf { BigDecimal(it.lineTotal.value) }
            val taxAmount = items.sumOf { BigDecimal(it.vatAmount.value) }

            writer.writeStartElement("cac:TaxSubtotal")
            writeElement(writer, "cbc:TaxableAmount", taxableAmount.toString(), "currencyID", CURRENCY_EUR)
            writeElement(writer, "cbc:TaxAmount", taxAmount.toString(), "currencyID", CURRENCY_EUR)

            writer.writeStartElement("cac:TaxCategory")
            writeElement(writer, "cbc:ID", "S") // Standard rate
            writeElement(writer, "cbc:Percent", vatRate.value)
            writer.writeStartElement("cac:TaxScheme")
            writeElement(writer, "cbc:ID", TAX_SCHEME_VAT)
            writer.writeEndElement() // TaxScheme
            writer.writeEndElement() // TaxCategory
            writer.writeEndElement() // TaxSubtotal
        }

        writer.writeEndElement() // TaxTotal
    }

    private fun writeLegalMonetaryTotal(writer: XMLStreamWriter, invoice: Invoice) {
        writer.writeStartElement("cac:LegalMonetaryTotal")
        writeElement(writer, "cbc:LineExtensionAmount", invoice.subtotalAmount.value, "currencyID", CURRENCY_EUR)
        writeElement(writer, "cbc:TaxExclusiveAmount", invoice.subtotalAmount.value, "currencyID", CURRENCY_EUR)
        writeElement(writer, "cbc:TaxInclusiveAmount", invoice.totalAmount.value, "currencyID", CURRENCY_EUR)
        writeElement(writer, "cbc:PayableAmount", invoice.totalAmount.value, "currencyID", CURRENCY_EUR)
        writer.writeEndElement() // LegalMonetaryTotal
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun writeInvoiceLine(writer: XMLStreamWriter, lineItem: InvoiceItem, lineNumber: Int) {
        writer.writeStartElement("cac:InvoiceLine")

        writeElement(writer, "cbc:ID", lineNumber.toString())
        writeElement(writer, "cbc:InvoicedQuantity", lineItem.quantity.value, "unitCode", "C62") // C62 = piece
        writeElement(writer, "cbc:LineExtensionAmount", lineItem.lineTotal.value, "currencyID", CURRENCY_EUR)

        // Item
        writer.writeStartElement("cac:Item")
        writeElement(writer, "cbc:Name", lineItem.description)

        // Tax category
        writer.writeStartElement("cac:ClassifiedTaxCategory")
        writeElement(writer, "cbc:ID", "S") // Standard rate
        writeElement(writer, "cbc:Percent", lineItem.vatRate.value)
        writer.writeStartElement("cac:TaxScheme")
        writeElement(writer, "cbc:ID", TAX_SCHEME_VAT)
        writer.writeEndElement() // TaxScheme
        writer.writeEndElement() // ClassifiedTaxCategory

        writer.writeEndElement() // Item

        // Price
        writer.writeStartElement("cac:Price")
        writeElement(writer, "cbc:PriceAmount", lineItem.unitPrice.value, "currencyID", CURRENCY_EUR)
        writer.writeEndElement() // Price

        writer.writeEndElement() // InvoiceLine
    }

    private fun writeElement(writer: XMLStreamWriter, name: String, value: String, vararg attributes: String) {
        writer.writeStartElement(name)

        // Write attributes in pairs
        var i = 0
        while (i < attributes.size - 1) {
            writer.writeAttribute(attributes[i], attributes[i + 1])
            i += 2
        }

        writer.writeCharacters(value)
        writer.writeEndElement()
    }
}

/**
 * Tenant information for Peppol invoicing
 */
data class TenantInfo(
    val companyName: String,
    val vatNumber: String?,
    val companyNumber: String?,
    val peppolId: String?,
    val email: String?,
    val phone: String?,
    val address: Address?
)

/**
 * Address information
 */
data class Address(
    val streetName: String,
    val cityName: String,
    val postalZone: String,
    val countryCode: String
)

/**
 * Peppol invoice validation result
 */
data class PeppolValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Peppol UBL validator
 * Validates UBL XML against Peppol BIS 3.0 rules
 */
class PeppolUblValidator {
    private val logger = LoggerFactory.getLogger(PeppolUblValidator::class.java)

    /**
     * Validate UBL XML against basic Peppol rules
     * TODO: Implement full Schematron validation for production
     */
    fun validate(ublXml: String): PeppolValidationResult {
        logger.info("Validating UBL XML (${ublXml.length} bytes)")

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Basic XML validation
        if (!ublXml.contains("<Invoice")) {
            errors.add("Missing Invoice root element")
        }

        // Required elements check
        val requiredElements = listOf(
            "cbc:CustomizationID",
            "cbc:ProfileID",
            "cbc:ID",
            "cbc:IssueDate",
            "cac:AccountingSupplierParty",
            "cac:AccountingCustomerParty",
            "cac:LegalMonetaryTotal"
        )

        requiredElements.forEach { element ->
            if (!ublXml.contains(element)) {
                errors.add("Missing required element: $element")
            }
        }

        // Belgium-specific validation
        if (!ublXml.contains("BE")) {
            warnings.add("No Belgian country code found")
        }

        // VAT validation
        if (!ublXml.contains("<cbc:ID>VAT</cbc:ID>")) {
            errors.add("Missing VAT tax scheme")
        }

        logger.info("Validation complete: ${errors.size} errors, ${warnings.size} warnings")

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
