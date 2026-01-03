package tech.dokus.features.contacts.datasource

import tech.dokus.domain.model.common.Feature
import tech.dokus.features.contacts.cache.ContactsCacheDatabase
import tech.dokus.foundation.app.database.DatabaseWrapper

/**
 * Database wrapper for the Contacts cache database.
 * Handles thread-safe initialization and provides access to the ContactsCacheDatabase.
 */
class ContactsDb private constructor() : DatabaseWrapper<ContactsCacheDatabase> by DatabaseWrapper(
    feature = Feature.Contacts,
    schema = ContactsCacheDatabase.Schema,
    createDatabase = { driver -> ContactsCacheDatabase(driver) }
) {
    companion object {
        fun create(): ContactsDb = ContactsDb()
    }
}
