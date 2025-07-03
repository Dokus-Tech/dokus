package ai.thepredict.ui

import androidx.compose.runtime.Composable

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    Themed { content() }
}