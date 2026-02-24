package tech.dokus.features.contacts

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.features.contacts.datasource.ContactCacheDataSource
import tech.dokus.features.contacts.datasource.ContactCacheDataSourceImpl
import tech.dokus.features.contacts.datasource.ContactLocalDataSource
import tech.dokus.features.contacts.datasource.ContactLocalDataSourceImpl
import tech.dokus.features.contacts.datasource.ContactsDb
import tech.dokus.features.contacts.initializer.ContactsDataInitializer
import tech.dokus.features.contacts.initializer.ContactsDataInitializerImpl
import tech.dokus.features.contacts.repository.ContactRemoteDataSource
import tech.dokus.features.contacts.repository.ContactRemoteDataSourceImpl
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCaseImpl
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCase
import tech.dokus.features.contacts.usecases.CreateContactNoteUseCaseImpl
import tech.dokus.features.contacts.usecases.CreateContactUseCase
import tech.dokus.features.contacts.usecases.CreateContactUseCaseImpl
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCase
import tech.dokus.features.contacts.usecases.DeleteContactNoteUseCaseImpl
import tech.dokus.features.contacts.usecases.DeleteContactUseCase
import tech.dokus.features.contacts.usecases.DeleteContactUseCaseImpl
import tech.dokus.features.contacts.usecases.FindContactsByNameUseCase
import tech.dokus.features.contacts.usecases.FindContactsByNameUseCaseImpl
import tech.dokus.features.contacts.usecases.FindContactsByVatUseCase
import tech.dokus.features.contacts.usecases.FindContactsByVatUseCaseImpl
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCaseImpl
import tech.dokus.features.contacts.usecases.GetContactActivityUseCase
import tech.dokus.features.contacts.usecases.GetContactActivityUseCaseImpl
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCaseImpl
import tech.dokus.features.contacts.usecases.ListContactNotesUseCase
import tech.dokus.features.contacts.usecases.ListContactNotesUseCaseImpl
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCaseImpl
import tech.dokus.features.contacts.usecases.ListCustomersUseCase
import tech.dokus.features.contacts.usecases.ListCustomersUseCaseImpl
import tech.dokus.features.contacts.usecases.ListVendorsUseCase
import tech.dokus.features.contacts.usecases.ListVendorsUseCaseImpl
import tech.dokus.features.contacts.usecases.LookupContactsUseCase
import tech.dokus.features.contacts.usecases.LookupContactsUseCaseImpl
import tech.dokus.features.contacts.usecases.MergeContactsUseCase
import tech.dokus.features.contacts.usecases.MergeContactsUseCaseImpl
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCase
import tech.dokus.features.contacts.usecases.UpdateContactNoteUseCaseImpl
import tech.dokus.features.contacts.usecases.UpdateContactUseCase
import tech.dokus.features.contacts.usecases.UpdateContactUseCaseImpl

val contactsNetworkModule = module {
    single<ContactRemoteDataSource> {
        ContactRemoteDataSourceImpl(httpClient = get<HttpClient>())
    }
}

val contactsDataModule = module {
    // Database
    single { ContactsDb.create() }
    single { get<ContactsDb>().get() }

    // Initialization
    singleOf(::ContactsDataInitializerImpl) bind ContactsDataInitializer::class

    // Local data source
    singleOf(::ContactLocalDataSourceImpl) bind ContactLocalDataSource::class

    // Cache data source
    singleOf(::ContactCacheDataSourceImpl) bind ContactCacheDataSource::class
}

val contactsDomainModule = module {
    // List use cases
    single<ListContactsUseCase> { ListContactsUseCaseImpl(get()) }
    single<LookupContactsUseCase> { LookupContactsUseCaseImpl(get()) }
    single<ListCustomersUseCase> { ListCustomersUseCaseImpl(get()) }
    single<ListVendorsUseCase> { ListVendorsUseCaseImpl(get()) }
    single<FindContactsByNameUseCase> { FindContactsByNameUseCaseImpl(get()) }
    single<FindContactsByVatUseCase> { FindContactsByVatUseCaseImpl(get()) }

    // CRUD use cases
    single<GetContactUseCase> { GetContactUseCaseImpl(get()) }
    single<CreateContactUseCase> { CreateContactUseCaseImpl(get()) }
    single<UpdateContactUseCase> { UpdateContactUseCaseImpl(get()) }
    single<DeleteContactUseCase> { DeleteContactUseCaseImpl(get()) }

    // Cache use cases
    single<GetCachedContactsUseCase> { GetCachedContactsUseCaseImpl(get()) }
    single<CacheContactsUseCase> { CacheContactsUseCaseImpl(get()) }

    // Notes use cases
    single<ListContactNotesUseCase> { ListContactNotesUseCaseImpl(get()) }
    single<CreateContactNoteUseCase> { CreateContactNoteUseCaseImpl(get()) }
    single<UpdateContactNoteUseCase> { UpdateContactNoteUseCaseImpl(get()) }
    single<DeleteContactNoteUseCase> { DeleteContactNoteUseCaseImpl(get()) }

    // Activity use cases
    single<GetContactActivityUseCase> { GetContactActivityUseCaseImpl(get()) }
    single<MergeContactsUseCase> { MergeContactsUseCaseImpl(get()) }
}
