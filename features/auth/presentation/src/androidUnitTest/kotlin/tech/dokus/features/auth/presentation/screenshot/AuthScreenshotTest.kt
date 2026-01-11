package tech.dokus.features.auth.presentation.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextField
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.features.auth.presentation.screenshot.ScreenshotTestWrapper

/**
 * Screenshot tests for auth screens.
 * Tests simplified versions of screens to capture UI layouts.
 */
class AuthScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 600,
            screenHeight = 960,
            density = Density.XXHIGH,
            softButtons = false
        ),
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun loginScreen_empty() {
        paparazzi.snapshot("LoginScreen_empty_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                LoginFormContent(
                    email = "",
                    password = "",
                    isLoading = false
                )
            }
        }
        paparazzi.snapshot("LoginScreen_empty_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                LoginFormContent(
                    email = "",
                    password = "",
                    isLoading = false
                )
            }
        }
    }

    @Test
    fun loginScreen_filled() {
        paparazzi.snapshot("LoginScreen_filled_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                LoginFormContent(
                    email = "user@example.com",
                    password = "password123",
                    isLoading = false
                )
            }
        }
        paparazzi.snapshot("LoginScreen_filled_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                LoginFormContent(
                    email = "user@example.com",
                    password = "password123",
                    isLoading = false
                )
            }
        }
    }

    @Test
    fun loginScreen_loading() {
        paparazzi.snapshot("LoginScreen_loading_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                LoginFormContent(
                    email = "user@example.com",
                    password = "password123",
                    isLoading = true
                )
            }
        }
    }

    @Test
    fun registerScreen() {
        paparazzi.snapshot("RegisterScreen_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                RegisterFormContent(
                    name = "",
                    email = "",
                    password = "",
                    isLoading = false
                )
            }
        }
        paparazzi.snapshot("RegisterScreen_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                RegisterFormContent(
                    name = "",
                    email = "",
                    password = "",
                    isLoading = false
                )
            }
        }
    }
}

@Composable
private fun LoginFormContent(
    email: String,
    password: String,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        DokusCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppNameText()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PTextField(
                    fieldName = "Email",
                    value = email,
                    icon = Icons.Default.Email,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )

                PTextField(
                    fieldName = "Password",
                    value = password,
                    icon = Icons.Default.Lock,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    error = null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )

                Spacer(modifier = Modifier.height(8.dp))

                PPrimaryButton(
                    text = "Sign In",
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                )

                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RegisterFormContent(
    name: String,
    email: String,
    password: String,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        DokusCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppNameText()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                PTextField(
                    fieldName = "Name",
                    value = name,
                    icon = null,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions.Default,
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )

                PTextField(
                    fieldName = "Email",
                    value = email,
                    icon = Icons.Default.Email,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )

                PTextField(
                    fieldName = "Password",
                    value = password,
                    icon = Icons.Default.Lock,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    error = null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )

                Spacer(modifier = Modifier.height(8.dp))

                PPrimaryButton(
                    text = "Create Account",
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                )

                Text(
                    text = "Already have an account? Sign in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
