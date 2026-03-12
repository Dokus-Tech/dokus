package tech.dokus.features.contacts.mvi

import tech.dokus.domain.Money
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCase
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.GetContactActivityUseCase
import tech.dokus.features.contacts.usecases.GetContactInvoiceSnapshotUseCase
import tech.dokus.features.contacts.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.features.contacts.usecases.ListContactNotesUseCase
import tech.dokus.features.contacts.usecases.ObserveContactChangesUseCase
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.features.contacts.usecases.UpdateContactUseCase

internal class StubGetContactUseCase(
    private val result: Result<ContactDto>
) : GetContactUseCase {
    override suspend fun invoke(contactId: ContactId) = result
}

internal class StubGetContactActivityUseCase : GetContactActivityUseCase {
    override suspend fun invoke(contactId: ContactId) = Result.success(
        ContactActivitySummary(contactId = contactId)
    )
}

internal class StubGetContactInvoiceSnapshotUseCase : GetContactInvoiceSnapshotUseCase {
    override suspend fun invoke(contactId: ContactId) = Result.success(
        ContactInvoiceSnapshot(
            documentsCount = 0,
            totalVolume = Money.ZERO,
            outstanding = Money.ZERO,
            recentDocuments = emptyList()
        )
    )
}

internal class StubGetContactPeppolStatusUseCase : GetContactPeppolStatusUseCase {
    override suspend fun invoke(contactId: ContactId, refresh: Boolean) = Result.success(
        PeppolStatusResponse(status = "not_found", refreshed = false)
    )
}

internal class StubListContactNotesUseCase(
    private val result: Result<List<ContactNoteDto>>
) : ListContactNotesUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        limit: Int,
        offset: Int
    ) = result
}

internal class StubCreateContactNoteUseCase(
    private val result: Result<ContactNoteDto>
) : CreateContactNoteUseCase {
    var calls = 0
        private set

    override suspend fun invoke(
        contactId: ContactId,
        request: CreateContactNoteRequest
    ): Result<ContactNoteDto> {
        calls++
        return result
    }
}

internal class StubUpdateContactNoteUseCase(
    private val result: Result<ContactNoteDto>
) : UpdateContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ) = result
}

internal class StubDeleteContactNoteUseCase(
    private val result: Result<Unit>
) : DeleteContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId
    ) = result
}

internal class StubGetCachedContactsUseCase : GetCachedContactsUseCase {
    override suspend fun invoke(tenantId: TenantId): List<ContactDto> = emptyList()
}

internal class StubCacheContactsUseCase : CacheContactsUseCase {
    override suspend fun invoke(tenantId: TenantId, contacts: List<ContactDto>) = Unit
}

internal class StubGetCurrentTenantIdUseCase(
    private val tenantId: TenantId?
) : GetCurrentTenantIdUseCase {
    override suspend fun invoke() = tenantId
}

internal class StubObserveContactChangesUseCase(
    private val flow: Flow<Unit> = emptyFlow()
) : ObserveContactChangesUseCase {
    override fun invoke(contactId: ContactId): Flow<Unit> = flow
}

internal class StubUpdateContactUseCase(
    private val result: Result<ContactDto> = Result.success(
        ContactDto(
            id = ContactId.generate(),
            tenantId = TenantId.generate(),
            name = tech.dokus.domain.Name("Updated"),
            isActive = true,
            createdAt = kotlinx.datetime.LocalDateTime(2026, 1, 1, 0, 0),
            updatedAt = kotlinx.datetime.LocalDateTime(2026, 1, 1, 0, 0),
        )
    )
) : UpdateContactUseCase {
    override suspend fun invoke(contactId: ContactId, request: UpdateContactRequest) = result
}
