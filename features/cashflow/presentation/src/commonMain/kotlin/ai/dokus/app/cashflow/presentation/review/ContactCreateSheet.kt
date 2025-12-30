package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.contacts.usecases.CreateContactUseCase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_address_placeholder
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_email_placeholder
import tech.dokus.aura.resources.contacts_name
import tech.dokus.aura.resources.contacts_name_placeholder
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.contacts_vat_placeholder
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

/**
 * Simplified contact creation sheet embedded in Document Review flow.
 * Pre-fills fields from extracted document data.
 *
 * On successful creation, calls onContactCreated with the new contact ID,
 * which triggers the container to bind the contact to the document.
 *
 * @param isVisible Whether the sheet is visible
 * @param onDismiss Callback when sheet is dismissed
 * @param preFillData Optional data extracted from document
 * @param onContactCreated Callback with new contact ID on success
 */
@Composable
fun ContactCreateSheet(
    isVisible: Boolean,
    isLargeScreen: Boolean,
    onDismiss: () -> Unit,
    preFillData: ContactPreFillData?,
    onContactCreated: (ContactId) -> Unit,
    createContactUseCase: CreateContactUseCase = koinInject(),
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Form state
    var name by remember(preFillData) { mutableStateOf(preFillData?.name ?: "") }
    var vatNumber by remember(preFillData) { mutableStateOf(preFillData?.vatNumber ?: "") }
    var email by remember(preFillData) { mutableStateOf(preFillData?.email ?: "") }
    var address by remember(preFillData) { mutableStateOf(preFillData?.address ?: "") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<DokusException?>(null) }

    val canSubmit = name.isNotBlank() && !isSubmitting

    fun submit() {
        if (!canSubmit) return

        isSubmitting = true
        errorMessage = null

        scope.launch {
            val request = CreateContactRequest(
                name = name.trim(),
                vatNumber = vatNumber.trim().takeIf { it.isNotBlank() },
                email = email.trim().takeIf { it.isNotBlank() },
                addressLine1 = address.trim().takeIf { it.isNotBlank() },
            )

            createContactUseCase(request).fold(
                onSuccess = { contact ->
                    isSubmitting = false
                    onContactCreated(contact.id)
                },
                onFailure = { error ->
                    isSubmitting = false
                    val exception = error.asDokusException
                    errorMessage = if (exception is DokusException.Unknown) {
                        DokusException.ContactCreateFailed
                    } else {
                        exception
                    }
                }
            )
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = isLargeScreen),
        ) {
            Surface(
                modifier = modifier
                    .then(
                        if (isLargeScreen) {
                            Modifier.widthIn(min = 480.dp, max = 640.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        }
                    ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Constrains.Spacing.large),
                ) {
                    SheetHeader(
                        onClose = onDismiss,
                        isSubmitting = isSubmitting,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Constrains.Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium),
                    ) {
                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                errorMessage?.let { exception ->
                                    Text(
                                        text = exception.localized,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(Constrains.Spacing.small),
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(Res.string.contacts_name)) },
                            placeholder = { Text(stringResource(Res.string.contacts_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSubmitting,
                            isError = name.isBlank(),
                        )

                        OutlinedTextField(
                            value = vatNumber,
                            onValueChange = { vatNumber = it },
                            label = { Text(stringResource(Res.string.contacts_vat_number)) },
                            placeholder = { Text(stringResource(Res.string.contacts_vat_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSubmitting,
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(Res.string.contacts_email)) },
                            placeholder = { Text(stringResource(Res.string.contacts_email_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isSubmitting,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(stringResource(Res.string.contacts_address)) },
                            placeholder = { Text(stringResource(Res.string.contacts_address_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            enabled = !isSubmitting,
                        )

                        Spacer(modifier = Modifier.height(Constrains.Spacing.small))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting,
                            ) {
                                Text(stringResource(Res.string.action_cancel))
                            }

                            Button(
                                onClick = { submit() },
                                modifier = Modifier.weight(1f),
                                enabled = canSubmit,
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(Res.string.contacts_create_contact))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    onClose: () -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constrains.Spacing.medium,
                vertical = Constrains.Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(48.dp))

        Text(
            text = stringResource(Res.string.contacts_create_contact),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )

        IconButton(
            onClick = onClose,
            enabled = !isSubmitting,
        ) {
            PIcon(
                icon = FeatherIcons.X,
                description = stringResource(Res.string.action_close),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
