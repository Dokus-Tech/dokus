package ai.dokus.app.contacts.di

import ai.dokus.app.contacts.cache.ContactLocalDataSource
import ai.dokus.app.contacts.cache.ContactLocalDataSourceImpl
import ai.dokus.app.contacts.cache.ContactsCacheDatabase
import ai.dokus.app.contacts.cache.ContactsDb
import ai.dokus.app.contacts.repository.CachedContactRepository
import ai.dokus.app.contacts.repository.ContactRepository
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
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import tech.dokus.foundation.app.mvi.container

val contactsRepositoryModule = module {
    // Remote repository (direct HTTP calls)
    singleOf(::ContactRepository)

    // Database wrapper for cache
    single { ContactsDb.create() }
    single<ContactsCacheDatabase> { get<ContactsDb>().get() }

    // Local data source for caching
    single<ContactLocalDataSource> {
        ContactLocalDataSourceImpl(get())
    }

    // Cached repository with cache-first pattern
    singleOf(::CachedContactRepository)
}

val contactsViewModelModule = module {
    // FlowMVI Containers
    container<ContactsContainer, ContactsState, ContactsIntent, ContactsAction> {
        ContactsContainer(
            contactRepository = get(),
            cachedContactRepository = get(),
            getCurrentTenantId = get()
        )
    }
    container<ContactDetailsContainer, ContactDetailsState, ContactDetailsIntent, ContactDetailsAction> { (params: ContactDetailsContainer.Companion.Params) ->
        ContactDetailsContainer(
            contactId = params.contactId,
            contactRepository = get(),
            localDataSource = get(),
            getCurrentTenantId = get()
        )
    }
    container<ContactFormContainer, ContactFormState, ContactFormIntent, ContactFormAction> { (params: ContactFormContainer.Companion.Params) ->
        ContactFormContainer(
            contactId = params.contactId,
            contactRepository = get()
        )
    }
}

val contactsPresentationModule = module {
    includes(contactsRepositoryModule)
    includes(contactsViewModelModule)
}
