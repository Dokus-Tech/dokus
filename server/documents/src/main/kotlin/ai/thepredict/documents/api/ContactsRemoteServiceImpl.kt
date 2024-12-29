package ai.thepredict.documents.api

import ai.thepredict.common.UserIdGetter
import kotlin.coroutines.CoroutineContext

class DocumentsRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : DocumentsRemoteService