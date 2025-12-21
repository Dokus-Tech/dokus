package ai.dokus.foundation.design.components.tiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import ai.dokus.foundation.design.components.AvatarShape
import ai.dokus.foundation.design.components.AvatarSize
import ai.dokus.foundation.design.components.CompanyAvatarImage

/**
 * Small square tile showing a company's avatar (or initial if no avatar) with its name below.
 * Uses Material theme shapes and color scheme.
 *
 * @param initial The initial letter to display as fallback (typically first letter of company name)
 * @param label The company name to display below the avatar
 * @param avatarUrl Optional URL of the company avatar image
 * @param onClick Click handler for the tile
 * @param modifier Modifier to be applied to the tile
 */
@Composable
fun CompanyTile(
    modifier: Modifier = Modifier,
    initial: String,
    label: String,
    avatarUrl: String? = null,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            CompanyAvatarImage(
                avatarUrl = avatarUrl,
                initial = initial,
                size = AvatarSize.Medium,
                shape = AvatarShape.RoundedSquare,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Outlined variant with a plus icon for creating a new workspace/company.
 */
@Composable
fun AddCompanyTile(
    modifier: Modifier = Modifier,
    label: String = "Add workspace",
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedCard(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
