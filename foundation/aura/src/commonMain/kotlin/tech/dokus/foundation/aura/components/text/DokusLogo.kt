package tech.dokus.foundation.aura.components.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

enum class DokusLogoEmphasis {
    Default,
    Hero,
}

object DokusLogo {

    @Composable
    fun Full(
        modifier: Modifier = Modifier,
        emphasis: DokusLogoEmphasis = DokusLogoEmphasis.Default,
    ) {
        val onSurface = MaterialTheme.colorScheme.onSurface
        val wordStyle = when (emphasis) {
            DokusLogoEmphasis.Default -> MaterialTheme.typography.headlineLarge
            DokusLogoEmphasis.Hero -> MaterialTheme.typography.displayMedium
        }.copy(letterSpacing = if (emphasis == DokusLogoEmphasis.Hero) (-0.03).em else (-0.02).em)

        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Mark(emphasis = emphasis)
            Spacer(modifier = Modifier.width(if (emphasis == DokusLogoEmphasis.Hero) 14.dp else 10.dp))
            Text(
                text = "Dokus",
                style = wordStyle,
                fontWeight = if (emphasis == DokusLogoEmphasis.Hero) FontWeight.Bold else FontWeight.SemiBold,
                color = onSurface,
            )
        }
    }

    @Composable
    fun Mark(
        modifier: Modifier = Modifier,
        emphasis: DokusLogoEmphasis = DokusLogoEmphasis.Default,
    ) {
        val gold = MaterialTheme.colorScheme.primary
        val shape = RoundedCornerShape(Constraints.CornerRadius.input)
        val horizontalPadding = if (emphasis == DokusLogoEmphasis.Hero) 12.dp else 9.dp
        val verticalPadding = if (emphasis == DokusLogoEmphasis.Hero) 7.dp else 5.dp
        val textStyle = if (emphasis == DokusLogoEmphasis.Hero) {
            MaterialTheme.typography.displaySmall
        } else {
            MaterialTheme.typography.titleMedium
        }

        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (emphasis == DokusLogoEmphasis.Hero) 0.92f else 0.86f),
            border = BorderStroke(1.dp, gold.copy(alpha = if (emphasis == DokusLogoEmphasis.Hero) 0.38f else 0.28f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                text = "[#]",
                style = textStyle,
                fontWeight = FontWeight.SemiBold,
                color = gold,
                letterSpacing = if (emphasis == DokusLogoEmphasis.Hero) (-0.06).em else (-0.07).em,
            )
        }
    }
}

@Preview
@Composable
private fun DokusLogoDefaultPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusLogo.Full()
    }
}

@Preview
@Composable
private fun DokusLogoHeroPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusLogo.Full(emphasis = DokusLogoEmphasis.Hero)
    }
}
