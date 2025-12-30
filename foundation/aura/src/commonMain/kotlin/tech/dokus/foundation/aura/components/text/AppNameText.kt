package tech.dokus.foundation.aura.components.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.style.brandGold

@Composable
fun AppNameText(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.brandGold
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Sigil
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.28f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                text = "[#]",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = gold,
                letterSpacing = (-0.8).sp
            )
        }

        Spacer(Modifier.width(10.dp))

        // Wordmark (mystical lockup with subtle gold spark)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = onSurface)) { append("Dokus") }
                // Mystical accent (subtle): a tiny gold spark dot, not a second word.
                withStyle(SpanStyle(color = gold.copy(alpha = 0.75f))) { append("·") }
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = onSurface
        )
    }
}