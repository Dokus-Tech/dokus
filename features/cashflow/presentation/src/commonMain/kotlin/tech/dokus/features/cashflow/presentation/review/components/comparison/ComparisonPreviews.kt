package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.review.models.LineItemUiData
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val PreviewSize = Modifier.width(1200.dp).height(800.dp)

private fun previewExistingInvoice() = DocumentUiData.Invoice(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "INV-2026-0005",
    issueDate = "2025-01-09",
    dueDate = "2025-01-08",
    subtotalAmount = Money(5700),
    vatAmount = Money(1197),
    totalAmount = Money(6898),
    currencySign = "€",
    lineItems = listOf(
        LineItemUiData("HP 304XL Cartridge Black", "€37.99"),
        LineItemUiData("Tefal Included KI5338", "€30.99"),
    ),
    notes = null,
    iban = null,
    primaryDescription = "Battery chargers, power adapters",
)

private fun previewIncomingInvoice() = DocumentUiData.Invoice(
    direction = DocumentDirection.Inbound,
    invoiceNumber = "INV-2026-0005",
    issueDate = "2026-02-14",
    dueDate = null,
    subtotalAmount = Money(23884),
    vatAmount = Money(5016),
    totalAmount = Money(28900),
    currencySign = "€",
    lineItems = listOf(
        LineItemUiData("Samsung ViewFinity S8 S80UD LS27D800AUXEN", "€238.84"),
        LineItemUiData("Incl. Recupel: 2.2 Monitors", "€1.40"),
    ),
    notes = null,
    iban = null,
    primaryDescription = "Battery chargers, power adapters",
)

// ═══════════════════════════════════════════════════════════════════
// FULL COMPARISON PANE
// ═══════════════════════════════════════════════════════════════════

@Preview
@Composable
private fun ComparisonMaterialConflictPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingUiData = previewExistingInvoice(),
            incomingUiData = previewIncomingInvoice(),
            existingCounterpartyName = "Coolblue België NV",
            incomingCounterpartyName = "COOLBLUE BELGIË",
            reasonType = ReviewReason.MaterialConflict,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun ComparisonFuzzyMatchPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingUiData = previewExistingInvoice(),
            incomingUiData = previewIncomingInvoice(),
            existingCounterpartyName = "Coolblue België NV",
            incomingCounterpartyName = "COOLBLUE BELGIË",
            reasonType = ReviewReason.FuzzyCandidate,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun ComparisonResolvingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingUiData = previewExistingInvoice(),
            incomingUiData = previewIncomingInvoice(),
            existingCounterpartyName = "Coolblue België NV",
            incomingCounterpartyName = "COOLBLUE BELGIË",
            reasonType = ReviewReason.MaterialConflict,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = true,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun ComparisonNoIncomingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentComparisonPane(
            existingUiData = previewExistingInvoice(),
            incomingUiData = null,
            existingCounterpartyName = "Coolblue België NV",
            incomingCounterpartyName = "",
            reasonType = ReviewReason.FuzzyCandidate,
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
            modifier = PreviewSize,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// COMPONENT PREVIEWS
// ═══════════════════════════════════════════════════════════════════

@Preview
@Composable
private fun ReasonBannerMaterialConflictPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonReasonBanner(reasonType = ReviewReason.MaterialConflict)
    }
}

@Preview
@Composable
private fun ReasonBannerFuzzyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonReasonBanner(reasonType = ReviewReason.FuzzyCandidate)
    }
}

@Preview
@Composable
private fun ComparisonActionBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonActionBar(
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = false,
        )
    }
}

@Preview
@Composable
private fun ComparisonActionBarResolvingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ComparisonActionBar(
            onSameDocument = {},
            onDifferentDocument = {},
            isResolving = true,
        )
    }
}
