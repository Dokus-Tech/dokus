package ai.dokus.app.cashflow.viewmodel

import ai.dokus.foundation.design.Res
import ai.dokus.foundation.design.delivery_email_description
import ai.dokus.foundation.design.delivery_export_pdf
import ai.dokus.foundation.design.delivery_pdf_description
import ai.dokus.foundation.design.delivery_peppol_description
import ai.dokus.foundation.design.delivery_peppol_warning
import ai.dokus.foundation.design.delivery_send_email
import ai.dokus.foundation.design.delivery_send_peppol
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource

/**
 * Sealed interface representing available delivery method options for invoices.
 * Each case is a data class that can hold contextual information like warnings.
 */
sealed interface DeliveryMethodOption {
    val hasWarning: Boolean

    /**
     * Export invoice as PDF file.
     */
    data class PdfExport(
        override val hasWarning: Boolean = false
    ) : DeliveryMethodOption

    /**
     * Send invoice via Peppol e-invoicing network.
     */
    data class Peppol(
        override val hasWarning: Boolean = false
    ) : DeliveryMethodOption

    /**
     * Send invoice via email.
     */
    data class Email(
        override val hasWarning: Boolean = false
    ) : DeliveryMethodOption

    companion object {
        /**
         * Returns all available delivery method options.
         * @param showPeppolWarning Whether to show warning on Peppol option
         */
        fun all(showPeppolWarning: Boolean = false): List<DeliveryMethodOption> = listOf(
            PdfExport(),
            Peppol(hasWarning = showPeppolWarning),
            Email()
        )
    }
}

// =============================================================================
// Extension Properties
// =============================================================================

/**
 * Returns the localized title for this delivery method.
 */
val DeliveryMethodOption.localized: String
    @Composable get() = when (this) {
        is DeliveryMethodOption.PdfExport -> stringResource(Res.string.delivery_export_pdf)
        is DeliveryMethodOption.Peppol -> stringResource(Res.string.delivery_send_peppol)
        is DeliveryMethodOption.Email -> stringResource(Res.string.delivery_send_email)
    }

/**
 * Returns the icon for this delivery method.
 */
val DeliveryMethodOption.iconized: ImageVector
    @Composable get() = when (this) {
        is DeliveryMethodOption.PdfExport -> Icons.Default.PictureAsPdf
        is DeliveryMethodOption.Peppol -> Icons.AutoMirrored.Filled.Send
        is DeliveryMethodOption.Email -> Icons.Default.Email
    }

/**
 * Returns the localized description for this delivery method.
 * The description may vary based on context (e.g., warnings).
 */
val DeliveryMethodOption.localizedDescription: String
    @Composable get() = when (this) {
        is DeliveryMethodOption.PdfExport -> stringResource(Res.string.delivery_pdf_description)
        is DeliveryMethodOption.Peppol -> if (hasWarning) {
            stringResource(Res.string.delivery_peppol_warning)
        } else {
            stringResource(Res.string.delivery_peppol_description)
        }
        is DeliveryMethodOption.Email -> stringResource(Res.string.delivery_email_description)
    }

/**
 * Returns whether this delivery method is currently enabled.
 * Disabled methods are shown with "Coming soon" badge.
 */
val DeliveryMethodOption.isEnabled: Boolean
    get() = when (this) {
        is DeliveryMethodOption.PdfExport -> true
        is DeliveryMethodOption.Peppol -> false
        is DeliveryMethodOption.Email -> false
    }

/**
 * Returns whether this delivery method should show "Coming soon" badge.
 * This is true when the method is not enabled.
 */
val DeliveryMethodOption.isComingSoon: Boolean
    get() = !isEnabled

/**
 * Maps this DeliveryMethodOption to the corresponding InvoiceDeliveryMethod enum.
 */
val DeliveryMethodOption.deliveryMethod: InvoiceDeliveryMethod
    get() = when (this) {
        is DeliveryMethodOption.PdfExport -> InvoiceDeliveryMethod.PDF_EXPORT
        is DeliveryMethodOption.Peppol -> InvoiceDeliveryMethod.PEPPOL
        is DeliveryMethodOption.Email -> InvoiceDeliveryMethod.EMAIL
    }

/**
 * Creates a DeliveryMethodOption from an InvoiceDeliveryMethod enum value.
 */
fun InvoiceDeliveryMethod.toOption(hasWarning: Boolean = false): DeliveryMethodOption = when (this) {
    InvoiceDeliveryMethod.PDF_EXPORT -> DeliveryMethodOption.PdfExport()
    InvoiceDeliveryMethod.PEPPOL -> DeliveryMethodOption.Peppol(hasWarning = hasWarning)
    InvoiceDeliveryMethod.EMAIL -> DeliveryMethodOption.Email()
}
