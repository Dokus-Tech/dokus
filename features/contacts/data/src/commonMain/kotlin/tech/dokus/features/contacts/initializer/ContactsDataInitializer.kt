package tech.dokus.features.contacts.initializer

import tech.dokus.features.contacts.datasource.ContactsDb
import tech.dokus.foundation.app.AppDataInitializer

interface ContactsDataInitializer : AppDataInitializer

internal class ContactsDataInitializerImpl(
    private val contactsDb: ContactsDb
) : ContactsDataInitializer {
    override suspend fun initialize() {
        contactsDb.initialize()
    }
}
