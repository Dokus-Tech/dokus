package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.ids.ContactId

interface ObserveContactChangesUseCase {
    operator fun invoke(contactId: ContactId): Flow<Unit>
}
