package ai.thepredict.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import compose.icons.FeatherIcons
import compose.icons.feathericons.Feather

@Composable
@Preview
fun PIconPreview() {
    PreviewWrapper {
        PIcon(FeatherIcons.Feather, "Feather")
    }
}

@Composable
@Preview
fun PIconPreviewError() {
    PreviewWrapper {
        PIcon(FeatherIcons.Feather, "Feather", isError = true)
    }
}