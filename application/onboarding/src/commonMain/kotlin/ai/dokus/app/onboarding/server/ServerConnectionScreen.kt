package ai.dokus.app.onboarding.server

import ai.dokus.app.core.constrains.isLargeScreen
import ai.dokus.app.navigation.AppNavigator
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.dokus.foundation.ui.text.AppNameText
import ai.dokus.foundation.ui.text.SectionTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
fun ServerConnectionScreen(navigator: AppNavigator) {
    val backgroundAnimationViewModel = koinInject<BackgroundAnimationViewModel>()

    Scaffold { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            if (isLargeScreen) {
                SloganWithBackgroundWithLeftContent(backgroundAnimationViewModel) {
                    ServerConnectionContent(
                        onBackPress = { navigator.navigateBack() },
                    )
                }
            } else {
                ServerConnectionScreenMobileContent(
                    onBackPress = { navigator.navigateBack() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun ServerConnectionScreenMobileContent(
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        ServerConnectionContent(
            onBackPress = onBackPress,
            modifier = Modifier.fillMaxSize()
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun ServerConnectionContent(
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        // Title
        SectionTitle("Server connection", onBackPress = onBackPress)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Available connections",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}