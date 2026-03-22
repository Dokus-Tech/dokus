package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.navigation.local.LocalNavController

/**
 * Key used to pass the ignore result back to the previous screen via savedStateHandle.
 * Format: "transactionId:reasonName"
 */
const val IGNORE_RESULT_KEY = "ignore_result"

/**
 * Navigation-aware route composable for the ignore reason dialog.
 * Manages reason selection locally and passes the result back
 * to the parent via [savedStateHandle][androidx.lifecycle.SavedStateHandle].
 */
@Composable
internal fun IgnoreReasonDialogRoute(transactionId: String) {
    val navController = LocalNavController.current
    var selectedReason by remember { mutableStateOf<IgnoredReason?>(null) }

    IgnoreReasonDialogContent(
        selectedReason = selectedReason,
        onReasonSelected = { selectedReason = it },
        onConfirm = {
            val reason = selectedReason ?: return@IgnoreReasonDialogContent
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(IGNORE_RESULT_KEY, "$transactionId:${reason.name}")
            navController.popBackStack()
        },
        onDismiss = { navController.popBackStack() },
    )
}
