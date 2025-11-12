package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.RegisterViewModel
import ai.dokus.app.core.extensions.SetupSecondaryPanel
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldName
import ai.dokus.foundation.design.components.fields.PTextFieldPassword
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.navigation.destinations.AppDestination
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.local.SecondaryPanelType
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current

    SetupSecondaryPanel(AppDestination.Slogan, SecondaryPanelType.Inline)

    // Handle navigation effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterViewModel.Effect.NavigateToHome -> {
                    navController.replace(CoreDestination.Home)
                }
            }
        }
    }

    val state = viewModel.state.collectAsState()
    val fieldsError: DokusException? =
        (state.value as? RegisterViewModel.State.Error)?.exception

    var firstName by remember { mutableStateOf(Name("")) }
    var lastName by remember { mutableStateOf(Name("")) }
    var email by remember { mutableStateOf(Email("")) }
    var password by remember { mutableStateOf(Password("")) }
    val mutableInteractionSource = remember { MutableInteractionSource() }

    val isLoading = state.value is RegisterViewModel.State.Loading
    val isFormValid = firstName.value.isNotBlank() &&
            lastName.value.isNotBlank() &&
            email.value.isNotBlank() &&
            password.value.isNotBlank()

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
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .withContentPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SectionTitle(
                    text = "Create Account",
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.limitWidthCenteredContent(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.limitWidthCenteredContent().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PTextFieldName(
                        fieldName = "First Name",
                        value = firstName,
                        onValueChange = { firstName = it },
                        error = if (fieldsError is DokusException.Validation.InvalidFirstName) fieldsError else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PTextFieldName(
                        fieldName = "Last Name",
                        value = lastName,
                        onValueChange = { lastName = it },
                        error = if (fieldsError is DokusException.Validation.InvalidLastName) fieldsError else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PTextFieldEmail(
                        fieldName = "Email",
                        value = email,
                        onValueChange = { email = it },
                        error = if (fieldsError is DokusException.Validation.InvalidEmail) fieldsError else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PTextFieldPassword(
                        fieldName = "Password",
                        value = password,
                        onValueChange = { password = it },
                        error = if (fieldsError is DokusException.Validation.WeakPassword) fieldsError else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onAction = {
                            if (isFormValid && !isLoading) {
                                viewModel.register(email, password, firstName, lastName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.limitWidthCenteredContent()
                    )
                } else {
                    PPrimaryButton(
                        text = "Register",
                        enabled = isFormValid,
                        onClick = {
                            viewModel.register(email, password, firstName, lastName)
                        },
                        modifier = Modifier.limitWidthCenteredContent().fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.limitWidthCenteredContent(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigateTo(AuthDestination.Login)
                        }
                    )
                }
            }
        }
    }
}