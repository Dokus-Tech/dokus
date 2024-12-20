package ai.thepredict.shared.api

import ai.thepredict.domain.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ContactsApi : RemoteService {
    suspend fun my(): Flow<Contact>
}