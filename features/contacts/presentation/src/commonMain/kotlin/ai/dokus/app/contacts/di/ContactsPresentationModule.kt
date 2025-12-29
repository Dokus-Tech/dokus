package ai.dokus.app.contacts.di

import ai.dokus.app.contacts.viewmodel.ContactDetailsAction
import ai.dokus.app.contacts.viewmodel.ContactDetailsContainer
import ai.dokus.app.contacts.viewmodel.ContactDetailsIntent
import ai.dokus.app.contacts.viewmodel.ContactDetailsState
import ai.dokus.app.contacts.viewmodel.ContactFormAction
import ai.dokus.app.contacts.viewmodel.ContactFormContainer
import ai.dokus.app.contacts.viewmodel.ContactFormIntent
import ai.dokus.app.contacts.viewmodel.ContactFormState
import ai.dokus.app.contacts.viewmodel.ContactsAction
import ai.dokus.app.contacts.viewmodel.ContactsContainer
import ai.dokus.app.contacts.viewmodel.ContactsIntent
import ai.dokus.app.contacts.viewmodel.ContactsState
import ai.dokus.app.contacts.viewmodel.CreateContactAction
import ai.dokus.app.contacts.viewmodel.CreateContactContainer
import ai.dokus.app.contacts.viewmodel.CreateContactIntent
import ai.dokus.app.contacts.viewmodel.CreateContactState
import org.koin.dsl.module
import tech.dokus.foundation.app.mvi.container

val contactsPresentationModule = module {
    // FlowMVI Containers
    container<ContactsContainer, ContactsState, ContactsIntent, ContactsAction> {
        ContactsContainer(
            listContacts = get(),
            listCustomers = get(),
            listVendors = get(),
            getCachedContacts = get(),
            cacheContacts = get(),
            getCurrentTenantId = get()
        )
    }
    container<ContactDetailsContainer, ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> { (params: ContactDetailsContainer.Companion.Params) ->
        ContactDetailsContainer(
            contactId = params.contactId,
            getContact = get(),
            getContactActivity = get(),
            listContactNotes = get(),
            createContactNote = get(),
            updateContactNote = get(),
            deleteContactNote = get(),
            updateContactPeppol = get(),
            getCachedContacts = get(),
            cacheContacts = get(),
            getCurrentTenantId = get()
        )
    }
    container<ContactFormContainer, ContactFormState, ContactFormIntent, ContactFormAction> { (params: ContactFormContainer.Companion.Params) ->
        ContactFormContainer(
            contactId = params.contactId,
            getContact = get(),
            listContacts = get(),
            createContact = get(),
            updateContact = get(),
            deleteContact = get()
        )
    }

    // Create Contact flow (new VAT-first split flow)
    container<CreateContactContainer, CreateContactState, CreateContactIntent, CreateContactAction> {
        CreateContactContainer(
            searchCompanyUseCase = get(),
            listContacts = get(),
            createContact = get()
        )
    }
}
