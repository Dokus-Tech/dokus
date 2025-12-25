package ai.dokus.app.contacts

import ai.dokus.app.contacts.datasource.ContactLocalDataSource
import ai.dokus.app.contacts.datasource.ContactLocalDataSourceImpl
import ai.dokus.app.contacts.datasource.ContactsDb
import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import ai.dokus.app.contacts.repository.ContactRemoteDataSourceImpl
import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.app.contacts.usecases.CacheContactsUseCase
import ai.dokus.app.contacts.usecases.CacheContactsUseCaseImpl
import ai.dokus.app.contacts.usecases.CreateContactNoteUseCase
import ai.dokus.app.contacts.usecases.CreateContactNoteUseCaseImpl
import ai.dokus.app.contacts.usecases.CreateContactUseCase
import ai.dokus.app.contacts.usecases.CreateContactUseCaseImpl
import ai.dokus.app.contacts.usecases.DeleteContactNoteUseCase
import ai.dokus.app.contacts.usecases.DeleteContactNoteUseCaseImpl
import ai.dokus.app.contacts.usecases.DeleteContactUseCase
import ai.dokus.app.contacts.usecases.DeleteContactUseCaseImpl
import ai.dokus.app.contacts.usecases.GetCachedContactsUseCase
import ai.dokus.app.contacts.usecases.GetCachedContactsUseCaseImpl
import ai.dokus.app.contacts.usecases.GetContactActivityUseCase
import ai.dokus.app.contacts.usecases.GetContactActivityUseCaseImpl
import ai.dokus.app.contacts.usecases.GetContactUseCase
import ai.dokus.app.contacts.usecases.GetContactUseCaseImpl
import ai.dokus.app.contacts.usecases.ListContactNotesUseCase
import ai.dokus.app.contacts.usecases.ListContactNotesUseCaseImpl
import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.app.contacts.usecases.ListContactsUseCaseImpl
import ai.dokus.app.contacts.usecases.ListCustomersUseCase
import ai.dokus.app.contacts.usecases.ListCustomersUseCaseImpl
import ai.dokus.app.contacts.usecases.ListVendorsUseCase
import ai.dokus.app.contacts.usecases.ListVendorsUseCaseImpl
import ai.dokus.app.contacts.usecases.MergeContactsUseCase
import ai.dokus.app.contacts.usecases.MergeContactsUseCaseImpl
import ai.dokus.app.contacts.usecases.UpdateContactNoteUseCase
import ai.dokus.app.contacts.usecases.UpdateContactNoteUseCaseImpl
import ai.dokus.app.contacts.usecases.UpdateContactPeppolUseCase
import ai.dokus.app.contacts.usecases.UpdateContactPeppolUseCaseImpl
import ai.dokus.app.contacts.usecases.UpdateContactUseCase
import ai.dokus.app.contacts.usecases.UpdateContactUseCaseImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

val contactsNetworkModule = module {
    single<ContactRemoteDataSource> {
        ContactRemoteDataSourceImpl(httpClient = get<HttpClient>())
    }
}

val contactsDataModule = module {
    // Database
    single { ContactsDb.create() }
    single { get<ContactsDb>().get() }

    // Local data source
    single<ContactLocalDataSource> {
        ContactLocalDataSourceImpl(database = get())
    }

    // Repository
    single {
        ContactRepository(
            remoteDataSource = get<ContactRemoteDataSource>(),
            localDataSource = get<ContactLocalDataSource>()
        )
    }
}

val contactsDomainModule = module {
    // List use cases
    single<ListContactsUseCase> { ListContactsUseCaseImpl(get()) }
    single<ListCustomersUseCase> { ListCustomersUseCaseImpl(get()) }
    single<ListVendorsUseCase> { ListVendorsUseCaseImpl(get()) }

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
    single<UpdateContactPeppolUseCase> { UpdateContactPeppolUseCaseImpl(get()) }
    single<MergeContactsUseCase> { MergeContactsUseCaseImpl(get()) }
}
