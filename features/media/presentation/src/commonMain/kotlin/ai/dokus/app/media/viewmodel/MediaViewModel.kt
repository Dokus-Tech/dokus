package ai.dokus.app.media.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.app.media.domain.usecases.ListMediaUseCase
import ai.dokus.app.media.domain.usecases.ListPendingMediaUseCase
import ai.dokus.app.media.domain.usecases.UploadMediaUseCase
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MediaViewModel : BaseViewModel<MediaViewModel.State>(State.Loading), KoinComponent {

    private val uploadMediaUseCase: UploadMediaUseCase by inject()
    private val listMediaUseCase: ListMediaUseCase by inject()
    private val listPendingMediaUseCase: ListPendingMediaUseCase by inject()

    private val logger = Logger.forClass<MediaViewModel>()

    sealed interface State {
        data object Loading : State
        data class Content(
            val media: List<MediaDto>,
            val isUploading: Boolean = false,
            val errorMessage: String? = null
        ) : State

        data class Error(val exception: DokusException) : State
    }

    init {
        refresh()
    }

    fun refresh(status: MediaStatus? = null) = scope.launch {
        mutableState.value = when (val current = state.value) {
            is State.Content -> current.copy(errorMessage = null)
            else -> State.Loading
        }

        try {
            val media = if (status != null) {
                listMediaUseCase(status = status)
            } else {
                listPendingMediaUseCase(limit = 100)
            }

            mutableState.value = State.Content(
                media = media.sortedByDescending { it.updatedAt }
            )
        } catch (e: Exception) {
            logger.e(e) { "Failed to load media" }
            mutableState.value = State.Error(asDokusException(e))
        }
    }

    fun upload(request: MediaUploadRequest) = scope.launch {
        val currentMedia = (state.value as? State.Content)?.media.orEmpty()
        mutableState.value = State.Content(
            media = currentMedia,
            isUploading = true
        )

        try {
            uploadMediaUseCase(request)
            refresh()
        } catch (e: Exception) {
            logger.e(e) { "Failed to upload media" }
            mutableState.value = State.Content(
                media = currentMedia,
                isUploading = false,
                errorMessage = asDokusException(e).message
            )
        }
    }

    private fun asDokusException(error: Throwable): DokusException = when (error) {
        is DokusException -> error
        else -> DokusException.ConnectionError()
    }
}
