package tech.dokus.features.contacts.initializer

import tech.dokus.features.contacts.datasource.ContactsDb
import tech.dokus.foundation.app.AppDataInitializer

internal class ContactsDataInitializer(
    private val contactsDb: ContactsDb
) : AppDataInitializer {
    override suspend fun initialize() {
        contactsDb.initialize()
    }
}
