package ai.dokus.app.auth.screen

import ai.dokus.app.auth.datasource.AccountRemoteDataSource
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.app.resources.generated.profile_deactivate_account
import ai.dokus.app.resources.generated.profile_deactivate_warning
import ai.dokus.app.resources.generated.profile_email
import ai.dokus.app.resources.generated.profile_first_name
import ai.dokus.app.resources.generated.profile_last_name
import ai.dokus.app.resources.generated.profile_personal_info
import ai.dokus.app.resources.generated.profile_settings_title
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.platform.Logger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Profile settings screen.
 * Displays user profile information (read-only for now).
 * Profile editing will be available when backend supports it.
 */
@Composable
fun ProfileSettingsScreen() {
    val logger = remember { Logger.withTag("ProfileSettingsScreen") }
    val accountDataSource: AccountRemoteDataSource = koinInject()

    var userState by remember { mutableStateOf<DokusState<User>>(DokusState.idle()) }

    LaunchedEffect(Unit) {
        logger.d { "Loading user profile" }
        userState = DokusState.loading()

        accountDataSource.getCurrentUser().fold(
            onSuccess = { user ->
                logger.i { "User profile loaded: ${user.email.value}" }
                userState = DokusState.success(user)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load user profile" }
                userState = DokusState.error(error) {}
            }
        )
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.profile_settings_title)
            )
        }
    ) { contentPadding ->
        when {
            userState.isLoading() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            userState.isSuccess() -> {
                val user = (userState as DokusState.Success).data
                Column(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .withContentPaddingForScrollable(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Personal Information Section
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.profile_personal_info),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(16.dp))

                            // Email
                            ProfileField(
                                label = stringResource(Res.string.profile_email),
                                value = user.email.value
                            )

                            Spacer(Modifier.height(12.dp))

                            // First Name
                            ProfileField(
                                label = stringResource(Res.string.profile_first_name),
                                value = user.firstName?.value ?: "-"
                            )

                            Spacer(Modifier.height(12.dp))

                            // Last Name
                            ProfileField(
                                label = stringResource(Res.string.profile_last_name),
                                value = user.lastName?.value ?: "-"
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "Profile editing coming soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Danger Zone
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.profile_danger_zone),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = stringResource(Res.string.profile_deactivate_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            POutlinedButton(
                                text = stringResource(Res.string.profile_deactivate_account),
                                onClick = { /* TODO: Implement deactivation dialog */ },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
            else -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load profile")
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
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
