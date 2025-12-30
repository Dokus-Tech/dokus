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
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                text = "[#]",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = gold,
                letterSpacing = (-0.6).sp
            )
        }

        Spacer(Modifier.width(10.dp))

        // Wordmark (clean)
        Text(
            text = "Dokus",
            style = MaterialTheme.typography.titleLarge, // use your display font if you want here
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            color = onSurface
        )
    }
}