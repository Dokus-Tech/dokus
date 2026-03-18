package tech.dokus.foundation.aura.components.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Renders markdown text in AI chat messages.
 * Supports bold, italic, code, tables, lists, headers, and links.
 * Styled to match the Dokus design system.
 */
@Composable
fun ChatMarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface,
            codeText = MaterialTheme.colorScheme.onSurface,
            codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            dividerColor = MaterialTheme.colorScheme.outlineVariant,
            linkText = MaterialTheme.colorScheme.primary,
        ),
        typography = markdownTypography(
            text = MaterialTheme.typography.bodyMedium,
            h1 = MaterialTheme.typography.titleLarge,
            h2 = MaterialTheme.typography.titleMedium,
            h3 = MaterialTheme.typography.titleSmall,
            h4 = MaterialTheme.typography.labelLarge,
            h5 = MaterialTheme.typography.labelMedium,
            h6 = MaterialTheme.typography.labelSmall,
            code = MaterialTheme.typography.bodySmall,
            quote = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.textMuted,
            ),
        ),
    )
}

@Preview
@Composable
private fun ChatMarkdownTextPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatMarkdownText(
            content = """
                # Invoice Summary

                Here are your **biggest expenses** in Q4:

                | Vendor | Amount |
                |--------|--------|
                | SRL Accounting | €798.60 |
                | Tesla Belgium | €346.97 |

                Total: *€1,145.57*

                > Note: These are `confirmed` documents only.
            """.trimIndent()
        )
    }
}
