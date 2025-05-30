package ai.thepredict.documents.api

import ai.thepredict.apispec.service.DocumentsRemoteService
import ai.thepredict.common.UserIdGetter
import kotlin.coroutines.CoroutineContext

class DocumentsRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : DocumentsRemoteService