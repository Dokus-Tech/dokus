package tech.dokus.foundation.aura.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Represents an action button in a DokusDialog.
 *
 * @param text The button label
 * @param onClick Called when the button is clicked
 * @param isLoading Whether to show a loading indicator instead of text
 * @param isDestructive Whether this is a destructive action (uses error color)
 * @param enabled Whether the button is enabled
 */
@Immutable
data class DokusDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val isLoading: Boolean = false,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * A custom dialog component using glass-morphism design.
 * Replaces Material AlertDialog with a Dokus-styled modal.
 *
 * Features:
 * - Glass-morphism surface (DokusGlassSurface)
 * - Focus trap within dialog
 * - Keyboard navigation: Escape dismisses, Enter triggers primary action
 * - Scrollable content area
 * - Desktop-friendly max width constraint
 *
 * @param onDismissRequest Called when the user requests to dismiss the dialog
 * @param title The dialog title
 * @param modifier Modifier for the dialog surface
 * @param icon Optional icon displayed above the title
 * @param content The main content of the dialog
 * @param primaryAction The primary action button (right-most, emphasized)
 * @param secondaryAction Optional secondary action button (left of primary)
 * @param dismissOnBackPress Whether to dismiss when back/escape is pressed
 * @param dismissOnClickOutside Whether to dismiss when clicking outside the dialog
 * @param scrollableContent Whether the content area should be scrollable. Set to false when
 *   content contains its own scrollable elements like LazyColumn.
 */
@Composable
fun DokusDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
    primaryAction: DokusDialogAction,
    secondaryAction: DokusDialogAction? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    scrollableContent: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        )
    ) {
        DokusGlassSurface(
            modifier = modifier
                .widthIn(max = Constraints.DialogSize.maxWidth)
                .padding(horizontal = Constraints.Spacing.large)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Escape -> {
                                if (dismissOnBackPress) {
                                    onDismissRequest()
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.Enter -> {
                                if (primaryAction.enabled && !primaryAction.isLoading) {
                                    primaryAction.onClick()
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(Constraints.Spacing.xLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon (optional)
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.large))

                // Content (optionally scrollable)
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .then(
                            if (scrollableContent) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            }
                        )
                ) {
                    content()
                }

                Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary action (optional)
                    secondaryAction?.let { action ->
                        TextButton(
                            onClick = action.onClick,
                            enabled = action.enabled && !action.isLoading
                        ) {
                            Text(
                                text = action.text,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Primary action
                    TextButton(
                        onClick = primaryAction.onClick,
                        enabled = primaryAction.enabled && !primaryAction.isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (primaryAction.isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        AnimatedVisibility(
                            visible = primaryAction.isLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(Constraints.IconSize.buttonLoading)
                                    .padding(end = Constraints.Spacing.small),
                                strokeWidth = 2.dp,
                                color = if (primaryAction.isDestructive) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Text(
                            text = primaryAction.text,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * A confirmation dialog with predefined destructive styling.
 * Convenience wrapper around DokusDialog for confirmation flows.
 *
 * @param onDismissRequest Called when the user requests to dismiss the dialog
 * @param title The dialog title
 * @param message The confirmation message
 * @param confirmText The confirm button text
 * @param cancelText The cancel button text
 * @param onConfirm Called when the user confirms
 * @param isConfirming Whether the confirmation is in progress (shows loading)
 */
@Composable
fun DokusConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    isConfirming: Boolean = false,
) {
    DokusDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        primaryAction = DokusDialogAction(
            text = confirmText,
            onClick = onConfirm,
            isLoading = isConfirming,
            isDestructive = true,
            enabled = !isConfirming
        ),
        secondaryAction = DokusDialogAction(
            text = cancelText,
            onClick = onDismissRequest
        ),
        dismissOnBackPress = !isConfirming,
        dismissOnClickOutside = !isConfirming
    )
}
