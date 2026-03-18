@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@file:Suppress("TooManyFunctions")

package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.*
import tech.dokus.database.tables.drafts.*
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.toKotlinUuid

// =============================================================================
// ProForma
// =============================================================================

fun ProFormaDraftEntity.Companion.from(row: ResultRow): ProFormaDraftEntity = ProFormaDraftEntity(
    id = row[ProFormaDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ProFormaDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ProFormaDraftsTable.documentId].toKotlinUuid()),
    direction = row[ProFormaDraftsTable.direction],
    createdAt = row[ProFormaDraftsTable.createdAt],
    updatedAt = row[ProFormaDraftsTable.updatedAt],
)

fun ProFormaConfirmedEntity.Companion.from(row: ResultRow): ProFormaConfirmedEntity = ProFormaConfirmedEntity(
    id = row[ProFormaConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ProFormaConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ProFormaConfirmedTable.documentId].toKotlinUuid()),
    direction = row[ProFormaConfirmedTable.direction],
    createdAt = row[ProFormaConfirmedTable.createdAt],
    updatedAt = row[ProFormaConfirmedTable.updatedAt],
)

// =============================================================================
// Quote
// =============================================================================

fun QuoteDraftEntity.Companion.from(row: ResultRow): QuoteDraftEntity = QuoteDraftEntity(
    id = row[QuoteDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[QuoteDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[QuoteDraftsTable.documentId].toKotlinUuid()),
    direction = row[QuoteDraftsTable.direction],
    createdAt = row[QuoteDraftsTable.createdAt],
    updatedAt = row[QuoteDraftsTable.updatedAt],
)

fun QuoteConfirmedEntity.Companion.from(row: ResultRow): QuoteConfirmedEntity = QuoteConfirmedEntity(
    id = row[QuoteConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[QuoteConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[QuoteConfirmedTable.documentId].toKotlinUuid()),
    direction = row[QuoteConfirmedTable.direction],
    createdAt = row[QuoteConfirmedTable.createdAt],
    updatedAt = row[QuoteConfirmedTable.updatedAt],
)

// =============================================================================
// OrderConfirmation
// =============================================================================

fun OrderConfirmationDraftEntity.Companion.from(row: ResultRow): OrderConfirmationDraftEntity = OrderConfirmationDraftEntity(
    id = row[OrderConfirmationDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OrderConfirmationDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OrderConfirmationDraftsTable.documentId].toKotlinUuid()),
    direction = row[OrderConfirmationDraftsTable.direction],
    createdAt = row[OrderConfirmationDraftsTable.createdAt],
    updatedAt = row[OrderConfirmationDraftsTable.updatedAt],
)

fun OrderConfirmationConfirmedEntity.Companion.from(row: ResultRow): OrderConfirmationConfirmedEntity = OrderConfirmationConfirmedEntity(
    id = row[OrderConfirmationConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OrderConfirmationConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OrderConfirmationConfirmedTable.documentId].toKotlinUuid()),
    direction = row[OrderConfirmationConfirmedTable.direction],
    createdAt = row[OrderConfirmationConfirmedTable.createdAt],
    updatedAt = row[OrderConfirmationConfirmedTable.updatedAt],
)

// =============================================================================
// DeliveryNote
// =============================================================================

fun DeliveryNoteDraftEntity.Companion.from(row: ResultRow): DeliveryNoteDraftEntity = DeliveryNoteDraftEntity(
    id = row[DeliveryNoteDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DeliveryNoteDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DeliveryNoteDraftsTable.documentId].toKotlinUuid()),
    direction = row[DeliveryNoteDraftsTable.direction],
    createdAt = row[DeliveryNoteDraftsTable.createdAt],
    updatedAt = row[DeliveryNoteDraftsTable.updatedAt],
)

fun DeliveryNoteConfirmedEntity.Companion.from(row: ResultRow): DeliveryNoteConfirmedEntity = DeliveryNoteConfirmedEntity(
    id = row[DeliveryNoteConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DeliveryNoteConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DeliveryNoteConfirmedTable.documentId].toKotlinUuid()),
    direction = row[DeliveryNoteConfirmedTable.direction],
    createdAt = row[DeliveryNoteConfirmedTable.createdAt],
    updatedAt = row[DeliveryNoteConfirmedTable.updatedAt],
)

// =============================================================================
// Reminder
// =============================================================================

fun ReminderDraftEntity.Companion.from(row: ResultRow): ReminderDraftEntity = ReminderDraftEntity(
    id = row[ReminderDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ReminderDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ReminderDraftsTable.documentId].toKotlinUuid()),
    direction = row[ReminderDraftsTable.direction],
    createdAt = row[ReminderDraftsTable.createdAt],
    updatedAt = row[ReminderDraftsTable.updatedAt],
)

fun ReminderConfirmedEntity.Companion.from(row: ResultRow): ReminderConfirmedEntity = ReminderConfirmedEntity(
    id = row[ReminderConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ReminderConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ReminderConfirmedTable.documentId].toKotlinUuid()),
    direction = row[ReminderConfirmedTable.direction],
    createdAt = row[ReminderConfirmedTable.createdAt],
    updatedAt = row[ReminderConfirmedTable.updatedAt],
)

// =============================================================================
// StatementOfAccount
// =============================================================================

fun StatementOfAccountDraftEntity.Companion.from(row: ResultRow): StatementOfAccountDraftEntity = StatementOfAccountDraftEntity(
    id = row[StatementOfAccountDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[StatementOfAccountDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[StatementOfAccountDraftsTable.documentId].toKotlinUuid()),
    direction = row[StatementOfAccountDraftsTable.direction],
    createdAt = row[StatementOfAccountDraftsTable.createdAt],
    updatedAt = row[StatementOfAccountDraftsTable.updatedAt],
)

fun StatementOfAccountConfirmedEntity.Companion.from(row: ResultRow): StatementOfAccountConfirmedEntity = StatementOfAccountConfirmedEntity(
    id = row[StatementOfAccountConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[StatementOfAccountConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[StatementOfAccountConfirmedTable.documentId].toKotlinUuid()),
    direction = row[StatementOfAccountConfirmedTable.direction],
    createdAt = row[StatementOfAccountConfirmedTable.createdAt],
    updatedAt = row[StatementOfAccountConfirmedTable.updatedAt],
)

// =============================================================================
// PurchaseOrder
// =============================================================================

fun PurchaseOrderDraftEntity.Companion.from(row: ResultRow): PurchaseOrderDraftEntity = PurchaseOrderDraftEntity(
    id = row[PurchaseOrderDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PurchaseOrderDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PurchaseOrderDraftsTable.documentId].toKotlinUuid()),
    direction = row[PurchaseOrderDraftsTable.direction],
    createdAt = row[PurchaseOrderDraftsTable.createdAt],
    updatedAt = row[PurchaseOrderDraftsTable.updatedAt],
)

fun PurchaseOrderConfirmedEntity.Companion.from(row: ResultRow): PurchaseOrderConfirmedEntity = PurchaseOrderConfirmedEntity(
    id = row[PurchaseOrderConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PurchaseOrderConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PurchaseOrderConfirmedTable.documentId].toKotlinUuid()),
    direction = row[PurchaseOrderConfirmedTable.direction],
    createdAt = row[PurchaseOrderConfirmedTable.createdAt],
    updatedAt = row[PurchaseOrderConfirmedTable.updatedAt],
)

// =============================================================================
// ExpenseClaim
// =============================================================================

fun ExpenseClaimDraftEntity.Companion.from(row: ResultRow): ExpenseClaimDraftEntity = ExpenseClaimDraftEntity(
    id = row[ExpenseClaimDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ExpenseClaimDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ExpenseClaimDraftsTable.documentId].toKotlinUuid()),
    direction = row[ExpenseClaimDraftsTable.direction],
    createdAt = row[ExpenseClaimDraftsTable.createdAt],
    updatedAt = row[ExpenseClaimDraftsTable.updatedAt],
)

fun ExpenseClaimConfirmedEntity.Companion.from(row: ResultRow): ExpenseClaimConfirmedEntity = ExpenseClaimConfirmedEntity(
    id = row[ExpenseClaimConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ExpenseClaimConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ExpenseClaimConfirmedTable.documentId].toKotlinUuid()),
    direction = row[ExpenseClaimConfirmedTable.direction],
    createdAt = row[ExpenseClaimConfirmedTable.createdAt],
    updatedAt = row[ExpenseClaimConfirmedTable.updatedAt],
)

// =============================================================================
// BankFee
// =============================================================================

fun BankFeeDraftEntity.Companion.from(row: ResultRow): BankFeeDraftEntity = BankFeeDraftEntity(
    id = row[BankFeeDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BankFeeDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[BankFeeDraftsTable.documentId].toKotlinUuid()),
    direction = row[BankFeeDraftsTable.direction],
    createdAt = row[BankFeeDraftsTable.createdAt],
    updatedAt = row[BankFeeDraftsTable.updatedAt],
)

fun BankFeeConfirmedEntity.Companion.from(row: ResultRow): BankFeeConfirmedEntity = BankFeeConfirmedEntity(
    id = row[BankFeeConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BankFeeConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[BankFeeConfirmedTable.documentId].toKotlinUuid()),
    direction = row[BankFeeConfirmedTable.direction],
    createdAt = row[BankFeeConfirmedTable.createdAt],
    updatedAt = row[BankFeeConfirmedTable.updatedAt],
)

// =============================================================================
// InterestStatement
// =============================================================================

fun InterestStatementDraftEntity.Companion.from(row: ResultRow): InterestStatementDraftEntity = InterestStatementDraftEntity(
    id = row[InterestStatementDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InterestStatementDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InterestStatementDraftsTable.documentId].toKotlinUuid()),
    direction = row[InterestStatementDraftsTable.direction],
    createdAt = row[InterestStatementDraftsTable.createdAt],
    updatedAt = row[InterestStatementDraftsTable.updatedAt],
)

fun InterestStatementConfirmedEntity.Companion.from(row: ResultRow): InterestStatementConfirmedEntity = InterestStatementConfirmedEntity(
    id = row[InterestStatementConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InterestStatementConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InterestStatementConfirmedTable.documentId].toKotlinUuid()),
    direction = row[InterestStatementConfirmedTable.direction],
    createdAt = row[InterestStatementConfirmedTable.createdAt],
    updatedAt = row[InterestStatementConfirmedTable.updatedAt],
)

// =============================================================================
// PaymentConfirmation
// =============================================================================

fun PaymentConfirmationDraftEntity.Companion.from(row: ResultRow): PaymentConfirmationDraftEntity = PaymentConfirmationDraftEntity(
    id = row[PaymentConfirmationDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PaymentConfirmationDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PaymentConfirmationDraftsTable.documentId].toKotlinUuid()),
    direction = row[PaymentConfirmationDraftsTable.direction],
    createdAt = row[PaymentConfirmationDraftsTable.createdAt],
    updatedAt = row[PaymentConfirmationDraftsTable.updatedAt],
)

fun PaymentConfirmationConfirmedEntity.Companion.from(row: ResultRow): PaymentConfirmationConfirmedEntity = PaymentConfirmationConfirmedEntity(
    id = row[PaymentConfirmationConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PaymentConfirmationConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PaymentConfirmationConfirmedTable.documentId].toKotlinUuid()),
    direction = row[PaymentConfirmationConfirmedTable.direction],
    createdAt = row[PaymentConfirmationConfirmedTable.createdAt],
    updatedAt = row[PaymentConfirmationConfirmedTable.updatedAt],
)

// =============================================================================
// VatReturn
// =============================================================================

fun VatReturnDraftEntity.Companion.from(row: ResultRow): VatReturnDraftEntity = VatReturnDraftEntity(
    id = row[VatReturnDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatReturnDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatReturnDraftsTable.documentId].toKotlinUuid()),
    direction = row[VatReturnDraftsTable.direction],
    createdAt = row[VatReturnDraftsTable.createdAt],
    updatedAt = row[VatReturnDraftsTable.updatedAt],
)

fun VatReturnConfirmedEntity.Companion.from(row: ResultRow): VatReturnConfirmedEntity = VatReturnConfirmedEntity(
    id = row[VatReturnConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatReturnConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatReturnConfirmedTable.documentId].toKotlinUuid()),
    direction = row[VatReturnConfirmedTable.direction],
    createdAt = row[VatReturnConfirmedTable.createdAt],
    updatedAt = row[VatReturnConfirmedTable.updatedAt],
)

// =============================================================================
// VatListing
// =============================================================================

fun VatListingDraftEntity.Companion.from(row: ResultRow): VatListingDraftEntity = VatListingDraftEntity(
    id = row[VatListingDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatListingDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatListingDraftsTable.documentId].toKotlinUuid()),
    direction = row[VatListingDraftsTable.direction],
    createdAt = row[VatListingDraftsTable.createdAt],
    updatedAt = row[VatListingDraftsTable.updatedAt],
)

fun VatListingConfirmedEntity.Companion.from(row: ResultRow): VatListingConfirmedEntity = VatListingConfirmedEntity(
    id = row[VatListingConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatListingConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatListingConfirmedTable.documentId].toKotlinUuid()),
    direction = row[VatListingConfirmedTable.direction],
    createdAt = row[VatListingConfirmedTable.createdAt],
    updatedAt = row[VatListingConfirmedTable.updatedAt],
)

// =============================================================================
// VatAssessment
// =============================================================================

fun VatAssessmentDraftEntity.Companion.from(row: ResultRow): VatAssessmentDraftEntity = VatAssessmentDraftEntity(
    id = row[VatAssessmentDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatAssessmentDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatAssessmentDraftsTable.documentId].toKotlinUuid()),
    direction = row[VatAssessmentDraftsTable.direction],
    createdAt = row[VatAssessmentDraftsTable.createdAt],
    updatedAt = row[VatAssessmentDraftsTable.updatedAt],
)

fun VatAssessmentConfirmedEntity.Companion.from(row: ResultRow): VatAssessmentConfirmedEntity = VatAssessmentConfirmedEntity(
    id = row[VatAssessmentConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VatAssessmentConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VatAssessmentConfirmedTable.documentId].toKotlinUuid()),
    direction = row[VatAssessmentConfirmedTable.direction],
    createdAt = row[VatAssessmentConfirmedTable.createdAt],
    updatedAt = row[VatAssessmentConfirmedTable.updatedAt],
)

// =============================================================================
// IcListing
// =============================================================================

fun IcListingDraftEntity.Companion.from(row: ResultRow): IcListingDraftEntity = IcListingDraftEntity(
    id = row[IcListingDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[IcListingDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[IcListingDraftsTable.documentId].toKotlinUuid()),
    direction = row[IcListingDraftsTable.direction],
    createdAt = row[IcListingDraftsTable.createdAt],
    updatedAt = row[IcListingDraftsTable.updatedAt],
)

fun IcListingConfirmedEntity.Companion.from(row: ResultRow): IcListingConfirmedEntity = IcListingConfirmedEntity(
    id = row[IcListingConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[IcListingConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[IcListingConfirmedTable.documentId].toKotlinUuid()),
    direction = row[IcListingConfirmedTable.direction],
    createdAt = row[IcListingConfirmedTable.createdAt],
    updatedAt = row[IcListingConfirmedTable.updatedAt],
)

// =============================================================================
// OssReturn
// =============================================================================

fun OssReturnDraftEntity.Companion.from(row: ResultRow): OssReturnDraftEntity = OssReturnDraftEntity(
    id = row[OssReturnDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OssReturnDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OssReturnDraftsTable.documentId].toKotlinUuid()),
    direction = row[OssReturnDraftsTable.direction],
    createdAt = row[OssReturnDraftsTable.createdAt],
    updatedAt = row[OssReturnDraftsTable.updatedAt],
)

fun OssReturnConfirmedEntity.Companion.from(row: ResultRow): OssReturnConfirmedEntity = OssReturnConfirmedEntity(
    id = row[OssReturnConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OssReturnConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OssReturnConfirmedTable.documentId].toKotlinUuid()),
    direction = row[OssReturnConfirmedTable.direction],
    createdAt = row[OssReturnConfirmedTable.createdAt],
    updatedAt = row[OssReturnConfirmedTable.updatedAt],
)

// =============================================================================
// CorporateTax
// =============================================================================

fun CorporateTaxDraftEntity.Companion.from(row: ResultRow): CorporateTaxDraftEntity = CorporateTaxDraftEntity(
    id = row[CorporateTaxDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CorporateTaxDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CorporateTaxDraftsTable.documentId].toKotlinUuid()),
    direction = row[CorporateTaxDraftsTable.direction],
    createdAt = row[CorporateTaxDraftsTable.createdAt],
    updatedAt = row[CorporateTaxDraftsTable.updatedAt],
)

fun CorporateTaxConfirmedEntity.Companion.from(row: ResultRow): CorporateTaxConfirmedEntity = CorporateTaxConfirmedEntity(
    id = row[CorporateTaxConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CorporateTaxConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CorporateTaxConfirmedTable.documentId].toKotlinUuid()),
    direction = row[CorporateTaxConfirmedTable.direction],
    createdAt = row[CorporateTaxConfirmedTable.createdAt],
    updatedAt = row[CorporateTaxConfirmedTable.updatedAt],
)

// =============================================================================
// CorporateTaxAdvance
// =============================================================================

fun CorporateTaxAdvanceDraftEntity.Companion.from(row: ResultRow): CorporateTaxAdvanceDraftEntity = CorporateTaxAdvanceDraftEntity(
    id = row[CorporateTaxAdvanceDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CorporateTaxAdvanceDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CorporateTaxAdvanceDraftsTable.documentId].toKotlinUuid()),
    direction = row[CorporateTaxAdvanceDraftsTable.direction],
    createdAt = row[CorporateTaxAdvanceDraftsTable.createdAt],
    updatedAt = row[CorporateTaxAdvanceDraftsTable.updatedAt],
)

fun CorporateTaxAdvanceConfirmedEntity.Companion.from(row: ResultRow): CorporateTaxAdvanceConfirmedEntity = CorporateTaxAdvanceConfirmedEntity(
    id = row[CorporateTaxAdvanceConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CorporateTaxAdvanceConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CorporateTaxAdvanceConfirmedTable.documentId].toKotlinUuid()),
    direction = row[CorporateTaxAdvanceConfirmedTable.direction],
    createdAt = row[CorporateTaxAdvanceConfirmedTable.createdAt],
    updatedAt = row[CorporateTaxAdvanceConfirmedTable.updatedAt],
)

// =============================================================================
// TaxAssessment
// =============================================================================

fun TaxAssessmentDraftEntity.Companion.from(row: ResultRow): TaxAssessmentDraftEntity = TaxAssessmentDraftEntity(
    id = row[TaxAssessmentDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[TaxAssessmentDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[TaxAssessmentDraftsTable.documentId].toKotlinUuid()),
    direction = row[TaxAssessmentDraftsTable.direction],
    createdAt = row[TaxAssessmentDraftsTable.createdAt],
    updatedAt = row[TaxAssessmentDraftsTable.updatedAt],
)

fun TaxAssessmentConfirmedEntity.Companion.from(row: ResultRow): TaxAssessmentConfirmedEntity = TaxAssessmentConfirmedEntity(
    id = row[TaxAssessmentConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[TaxAssessmentConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[TaxAssessmentConfirmedTable.documentId].toKotlinUuid()),
    direction = row[TaxAssessmentConfirmedTable.direction],
    createdAt = row[TaxAssessmentConfirmedTable.createdAt],
    updatedAt = row[TaxAssessmentConfirmedTable.updatedAt],
)

// =============================================================================
// PersonalTax
// =============================================================================

fun PersonalTaxDraftEntity.Companion.from(row: ResultRow): PersonalTaxDraftEntity = PersonalTaxDraftEntity(
    id = row[PersonalTaxDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PersonalTaxDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PersonalTaxDraftsTable.documentId].toKotlinUuid()),
    direction = row[PersonalTaxDraftsTable.direction],
    createdAt = row[PersonalTaxDraftsTable.createdAt],
    updatedAt = row[PersonalTaxDraftsTable.updatedAt],
)

fun PersonalTaxConfirmedEntity.Companion.from(row: ResultRow): PersonalTaxConfirmedEntity = PersonalTaxConfirmedEntity(
    id = row[PersonalTaxConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PersonalTaxConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PersonalTaxConfirmedTable.documentId].toKotlinUuid()),
    direction = row[PersonalTaxConfirmedTable.direction],
    createdAt = row[PersonalTaxConfirmedTable.createdAt],
    updatedAt = row[PersonalTaxConfirmedTable.updatedAt],
)

// =============================================================================
// WithholdingTax
// =============================================================================

fun WithholdingTaxDraftEntity.Companion.from(row: ResultRow): WithholdingTaxDraftEntity = WithholdingTaxDraftEntity(
    id = row[WithholdingTaxDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[WithholdingTaxDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[WithholdingTaxDraftsTable.documentId].toKotlinUuid()),
    direction = row[WithholdingTaxDraftsTable.direction],
    createdAt = row[WithholdingTaxDraftsTable.createdAt],
    updatedAt = row[WithholdingTaxDraftsTable.updatedAt],
)

fun WithholdingTaxConfirmedEntity.Companion.from(row: ResultRow): WithholdingTaxConfirmedEntity = WithholdingTaxConfirmedEntity(
    id = row[WithholdingTaxConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[WithholdingTaxConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[WithholdingTaxConfirmedTable.documentId].toKotlinUuid()),
    direction = row[WithholdingTaxConfirmedTable.direction],
    createdAt = row[WithholdingTaxConfirmedTable.createdAt],
    updatedAt = row[WithholdingTaxConfirmedTable.updatedAt],
)

// =============================================================================
// SocialContribution
// =============================================================================

fun SocialContributionDraftEntity.Companion.from(row: ResultRow): SocialContributionDraftEntity = SocialContributionDraftEntity(
    id = row[SocialContributionDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SocialContributionDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SocialContributionDraftsTable.documentId].toKotlinUuid()),
    direction = row[SocialContributionDraftsTable.direction],
    createdAt = row[SocialContributionDraftsTable.createdAt],
    updatedAt = row[SocialContributionDraftsTable.updatedAt],
)

fun SocialContributionConfirmedEntity.Companion.from(row: ResultRow): SocialContributionConfirmedEntity = SocialContributionConfirmedEntity(
    id = row[SocialContributionConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SocialContributionConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SocialContributionConfirmedTable.documentId].toKotlinUuid()),
    direction = row[SocialContributionConfirmedTable.direction],
    createdAt = row[SocialContributionConfirmedTable.createdAt],
    updatedAt = row[SocialContributionConfirmedTable.updatedAt],
)

// =============================================================================
// SocialFund
// =============================================================================

fun SocialFundDraftEntity.Companion.from(row: ResultRow): SocialFundDraftEntity = SocialFundDraftEntity(
    id = row[SocialFundDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SocialFundDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SocialFundDraftsTable.documentId].toKotlinUuid()),
    direction = row[SocialFundDraftsTable.direction],
    createdAt = row[SocialFundDraftsTable.createdAt],
    updatedAt = row[SocialFundDraftsTable.updatedAt],
)

fun SocialFundConfirmedEntity.Companion.from(row: ResultRow): SocialFundConfirmedEntity = SocialFundConfirmedEntity(
    id = row[SocialFundConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SocialFundConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SocialFundConfirmedTable.documentId].toKotlinUuid()),
    direction = row[SocialFundConfirmedTable.direction],
    createdAt = row[SocialFundConfirmedTable.createdAt],
    updatedAt = row[SocialFundConfirmedTable.updatedAt],
)

// =============================================================================
// SelfEmployedContribution
// =============================================================================

fun SelfEmployedContributionDraftEntity.Companion.from(row: ResultRow): SelfEmployedContributionDraftEntity = SelfEmployedContributionDraftEntity(
    id = row[SelfEmployedContributionDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SelfEmployedContributionDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SelfEmployedContributionDraftsTable.documentId].toKotlinUuid()),
    direction = row[SelfEmployedContributionDraftsTable.direction],
    createdAt = row[SelfEmployedContributionDraftsTable.createdAt],
    updatedAt = row[SelfEmployedContributionDraftsTable.updatedAt],
)

fun SelfEmployedContributionConfirmedEntity.Companion.from(row: ResultRow): SelfEmployedContributionConfirmedEntity = SelfEmployedContributionConfirmedEntity(
    id = row[SelfEmployedContributionConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SelfEmployedContributionConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SelfEmployedContributionConfirmedTable.documentId].toKotlinUuid()),
    direction = row[SelfEmployedContributionConfirmedTable.direction],
    createdAt = row[SelfEmployedContributionConfirmedTable.createdAt],
    updatedAt = row[SelfEmployedContributionConfirmedTable.updatedAt],
)

// =============================================================================
// Vapz
// =============================================================================

fun VapzDraftEntity.Companion.from(row: ResultRow): VapzDraftEntity = VapzDraftEntity(
    id = row[VapzDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VapzDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VapzDraftsTable.documentId].toKotlinUuid()),
    direction = row[VapzDraftsTable.direction],
    createdAt = row[VapzDraftsTable.createdAt],
    updatedAt = row[VapzDraftsTable.updatedAt],
)

fun VapzConfirmedEntity.Companion.from(row: ResultRow): VapzConfirmedEntity = VapzConfirmedEntity(
    id = row[VapzConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[VapzConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[VapzConfirmedTable.documentId].toKotlinUuid()),
    direction = row[VapzConfirmedTable.direction],
    createdAt = row[VapzConfirmedTable.createdAt],
    updatedAt = row[VapzConfirmedTable.updatedAt],
)

// =============================================================================
// SalarySlip
// =============================================================================

fun SalarySlipDraftEntity.Companion.from(row: ResultRow): SalarySlipDraftEntity = SalarySlipDraftEntity(
    id = row[SalarySlipDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SalarySlipDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SalarySlipDraftsTable.documentId].toKotlinUuid()),
    direction = row[SalarySlipDraftsTable.direction],
    createdAt = row[SalarySlipDraftsTable.createdAt],
    updatedAt = row[SalarySlipDraftsTable.updatedAt],
)

fun SalarySlipConfirmedEntity.Companion.from(row: ResultRow): SalarySlipConfirmedEntity = SalarySlipConfirmedEntity(
    id = row[SalarySlipConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SalarySlipConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SalarySlipConfirmedTable.documentId].toKotlinUuid()),
    direction = row[SalarySlipConfirmedTable.direction],
    createdAt = row[SalarySlipConfirmedTable.createdAt],
    updatedAt = row[SalarySlipConfirmedTable.updatedAt],
)

// =============================================================================
// PayrollSummary
// =============================================================================

fun PayrollSummaryDraftEntity.Companion.from(row: ResultRow): PayrollSummaryDraftEntity = PayrollSummaryDraftEntity(
    id = row[PayrollSummaryDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PayrollSummaryDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PayrollSummaryDraftsTable.documentId].toKotlinUuid()),
    direction = row[PayrollSummaryDraftsTable.direction],
    createdAt = row[PayrollSummaryDraftsTable.createdAt],
    updatedAt = row[PayrollSummaryDraftsTable.updatedAt],
)

fun PayrollSummaryConfirmedEntity.Companion.from(row: ResultRow): PayrollSummaryConfirmedEntity = PayrollSummaryConfirmedEntity(
    id = row[PayrollSummaryConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PayrollSummaryConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PayrollSummaryConfirmedTable.documentId].toKotlinUuid()),
    direction = row[PayrollSummaryConfirmedTable.direction],
    createdAt = row[PayrollSummaryConfirmedTable.createdAt],
    updatedAt = row[PayrollSummaryConfirmedTable.updatedAt],
)

// =============================================================================
// EmploymentContract
// =============================================================================

fun EmploymentContractDraftEntity.Companion.from(row: ResultRow): EmploymentContractDraftEntity = EmploymentContractDraftEntity(
    id = row[EmploymentContractDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[EmploymentContractDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[EmploymentContractDraftsTable.documentId].toKotlinUuid()),
    direction = row[EmploymentContractDraftsTable.direction],
    createdAt = row[EmploymentContractDraftsTable.createdAt],
    updatedAt = row[EmploymentContractDraftsTable.updatedAt],
)

fun EmploymentContractConfirmedEntity.Companion.from(row: ResultRow): EmploymentContractConfirmedEntity = EmploymentContractConfirmedEntity(
    id = row[EmploymentContractConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[EmploymentContractConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[EmploymentContractConfirmedTable.documentId].toKotlinUuid()),
    direction = row[EmploymentContractConfirmedTable.direction],
    createdAt = row[EmploymentContractConfirmedTable.createdAt],
    updatedAt = row[EmploymentContractConfirmedTable.updatedAt],
)

// =============================================================================
// Dimona
// =============================================================================

fun DimonaDraftEntity.Companion.from(row: ResultRow): DimonaDraftEntity = DimonaDraftEntity(
    id = row[DimonaDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DimonaDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DimonaDraftsTable.documentId].toKotlinUuid()),
    direction = row[DimonaDraftsTable.direction],
    createdAt = row[DimonaDraftsTable.createdAt],
    updatedAt = row[DimonaDraftsTable.updatedAt],
)

fun DimonaConfirmedEntity.Companion.from(row: ResultRow): DimonaConfirmedEntity = DimonaConfirmedEntity(
    id = row[DimonaConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DimonaConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DimonaConfirmedTable.documentId].toKotlinUuid()),
    direction = row[DimonaConfirmedTable.direction],
    createdAt = row[DimonaConfirmedTable.createdAt],
    updatedAt = row[DimonaConfirmedTable.updatedAt],
)

// =============================================================================
// C4
// =============================================================================

fun C4DraftEntity.Companion.from(row: ResultRow): C4DraftEntity = C4DraftEntity(
    id = row[C4DraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[C4DraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[C4DraftsTable.documentId].toKotlinUuid()),
    direction = row[C4DraftsTable.direction],
    createdAt = row[C4DraftsTable.createdAt],
    updatedAt = row[C4DraftsTable.updatedAt],
)

fun C4ConfirmedEntity.Companion.from(row: ResultRow): C4ConfirmedEntity = C4ConfirmedEntity(
    id = row[C4ConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[C4ConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[C4ConfirmedTable.documentId].toKotlinUuid()),
    direction = row[C4ConfirmedTable.direction],
    createdAt = row[C4ConfirmedTable.createdAt],
    updatedAt = row[C4ConfirmedTable.updatedAt],
)

// =============================================================================
// HolidayPay
// =============================================================================

fun HolidayPayDraftEntity.Companion.from(row: ResultRow): HolidayPayDraftEntity = HolidayPayDraftEntity(
    id = row[HolidayPayDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[HolidayPayDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[HolidayPayDraftsTable.documentId].toKotlinUuid()),
    direction = row[HolidayPayDraftsTable.direction],
    createdAt = row[HolidayPayDraftsTable.createdAt],
    updatedAt = row[HolidayPayDraftsTable.updatedAt],
)

fun HolidayPayConfirmedEntity.Companion.from(row: ResultRow): HolidayPayConfirmedEntity = HolidayPayConfirmedEntity(
    id = row[HolidayPayConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[HolidayPayConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[HolidayPayConfirmedTable.documentId].toKotlinUuid()),
    direction = row[HolidayPayConfirmedTable.direction],
    createdAt = row[HolidayPayConfirmedTable.createdAt],
    updatedAt = row[HolidayPayConfirmedTable.updatedAt],
)

// =============================================================================
// Contract
// =============================================================================

fun ContractDraftEntity.Companion.from(row: ResultRow): ContractDraftEntity = ContractDraftEntity(
    id = row[ContractDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ContractDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ContractDraftsTable.documentId].toKotlinUuid()),
    direction = row[ContractDraftsTable.direction],
    createdAt = row[ContractDraftsTable.createdAt],
    updatedAt = row[ContractDraftsTable.updatedAt],
)

fun ContractConfirmedEntity.Companion.from(row: ResultRow): ContractConfirmedEntity = ContractConfirmedEntity(
    id = row[ContractConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ContractConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ContractConfirmedTable.documentId].toKotlinUuid()),
    direction = row[ContractConfirmedTable.direction],
    createdAt = row[ContractConfirmedTable.createdAt],
    updatedAt = row[ContractConfirmedTable.updatedAt],
)

// =============================================================================
// Lease
// =============================================================================

fun LeaseDraftEntity.Companion.from(row: ResultRow): LeaseDraftEntity = LeaseDraftEntity(
    id = row[LeaseDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[LeaseDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[LeaseDraftsTable.documentId].toKotlinUuid()),
    direction = row[LeaseDraftsTable.direction],
    createdAt = row[LeaseDraftsTable.createdAt],
    updatedAt = row[LeaseDraftsTable.updatedAt],
)

fun LeaseConfirmedEntity.Companion.from(row: ResultRow): LeaseConfirmedEntity = LeaseConfirmedEntity(
    id = row[LeaseConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[LeaseConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[LeaseConfirmedTable.documentId].toKotlinUuid()),
    direction = row[LeaseConfirmedTable.direction],
    createdAt = row[LeaseConfirmedTable.createdAt],
    updatedAt = row[LeaseConfirmedTable.updatedAt],
)

// =============================================================================
// Loan
// =============================================================================

fun LoanDraftEntity.Companion.from(row: ResultRow): LoanDraftEntity = LoanDraftEntity(
    id = row[LoanDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[LoanDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[LoanDraftsTable.documentId].toKotlinUuid()),
    direction = row[LoanDraftsTable.direction],
    createdAt = row[LoanDraftsTable.createdAt],
    updatedAt = row[LoanDraftsTable.updatedAt],
)

fun LoanConfirmedEntity.Companion.from(row: ResultRow): LoanConfirmedEntity = LoanConfirmedEntity(
    id = row[LoanConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[LoanConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[LoanConfirmedTable.documentId].toKotlinUuid()),
    direction = row[LoanConfirmedTable.direction],
    createdAt = row[LoanConfirmedTable.createdAt],
    updatedAt = row[LoanConfirmedTable.updatedAt],
)

// =============================================================================
// Insurance
// =============================================================================

fun InsuranceDraftEntity.Companion.from(row: ResultRow): InsuranceDraftEntity = InsuranceDraftEntity(
    id = row[InsuranceDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InsuranceDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InsuranceDraftsTable.documentId].toKotlinUuid()),
    direction = row[InsuranceDraftsTable.direction],
    createdAt = row[InsuranceDraftsTable.createdAt],
    updatedAt = row[InsuranceDraftsTable.updatedAt],
)

fun InsuranceConfirmedEntity.Companion.from(row: ResultRow): InsuranceConfirmedEntity = InsuranceConfirmedEntity(
    id = row[InsuranceConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InsuranceConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InsuranceConfirmedTable.documentId].toKotlinUuid()),
    direction = row[InsuranceConfirmedTable.direction],
    createdAt = row[InsuranceConfirmedTable.createdAt],
    updatedAt = row[InsuranceConfirmedTable.updatedAt],
)

// =============================================================================
// Dividend
// =============================================================================

fun DividendDraftEntity.Companion.from(row: ResultRow): DividendDraftEntity = DividendDraftEntity(
    id = row[DividendDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DividendDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DividendDraftsTable.documentId].toKotlinUuid()),
    direction = row[DividendDraftsTable.direction],
    createdAt = row[DividendDraftsTable.createdAt],
    updatedAt = row[DividendDraftsTable.updatedAt],
)

fun DividendConfirmedEntity.Companion.from(row: ResultRow): DividendConfirmedEntity = DividendConfirmedEntity(
    id = row[DividendConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DividendConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DividendConfirmedTable.documentId].toKotlinUuid()),
    direction = row[DividendConfirmedTable.direction],
    createdAt = row[DividendConfirmedTable.createdAt],
    updatedAt = row[DividendConfirmedTable.updatedAt],
)

// =============================================================================
// ShareholderRegister
// =============================================================================

fun ShareholderRegisterDraftEntity.Companion.from(row: ResultRow): ShareholderRegisterDraftEntity = ShareholderRegisterDraftEntity(
    id = row[ShareholderRegisterDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ShareholderRegisterDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ShareholderRegisterDraftsTable.documentId].toKotlinUuid()),
    direction = row[ShareholderRegisterDraftsTable.direction],
    createdAt = row[ShareholderRegisterDraftsTable.createdAt],
    updatedAt = row[ShareholderRegisterDraftsTable.updatedAt],
)

fun ShareholderRegisterConfirmedEntity.Companion.from(row: ResultRow): ShareholderRegisterConfirmedEntity = ShareholderRegisterConfirmedEntity(
    id = row[ShareholderRegisterConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ShareholderRegisterConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ShareholderRegisterConfirmedTable.documentId].toKotlinUuid()),
    direction = row[ShareholderRegisterConfirmedTable.direction],
    createdAt = row[ShareholderRegisterConfirmedTable.createdAt],
    updatedAt = row[ShareholderRegisterConfirmedTable.updatedAt],
)

// =============================================================================
// CompanyExtract
// =============================================================================

fun CompanyExtractDraftEntity.Companion.from(row: ResultRow): CompanyExtractDraftEntity = CompanyExtractDraftEntity(
    id = row[CompanyExtractDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CompanyExtractDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CompanyExtractDraftsTable.documentId].toKotlinUuid()),
    direction = row[CompanyExtractDraftsTable.direction],
    createdAt = row[CompanyExtractDraftsTable.createdAt],
    updatedAt = row[CompanyExtractDraftsTable.updatedAt],
)

fun CompanyExtractConfirmedEntity.Companion.from(row: ResultRow): CompanyExtractConfirmedEntity = CompanyExtractConfirmedEntity(
    id = row[CompanyExtractConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CompanyExtractConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CompanyExtractConfirmedTable.documentId].toKotlinUuid()),
    direction = row[CompanyExtractConfirmedTable.direction],
    createdAt = row[CompanyExtractConfirmedTable.createdAt],
    updatedAt = row[CompanyExtractConfirmedTable.updatedAt],
)

// =============================================================================
// AnnualAccounts
// =============================================================================

fun AnnualAccountsDraftEntity.Companion.from(row: ResultRow): AnnualAccountsDraftEntity = AnnualAccountsDraftEntity(
    id = row[AnnualAccountsDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[AnnualAccountsDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[AnnualAccountsDraftsTable.documentId].toKotlinUuid()),
    direction = row[AnnualAccountsDraftsTable.direction],
    createdAt = row[AnnualAccountsDraftsTable.createdAt],
    updatedAt = row[AnnualAccountsDraftsTable.updatedAt],
)

fun AnnualAccountsConfirmedEntity.Companion.from(row: ResultRow): AnnualAccountsConfirmedEntity = AnnualAccountsConfirmedEntity(
    id = row[AnnualAccountsConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[AnnualAccountsConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[AnnualAccountsConfirmedTable.documentId].toKotlinUuid()),
    direction = row[AnnualAccountsConfirmedTable.direction],
    createdAt = row[AnnualAccountsConfirmedTable.createdAt],
    updatedAt = row[AnnualAccountsConfirmedTable.updatedAt],
)

// =============================================================================
// BoardMinutes
// =============================================================================

fun BoardMinutesDraftEntity.Companion.from(row: ResultRow): BoardMinutesDraftEntity = BoardMinutesDraftEntity(
    id = row[BoardMinutesDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BoardMinutesDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[BoardMinutesDraftsTable.documentId].toKotlinUuid()),
    direction = row[BoardMinutesDraftsTable.direction],
    createdAt = row[BoardMinutesDraftsTable.createdAt],
    updatedAt = row[BoardMinutesDraftsTable.updatedAt],
)

fun BoardMinutesConfirmedEntity.Companion.from(row: ResultRow): BoardMinutesConfirmedEntity = BoardMinutesConfirmedEntity(
    id = row[BoardMinutesConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BoardMinutesConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[BoardMinutesConfirmedTable.documentId].toKotlinUuid()),
    direction = row[BoardMinutesConfirmedTable.direction],
    createdAt = row[BoardMinutesConfirmedTable.createdAt],
    updatedAt = row[BoardMinutesConfirmedTable.updatedAt],
)

// =============================================================================
// Subsidy
// =============================================================================

fun SubsidyDraftEntity.Companion.from(row: ResultRow): SubsidyDraftEntity = SubsidyDraftEntity(
    id = row[SubsidyDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SubsidyDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SubsidyDraftsTable.documentId].toKotlinUuid()),
    direction = row[SubsidyDraftsTable.direction],
    createdAt = row[SubsidyDraftsTable.createdAt],
    updatedAt = row[SubsidyDraftsTable.updatedAt],
)

fun SubsidyConfirmedEntity.Companion.from(row: ResultRow): SubsidyConfirmedEntity = SubsidyConfirmedEntity(
    id = row[SubsidyConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[SubsidyConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[SubsidyConfirmedTable.documentId].toKotlinUuid()),
    direction = row[SubsidyConfirmedTable.direction],
    createdAt = row[SubsidyConfirmedTable.createdAt],
    updatedAt = row[SubsidyConfirmedTable.updatedAt],
)

// =============================================================================
// Fine
// =============================================================================

fun FineDraftEntity.Companion.from(row: ResultRow): FineDraftEntity = FineDraftEntity(
    id = row[FineDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[FineDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[FineDraftsTable.documentId].toKotlinUuid()),
    direction = row[FineDraftsTable.direction],
    createdAt = row[FineDraftsTable.createdAt],
    updatedAt = row[FineDraftsTable.updatedAt],
)

fun FineConfirmedEntity.Companion.from(row: ResultRow): FineConfirmedEntity = FineConfirmedEntity(
    id = row[FineConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[FineConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[FineConfirmedTable.documentId].toKotlinUuid()),
    direction = row[FineConfirmedTable.direction],
    createdAt = row[FineConfirmedTable.createdAt],
    updatedAt = row[FineConfirmedTable.updatedAt],
)

// =============================================================================
// Permit
// =============================================================================

fun PermitDraftEntity.Companion.from(row: ResultRow): PermitDraftEntity = PermitDraftEntity(
    id = row[PermitDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PermitDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PermitDraftsTable.documentId].toKotlinUuid()),
    direction = row[PermitDraftsTable.direction],
    createdAt = row[PermitDraftsTable.createdAt],
    updatedAt = row[PermitDraftsTable.updatedAt],
)

fun PermitConfirmedEntity.Companion.from(row: ResultRow): PermitConfirmedEntity = PermitConfirmedEntity(
    id = row[PermitConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[PermitConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[PermitConfirmedTable.documentId].toKotlinUuid()),
    direction = row[PermitConfirmedTable.direction],
    createdAt = row[PermitConfirmedTable.createdAt],
    updatedAt = row[PermitConfirmedTable.updatedAt],
)

// =============================================================================
// CustomsDeclaration
// =============================================================================

fun CustomsDeclarationDraftEntity.Companion.from(row: ResultRow): CustomsDeclarationDraftEntity = CustomsDeclarationDraftEntity(
    id = row[CustomsDeclarationDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CustomsDeclarationDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CustomsDeclarationDraftsTable.documentId].toKotlinUuid()),
    direction = row[CustomsDeclarationDraftsTable.direction],
    createdAt = row[CustomsDeclarationDraftsTable.createdAt],
    updatedAt = row[CustomsDeclarationDraftsTable.updatedAt],
)

fun CustomsDeclarationConfirmedEntity.Companion.from(row: ResultRow): CustomsDeclarationConfirmedEntity = CustomsDeclarationConfirmedEntity(
    id = row[CustomsDeclarationConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CustomsDeclarationConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CustomsDeclarationConfirmedTable.documentId].toKotlinUuid()),
    direction = row[CustomsDeclarationConfirmedTable.direction],
    createdAt = row[CustomsDeclarationConfirmedTable.createdAt],
    updatedAt = row[CustomsDeclarationConfirmedTable.updatedAt],
)

// =============================================================================
// Intrastat
// =============================================================================

fun IntrastatDraftEntity.Companion.from(row: ResultRow): IntrastatDraftEntity = IntrastatDraftEntity(
    id = row[IntrastatDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[IntrastatDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[IntrastatDraftsTable.documentId].toKotlinUuid()),
    direction = row[IntrastatDraftsTable.direction],
    createdAt = row[IntrastatDraftsTable.createdAt],
    updatedAt = row[IntrastatDraftsTable.updatedAt],
)

fun IntrastatConfirmedEntity.Companion.from(row: ResultRow): IntrastatConfirmedEntity = IntrastatConfirmedEntity(
    id = row[IntrastatConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[IntrastatConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[IntrastatConfirmedTable.documentId].toKotlinUuid()),
    direction = row[IntrastatConfirmedTable.direction],
    createdAt = row[IntrastatConfirmedTable.createdAt],
    updatedAt = row[IntrastatConfirmedTable.updatedAt],
)

// =============================================================================
// DepreciationSchedule
// =============================================================================

fun DepreciationScheduleDraftEntity.Companion.from(row: ResultRow): DepreciationScheduleDraftEntity = DepreciationScheduleDraftEntity(
    id = row[DepreciationScheduleDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DepreciationScheduleDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DepreciationScheduleDraftsTable.documentId].toKotlinUuid()),
    direction = row[DepreciationScheduleDraftsTable.direction],
    createdAt = row[DepreciationScheduleDraftsTable.createdAt],
    updatedAt = row[DepreciationScheduleDraftsTable.updatedAt],
)

fun DepreciationScheduleConfirmedEntity.Companion.from(row: ResultRow): DepreciationScheduleConfirmedEntity = DepreciationScheduleConfirmedEntity(
    id = row[DepreciationScheduleConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[DepreciationScheduleConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[DepreciationScheduleConfirmedTable.documentId].toKotlinUuid()),
    direction = row[DepreciationScheduleConfirmedTable.direction],
    createdAt = row[DepreciationScheduleConfirmedTable.createdAt],
    updatedAt = row[DepreciationScheduleConfirmedTable.updatedAt],
)

// =============================================================================
// Inventory
// =============================================================================

fun InventoryDraftEntity.Companion.from(row: ResultRow): InventoryDraftEntity = InventoryDraftEntity(
    id = row[InventoryDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InventoryDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InventoryDraftsTable.documentId].toKotlinUuid()),
    direction = row[InventoryDraftsTable.direction],
    createdAt = row[InventoryDraftsTable.createdAt],
    updatedAt = row[InventoryDraftsTable.updatedAt],
)

fun InventoryConfirmedEntity.Companion.from(row: ResultRow): InventoryConfirmedEntity = InventoryConfirmedEntity(
    id = row[InventoryConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InventoryConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InventoryConfirmedTable.documentId].toKotlinUuid()),
    direction = row[InventoryConfirmedTable.direction],
    createdAt = row[InventoryConfirmedTable.createdAt],
    updatedAt = row[InventoryConfirmedTable.updatedAt],
)

// =============================================================================
// Other
// =============================================================================

fun OtherDraftEntity.Companion.from(row: ResultRow): OtherDraftEntity = OtherDraftEntity(
    id = row[OtherDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OtherDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OtherDraftsTable.documentId].toKotlinUuid()),
    direction = row[OtherDraftsTable.direction],
    createdAt = row[OtherDraftsTable.createdAt],
    updatedAt = row[OtherDraftsTable.updatedAt],
)

fun OtherConfirmedEntity.Companion.from(row: ResultRow): OtherConfirmedEntity = OtherConfirmedEntity(
    id = row[OtherConfirmedTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[OtherConfirmedTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[OtherConfirmedTable.documentId].toKotlinUuid()),
    direction = row[OtherConfirmedTable.direction],
    createdAt = row[OtherConfirmedTable.createdAt],
    updatedAt = row[OtherConfirmedTable.updatedAt],
)
