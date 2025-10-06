package ai.dokus.foundation.ui.common

import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.ui.PButton
import ai.dokus.foundation.ui.PErrorText
import ai.dokus.foundation.ui.PTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw

@Composable
fun ErrorBox(exception: PredictException, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PTitle("Oops")
        Spacer(Modifier.padding(vertical = 8.dp))
        PErrorText(exception)
        if (exception.recoverable) {
            Spacer(Modifier.padding(vertical = 8.dp))
            PButton("Try again", icon = FeatherIcons.RefreshCw, onClick = onRetry)
        }
    }
}