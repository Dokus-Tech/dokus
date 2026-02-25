package tech.dokus.features.contacts.di

import org.koin.dsl.module
import tech.dokus.features.contacts.mvi.ContactDetailsAction
import tech.dokus.features.contacts.mvi.ContactDetailsContainer
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.ContactFormAction
import tech.dokus.features.contacts.mvi.ContactFormContainer
import tech.dokus.features.contacts.mvi.ContactFormIntent
import tech.dokus.features.contacts.mvi.ContactFormState
import tech.dokus.features.contacts.mvi.ContactMergeAction
import tech.dokus.features.contacts.mvi.ContactMergeContainer
import tech.dokus.features.contacts.mvi.ContactMergeIntent
import tech.dokus.features.contacts.mvi.ContactMergeState
import tech.dokus.features.contacts.mvi.ContactsAction
import tech.dokus.features.contacts.mvi.ContactsContainer
import tech.dokus.features.contacts.mvi.ContactsIntent
import tech.dokus.features.contacts.mvi.ContactsState
import tech.dokus.features.contacts.mvi.CreateContactAction
import tech.dokus.features.contacts.mvi.CreateContactContainer
import tech.dokus.features.contacts.mvi.CreateContactIntent
import tech.dokus.features.contacts.mvi.CreateContactState
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
    container<ContactDetailsContainer, ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> {
            (params: ContactDetailsContainer.Companion.Params) ->
        ContactDetailsContainer(
            contactId = params.contactId,
            getContact = get(),
            getContactActivity = get(),
            listContactNotes = get(),
            createContactNote = get(),
            updateContactNote = get(),
            deleteContactNote = get(),
            getCachedContacts = get(),
            cacheContacts = get(),
            getCurrentTenantId = get()
        )
    }
    container<ContactFormContainer, ContactFormState, ContactFormIntent, ContactFormAction> {
            (params: ContactFormContainer.Companion.Params) ->
        ContactFormContainer(
            contactId = params.contactId,
            getContact = get(),
            lookupContacts = get(),
            createContact = get(),
            updateContact = get(),
            deleteContact = get()
        )
    }
    container<ContactMergeContainer, ContactMergeState, ContactMergeIntent, ContactMergeAction> {
            (params: ContactMergeContainer.Params) ->
        ContactMergeContainer(
            sourceContact = params.sourceContact,
            sourceActivity = params.sourceActivity,
            preselectedTarget = params.preselectedTarget,
            lookupContacts = get(),
            mergeContacts = get()
        )
    }

    // Create Contact flow (new VAT-first split flow)
    container<CreateContactContainer, CreateContactState, CreateContactIntent, CreateContactAction> {
        CreateContactContainer(
            searchCompanyUseCase = get(),
            findContactsByName = get(),
            findContactsByVat = get(),
            createContact = get()
        )
    }
}
