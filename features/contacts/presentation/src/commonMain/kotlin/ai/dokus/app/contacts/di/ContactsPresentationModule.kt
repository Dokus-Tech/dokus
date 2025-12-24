package ai.dokus.app.contacts.di

import ai.dokus.app.contacts.cache.ContactLocalDataSource
import ai.dokus.app.contacts.cache.ContactLocalDataSourceImpl
import ai.dokus.app.contacts.cache.ContactsCacheDatabase
import ai.dokus.app.contacts.cache.ContactsDb
import ai.dokus.app.contacts.repository.CachedContactRepository
import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.app.contacts.viewmodel.ContactDetailsViewModel
import ai.dokus.app.contacts.viewmodel.ContactFormViewModel
import ai.dokus.app.contacts.viewmodel.ContactsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

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
    viewModel { ContactsViewModel() }
    viewModel { ContactFormViewModel() }
    viewModel { ContactDetailsViewModel() }
}

val contactsPresentationModule = module {
    includes(contactsRepositoryModule)
    includes(contactsViewModelModule)
}
