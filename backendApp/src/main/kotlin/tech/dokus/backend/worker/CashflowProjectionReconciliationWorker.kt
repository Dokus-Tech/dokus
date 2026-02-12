package tech.dokus.backend.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.dokus.backend.services.cashflow.CashflowProjectionReconciliationService
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Best-effort startup sweep that backfills missing cashflow projections for confirmed documents.
 */
class CashflowProjectionReconciliationWorker(
    private val tenantRepository: TenantRepository,
    private val documentRepository: DocumentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val reconciliationService: CashflowProjectionReconciliationService
) {
    private val logger = loggerFor()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) {
            logger.warn("Cashflow projection reconciliation worker already started")
            return
        }

        job = scope.launch {
            runSweepOnce()
        }
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) {
            return
        }
        job?.cancel()
        job = null
    }

    suspend fun runSweepOnce() {
        var scanned = 0
        var repaired = 0
        var failed = 0

        try {
            val tenants = tenantRepository.listActiveTenants()
            for (tenant in tenants) {
                val tenantId = tenant.id
                val pageSize = 100
                var page = 0

                while (true) {
                    val (documents, total) = documentRepository.listWithDraftsAndIngestion(
                        tenantId = tenantId,
                        filter = DocumentListFilter.Confirmed,
                        page = page,
                        limit = pageSize
                    )

                    if (documents.isEmpty()) break

                    for (documentWithInfo in documents) {
                        scanned += 1
                        val documentId = documentWithInfo.document.id

                        runCatching {
                            val hadProjection = cashflowEntriesRepository
                                .getByDocumentId(tenantId, documentId)
                                .getOrThrow() != null

                            val entity = resolveConfirmedEntity(
                                tenantId = tenantId,
                                documentId = documentId,
                                documentType = documentWithInfo.draft?.documentType
                            ) ?: return@runCatching

                            val reconciledEntryId = reconciliationService
                                .ensureProjectionIfMissing(tenantId, documentId, entity)
                                .getOrThrow()

                            if (!hadProjection && reconciledEntryId != null) {
                                repaired += 1
                            }
                        }.onFailure { throwable ->
                            failed += 1
                            logger.warn(
                                "Failed to reconcile cashflow projection for tenant={}, document={}",
                                tenantId,
                                documentId,
                                throwable
                            )
                        }
                    }

                    page += 1
                    if (page * pageSize >= total) break
                }
            }
        } catch (throwable: Throwable) {
            logger.warn("Cashflow projection startup sweep failed", throwable)
        } finally {
            logger.info(
                "Cashflow projection startup sweep finished: scanned={}, repaired={}, failed={}",
                scanned,
                repaired,
                failed
            )
        }
    }

    private suspend fun resolveConfirmedEntity(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType?
    ): FinancialDocumentDto? {
        return when (documentType) {
            DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
            DocumentType.Receipt -> expenseRepository.findByDocumentId(tenantId, documentId)
            DocumentType.CreditNote -> creditNoteRepository.findByDocumentId(tenantId, documentId)
            else -> invoiceRepository.findByDocumentId(tenantId, documentId)
                ?: expenseRepository.findByDocumentId(tenantId, documentId)
                ?: creditNoteRepository.findByDocumentId(tenantId, documentId)
        }
    }
}
