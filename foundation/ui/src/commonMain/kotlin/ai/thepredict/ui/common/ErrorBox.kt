package ai.thepredict.ui.common

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.PTitle
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