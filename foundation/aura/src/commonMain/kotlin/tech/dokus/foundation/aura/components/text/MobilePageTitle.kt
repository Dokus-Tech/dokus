package tech.dokus.foundation.aura.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Page title rendered inside scrollable content on mobile.
 * On desktop this renders nothing — the shell header handles titles.
 *
 * v2 spec: 22sp, 700 weight, -0.03em tracking, margin-bottom 16dp.
 */
@Composable
fun MobilePageTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    if (LocalScreenSize.isLarge) return
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.66).sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 16.dp),
    )
}

@Preview
@Composable
private fun MobilePageTitlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MobilePageTitle(title = "Dashboard")
    }
}
