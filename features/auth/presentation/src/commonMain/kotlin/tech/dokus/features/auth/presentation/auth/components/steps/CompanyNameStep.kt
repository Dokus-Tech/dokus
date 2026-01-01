package tech.dokus.features.auth.presentation.auth.components.steps

import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_company_name_label
import tech.dokus.aura.resources.auth_company_name_prompt
import tech.dokus.aura.resources.auth_company_name_searching
import tech.dokus.aura.resources.auth_company_name_subtitle
import tech.dokus.foundation.aura.components.fields.PTextFieldWorkspaceName
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.extensions.localized
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

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
            onBackPress = onBackPress
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.auth_company_name_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PTextFieldWorkspaceName(
            fieldName = stringResource(Res.string.auth_company_name_label),
            value = companyName,
            enabled = lookupState !is LookupState.Loading,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onCompanyNameChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (lookupState) {
            is LookupState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_company_name_searching),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is LookupState.Error -> {
                Text(
                    text = lookupState.exception.localized,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> Unit
        }
    }
}
