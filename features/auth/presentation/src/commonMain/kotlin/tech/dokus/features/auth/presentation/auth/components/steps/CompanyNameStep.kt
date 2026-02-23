package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_company_name_label
import tech.dokus.aura.resources.auth_company_name_prompt
import tech.dokus.aura.resources.auth_company_name_searching
import tech.dokus.aura.resources.auth_company_name_subtitle
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.fields.PTextFieldWorkspaceName
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun CompanyNameStep(
    companyName: String,
    lookupState: LookupState,
    onCompanyNameChanged: (String) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionTitle(
            text = stringResource(Res.string.auth_company_name_prompt),
            horizontalArrangement = Arrangement.Start,
            onBackPress = onBackPress,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        Text(
            text = stringResource(Res.string.auth_company_name_subtitle),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        PTextFieldWorkspaceName(
            fieldName = stringResource(Res.string.auth_company_name_label),
            value = companyName,
            enabled = lookupState !is LookupState.Loading,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onCompanyNameChanged,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        when (lookupState) {
            is LookupState.Loading -> {
                DokusLoader(size = DokusLoaderSize.Small)
                Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
                Text(
                    text = stringResource(Res.string.auth_company_name_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is LookupState.Error -> {
                Text(
                    text = lookupState.exception.localized,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> Unit
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CompanyNameStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        CompanyNameStep(
            companyName = "Acme Corp",
            lookupState = LookupState.Idle,
            onCompanyNameChanged = {},
            onBackPress = {},
        )
    }
}
