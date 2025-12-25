package ai.dokus.app.contacts.datasource

import ai.dokus.app.contacts.cache.ContactsCacheDatabase
import ai.dokus.foundation.domain.model.common.Feature
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
