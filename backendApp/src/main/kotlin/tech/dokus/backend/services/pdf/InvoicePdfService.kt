package tech.dokus.backend.services.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.storage.UploadResult
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

    suspend fun generateAndUploadPdf(
        invoice: FinancialDocumentDto.InvoiceDto,
        contactDisplayName: String
    ): Result<UploadResult> = runCatching {
        val bytes = renderPdf(invoice, contactDisplayName)
        documentStorageService.uploadDocument(
            tenantId = invoice.tenantId,
            prefix = "invoice-pdf-exports",
            filename = "${invoice.invoiceNumber}.pdf",
            data = bytes,
            contentType = "application/pdf"
        )
    }.onFailure { error ->
        logger.error("Failed to generate PDF for invoice {}", invoice.id, error)
    }

    private fun renderPdf(
        invoice: FinancialDocumentDto.InvoiceDto,
        contactDisplayName: String
    ): ByteArray {
        PDDocument().use { document ->
            val bodyFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            val headingFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
            val writer = PdfWriter(document, bodyFont, headingFont)

            writer.writeLine(headingFont, PdfHeadingSize, "Invoice ${invoice.invoiceNumber}")
            writer.addSpacing()
            writer.writeLine(bodyFont, PdfBodySize, "Issue date: ${invoice.issueDate}")
            writer.writeLine(bodyFont, PdfBodySize, "Due date: ${invoice.dueDate}")
            writer.writeLine(bodyFont, PdfBodySize, "Bill to: $contactDisplayName")

            invoice.senderIban?.let {
                writer.writeLine(bodyFont, PdfBodySize, "Sender IBAN: ${it.value}")
            }
            invoice.senderBic?.let {
                writer.writeLine(bodyFont, PdfBodySize, "Sender BIC: ${it.value}")
            }
            invoice.structuredCommunication?.let {
                writer.writeLine(bodyFont, PdfBodySize, "Structured ref: ${it.value}")
            }

            writer.addSpacing()
            writer.writeLine(headingFont, 12f, "Line items")

            if (invoice.items.isEmpty()) {
                writer.writeLine(bodyFont, PdfBodySize, "No line items")
            } else {
                invoice.items.forEach { item ->
                    val row = buildString {
                        append(item.description.take(42))
                        append(" | qty ")
                        append(item.quantity)
                        append(" | unit ")
                        append(item.unitPrice)
                        append(" | total ")
                        append(item.lineTotal)
                    }
                    writer.writeLine(bodyFont, PdfBodySize, row)
                }
            }

            writer.addSpacing()
            writer.writeLine(headingFont, 12f, "Totals")
            writer.writeLine(bodyFont, PdfBodySize, "Subtotal: ${invoice.subtotalAmount}")
            writer.writeLine(bodyFont, PdfBodySize, "VAT: ${invoice.vatAmount}")
            writer.writeLine(bodyFont, 11f, "Total: ${invoice.totalAmount}")

            invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                writer.addSpacing()
                writer.writeLine(headingFont, 12f, "Notes")
                notes.split("\n").forEach { line ->
                    writer.writeLine(bodyFont, PdfBodySize, line.take(100))
                }
            }

            writer.close()

            return ByteArrayOutputStream().use { output ->
                document.save(output)
                output.toByteArray()
            }
        }
    }
}

private class PdfWriter(
    private val document: PDDocument,
    private val bodyFont: PDType1Font,
    private val headingFont: PDType1Font
) {
    private var cursorY = PdfTopMargin
    private var contentStream: PDPageContentStream = newPage()

    fun writeLine(font: PDType1Font, fontSize: Float, text: String) {
        if (cursorY <= PdfBottomMargin) {
            contentStream.close()
            contentStream = newPage()
        }
        contentStream.beginText()
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(PdfLeftMargin, cursorY)
        contentStream.showText(safePdfText(text))
        contentStream.endText()
        cursorY -= maxOf(PdfLineHeight, fontSize * 1.4f)
    }

    fun addSpacing() {
        cursorY -= PdfSectionSpacing
    }

    fun close() {
        contentStream.close()
    }

    private fun newPage(): PDPageContentStream {
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)
        cursorY = PdfTopMargin
        return PDPageContentStream(document, page)
    }

    private fun safePdfText(text: String): String {
        return text
            .replace(Regex("[^\\x20-\\x7E]"), "?")
            .ifBlank { "-" }
    }
}
