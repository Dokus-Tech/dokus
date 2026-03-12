package tech.dokus.features.auth.presentation.auth.components.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.auth_company_lookup_idle_line_1
import tech.dokus.aura.resources.auth_company_lookup_idle_line_2
import tech.dokus.aura.resources.auth_company_lookup_not_found_subtitle
import tech.dokus.aura.resources.auth_company_lookup_not_found_title
import tech.dokus.aura.resources.auth_company_lookup_prompt
import tech.dokus.aura.resources.auth_company_lookup_results_count
import tech.dokus.aura.resources.auth_company_name_searching
import tech.dokus.aura.resources.auth_entity_manual_entry
import tech.dokus.aura.resources.console_clients_search_placeholder
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Country
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityAddress
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.common.PBackIconButton
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val MinLookupCharacters = 3

@Composable
internal fun CompanyNameStep(
    query: String,
    lookupState: LookupState,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (EntityLookup) -> Unit,
    onEnterManually: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedQuery = query.trim()
    val isIdle = normalizedQuery.length < MinLookupCharacters
    val results =
        if (!isIdle && lookupState is LookupState.Success) lookupState.results else emptyList()
    val isLoading =
        !isIdle && (lookupState is LookupState.Loading || lookupState is LookupState.Idle)
    val isError = !isIdle && lookupState is LookupState.Error
    val showNoResults = !isIdle && lookupState is LookupState.Success && results.isEmpty()
    val isLargeScreen = LocalScreenSize.current.isLarge

    DokusGlassSurface(modifier) {
        if (isLargeScreen) {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                LookupInputPane(
                    query = query,
                    lookupState = lookupState,
                    results = results,
                    onQueryChanged = onQueryChanged,
                    onBackPress = onBackPress,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LookupResultsPane(
                    isIdle = isIdle,
                    isLoading = isLoading,
                    isError = isError,
                    showNoResults = showNoResults,
                    lookupState = lookupState,
                    results = results,
                    query = normalizedQuery,
                    onResultSelected = onResultSelected,
                    onEnterManually = onEnterManually,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column {
                LookupInputPane(
                    query = query,
                    lookupState = lookupState,
                    results = results,
                    onQueryChanged = onQueryChanged,
                    onBackPress = onBackPress,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LookupResultsPane(
                    isIdle = isIdle,
                    isLoading = isLoading,
                    isError = isError,
                    showNoResults = showNoResults,
                    lookupState = lookupState,
                    results = results,
                    query = normalizedQuery,
                    onResultSelected = onResultSelected,
                    onEnterManually = onEnterManually,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LookupInputPane(
    query: String,
    lookupState: LookupState,
    results: List<EntityLookup>,
    onQueryChanged: (String) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCount =
        lookupState is LookupState.Success && query.trim().length >= MinLookupCharacters && results.isNotEmpty()

    Column(
        modifier = modifier
            .padding(Constraints.Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
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
                text = stringResource(Res.string.auth_company_lookup_prompt),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        PSearchFieldCompact(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(Res.string.console_clients_search_placeholder),
            modifier = Modifier.fillMaxWidth(),
            onClear = { onQueryChanged("") },
        )

        if (showCount) {
            Text(
                text = stringResource(Res.string.auth_company_lookup_results_count, results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (lookupState is LookupState.Loading && query.trim().length >= MinLookupCharacters) {
            Text(
                text = stringResource(Res.string.auth_company_name_searching),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
        }
    }
}

@Composable
private fun LookupResultsPane(
    isIdle: Boolean,
    isLoading: Boolean,
    isError: Boolean,
    showNoResults: Boolean,
    lookupState: LookupState,
    results: List<EntityLookup>,
    query: String,
    onResultSelected: (EntityLookup) -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(Constraints.Spacing.large),
    ) {
        when {
            isIdle -> {
                EmptyLookupState(
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                    title = stringResource(Res.string.auth_company_lookup_idle_line_1),
                    subtitle = stringResource(Res.string.auth_company_lookup_idle_line_2),
                )
            }

            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                    Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                    Text(
                        text = stringResource(Res.string.auth_company_name_searching),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            isError -> {
                val error = lookupState as LookupState.Error
                EmptyLookupState(
                    title = error.exception.localized,
                    subtitle = stringResource(Res.string.auth_company_lookup_not_found_subtitle),
                    subtitleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.86f),
                    action = {
                        PPrimaryButton(
                            text = stringResource(Res.string.auth_entity_manual_entry),
                            onClick = onEnterManually,
                        )
                    },
                )
            }

            showNoResults -> {
                EmptyLookupState(
                    title = stringResource(Res.string.auth_company_lookup_not_found_title, query),
                    subtitle = stringResource(Res.string.auth_company_lookup_not_found_subtitle),
                    action = {
                        PPrimaryButton(
                            text = stringResource(Res.string.auth_entity_manual_entry),
                            onClick = onEnterManually,
                        )
                    },
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                ) {
                    items(
                        items = results,
                        key = { it.enterpriseNumber },
                    ) { entity ->
                        LookupResultCard(
                            entity = entity,
                            onClick = { onResultSelected(entity) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupResultCard(
    entity: EntityLookup,
    onClick: () -> Unit,
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium
                ),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = Color(0xFF3CC98A), shape = CircleShape),
                )
                Text(
                    text = entity.name.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = buildString {
                    append(entity.vatNumber.value)
                    val addressLine = entity.address.fullAddress()
                    if (addressLine.isNotBlank()) {
                        append("    ")
                        append(addressLine)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyLookupState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Lucide.Building2,
            contentDescription = null,
            tint = iconTint,
        )
        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            textAlign = TextAlign.Center,
        )

        action?.let {
            Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))
            it()
        }
    }
}

private fun EntityAddress?.fullAddress(): String {
    if (this == null) return ""
    return buildString {
        append(streetLine1)
        streetLine2?.takeIf { it.isNotBlank() }?.let {
            append(", ")
            append(it)
        }
        append(", ")
        append(postalCode)
        append(" ")
        append(city)
    }
}

private fun previewEntity(
    name: String,
    vat: String,
    street: String,
    postalCode: String,
    city: String,
) = EntityLookup(
    enterpriseNumber = vat.removePrefix("BE"),
    vatNumber = VatNumber(vat),
    name = LegalName(name),
    address = EntityAddress(
        streetLine1 = street,
        city = city,
        postalCode = postalCode,
        country = Country.Belgium,
    ),
    status = EntityStatus.Active,
)

@Preview
@Composable
private fun CompanyNameStepIdlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "In",
            lookupState = LookupState.Idle,
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}

@Preview
@Composable
private fun CompanyNameStepLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "Invoid",
            lookupState = LookupState.Loading,
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}

@Preview
@Composable
private fun CompanyNameStepResultsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "Invoid",
            lookupState = LookupState.Success(
                listOf(
                    previewEntity(
                        name = "INVOID VISION",
                        vat = "BE0777887045",
                        street = "Balegemstraat 17, Box 7",
                        postalCode = "9860",
                        city = "Oosterzele",
                    ),
                    previewEntity(
                        name = "INVOID BVBA",
                        vat = "BE0654321098",
                        street = "Kortrijksesteenweg 114",
                        postalCode = "9000",
                        city = "Gent",
                    ),
                )
            ),
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}

@Preview
@Composable
private fun CompanyNameStepNoResultsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "asdnajksdakjsd",
            lookupState = LookupState.Success(emptyList()),
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}

@Preview
@Composable
private fun CompanyNameStepErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "Invoid",
            lookupState = LookupState.Error(DokusException.CompanyLookupFailed),
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}

@Preview(name = "Company Lookup Desktop", widthDp = 1200, heightDp = 760)
@Composable
private fun CompanyNameStepDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyNameStep(
            query = "Invoid",
            lookupState = LookupState.Success(
                listOf(
                    previewEntity(
                        name = "INVOID VISION",
                        vat = "BE0777887045",
                        street = "Balegemstraat 17, Box 7",
                        postalCode = "9860",
                        city = "Oosterzele",
                    ),
                    previewEntity(
                        name = "INVOID BVBA",
                        vat = "BE0654321098",
                        street = "Kortrijksesteenweg 114",
                        postalCode = "9000",
                        city = "Gent",
                    ),
                )
            ),
            onQueryChanged = {},
            onResultSelected = {},
            onEnterManually = {},
            onBackPress = {},
        )
    }
}
