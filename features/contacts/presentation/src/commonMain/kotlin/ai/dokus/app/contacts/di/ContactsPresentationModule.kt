package ai.dokus.app.contacts.di

import ai.dokus.app.contacts.viewmodel.ContactDetailsViewModel
import ai.dokus.app.contacts.viewmodel.ContactFormViewModel
import ai.dokus.app.contacts.viewmodel.ContactsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactsViewModelModule = module {
    viewModel { ContactsViewModel() }
    viewModel { ContactFormViewModel() }
    viewModel { ContactDetailsViewModel() }
}

val contactsPresentationModule = module {
    includes(contactsViewModelModule)
}
