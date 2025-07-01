package ai.thepredict.ui

import androidx.compose.foundation.layout.Box
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
fun SloganWithBackground(modifier: Modifier = Modifier) {
    Box(modifier) {
        BackgroundGradientAnimated()
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