package tech.dokus.features.ai.graph

import kotlinx.serialization.Serializable
import tech.dokus.domain.Name
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.InputWithUserFeedback
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable
sealed interface AcceptDocumentInput : InputWithDocumentId, InputWithTenantContext,
    InputWithUserFeedback {
    override val maxPagesOverride: Int?
    override val dpiOverride: Dpi?

    data class Upload(
        override val documentId: DocumentId,
        override val tenant: Tenant,
        override val associatedPersonNames: List<Name>,
        override val userFeedback: String?,
        override val maxPagesOverride: Int?,
        override val dpiOverride: Dpi?,
    ) : AcceptDocumentInput

    data class Peppol(
        override val documentId: DocumentId,
        override val tenant: Tenant,
        override val associatedPersonNames: List<Name>,
        override val userFeedback: String?,
        override val maxPagesOverride: Int?,
        override val dpiOverride: Dpi?,
        val peppolStructuredSnapshotJson: String,
        val peppolSnapshotVersion: Int,
    ) : AcceptDocumentInput {
        val asUpload: Upload
            get() = Upload(
                documentId = documentId,
                tenant = tenant,
                associatedPersonNames = associatedPersonNames,
                userFeedback = userFeedback,
                maxPagesOverride = maxPagesOverride,
                dpiOverride = dpiOverride,
            )
    }
}

@OptIn(ExperimentalContracts::class)
fun AcceptDocumentInput?.isUpload(): Boolean {
    contract {
        returns(true) implies (this@isUpload is AcceptDocumentInput.Upload)
    }
    return this is AcceptDocumentInput.Upload
}

@OptIn(ExperimentalContracts::class)
fun AcceptDocumentInput?.isPeppol(): Boolean {
    contract {
        returns(true) implies (this@isPeppol is AcceptDocumentInput.Peppol)
    }
    return this is AcceptDocumentInput.Peppol
}