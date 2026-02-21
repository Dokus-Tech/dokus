package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_scope_all_documents
import tech.dokus.domain.model.ai.ChatScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.chips.PChoiceChips
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun ScopeSelectorChips(
    currentScope: ChatScope,
    onScopeChange: (ChatScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ChatScope.AllDocs)
    PChoiceChips(
        options = options,
        selected = currentScope.takeIf { it == ChatScope.AllDocs },
        onSelect = onScopeChange,
        optionLabel = { stringResource(Res.string.chat_scope_all_documents) },
        modifier = modifier,
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun ScopeSelectorChipsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ScopeSelectorChips(
            currentScope = ChatScope.AllDocs,
            onScopeChange = {}
        )
    }
}
