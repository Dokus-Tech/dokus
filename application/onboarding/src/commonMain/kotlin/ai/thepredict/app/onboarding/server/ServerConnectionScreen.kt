package ai.thepredict.app.onboarding.server

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.core.di
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.thepredict.ui.text.AppNameText
import ai.thepredict.ui.text.SectionTitle
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.instance

internal class ServerConnectionScreen : Screen {
    @Composable
    override fun Content() {
        val backgroundAnimationViewModel by di.instance<BackgroundAnimationViewModel>()

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        Scaffold { contentPadding ->
            Box(Modifier.padding(contentPadding)) {
                if (isLargeScreen) {
                    SloganWithBackgroundWithLeftContent(backgroundAnimationViewModel) {
                        ServerConnectionContent(
                            onBackPress = { navigator.pop() },
                        )
                    }
                } else {
                    ServerConnectionScreenMobileContent(
                        onBackPress = { navigator.pop() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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