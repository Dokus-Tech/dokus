package tech.dokus.features.cashflow.presentation.cashflow.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.cashflow.mvi.AddDocumentAction
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.mvi.AddDocumentIntent
import tech.dokus.features.cashflow.presentation.cashflow.components.rememberDocumentFilePicker
import tech.dokus.features.cashflow.presentation.cashflow.screen.AddDocumentScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun AddDocumentRoute(
    container: AddDocumentContainer = container()
) {
    val navController = LocalNavController.current
    val filePickerLauncher = rememberDocumentFilePicker { files ->
        if (files.isNotEmpty()) container.store.intent(AddDocumentIntent.Upload(files))
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            AddDocumentAction.LaunchFilePicker -> filePickerLauncher.launch()
            AddDocumentAction.NavigateBack -> navController.navigateUp()
        }
    }

    val uploadTasks by container.uploadTasks.collectAsState()
    val uploadedDocuments by container.uploadedDocuments.collectAsState()
    val deletionHandles by container.deletionHandles.collectAsState()

    AddDocumentScreen(
        state = state,
        uploadTasks = uploadTasks,
        uploadedDocuments = uploadedDocuments,
        deletionHandles = deletionHandles,
        uploadManager = container.provideUploadManager(),
        onIntent = { container.store.intent(it) }
    )
}
