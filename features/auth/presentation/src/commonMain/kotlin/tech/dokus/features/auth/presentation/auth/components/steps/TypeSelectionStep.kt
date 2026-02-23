package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
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
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_workspace_type_company_description
import tech.dokus.aura.resources.auth_workspace_type_freelancer_description
import tech.dokus.aura.resources.auth_workspace_type_prompt
import tech.dokus.aura.resources.auth_workspace_type_unavailable
import tech.dokus.aura.resources.workspace_type_company
import tech.dokus.aura.resources.workspace_type_freelancer
import tech.dokus.domain.enums.TenantType
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints

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
            onBackPress = onBackPress,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        TypeCard(
            icon = Icons.Outlined.Business,
            title = stringResource(Res.string.workspace_type_company),
            description = stringResource(Res.string.auth_workspace_type_company_description),
            isSelected = selectedType == TenantType.Company,
            isEnabled = true,
            onClick = { onTypeSelected(TenantType.Company) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        TypeCard(
            icon = Icons.Outlined.Person,
            title = stringResource(Res.string.workspace_type_freelancer),
            description = stringResource(Res.string.auth_workspace_type_freelancer_description),
            isSelected = selectedType == TenantType.Freelancer,
            isEnabled = !hasFreelancerWorkspace,
            onClick = { onTypeSelected(TenantType.Freelancer) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (hasFreelancerWorkspace) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
            Text(
                text = stringResource(Res.string.auth_workspace_type_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TypeCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }

    DokusCardSurface(
        modifier = if (isSelected) {
            modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            )
        } else {
            modifier
        },
        onClick = onClick,
        enabled = isEnabled,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(46.dp),
            )

            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))

            Text(
                text = description,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TypeSelectionStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        TypeSelectionStep(
            selectedType = TenantType.Company,
            hasFreelancerWorkspace = false,
            onTypeSelected = {},
            onBackPress = {},
        )
    }
}
