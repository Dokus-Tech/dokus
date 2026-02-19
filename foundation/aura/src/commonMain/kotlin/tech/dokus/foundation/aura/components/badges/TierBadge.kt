package tech.dokus.foundation.aura.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber

private val BadgeRadius = 5.dp
private val PaddingH = 10.dp
private val PaddingV = 3.dp

/**
 * Subscription tier badge.
 *
 * Uses: Profile popover, team page.
 *
 * @param label Tier name (e.g. "Core", "Owner")
 */
@Composable
fun TierBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(BadgeRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.amberSoft)
            .border(1.dp, MaterialTheme.colorScheme.borderAmber, shape)
            .padding(horizontal = PaddingH, vertical = PaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}
