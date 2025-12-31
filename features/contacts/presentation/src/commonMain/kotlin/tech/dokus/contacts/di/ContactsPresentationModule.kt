package tech.dokus.contacts.di

import org.koin.dsl.module
import tech.dokus.contacts.viewmodel.ContactDetailsAction
import tech.dokus.contacts.viewmodel.ContactDetailsContainer
import tech.dokus.contacts.viewmodel.ContactDetailsIntent
import tech.dokus.contacts.viewmodel.ContactDetailsState
import tech.dokus.contacts.viewmodel.ContactFormAction
import tech.dokus.contacts.viewmodel.ContactFormContainer
import tech.dokus.contacts.viewmodel.ContactFormIntent
import tech.dokus.contacts.viewmodel.ContactFormState
import tech.dokus.contacts.viewmodel.ContactMergeAction
import tech.dokus.contacts.viewmodel.ContactMergeContainer
import tech.dokus.contacts.viewmodel.ContactMergeIntent
import tech.dokus.contacts.viewmodel.ContactMergeState
import tech.dokus.contacts.viewmodel.ContactsAction
import tech.dokus.contacts.viewmodel.ContactsContainer
import tech.dokus.contacts.viewmodel.ContactsIntent
import tech.dokus.contacts.viewmodel.ContactsState
import tech.dokus.contacts.viewmodel.CreateContactAction
import tech.dokus.contacts.viewmodel.CreateContactContainer
import tech.dokus.contacts.viewmodel.CreateContactIntent
import tech.dokus.contacts.viewmodel.CreateContactState
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
    container<ContactMergeContainer, ContactMergeState, ContactMergeIntent, ContactMergeAction> { (params: ContactMergeContainer.Params) ->
        ContactMergeContainer(
            sourceContact = params.sourceContact,
            sourceActivity = params.sourceActivity,
            preselectedTarget = params.preselectedTarget,
            listContacts = get(),
            mergeContacts = get()
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
