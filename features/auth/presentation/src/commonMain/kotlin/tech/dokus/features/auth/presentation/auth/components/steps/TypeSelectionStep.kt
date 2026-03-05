package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Description
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
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.auth_workspace_type_bookkeeper_description
import tech.dokus.aura.resources.auth_workspace_type_company_description
import tech.dokus.aura.resources.auth_workspace_type_freelancer_description
import tech.dokus.aura.resources.auth_workspace_type_prompt
import tech.dokus.aura.resources.auth_workspace_type_unavailable
import tech.dokus.aura.resources.workspace_type_bookkeeper
import tech.dokus.aura.resources.workspace_type_company
import tech.dokus.aura.resources.workspace_type_freelancer
import tech.dokus.features.auth.presentation.auth.model.WorkspaceCreateType
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PBackIconButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize

private val TypeCardMinHeight = 160.dp

@Composable
internal fun TypeSelectionStep(
    selectedType: WorkspaceCreateType,
    hasFreelancerWorkspace: Boolean,
    onTypeSelected: (WorkspaceCreateType) -> Unit,
    onBackPress: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    DokusGlassSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.xLarge),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                PBackIconButton(
                    onClick = onBackPress,
                    contentDescription = stringResource(Res.string.action_back),
                )
                Text(
                    text = stringResource(Res.string.auth_workspace_type_prompt),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (isLargeScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    TypeCard(
                        icon = Icons.Outlined.Business,
                        title = stringResource(Res.string.workspace_type_company),
                        description = stringResource(Res.string.auth_workspace_type_company_description),
                        isSelected = selectedType == WorkspaceCreateType.Company,
                        isEnabled = true,
                        onClick = { onTypeSelected(WorkspaceCreateType.Company) },
                        modifier = Modifier.weight(1f),
                    )
                    TypeCard(
                        icon = Icons.Outlined.Person,
                        title = stringResource(Res.string.workspace_type_freelancer),
                        description = stringResource(Res.string.auth_workspace_type_freelancer_description),
                        isSelected = selectedType == WorkspaceCreateType.Freelancer,
                        isEnabled = !hasFreelancerWorkspace,
                        onClick = { onTypeSelected(WorkspaceCreateType.Freelancer) },
                        modifier = Modifier.weight(1f),
                    )
                    TypeCard(
                        icon = Icons.Outlined.Description,
                        title = stringResource(Res.string.workspace_type_bookkeeper),
                        description = stringResource(Res.string.auth_workspace_type_bookkeeper_description),
                        isSelected = selectedType == WorkspaceCreateType.Bookkeeper,
                        isEnabled = true,
                        onClick = { onTypeSelected(WorkspaceCreateType.Bookkeeper) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    TypeCard(
                        icon = Icons.Outlined.Business,
                        title = stringResource(Res.string.workspace_type_company),
                        description = stringResource(Res.string.auth_workspace_type_company_description),
                        isSelected = selectedType == WorkspaceCreateType.Company,
                        isEnabled = true,
                        onClick = { onTypeSelected(WorkspaceCreateType.Company) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TypeCard(
                        icon = Icons.Outlined.Person,
                        title = stringResource(Res.string.workspace_type_freelancer),
                        description = stringResource(Res.string.auth_workspace_type_freelancer_description),
                        isSelected = selectedType == WorkspaceCreateType.Freelancer,
                        isEnabled = !hasFreelancerWorkspace,
                        onClick = { onTypeSelected(WorkspaceCreateType.Freelancer) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TypeCard(
                        icon = Icons.Outlined.Description,
                        title = stringResource(Res.string.workspace_type_bookkeeper),
                        description = stringResource(Res.string.auth_workspace_type_bookkeeper_description),
                        isSelected = selectedType == WorkspaceCreateType.Bookkeeper,
                        isEnabled = true,
                        onClick = { onTypeSelected(WorkspaceCreateType.Bookkeeper) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (hasFreelancerWorkspace) {
                Text(
                    text = stringResource(Res.string.auth_workspace_type_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(Constraints.Spacing.small))

            PPrimaryButton(
                text = stringResource(Res.string.action_continue),
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue,
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
        modifier = modifier.height(TypeCardMinHeight),
        variant = DokusCardVariant.Soft,
        onClick = onClick,
        enabled = isEnabled,
        accent = isSelected,
        shadow = isSelected,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.small),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.height(Constraints.Spacing.small))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
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
            selectedType = WorkspaceCreateType.Company,
            hasFreelancerWorkspace = false,
            onTypeSelected = {},
            onBackPress = {},
            onContinue = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Type Selection Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun TypeSelectionStepDesktopPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        TypeSelectionStep(
            selectedType = WorkspaceCreateType.Bookkeeper,
            hasFreelancerWorkspace = true,
            onTypeSelected = {},
            onBackPress = {},
            onContinue = {},
        )
    }
}
