package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Preview(name = "Documents Add Sheet", widthDp = 390, heightDp = 844)
@Composable
private fun DocumentsAddDocumentSheetPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentsAddDocumentSheet(
            isVisible = true,
            onDismiss = {},
            onUploadFile = {}
        )
    }
}
