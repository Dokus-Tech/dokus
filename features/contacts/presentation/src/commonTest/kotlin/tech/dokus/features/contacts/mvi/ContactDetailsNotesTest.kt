@file:Suppress("LongMethod") // Test methods with setup and assertions

package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.Money
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
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
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.foundation.app.state.isSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsNotesTest {

    private val contactId = ContactId.generate()
    private val tenantId = TenantId.generate()
    private val now = LocalDateTime(2026, 1, 15, 10, 0)

    private val testContact = ContactDto(
        id = contactId,
        tenantId = tenantId,
        name = Name("Test Contact"),
        createdAt = now,
        updatedAt = now
    )

    private val testNote = ContactNoteDto(
        id = ContactNoteId.generate(),
        contactId = contactId,
        tenantId = tenantId,
        content = "Existing note content",
        authorName = "Test Author",
        createdAt = now,
        updatedAt = now
    )

    private fun createContainer(
        listNotesResult: Result<List<ContactNoteDto>> = Result.success(listOf(testNote)),
        createNoteResult: Result<ContactNoteDto> = Result.success(testNote),
        updateNoteResult: Result<ContactNoteDto> = Result.success(testNote),
        deleteNoteResult: Result<Unit> = Result.success(Unit),
    ): ContactDetailsContainer {
        return ContactDetailsContainer(
            contactId = contactId,
            getContact = FakeGetContactUseCase(Result.success(testContact)),
            getContactActivity = FakeGetContactActivityUseCase(),
            getContactInvoiceSnapshot = FakeGetContactInvoiceSnapshotUseCase(),
            getContactPeppolStatus = FakeGetContactPeppolStatusUseCase(),
            listContactNotes = FakeListContactNotesUseCase(listNotesResult),
            createContactNote = FakeCreateContactNoteUseCase(createNoteResult),
            updateContactNote = FakeUpdateContactNoteUseCase(updateNoteResult),
            deleteContactNote = FakeDeleteContactNoteUseCase(deleteNoteResult),
            getCachedContacts = StubGetCachedContactsUseCase(),
            cacheContacts = StubCacheContactsUseCase(),
            getCurrentTenantId = StubGetCurrentTenantIdUseCase(tenantId),
        )
    }

    // ========================================================================
    // ShowAddNoteDialog
    // ========================================================================

    @Test
    fun `ShowAddNoteDialog clears edit and delete state`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            // First load the contact so the container is ready
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Set up some edit state first
            intent(ContactDetailsIntent.ShowEditNoteDialog(testNote))
            advanceUntilIdle()

            // Now show add dialog
            intent(ContactDetailsIntent.ShowAddNoteDialog)
            advanceUntilIdle()

            val uiState = states.value.uiState
            assertTrue(uiState.showAddNoteDialog)
            assertFalse(uiState.showEditNoteDialog)
            assertNull(uiState.editingNote)
            assertFalse(uiState.showDeleteNoteConfirmation)
            assertNull(uiState.deletingNote)
            assertEquals("", uiState.noteContent)
        }
    }

    // ========================================================================
    // ShowEditNoteDialog
    // ========================================================================

    @Test
    fun `ShowEditNoteDialog seeds content from selected note`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(ContactDetailsIntent.ShowEditNoteDialog(testNote))
            advanceUntilIdle()

            val uiState = states.value.uiState
            assertFalse(uiState.showAddNoteDialog)
            assertTrue(uiState.showEditNoteDialog)
            assertEquals(testNote, uiState.editingNote)
            assertEquals(testNote.content, uiState.noteContent)
            assertFalse(uiState.showDeleteNoteConfirmation)
            assertNull(uiState.deletingNote)
        }
    }

    // ========================================================================
    // Hiding sheet/pane clears transient state
    // ========================================================================

    @Test
    fun `HideNotesSidePanel clears all note transient state`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Open side panel and add mode
            intent(ContactDetailsIntent.ShowNotesSidePanel)
            intent(ContactDetailsIntent.ShowAddNoteDialog)
            intent(ContactDetailsIntent.UpdateNoteContent("draft text"))
            advanceUntilIdle()

            // Close the panel
            intent(ContactDetailsIntent.HideNotesSidePanel)
            advanceUntilIdle()

            val uiState = states.value.uiState
            assertFalse(uiState.showNotesSidePanel)
            assertFalse(uiState.showAddNoteDialog)
            assertFalse(uiState.showEditNoteDialog)
            assertNull(uiState.editingNote)
            assertFalse(uiState.showDeleteNoteConfirmation)
            assertNull(uiState.deletingNote)
            assertEquals("", uiState.noteContent)
        }
    }

    @Test
    fun `HideNotesBottomSheet clears all note transient state`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Open bottom sheet and edit mode
            intent(ContactDetailsIntent.ShowNotesBottomSheet)
            intent(ContactDetailsIntent.ShowEditNoteDialog(testNote))
            advanceUntilIdle()

            // Close the sheet
            intent(ContactDetailsIntent.HideNotesBottomSheet)
            advanceUntilIdle()

            val uiState = states.value.uiState
            assertFalse(uiState.showNotesBottomSheet)
            assertFalse(uiState.showAddNoteDialog)
            assertFalse(uiState.showEditNoteDialog)
            assertNull(uiState.editingNote)
            assertEquals("", uiState.noteContent)
        }
    }

    // ========================================================================
    // Add note success
    // ========================================================================

    @Test
    fun `AddNote success reloads notes and clears add state`() = runTest {
        val createdNote = testNote.copy(
            id = ContactNoteId.generate(),
            content = "New note"
        )
        val listNotes = FakeListContactNotesUseCase(Result.success(listOf(testNote, createdNote)))
        val container = ContactDetailsContainer(
            contactId = contactId,
            getContact = FakeGetContactUseCase(Result.success(testContact)),
            getContactActivity = FakeGetContactActivityUseCase(),
            getContactInvoiceSnapshot = FakeGetContactInvoiceSnapshotUseCase(),
            getContactPeppolStatus = FakeGetContactPeppolStatusUseCase(),
            listContactNotes = listNotes,
            createContactNote = FakeCreateContactNoteUseCase(Result.success(createdNote)),
            updateContactNote = FakeUpdateContactNoteUseCase(Result.success(testNote)),
            deleteContactNote = FakeDeleteContactNoteUseCase(Result.success(Unit)),
            getCachedContacts = StubGetCachedContactsUseCase(),
            cacheContacts = StubCacheContactsUseCase(),
            getCurrentTenantId = StubGetCurrentTenantIdUseCase(tenantId),
        )

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Enter add mode and type content
            intent(ContactDetailsIntent.ShowAddNoteDialog)
            intent(ContactDetailsIntent.UpdateNoteContent("New note"))
            advanceUntilIdle()

            // Save the note
            intent(ContactDetailsIntent.AddNote)
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isSavingNote)
            assertFalse(state.uiState.showAddNoteDialog)
            assertEquals("", state.uiState.noteContent)
            // Notes should have been reloaded
            assertTrue(state.notesState.isSuccess())
        }
    }

    // ========================================================================
    // Update note success
    // ========================================================================

    @Test
    fun `UpdateNote success reloads notes and clears edit state`() = runTest {
        val updatedNote = testNote.copy(content = "Updated content")
        val container = createContainer(
            updateNoteResult = Result.success(updatedNote)
        )

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Enter edit mode
            intent(ContactDetailsIntent.ShowEditNoteDialog(testNote))
            intent(ContactDetailsIntent.UpdateNoteContent("Updated content"))
            advanceUntilIdle()

            // Save the update
            intent(ContactDetailsIntent.UpdateNote)
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isSavingNote)
            assertFalse(state.uiState.showEditNoteDialog)
            assertNull(state.uiState.editingNote)
            assertEquals("", state.uiState.noteContent)
            assertTrue(state.notesState.isSuccess())
        }
    }

    // ========================================================================
    // Delete note success
    // ========================================================================

    @Test
    fun `DeleteNote success reloads notes and clears delete state`() = runTest {
        val container = createContainer()

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            // Show delete confirmation
            intent(ContactDetailsIntent.ShowDeleteNoteConfirmation(testNote))
            advanceUntilIdle()
            assertTrue(states.value.uiState.showDeleteNoteConfirmation)
            assertEquals(testNote, states.value.uiState.deletingNote)

            // Confirm delete
            intent(ContactDetailsIntent.DeleteNote)
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isDeletingNote)
            assertFalse(state.uiState.showDeleteNoteConfirmation)
            assertNull(state.uiState.deletingNote)
            assertTrue(state.notesState.isSuccess())
        }
    }

    // ========================================================================
    // Validation
    // ========================================================================

    @Test
    fun `AddNote with blank content does not save`() = runTest {
        val createNote = FakeCreateContactNoteUseCase(Result.success(testNote))
        val container = ContactDetailsContainer(
            contactId = contactId,
            getContact = FakeGetContactUseCase(Result.success(testContact)),
            getContactActivity = FakeGetContactActivityUseCase(),
            getContactInvoiceSnapshot = FakeGetContactInvoiceSnapshotUseCase(),
            getContactPeppolStatus = FakeGetContactPeppolStatusUseCase(),
            listContactNotes = FakeListContactNotesUseCase(Result.success(emptyList())),
            createContactNote = createNote,
            updateContactNote = FakeUpdateContactNoteUseCase(Result.success(testNote)),
            deleteContactNote = FakeDeleteContactNoteUseCase(Result.success(Unit)),
            getCachedContacts = StubGetCachedContactsUseCase(),
            cacheContacts = StubCacheContactsUseCase(),
            getCurrentTenantId = StubGetCurrentTenantIdUseCase(tenantId),
        )

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(ContactDetailsIntent.ShowAddNoteDialog)
            intent(ContactDetailsIntent.UpdateNoteContent("   "))
            advanceUntilIdle()

            intent(ContactDetailsIntent.AddNote)
            advanceUntilIdle()

            // Should not have called create
            assertEquals(0, createNote.calls)
            assertFalse(states.value.isSavingNote)
        }
    }
}

// ============================================================================
// FAKES
// ============================================================================

private class FakeGetContactUseCase(
    private val result: Result<ContactDto>
) : GetContactUseCase {
    override suspend fun invoke(contactId: ContactId) = result
}

private class FakeGetContactActivityUseCase : GetContactActivityUseCase {
    override suspend fun invoke(contactId: ContactId) = Result.success(
        ContactActivitySummary(contactId = contactId)
    )
}

private class FakeGetContactInvoiceSnapshotUseCase : GetContactInvoiceSnapshotUseCase {
    override suspend fun invoke(contactId: ContactId) = Result.success(
        ContactInvoiceSnapshot(
            documentsCount = 0,
            totalVolume = Money.ZERO,
            outstanding = Money.ZERO,
            recentDocuments = emptyList()
        )
    )
}

private class FakeGetContactPeppolStatusUseCase : GetContactPeppolStatusUseCase {
    override suspend fun invoke(contactId: ContactId, refresh: Boolean) = Result.success(
        PeppolStatusResponse(status = "not_found", refreshed = false)
    )
}

private class FakeListContactNotesUseCase(
    private val result: Result<List<ContactNoteDto>>
) : ListContactNotesUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        limit: Int,
        offset: Int
    ) = result
}

private class FakeCreateContactNoteUseCase(
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

private class FakeUpdateContactNoteUseCase(
    private val result: Result<ContactNoteDto>
) : UpdateContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ) = result
}

private class FakeDeleteContactNoteUseCase(
    private val result: Result<Unit>
) : DeleteContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId
    ) = result
}

private class StubGetCachedContactsUseCase : GetCachedContactsUseCase {
    override suspend fun invoke(tenantId: TenantId): List<ContactDto> = emptyList()
}

private class StubCacheContactsUseCase : CacheContactsUseCase {
    override suspend fun invoke(tenantId: TenantId, contacts: List<ContactDto>) = Unit
}

private class StubGetCurrentTenantIdUseCase(
    private val tenantId: TenantId?
) : GetCurrentTenantIdUseCase {
    override suspend fun invoke() = tenantId
}
