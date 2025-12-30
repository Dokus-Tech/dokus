package ai.dokus.app.auth.components.steps

import ai.dokus.app.auth.model.LookupState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.auth_company_name_label
import ai.dokus.app.resources.generated.auth_company_name_prompt
import ai.dokus.app.resources.generated.auth_company_name_searching
import ai.dokus.app.resources.generated.auth_company_name_subtitle
import ai.dokus.foundation.design.components.fields.PTextFieldWorkspaceName
import ai.dokus.foundation.design.components.text.SectionTitle
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
                    text = stringResource(lookupState.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> Unit
        }
    }
}
