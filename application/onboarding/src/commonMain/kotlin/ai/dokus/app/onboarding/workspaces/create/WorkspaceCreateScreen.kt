package ai.dokus.app.onboarding.workspaces.create

import ai.dokus.foundation.ui.constrains.isLargeScreen
import ai.dokus.foundation.domain.flags.FeatureFlags
import ai.dokus.foundation.navigation.AppNavigator
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.Country
import ai.dokus.foundation.ui.PCardPlusIcon
import ai.dokus.foundation.ui.PPrimaryButton
import ai.dokus.foundation.ui.extensions.localized
import ai.dokus.foundation.ui.fields.PTextFieldStandard
import ai.dokus.foundation.ui.fields.PTextFieldTaxNumber
import ai.dokus.foundation.ui.fields.PTextFieldTaxNumberDefaults
import ai.dokus.foundation.ui.fields.PTextFieldWorkspaceName
import ai.dokus.foundation.ui.fields.PTextFieldWorkspaceNameDefaults
import ai.dokus.foundation.ui.text.AppNameText
import ai.dokus.foundation.ui.text.CopyRightText
import ai.dokus.foundation.ui.text.SectionTitle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import compose.icons.FeatherIcons
import compose.icons.feathericons.MapPin
import kotlinx.coroutines.launch

private val WorkspaceCreateViewModel.State.exceptionOrNull: DokusException?
    get() = when (this) {
        is WorkspaceCreateViewModel.State.Error -> exception
        else -> null
    }

@Composable
fun WorkspaceCreateScreen(navigator: AppNavigator) {
    val viewModel = remember { WorkspaceCreateViewModel() }
    val data = viewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val fieldsError: DokusException? = data.value.exceptionOrNull

    val defaultCountry = Country.default.localized

    var workspaceName by remember { mutableStateOf("") }
    var taxNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(Address(country = defaultCountry)) }
    val mutableInteractionSource = remember { MutableInteractionSource() }

    val handleEffect = { effect: WorkspaceCreateViewModel.Effect ->
        when (effect) {
            is WorkspaceCreateViewModel.Effect.NavigateHome -> navigator.navigateToHome()
        }
    }

    LaunchedEffect("workspace-create") {
        scope.launch { viewModel.effect.collect(handleEffect) }
    }

    Scaffold { contentPadding ->
        Box(
            Modifier
                .padding(contentPadding)
                .clickable(
                    indication = null,
                    interactionSource = mutableInteractionSource
                ) {
                    focusManager.clearFocus()
                }
        ) {
            if (isLargeScreen) {
                WorkspaceCreateScreenDesktopContent(
                    state = data.value,
                    focusManager = focusManager,
                    workspaceName = workspaceName,
                    onWorkspaceNameChange = { workspaceName = it },
                    vatNumber = taxNumber,
                    onVatNumberChange = { taxNumber = it },
                    address = address,
                    onAddressChange = { address = it },
                    fieldsError = fieldsError,
                    onAddAvatarClick = {},
                    onCreateClick = {
                        viewModel.create(
                            name = workspaceName,
                            taxNumber = taxNumber,
                            address = address
                        )
                    },
                    onBackClick = {
                        navigator.navigateBack()
                    },
                )
            } else {
                WorkspaceCreateScreenMobileContent(
                    focusManager = focusManager,
                    workspaceName = workspaceName,
                    onWorkspaceNameChange = { workspaceName = it },
                    vatNumber = taxNumber,
                    onVatNumberChange = { taxNumber = it },
                    address = address,
                    onAddressChange = { address = it },
                    fieldsError = fieldsError,
                    onAddAvatarClick = {},
                    onCreateClick = {
                        viewModel.create(
                            name = workspaceName,
                            taxNumber = taxNumber,
                            address = address
                        )
                    },
                    onBackClick = {
                        navigator.navigateBack()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun WorkspaceCreateScreenMobileContent(
    focusManager: FocusManager,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    address: Address,
    onAddressChange: (Address) -> Unit,
    fieldsError: DokusException?,
    onAddAvatarClick: () -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        WorkspaceCreateForm(
            focusManager = focusManager,
            workspaceName = workspaceName,
            onWorkspaceNameChange = onWorkspaceNameChange,
            vatNumber = vatNumber,
            onVatNumberChange = onVatNumberChange,
            address = address,
            onAddressChange = onAddressChange,
            fieldsError = fieldsError,
            onAddAvatarClick = onAddAvatarClick,
            onCreateClick = onCreateClick,
            onBackClick = onBackClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun WorkspaceCreateScreenDesktopContent(
    state: WorkspaceCreateViewModel.State,
    focusManager: FocusManager,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    address: Address,
    onAddressChange: (Address) -> Unit,
    fieldsError: DokusException?,
    onAddAvatarClick: () -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        AppNameText()

        when (state) {
            is WorkspaceCreateViewModel.State.Loading -> {
                Box(modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AdaptiveCircularProgressIndicator()
                }
            }

            else -> {
                WorkspaceCreateForm(
                    focusManager = focusManager,
                    workspaceName = workspaceName,
                    onWorkspaceNameChange = onWorkspaceNameChange,
                    vatNumber = vatNumber,
                    onVatNumberChange = onVatNumberChange,
                    address = address,
                    onAddressChange = onAddressChange,
                    fieldsError = fieldsError,
                    onAddAvatarClick = onAddAvatarClick,
                    onCreateClick = onCreateClick,
                    onBackClick = onBackClick,
                    modifier = Modifier.weight(1f).widthIn(max = 320.dp)
                )
            }
        }

        CopyRightText()
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
internal fun WorkspaceCreateForm(
    focusManager: FocusManager,
    workspaceName: String,
    onWorkspaceNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    address: Address,
    onAddressChange: (Address) -> Unit,
    fieldsError: DokusException?,
    onAddAvatarClick: () -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle("Create workspace", onBackPress = onBackClick)

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.heightIn(max = 80.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.weight(2f)
                    ) {
                        PCardPlusIcon(
                            modifier = Modifier
                                .size(80.dp)
                                .clickable(enabled = FeatureFlags.addWorkspaceAvatar) { onAddAvatarClick() }
                        )
                        Box(Modifier.padding(start = 12.dp)) {
                            Text(
                                "Workspace avatar",
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }
            item {
                PTextFieldWorkspaceName(
                    fieldName = "Company name",
                    error = fieldsError.takeIf { it is DokusException.InvalidWorkspaceName },
                    value = workspaceName,
                    keyboardOptions = PTextFieldWorkspaceNameDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = onWorkspaceNameChange
                )
            }
            item {
                PTextFieldTaxNumber(
                    fieldName = "VAT number",
                    error = fieldsError.takeIf { it is DokusException.InvalidTaxNumber },
                    value = vatNumber,
                    singleLine = true,
                    keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = onVatNumberChange
                )
            }
            item {
                PTextFieldStandard(
                    fieldName = "Street",
                    error = fieldsError.takeIf { it is DokusException.InvalidAddress.InvalidStreetName },
                    value = address.streetName.orEmpty(),
                    singleLine = true,
                    keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    icon = FeatherIcons.MapPin,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {
                        onAddressChange(address.copy(streetName = it))
                    }
                )
            }
            item {
                PTextFieldStandard(
                    fieldName = "City",
                    error = fieldsError.takeIf { it is DokusException.InvalidAddress.InvalidStreetName },
                    value = address.city.orEmpty(),
                    singleLine = true,
                    keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    icon = FeatherIcons.MapPin,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {
                        onAddressChange(address.copy(city = it))
                    }
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PTextFieldStandard(
                        fieldName = "Postal code",
                        error = fieldsError.takeIf { it is DokusException.InvalidAddress.InvalidStreetName },
                        value = address.postalCode.orEmpty(),
                        singleLine = true,
                        keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                        onAction = { focusManager.moveFocus(FocusDirection.Next) },
                        icon = FeatherIcons.MapPin,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            onAddressChange(address.copy(postalCode = it))
                        }
                    )
                    PTextFieldStandard(
                        fieldName = "Country",
                        error = fieldsError.takeIf { it is DokusException.InvalidAddress.InvalidStreetName },
                        value = address.country.orEmpty(),
                        singleLine = true,
                        keyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions.copy(imeAction = ImeAction.Done),
                        onAction = { focusManager.moveFocus(FocusDirection.Next) },
                        icon = FeatherIcons.MapPin,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            onAddressChange(address.copy(country = it))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Create",
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}