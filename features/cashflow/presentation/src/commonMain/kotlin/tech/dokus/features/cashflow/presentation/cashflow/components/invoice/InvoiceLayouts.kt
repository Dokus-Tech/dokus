package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.action_next
import tech.dokus.aura.resources.cashflow_create_invoice
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.invoice_contact_search_help
import tech.dokus.aura.resources.invoice_contact_search_label
import tech.dokus.aura.resources.invoice_contact_search_placeholder
import tech.dokus.aura.resources.invoice_edit_hint_desktop
import tech.dokus.aura.resources.invoice_edit_hint_mobile
import tech.dokus.aura.resources.invoice_number_preview
import tech.dokus.aura.resources.invoice_select_client
import tech.dokus.aura.resources.invoice_selected_contact
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.presentation.contacts.components.ContactAutoFillData
import tech.dokus.features.contacts.presentation.contacts.components.ContactAutocomplete
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Desktop layout constants
private val DesktopHorizontalPadding = 32.dp
private val DesktopColumnSpacing = 24.dp
private val DesktopSectionSpacing = 16.dp
private val DesktopTopSpacing = 8.dp
private val DesktopSendOptionsMinWidth = 320.dp
private const val DesktopMainColumnWeight = 1.6f

// Mobile layout constants
private val MobileHorizontalPadding = 16.dp
private val MobileSectionSpacing = 16.dp
private val MobileTopSpacing = 8.dp
private val MobileBottomSpacing = 16.dp

// Contact selection panel constants
private const val PanelFadeDurationMs = 200
private const val PanelSlideDurationMs = 300
private const val ScrimAlpha = 0.32f
private const val SidebarWidthFraction = 3
private val SidebarMinWidth = 320.dp
private val SidebarMaxWidth = 400.dp
private val SelectedContactSpacing = 8.dp

@Composable
fun DesktopInvoiceLayout(
    contentPadding: PaddingValues,
    invoiceNumberPreview: String?,
    onBackPress: () -> Unit,
    invoiceContent: @Composable () -> Unit,
    sendOptionsContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = DesktopHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(DesktopColumnSpacing)
    ) {
        Column(
            modifier = Modifier
                .weight(DesktopMainColumnWeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesktopSectionSpacing)
        ) {
            Spacer(modifier = Modifier.height(DesktopTopSpacing))
            SectionTitle(
                text = stringResource(Res.string.cashflow_create_invoice),
                onBackPress = onBackPress
            )
            if (invoiceNumberPreview != null) {
                Text(
                    text = stringResource(Res.string.invoice_number_preview, invoiceNumberPreview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(Res.string.invoice_edit_hint_desktop),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            invoiceContent()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = DesktopSendOptionsMinWidth)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(DesktopTopSpacing))
            sendOptionsContent()
            Spacer(modifier = Modifier.height(DesktopTopSpacing))
        }
    }
}

@Composable
fun MobileInvoiceEditLayout(
    contentPadding: PaddingValues,
    invoiceNumberPreview: String?,
    onBackPress: () -> Unit,
    invoiceContent: @Composable () -> Unit,
    onNextClick: () -> Unit,
    isNextEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MobileHorizontalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MobileSectionSpacing)
        ) {
            Spacer(modifier = Modifier.height(MobileTopSpacing))
            SectionTitle(
                text = stringResource(Res.string.cashflow_create_invoice),
                onBackPress = onBackPress
            )
            if (invoiceNumberPreview != null) {
                Text(
                    text = stringResource(Res.string.invoice_number_preview, invoiceNumberPreview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(Res.string.invoice_edit_hint_mobile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            invoiceContent()
            Spacer(modifier = Modifier.height(MobileBottomSpacing))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MobileHorizontalPadding),
            horizontalArrangement = Arrangement.End
        ) {
            PButton(
                text = stringResource(Res.string.action_next),
                variant = PButtonVariant.Default,
                onClick = onNextClick,
                isEnabled = isNextEnabled
            )
        }
    }
}

@Preview
@Composable
private fun DesktopInvoiceLayoutPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DesktopInvoiceLayout(
            contentPadding = PaddingValues(0.dp),
            invoiceNumberPreview = "INV-001",
            onBackPress = {},
            invoiceContent = { Text("Invoice content") },
            sendOptionsContent = { Text("Send options") }
        )
    }
}

@Preview
@Composable
private fun MobileInvoiceEditLayoutPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MobileInvoiceEditLayout(
            contentPadding = PaddingValues(0.dp),
            invoiceNumberPreview = "INV-001",
            onBackPress = {},
            invoiceContent = { Text("Invoice content") },
            onNextClick = {},
            isNextEnabled = true
        )
    }
}

@Composable
fun ContactSelectionPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedContact: ContactDto?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onContactSelected: (ContactAutoFillData) -> Unit,
    onAddNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(PanelFadeDurationMs)),
            exit = fadeOut(tween(PanelFadeDurationMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(PanelSlideDurationMs)
            ) + fadeIn(tween(PanelSlideDurationMs)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(PanelSlideDurationMs)
            ) + fadeOut(tween(PanelSlideDurationMs)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / SidebarWidthFraction).coerceIn(SidebarMinWidth, SidebarMaxWidth)

                DokusCardSurface(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Constraints.Spacing.medium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.invoice_select_client),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.action_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

                        ContactAutocomplete(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            selectedContact = selectedContact,
                            onContactSelected = onContactSelected,
                            onAddNewContact = onAddNewContact,
                            placeholder = stringResource(Res.string.invoice_contact_search_placeholder),
                            label = stringResource(Res.string.invoice_contact_search_label),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

                        Text(
                            text = stringResource(Res.string.invoice_contact_search_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (selectedContact != null) {
                            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
                            Text(
                                text = stringResource(Res.string.invoice_selected_contact),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(SelectedContactSpacing))
                            Text(
                                text = selectedContact.name.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(
                                    Res.string.common_vat_value,
                                    selectedContact.vatNumber?.value ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
