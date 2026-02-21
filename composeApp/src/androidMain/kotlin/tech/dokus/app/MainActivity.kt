package tech.dokus.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tech.dokus.app.navigation.ExternalUriHandler
import tech.dokus.app.share.ExternalShareImportHandler
import tech.dokus.app.share.SharedImportFile
import tech.dokus.foundation.platform.Logger

class MainActivity : ComponentActivity() {
    private val logger = Logger.forClass<MainActivity>()

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
        if (handleShareIntent(intent)) {
            return
        }

        val data = intent.data

        if (data == null) {
            logger.d { "No URI in intent" }
            return
        }

        val action = intent.action
        logger.d { "Intent action=$action, uri=$data" }

        // Pass the full URI string to the handler
        ExternalUriHandler.onNewUri(data.toString())

        // Consume the intent data to prevent re-processing (Android best practice)
        intent.data = null
        logger.d { "Intent data consumed" }
    }

    private fun handleShareIntent(intent: Intent): Boolean {
        val action = intent.action ?: return false
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return false
        }

        val uris = extractShareUris(intent, action)
        if (uris.isEmpty()) {
            consumeShareExtras(intent)
            return true
        }

        val sharedPdfs = uris.mapNotNull(::readSharedPdf)
        if (sharedPdfs.isNotEmpty()) {
            ExternalShareImportHandler.onNewSharedFiles(sharedPdfs)
            logger.i { "Imported ${sharedPdfs.size} shared PDF(s)" }
        } else {
            logger.w { "Share intent had no supported PDF" }
        }

        consumeShareExtras(intent)
        return true
    }

    private fun extractShareUris(intent: Intent, action: String): List<Uri> {
        if (action == Intent.ACTION_SEND) {
            val uris = buildList {
                intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let(::add)
                val clipData = intent.clipData
                if (clipData != null) {
                    for (index in 0 until clipData.itemCount) {
                        clipData.getItemAt(index)?.uri?.let(::add)
                    }
                }
            }
            return uris.distinctBy(Uri::toString)
        }

        val multi = intent.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)
        if (!multi.isNullOrEmpty()) {
            return multi.distinctBy(Uri::toString)
        }

        val clipData = intent.clipData ?: return emptyList()
        return buildList {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index)?.uri?.let(::add)
            }
        }.distinctBy(Uri::toString)
    }

    private fun readSharedPdf(uri: Uri): SharedImportFile? {
        return runCatching {
            val mimeType = contentResolver.getType(uri) ?: "application/pdf"
            val name = resolveDisplayName(uri) ?: "shared.pdf"

            val inferredIsPdf = mimeType == "application/pdf" || name.lowercase().endsWith(".pdf")
            if (!inferredIsPdf) return null

            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            SharedImportFile(
                name = name,
                bytes = bytes,
                mimeType = "application/pdf"
            )
        }.getOrNull()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null) ?: return uri.lastPathSegment
        cursor.use {
            if (!it.moveToFirst()) return uri.lastPathSegment
            val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex == -1) return uri.lastPathSegment
            return it.getString(columnIndex)
        }
    }

    private fun consumeShareExtras(intent: Intent) {
        intent.removeExtra(Intent.EXTRA_STREAM)
        intent.clipData = null
        intent.action = null
    }

    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableArrayListExtraCompat(
        key: String
    ): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(key)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}

