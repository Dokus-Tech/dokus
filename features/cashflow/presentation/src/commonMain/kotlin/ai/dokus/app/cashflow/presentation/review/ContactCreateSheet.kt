package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.contacts.usecases.CreateContactUseCase
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_cancel
import ai.dokus.app.resources.generated.action_close
import ai.dokus.app.resources.generated.contacts_address
import ai.dokus.app.resources.generated.contacts_address_placeholder
import ai.dokus.app.resources.generated.contacts_create_contact
import ai.dokus.app.resources.generated.contacts_email
import ai.dokus.app.resources.generated.contacts_email_placeholder
import ai.dokus.app.resources.generated.contacts_name
import ai.dokus.app.resources.generated.contacts_name_placeholder
import ai.dokus.app.resources.generated.contacts_vat_number
import ai.dokus.app.resources.generated.contacts_vat_placeholder
import ai.dokus.foundation.design.extensions.localized
import ai.dokus.foundation.design.components.PIcon
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCreateSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    preFillData: ContactPreFillData?,
    onContactCreated: (ContactId) -> Unit,
    createContactUseCase: CreateContactUseCase = koinInject(),
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val scope = rememberCoroutineScope()

    // Form state
    var name by remember(preFillData) { mutableStateOf(preFillData?.name ?: "") }
    var vatNumber by remember(preFillData) { mutableStateOf(preFillData?.vatNumber ?: "") }
    var email by remember(preFillData) { mutableStateOf(preFillData?.email ?: "") }
    var address by remember(preFillData) { mutableStateOf(preFillData?.address ?: "") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<DokusException?>(null) }

    val canSubmit = name.isNotBlank() && !isSubmitting

    // Handle submission
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
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constrains.Spacing.large),
            ) {
                // Header
                SheetHeader(
                    onClose = onDismiss,
                    isSubmitting = isSubmitting,
                )

                // Form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Constrains.Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium),
                ) {
                    // Error banner
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

                    // Name field (required)
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

                    // VAT number field
                    OutlinedTextField(
                        value = vatNumber,
                        onValueChange = { vatNumber = it },
                        label = { Text(stringResource(Res.string.contacts_vat_number)) },
                        placeholder = { Text(stringResource(Res.string.contacts_vat_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSubmitting,
                    )

                    // Email field
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

                    // Address field
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

                    // Action buttons
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
