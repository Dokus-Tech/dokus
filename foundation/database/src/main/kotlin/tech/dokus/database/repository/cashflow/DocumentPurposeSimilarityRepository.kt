package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.tables.documents.DocumentPurposeExamplesTable
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor
import java.sql.Connection
import java.util.UUID
import kotlin.uuid.toJavaUuid

class DocumentPurposeSimilarityRepository {
    private val logger = loggerFor()

    @Suppress("LongParameterList")
    suspend fun upsertForDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        counterpartyKey: String?,
        merchantToken: String?,
        purposeBase: String,
        purposeRendered: String?,
        purposeSource: DocumentPurposeSource?,
        embedding: List<Float>?,
        embeddingModel: String?
    ) = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        val documentUuid = documentId.value.toJavaUuid()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        DocumentPurposeExamplesTable.upsert(
            DocumentPurposeExamplesTable.tenantId,
            DocumentPurposeExamplesTable.documentId,
            onUpdate = { stmt ->
                stmt[DocumentPurposeExamplesTable.documentType] = documentType
                stmt[DocumentPurposeExamplesTable.counterpartyKey] = counterpartyKey
                stmt[DocumentPurposeExamplesTable.merchantToken] = merchantToken
                stmt[DocumentPurposeExamplesTable.purposeBase] = purposeBase
                stmt[DocumentPurposeExamplesTable.purposeRendered] = purposeRendered
                stmt[DocumentPurposeExamplesTable.purposeSource] = purposeSource
                stmt[DocumentPurposeExamplesTable.embedding] = embedding
                stmt[DocumentPurposeExamplesTable.embeddingModel] = embeddingModel
                stmt[DocumentPurposeExamplesTable.indexedAt] = now
                stmt[DocumentPurposeExamplesTable.updatedAt] = now
            }
        ) {
            it[DocumentPurposeExamplesTable.tenantId] = tenantUuid
            it[DocumentPurposeExamplesTable.documentId] = documentUuid
            it[DocumentPurposeExamplesTable.documentType] = documentType
            it[DocumentPurposeExamplesTable.counterpartyKey] = counterpartyKey
            it[DocumentPurposeExamplesTable.merchantToken] = merchantToken
            it[DocumentPurposeExamplesTable.purposeBase] = purposeBase
            it[DocumentPurposeExamplesTable.purposeRendered] = purposeRendered
            it[DocumentPurposeExamplesTable.purposeSource] = purposeSource
            it[DocumentPurposeExamplesTable.embedding] = embedding
            it[DocumentPurposeExamplesTable.embeddingModel] = embeddingModel
            it[indexedAt] = now
            it[createdAt] = now
            it[updatedAt] = now
        }
    }

    @Suppress("LongParameterList")
    suspend fun searchSimilarPurposeBases(
        tenantId: TenantId,
        documentType: DocumentType,
        counterpartyKey: String?,
        merchantToken: String?,
        queryEmbedding: List<Float>,
        minSimilarity: Float,
        topK: Int,
        confirmedOnly: Boolean = true
    ): List<String> = newSuspendedTransaction {
        val effectiveCounterpartyKey = counterpartyKey?.takeIf { it.isNotBlank() }
        val effectiveMerchantToken = merchantToken?.takeIf { it.isNotBlank() }
        if (effectiveCounterpartyKey == null && effectiveMerchantToken == null) {
            return@newSuspendedTransaction emptyList()
        }
        if (queryEmbedding.isEmpty()) {
            return@newSuspendedTransaction emptyList()
        }

        val vectorString = "[${queryEmbedding.joinToString(",")}]"
        val tenantUuid = tenantId.value.toJavaUuid()
        val sql = buildString {
            append("SELECT dpe.purpose_base, ")
            append("(1 - (dpe.embedding <=> '${escapeSqlLiteral(vectorString)}'::vector)) AS similarity ")
            append("FROM document_purpose_examples dpe ")
            if (confirmedOnly) {
                append("INNER JOIN document_drafts dd ON dpe.document_id = dd.document_id AND dpe.tenant_id = dd.tenant_id ")
            }
            append("WHERE dpe.tenant_id = '${tenantUuid}' ")
            append("AND dpe.document_type = '${documentType.dbValue}' ")
            append("AND dpe.embedding IS NOT NULL ")
            if (effectiveCounterpartyKey != null) {
                append("AND dpe.counterparty_key = '${escapeSqlLiteral(effectiveCounterpartyKey)}' ")
            } else {
                append("AND dpe.merchant_token = '${escapeSqlLiteral(requireNotNull(effectiveMerchantToken))}' ")
            }
            if (confirmedOnly) {
                append("AND dd.document_status = 'CONFIRMED' ")
            }
            append("AND (1 - (dpe.embedding <=> '${escapeSqlLiteral(vectorString)}'::vector)) >= $minSimilarity ")
            append("ORDER BY dpe.embedding <=> '${escapeSqlLiteral(vectorString)}'::vector ")
            append("LIMIT $topK")
        }

        val connection = this.connection.connection as Connection
        val orderedCandidates = mutableListOf<String>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    val base = rs.getString("purpose_base")?.trim()
                    if (!base.isNullOrBlank() && base !in orderedCandidates) {
                        orderedCandidates += base
                    }
                }
            }
        }

        logger.debug(
            "Purpose similarity search: tenant={}, type={}, key={}, count={}",
            tenantId,
            documentType,
            effectiveCounterpartyKey ?: effectiveMerchantToken,
            orderedCandidates.size
        )
        orderedCandidates
    }

    private fun escapeSqlLiteral(value: String): String = value.replace("'", "''")
}
