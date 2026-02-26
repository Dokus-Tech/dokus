package tech.dokus.backend.services.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import java.io.ByteArrayOutputStream

private const val PdfTopMargin = 780f
private const val PdfBottomMargin = 48f
private const val PdfLeftMargin = 48f
private const val PdfLineHeight = 16f
private const val PdfBodySize = 10f
private const val PdfHeadingSize = 20f
private const val PdfSectionSpacing = 10f

class InvoicePdfService(
    private val documentStorageService: DocumentStorageService
) {
    private val logger = loggerFor()
    private val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val headingFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    suspend fun generateAndUploadPdf(
        invoice: FinancialDocumentDto.InvoiceDto,
        contactDisplayName: String?
    ): Result<String> = runCatching {
        val bytes = renderPdf(invoice, contactDisplayName)
        val upload = documentStorageService.uploadDocument(
            tenantId = invoice.tenantId,
            prefix = "invoice-pdf-exports",
            filename = "${invoice.invoiceNumber}.pdf",
            data = bytes,
            contentType = "application/pdf"
        )
        upload.url
    }.onFailure { error ->
        logger.error("Failed to generate PDF for invoice {}", invoice.id, error)
    }

    private fun renderPdf(
        invoice: FinancialDocumentDto.InvoiceDto,
        contactDisplayName: String?
    ): ByteArray {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            PDPageContentStream(document, page).use { content ->
                var cursorY = PdfTopMargin

                cursorY = writeLine(
                    content = content,
                    font = headingFont,
                    fontSize = PdfHeadingSize,
                    text = "Invoice ${invoice.invoiceNumber}",
                    y = cursorY
                )

                cursorY -= PdfSectionSpacing
                cursorY = writeLine(content, bodyFont, PdfBodySize, "Issue date: ${invoice.issueDate}", cursorY)
                cursorY = writeLine(content, bodyFont, PdfBodySize, "Due date: ${invoice.dueDate}", cursorY)
                cursorY = writeLine(
                    content,
                    bodyFont,
                    PdfBodySize,
                    "Bill to: ${contactDisplayName ?: invoice.contactId}",
                    cursorY
                )

                invoice.senderIban?.let {
                    cursorY = writeLine(content, bodyFont, PdfBodySize, "Sender IBAN: ${it.value}", cursorY)
                }
                invoice.senderBic?.let {
                    cursorY = writeLine(content, bodyFont, PdfBodySize, "Sender BIC: ${it.value}", cursorY)
                }
                invoice.structuredCommunication?.let {
                    cursorY = writeLine(content, bodyFont, PdfBodySize, "Structured ref: ${it.value}", cursorY)
                }

                cursorY -= PdfSectionSpacing
                cursorY = writeLine(content, headingFont, 12f, "Line items", cursorY)

                if (invoice.items.isEmpty()) {
                    cursorY = writeLine(content, bodyFont, PdfBodySize, "No line items", cursorY)
                } else {
                    invoice.items.forEach { item ->
                        if (cursorY <= PdfBottomMargin) {
                            return@forEach
                        }
                        val row = buildString {
                            append(item.description.take(42))
                            append(" | qty ")
                            append(item.quantity)
                            append(" | unit ")
                            append(item.unitPrice)
                            append(" | total ")
                            append(item.lineTotal)
                        }
                        cursorY = writeLine(content, bodyFont, PdfBodySize, row, cursorY)
                    }
                }

                cursorY -= PdfSectionSpacing
                cursorY = writeLine(content, headingFont, 12f, "Totals", cursorY)
                cursorY = writeLine(content, bodyFont, PdfBodySize, "Subtotal: ${invoice.subtotalAmount}", cursorY)
                cursorY = writeLine(content, bodyFont, PdfBodySize, "VAT: ${invoice.vatAmount}", cursorY)
                cursorY = writeLine(content, bodyFont, 11f, "Total: ${invoice.totalAmount}", cursorY)

                invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    cursorY -= PdfSectionSpacing
                    cursorY = writeLine(content, headingFont, 12f, "Notes", cursorY)
                    notes.split("\n").forEach { line ->
                        if (cursorY > PdfBottomMargin) {
                            cursorY = writeLine(content, bodyFont, PdfBodySize, line.take(100), cursorY)
                        }
                    }
                }
            }

            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }

    private fun writeLine(
        content: PDPageContentStream,
        font: PDType1Font,
        fontSize: Float,
        text: String,
        y: Float
    ): Float {
        content.beginText()
        content.setFont(font, fontSize)
        content.newLineAtOffset(PdfLeftMargin, y)
        content.showText(safePdfText(text))
        content.endText()
        return y - PdfLineHeight
    }

    private fun safePdfText(text: String): String {
        return text
            .replace(Regex("[^\\x20-\\x7E]"), "?")
            .ifBlank { "-" }
    }
}
