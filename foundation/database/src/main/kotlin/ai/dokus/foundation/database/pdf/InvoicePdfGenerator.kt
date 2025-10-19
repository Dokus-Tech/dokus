package ai.dokus.foundation.database.pdf

import ai.dokus.foundation.domain.model.Invoice
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * Invoice PDF Generator using iText 7
 * Generates Belgian-compliant invoices in PDF format
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class InvoicePdfGenerator {
    private val logger = LoggerFactory.getLogger(InvoicePdfGenerator::class.java)

    // Brand colors
    private val primaryColor = DeviceRgb(41, 128, 185) // #2980b9
    private val darkGray = DeviceRgb(52, 73, 94) // #34495e
    private val lightGray = DeviceRgb(236, 240, 241) // #ecf0f1

    /**
     * Generates a PDF for an invoice
     *
     * @param invoice The invoice to generate PDF for
     * @param companyName Company name (TODO: fetch from tenant settings)
     * @param companyAddress Company address (TODO: fetch from tenant settings)
     * @param companyVat Company VAT number (TODO: fetch from tenant settings)
     * @return PDF content as ByteArray
     */
    fun generate(
        invoice: Invoice,
        companyName: String = "Your Company Name",
        companyAddress: String = "Your Company Address\nCity, ZIP Code\nBelgium",
        companyVat: String = "BE0123456789"
    ): ByteArray {
        logger.info("Generating PDF for invoice ${invoice.invoiceNumber.value}")

        val outputStream = ByteArrayOutputStream()

        try {
            val writer = PdfWriter(outputStream)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)

            // Set document margins
            document.setMargins(40f, 40f, 40f, 40f)

            // Fonts
            val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
            val normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)

            // Header Section
            addHeader(document, companyName, companyAddress, companyVat, boldFont, normalFont)

            // Invoice Title
            val title = Paragraph("INVOICE")
                .setFont(boldFont)
                .setFontSize(28f)
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f)
                .setMarginBottom(20f)

            document.add(title)

            // Invoice Info Section
            addInvoiceInfo(document, invoice, boldFont, normalFont)

            // Line Items Table
            addLineItemsTable(document, invoice, boldFont, normalFont)

            // Totals Section
            addTotalsSection(document, invoice, boldFont, normalFont)

            // Notes Section
            invoice.notes?.let { notes ->
                addNotesSection(document, notes, boldFont, normalFont)
            }

            // Footer
            addFooter(document, normalFont)

            document.close()

            logger.info("PDF generated successfully for invoice ${invoice.invoiceNumber.value} (${outputStream.size()} bytes)")

            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Failed to generate PDF for invoice ${invoice.invoiceNumber.value}", e)
            throw RuntimeException("Failed to generate invoice PDF: ${e.message}", e)
        }
    }

    private fun addHeader(
        document: Document,
        companyName: String,
        companyAddress: String,
        companyVat: String,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Company name
        val companyNamePara = Paragraph(companyName)
            .setFont(boldFont)
            .setFontSize(18f)
            .setFontColor(darkGray)

        document.add(companyNamePara)

        // Company details
        val companyDetails = Paragraph(companyAddress)
            .setFont(normalFont)
            .setFontSize(10f)
            .setFontColor(darkGray)
            .setMarginBottom(2f)

        document.add(companyDetails)

        val vatPara = Paragraph("VAT: $companyVat")
            .setFont(normalFont)
            .setFontSize(10f)
            .setFontColor(darkGray)
            .setMarginBottom(15f)

        document.add(vatPara)
    }

    private fun addInvoiceInfo(
        document: Document,
        invoice: Invoice,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Create table for invoice info (2 columns)
        val infoTable = Table(floatArrayOf(1f, 1f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)

        // Left column - Invoice details
        val leftCell = Cell()
            .setBorder(Border.NO_BORDER)
            .add(createInfoParagraph("Invoice Number:", invoice.invoiceNumber.value, boldFont, normalFont))
            .add(createInfoParagraph("Issue Date:", invoice.issueDate.toString(), boldFont, normalFont))
            .add(createInfoParagraph("Due Date:", invoice.dueDate.toString(), boldFont, normalFont))
            .add(createInfoParagraph("Status:", invoice.status.name, boldFont, normalFont))

        // Right column - Client details
        val rightCell = Cell()
            .setBorder(Border.NO_BORDER)
            .add(Paragraph("Bill To:").setFont(boldFont).setFontSize(12f).setFontColor(darkGray))
            .add(Paragraph("Client ID: ${invoice.clientId.value}").setFont(normalFont).setFontSize(10f).setMarginTop(5f))
        // TODO: Add actual client details when ClientService is integrated

        infoTable.addCell(leftCell)
        infoTable.addCell(rightCell)

        document.add(infoTable)
    }

    private fun createInfoParagraph(
        label: String,
        value: String,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ): Paragraph {
        return Paragraph()
            .setFontSize(10f)
            .setMarginBottom(3f)
            .add(com.itextpdf.layout.element.Text(label).setFont(boldFont).setFontColor(darkGray))
            .add(com.itextpdf.layout.element.Text(" $value").setFont(normalFont).setFontColor(darkGray))
    }

    private fun addLineItemsTable(
        document: Document,
        invoice: Invoice,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Table with 5 columns: Description, Quantity, Unit Price, VAT Rate, Total
        val table = Table(floatArrayOf(3f, 1f, 1.5f, 1f, 1.5f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(15f)

        // Header row
        val headerColor = lightGray
        addTableHeaderCell(table, "Description", boldFont, headerColor)
        addTableHeaderCell(table, "Quantity", boldFont, headerColor)
        addTableHeaderCell(table, "Unit Price", boldFont, headerColor)
        addTableHeaderCell(table, "VAT Rate", boldFont, headerColor)
        addTableHeaderCell(table, "Total", boldFont, headerColor)

        // Data rows
        invoice.items.forEach { item ->
            val lineTotal = BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)

            addTableDataCell(table, item.description, normalFont)
            addTableDataCell(table, item.quantity.value, normalFont, TextAlignment.CENTER)
            addTableDataCell(table, "€ ${item.unitPrice.value}", normalFont, TextAlignment.RIGHT)
            addTableDataCell(table, "${item.vatRate.value}%", normalFont, TextAlignment.CENTER)
            addTableDataCell(table, "€ ${formatMoney(lineTotal)}", normalFont, TextAlignment.RIGHT)
        }

        document.add(table)
    }

    private fun addTableHeaderCell(
        table: Table,
        text: String,
        font: com.itextpdf.kernel.font.PdfFont,
        bgColor: DeviceRgb
    ) {
        val cell = Cell()
            .add(Paragraph(text).setFont(font).setFontSize(11f).setFontColor(darkGray))
            .setBackgroundColor(bgColor)
            .setPadding(8f)
            .setTextAlignment(TextAlignment.LEFT)

        table.addHeaderCell(cell)
    }

    private fun addTableDataCell(
        table: Table,
        text: String,
        font: com.itextpdf.kernel.font.PdfFont,
        alignment: TextAlignment = TextAlignment.LEFT
    ) {
        val cell = Cell()
            .add(Paragraph(text).setFont(font).setFontSize(10f))
            .setPadding(8f)
            .setTextAlignment(alignment)
            .setBorder(SolidBorder(lightGray, 0.5f))

        table.addCell(cell)
    }

    private fun addTotalsSection(
        document: Document,
        invoice: Invoice,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Create totals table aligned to the right
        val totalsTable = Table(floatArrayOf(1f, 1f))
            .setWidth(UnitValue.createPercentValue(40f))
            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT)
            .setMarginBottom(20f)

        // Subtotal
        addTotalRow(totalsTable, "Subtotal:", "€ ${invoice.subtotalAmount.value}", normalFont, false)

        // VAT Amount
        addTotalRow(totalsTable, "VAT Amount:", "€ ${invoice.vatAmount.value}", normalFont, false)

        // Total
        addTotalRow(totalsTable, "TOTAL:", "€ ${invoice.totalAmount.value}", boldFont, true)

        // Paid Amount
        if (BigDecimal(invoice.paidAmount.value) > BigDecimal.ZERO) {
            addTotalRow(totalsTable, "Paid:", "€ ${invoice.paidAmount.value}", normalFont, false)

            val remaining = BigDecimal(invoice.totalAmount.value) - BigDecimal(invoice.paidAmount.value)
            addTotalRow(totalsTable, "Amount Due:", "€ ${formatMoney(remaining)}", boldFont, true)
        }

        document.add(totalsTable)
    }

    private fun addTotalRow(
        table: Table,
        label: String,
        value: String,
        font: com.itextpdf.kernel.font.PdfFont,
        isBold: Boolean
    ) {
        val labelCell = Cell()
            .add(Paragraph(label).setFont(font).setFontSize(if (isBold) 12f else 10f))
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)
            .setTextAlignment(TextAlignment.RIGHT)

        val valueCell = Cell()
            .add(Paragraph(value).setFont(font).setFontSize(if (isBold) 12f else 10f))
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)
            .setTextAlignment(TextAlignment.RIGHT)

        if (isBold) {
            labelCell.setBackgroundColor(lightGray)
            valueCell.setBackgroundColor(lightGray)
        }

        table.addCell(labelCell)
        table.addCell(valueCell)
    }

    private fun addNotesSection(
        document: Document,
        notes: String,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val notesTitle = Paragraph("Notes:")
            .setFont(boldFont)
            .setFontSize(12f)
            .setFontColor(darkGray)
            .setMarginBottom(5f)

        document.add(notesTitle)

        val notesPara = Paragraph(notes)
            .setFont(normalFont)
            .setFontSize(10f)
            .setFontColor(darkGray)
            .setMarginBottom(20f)

        document.add(notesPara)
    }

    private fun addFooter(
        document: Document,
        normalFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val footer = Paragraph("Thank you for your business!")
            .setFont(normalFont)
            .setFontSize(10f)
            .setFontColor(darkGray)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30f)

        document.add(footer)
    }

    private fun formatMoney(amount: BigDecimal): String {
        return String.format("%.2f", amount)
    }
}
