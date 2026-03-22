@file:Suppress("LongMethod") // Test methods with setup and assertions

package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.features.contacts.mvi.notes.ContactNotesContainer
import tech.dokus.features.contacts.mvi.notes.ContactNotesIntent
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

    private fun noteIntent(intent: ContactNotesIntent): ContactDetailsIntent =
        ContactDetailsIntent.Notes(intent)

    private fun createContainer(
        listNotesResult: Result<List<ContactNoteDto>> = Result.success(listOf(testNote)),
        createNoteResult: Result<ContactNoteDto> = Result.success(testNote),
        updateNoteResult: Result<ContactNoteDto> = Result.success(testNote),
        deleteNoteResult: Result<Unit> = Result.success(Unit),
    ): ContactDetailsContainer {
        return ContactDetailsContainer(
            contactId = contactId,
            getContact = StubGetContactUseCase(Result.success(testContact)),
            getContactActivity = StubGetContactActivityUseCase(),
            getContactInvoiceSnapshot = StubGetContactInvoiceSnapshotUseCase(),
            getContactPeppolStatus = StubGetContactPeppolStatusUseCase(),
            getCachedContacts = StubGetCachedContactsUseCase(),
            cacheContacts = StubCacheContactsUseCase(),
            getCurrentTenantId = StubGetCurrentTenantIdUseCase(tenantId),
            observeContactChanges = StubObserveContactChangesUseCase(),
            updateContact = StubUpdateContactUseCase(),
            notesContainer = ContactNotesContainer(
                contactId = contactId,
                listContactNotes = StubListContactNotesUseCase(listNotesResult),
                createContactNote = StubCreateContactNoteUseCase(createNoteResult),
                updateContactNote = StubUpdateContactNoteUseCase(updateNoteResult),
                deleteContactNote = StubDeleteContactNoteUseCase(deleteNoteResult),
            ),
        )
    }

    @Test
    fun `ShowAddNoteDialog clears edit and delete state`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowEditNoteDialog(testNote)))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowAddNoteDialog))
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

    @Test
    fun `ShowEditNoteDialog seeds content from selected note`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowEditNoteDialog(testNote)))
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

    @Test
    fun `HideNotesSidePanel clears all note transient state`() = runTest {
        val container = createContainer()
        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowNotesSidePanel))
            intent(noteIntent(ContactNotesIntent.ShowAddNoteDialog))
            intent(noteIntent(ContactNotesIntent.UpdateNoteContent("draft text")))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.HideNotesSidePanel))
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

            intent(noteIntent(ContactNotesIntent.ShowNotesBottomSheet))
            intent(noteIntent(ContactNotesIntent.ShowEditNoteDialog(testNote)))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.HideNotesBottomSheet))
            advanceUntilIdle()

            val uiState = states.value.uiState
            assertFalse(uiState.showNotesBottomSheet)
            assertFalse(uiState.showAddNoteDialog)
            assertFalse(uiState.showEditNoteDialog)
            assertNull(uiState.editingNote)
            assertEquals("", uiState.noteContent)
        }
    }

    @Test
    fun `AddNote success reloads notes and clears add state`() = runTest {
        val createdNote = testNote.copy(id = ContactNoteId.generate(), content = "New note")
        val container = createContainer(createNoteResult = Result.success(createdNote))

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowAddNoteDialog))
            intent(noteIntent(ContactNotesIntent.UpdateNoteContent("New note")))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.AddNote))
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isSavingNote)
            assertFalse(state.uiState.showAddNoteDialog)
            assertEquals("", state.uiState.noteContent)
            assertTrue(state.notesState.isSuccess())
        }
    }

    @Test
    fun `UpdateNote success reloads notes and clears edit state`() = runTest {
        val container = createContainer(
            updateNoteResult = Result.success(testNote.copy(content = "Updated content"))
        )

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowEditNoteDialog(testNote)))
            intent(noteIntent(ContactNotesIntent.UpdateNoteContent("Updated content")))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.UpdateNote))
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isSavingNote)
            assertFalse(state.uiState.showEditNoteDialog)
            assertNull(state.uiState.editingNote)
            assertEquals("", state.uiState.noteContent)
            assertTrue(state.notesState.isSuccess())
        }
    }

    @Test
    fun `DeleteNote success reloads notes and clears delete state`() = runTest {
        val container = createContainer()

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowDeleteNoteConfirmation(testNote)))
            advanceUntilIdle()
            assertTrue(states.value.uiState.showDeleteNoteConfirmation)
            assertEquals(testNote, states.value.uiState.deletingNote)

            intent(noteIntent(ContactNotesIntent.DeleteNote))
            advanceUntilIdle()

            val state = states.value
            assertFalse(state.isDeletingNote)
            assertFalse(state.uiState.showDeleteNoteConfirmation)
            assertNull(state.uiState.deletingNote)
            assertTrue(state.notesState.isSuccess())
        }
    }

    @Test
    fun `AddNote with blank content does not save`() = runTest {
        val createNote = StubCreateContactNoteUseCase(Result.success(testNote))
        val container = ContactDetailsContainer(
            contactId = contactId,
            getContact = StubGetContactUseCase(Result.success(testContact)),
            getContactActivity = StubGetContactActivityUseCase(),
            getContactInvoiceSnapshot = StubGetContactInvoiceSnapshotUseCase(),
            getContactPeppolStatus = StubGetContactPeppolStatusUseCase(),
            getCachedContacts = StubGetCachedContactsUseCase(),
            cacheContacts = StubCacheContactsUseCase(),
            getCurrentTenantId = StubGetCurrentTenantIdUseCase(tenantId),
            observeContactChanges = StubObserveContactChangesUseCase(),
            updateContact = StubUpdateContactUseCase(),
            notesContainer = ContactNotesContainer(
                contactId = contactId,
                listContactNotes = StubListContactNotesUseCase(Result.success(emptyList())),
                createContactNote = createNote,
                updateContactNote = StubUpdateContactNoteUseCase(Result.success(testNote)),
                deleteContactNote = StubDeleteContactNoteUseCase(Result.success(Unit)),
            ),
        )

        container.store.subscribeAndTest {
            intent(ContactDetailsIntent.LoadContact(contactId))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.ShowAddNoteDialog))
            intent(noteIntent(ContactNotesIntent.UpdateNoteContent("   ")))
            advanceUntilIdle()

            intent(noteIntent(ContactNotesIntent.AddNote))
            advanceUntilIdle()

            assertEquals(0, createNote.calls)
            assertFalse(states.value.isSavingNote)
        }
    }
}
