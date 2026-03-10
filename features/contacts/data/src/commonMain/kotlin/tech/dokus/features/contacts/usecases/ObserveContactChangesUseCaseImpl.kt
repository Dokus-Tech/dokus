package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.repository.ContactRemoteDataSource
import kotlin.time.Duration.Companion.milliseconds

private val CoalesceWindow = 250.milliseconds

internal class ObserveContactChangesUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource,
) : ObserveContactChangesUseCase {
    @OptIn(FlowPreview::class)
    override fun invoke(contactId: ContactId): Flow<Unit> {
        return remoteDataSource.observeContactChanges(contactId)
            .conflate()
            .debounce(CoalesceWindow)
    }
}
