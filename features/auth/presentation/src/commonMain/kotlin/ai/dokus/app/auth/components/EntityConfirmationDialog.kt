package ai.dokus.app.auth.components

import ai.dokus.app.auth.model.EntityConfirmationState
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.domain.model.EntityLookup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntityConfirmationDialog(
    state: EntityConfirmationState,
    onEntitySelected: (EntityLookup) -> Unit,
    onEnterManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is EntityConfirmationState.Hidden) return

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is EntityConfirmationState.SingleResult -> {
                        SingleResultContent(
                            entity = state.entity,
                            onConfirm = { onEntitySelected(state.entity) },
                            onEnterManually = onEnterManually
                        )
                    }
                    is EntityConfirmationState.MultipleResults -> {
                        MultipleResultsContent(
                            entities = state.entities,
                            onEntitySelected = onEntitySelected,
                            onEnterManually = onEnterManually
                        )
                    }
                    EntityConfirmationState.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun SingleResultContent(
    entity: EntityLookup,
    onConfirm: () -> Unit,
    onEnterManually: () -> Unit,
) {
    Icon(
        imageVector = Icons.Outlined.Business,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.height(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Did we catch you correctly?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    EntityCard(
        entity = entity,
        isClickable = false,
        onClick = {}
    )

    Spacer(modifier = Modifier.height(24.dp))

    PPrimaryButton(
        text = "Yes, that's me",
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    POutlinedButton(
        text = "Enter manually",
        onClick = onEnterManually,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MultipleResultsContent(
    entities: List<EntityLookup>,
    onEntitySelected: (EntityLookup) -> Unit,
    onEnterManually: () -> Unit,
) {
    Text(
        text = "We found multiple matches",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Select your company from the list below",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entities) { entity ->
            EntityCard(
                entity = entity,
                isClickable = true,
                onClick = { onEntitySelected(entity) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Enter manually",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onEnterManually() }
        )
    }
}

@Composable
private fun EntityCard(
    entity: EntityLookup,
    isClickable: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = isClickable,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = entity.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            entity.vatNumber?.let { vat ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VAT: ${vat.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entity.address?.let { address ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(address.streetLine1)
                        address.streetLine2?.let { append(", $it") }
                        append(", ${address.postalCode} ${address.city}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
