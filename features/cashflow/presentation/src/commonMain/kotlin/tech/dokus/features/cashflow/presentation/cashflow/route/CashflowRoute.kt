package tech.dokus.features.cashflow.presentation.cashflow.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_invoice_create_success
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.mvi.CashflowAction
import tech.dokus.features.cashflow.mvi.CashflowContainer
import tech.dokus.features.cashflow.mvi.CashflowIntent
import tech.dokus.features.cashflow.mvi.CashflowSuccess
import tech.dokus.features.cashflow.presentation.cashflow.screen.CashflowScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CashflowRoute(
    container: CashflowContainer = container(),
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<CashflowSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            CashflowSuccess.InvoiceCreated -> stringResource(Res.string.cashflow_invoice_create_success)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CashflowAction.NavigateToDocument -> {
                // TODO: Navigate to document detail
            }
            is CashflowAction.NavigateToCreateInvoice -> {
                navController.navigateTo(CashFlowDestination.CreateInvoice)
            }
            is CashflowAction.NavigateToAddDocument -> {
                navController.navigateTo(CashFlowDestination.AddDocument)
            }
            is CashflowAction.NavigateToSettings -> {
                // TODO: Navigate to settings
            }
            is CashflowAction.ShowError -> {
                pendingError = action.error
            }
            is CashflowAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
        }
    }

    val uploadTasks by container.uploadTasks.collectAsState()
    val uploadedDocuments by container.uploadedDocuments.collectAsState()
    val deletionHandles by container.deletionHandles.collectAsState()

    ConnectionSnackbarEffect(snackbarHostState)

    LaunchedEffect(Unit) {
        container.store.intent(CashflowIntent.Refresh)
    }

    CashflowScreen(
        state = state,
        uploadTasks = uploadTasks,
        uploadedDocuments = uploadedDocuments,
        deletionHandles = deletionHandles,
        uploadManager = container.provideUploadManager(),
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onNavigateToAddDocument = { navController.navigateTo(CashFlowDestination.AddDocument) },
        onNavigateToCreateInvoice = { navController.navigateTo(CashFlowDestination.CreateInvoice) },
        onNavigateToDocumentReview = { documentId ->
            navController.navigateTo(CashFlowDestination.DocumentReview(documentId.toString()))
        }
    )
}
