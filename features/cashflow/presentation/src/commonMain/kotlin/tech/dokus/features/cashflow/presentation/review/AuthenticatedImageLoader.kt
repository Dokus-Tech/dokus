package tech.dokus.features.cashflow.presentation.review

import coil3.ImageLoader
import androidx.compose.runtime.Composable
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader as rememberSharedAuthenticatedImageLoader

/**
 * Backward-compatible wrapper around shared authenticated image loader.
 */
@Composable
fun rememberAuthenticatedImageLoader(): ImageLoader = rememberSharedAuthenticatedImageLoader()
