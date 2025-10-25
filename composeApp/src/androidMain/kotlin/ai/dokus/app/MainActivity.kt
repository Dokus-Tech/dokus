package ai.dokus.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ai.dokus.app.navigation.ExternalUriHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle deeplink from the initial intent
        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update current intent reference (Android best practice)
        handleIntent(intent)
    }

    /**
     * Extracts the URI from the intent and passes it to the ExternalUriHandler.
     * Follows Android best practices by consuming the intent data after processing.
     */
    private fun handleIntent(intent: Intent) {
        val data = intent.data

        if (data == null) {
            println("[MainActivity] No URI in intent")
            return
        }

        val action = intent.action
        println("[MainActivity] Intent action: $action, URI: $data")

        // Pass the full URI string to the handler
        ExternalUriHandler.onNewUri(data.toString())

        // Consume the intent data to prevent re-processing (Android best practice)
        intent.data = null
        println("[MainActivity] Intent data consumed")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}