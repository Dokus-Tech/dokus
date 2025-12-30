package ai.dokus.app.auth.components.steps

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.auth_workspace_type_company_description
import ai.dokus.app.resources.generated.auth_workspace_type_freelancer_description
import ai.dokus.app.resources.generated.auth_workspace_type_prompt
import ai.dokus.app.resources.generated.auth_workspace_type_unavailable
import ai.dokus.app.resources.generated.workspace_type_company
import ai.dokus.app.resources.generated.workspace_type_freelancer
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.constrains.Constrains
import tech.dokus.domain.enums.TenantType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TypeSelectionStep(
    selectedType: TenantType,
    hasFreelancerWorkspace: Boolean,
    onTypeSelected: (TenantType) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionTitle(
            text = stringResource(Res.string.auth_workspace_type_prompt),
            horizontalArrangement = Arrangement.Start,
            onBackPress = onBackPress
        )

        Spacer(modifier = Modifier.height(32.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth > Constrains.Breakpoint.SMALL.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    TypeCard(
                        type = TenantType.Company,
                        icon = Icons.Outlined.Business,
                        title = stringResource(Res.string.workspace_type_company),
                        description = stringResource(Res.string.auth_workspace_type_company_description),
                        isSelected = selectedType == TenantType.Company,
                        isEnabled = true,
                        onClick = { onTypeSelected(TenantType.Company) },
                        modifier = Modifier.weight(1f)
                    )
                    TypeCard(
                        type = TenantType.Freelancer,
                        icon = Icons.Outlined.Person,
                        title = stringResource(Res.string.workspace_type_freelancer),
                        description = stringResource(Res.string.auth_workspace_type_freelancer_description),
                        isSelected = selectedType == TenantType.Freelancer,
                        isEnabled = !hasFreelancerWorkspace,
                        onClick = { onTypeSelected(TenantType.Freelancer) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TypeCard(
                        type = TenantType.Company,
                        icon = Icons.Outlined.Business,
                        title = stringResource(Res.string.workspace_type_company),
                        description = stringResource(Res.string.auth_workspace_type_company_description),
                        isSelected = selectedType == TenantType.Company,
                        isEnabled = true,
                        onClick = { onTypeSelected(TenantType.Company) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TypeCard(
                        type = TenantType.Freelancer,
                        icon = Icons.Outlined.Person,
                        title = stringResource(Res.string.workspace_type_freelancer),
                        description = stringResource(Res.string.auth_workspace_type_freelancer_description),
                        isSelected = selectedType == TenantType.Freelancer,
                        isEnabled = !hasFreelancerWorkspace,
                        onClick = { onTypeSelected(TenantType.Freelancer) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (hasFreelancerWorkspace) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.auth_workspace_type_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TypeCard(
    type: TenantType,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        !isEnabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
