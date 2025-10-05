package ai.thepredict.ui.brandsugar

import ai.thepredict.ui.text.AppNameText
import ai.thepredict.ui.text.CopyRightText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SloganWithBackground(
    modifier: Modifier = Modifier,
    backgroundAnimationViewModel: BackgroundAnimationViewModel
) {
    Box(modifier) {
        BackgroundGradientAnimated(animationViewModel = backgroundAnimationViewModel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp), // Add padding around the text
            contentAlignment = Alignment.Center // Center the text within the Box
        ) {
            Text(
                text = "Financial forecasting without the headaches or jargon. Just smart insights that help your Belgian business thrive.",
                color = MaterialTheme.colorScheme.onPrimary, // White text color
                style = MaterialTheme.typography.headlineMedium, // Adjust text style as needed
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp // Adjust line height for better readability
            )
        }
    }
}

@Composable
fun SloganWithBackgroundWithLeftContent(
    backgroundAnimationViewModel: BackgroundAnimationViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        Row(Modifier.weight(1f).padding(32.dp)) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(2f).fillMaxHeight()
            ) {
                AppNameText()
                CopyRightText()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(3f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
            Spacer(modifier = Modifier.weight(2f))
        }
        SloganWithBackground(
            modifier = Modifier.weight(1f),
            backgroundAnimationViewModel = backgroundAnimationViewModel
        )
    }
}