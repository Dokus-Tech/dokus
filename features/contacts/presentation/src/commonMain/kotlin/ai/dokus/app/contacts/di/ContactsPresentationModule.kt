package ai.dokus.app.contacts.di

import org.koin.dsl.module

val contactsViewModelModule = module {
    // ViewModels will be added here when needed
}

val contactsPresentationModule = module {
    includes(contactsViewModelModule)
}
