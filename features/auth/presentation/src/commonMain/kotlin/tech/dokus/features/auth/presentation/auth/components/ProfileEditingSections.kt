package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_cancel
import tech.dokus.aura.resources.profile_email
import tech.dokus.aura.resources.profile_first_name
import tech.dokus.aura.resources.profile_last_name
import tech.dokus.aura.resources.profile_personal_info
import tech.dokus.aura.resources.profile_save
import tech.dokus.domain.Name
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldName

@Composable
internal fun ProfileEditingSection(
    email: String,
    editFirstName: Name,
    editLastName: Name,
    canSave: Boolean,
    onFirstNameChange: (Name) -> Unit,
    onLastNameChange: (Name) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = email
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_first_name),
                value = editFirstName,
                icon = null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onFirstNameChange
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_last_name),
                value = editLastName,
                icon = null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                onAction = { if (canSave) onSave() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onLastNameChange
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                POutlinedButton(
                    text = stringResource(Res.string.profile_cancel),
                    onClick = onCancel
                )
                Spacer(Modifier.width(8.dp))
                PPrimaryButton(
                    text = stringResource(Res.string.profile_save),
                    enabled = canSave,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
internal fun ProfileSavingSection(
    email: String,
    editFirstName: Name,
    editLastName: Name,
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(label = stringResource(Res.string.profile_email), value = email)
            Spacer(Modifier.height(12.dp))
            ProfileField(label = stringResource(Res.string.profile_first_name), value = editFirstName.value)
            Spacer(Modifier.height(12.dp))
            ProfileField(label = stringResource(Res.string.profile_last_name), value = editLastName.value)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.height(40.dp).width(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
                }
            }
        }
    }
}

@Composable
internal fun ProfileField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
