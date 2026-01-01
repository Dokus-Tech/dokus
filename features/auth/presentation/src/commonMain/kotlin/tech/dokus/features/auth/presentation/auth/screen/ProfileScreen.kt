package tech.dokus.features.auth.presentation.auth.screen

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_settings_title
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen() {
    Scaffold {
        Box(modifier = Modifier.padding(it)) {
            Text(
                text = stringResource(Res.string.profile_settings_title),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
