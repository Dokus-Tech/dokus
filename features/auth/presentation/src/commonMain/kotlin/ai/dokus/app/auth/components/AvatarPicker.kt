package ai.dokus.app.auth.components

import androidx.compose.runtime.Composable

/**
 * Result of picking an image for avatar.
 */
data class PickedImage(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String?
)

/**
 * Platform-agnostic image picker launcher for avatars.
 *
 * Each platform provides its own implementation:
 * - Desktop: AWT FileDialog with image filter
 * - Android/iOS/Web: Uses Calf file picker with image type
 */
expect class AvatarPickerLauncher {
    fun launch()
}

/**
 * Remember an image picker launcher for selecting avatar images.
 *
 * @param onImageSelected Callback with the selected image as [PickedImage]
 * @return A launcher that can be used to open the image picker
 */
@Composable
expect fun rememberAvatarPicker(
    onImageSelected: (PickedImage) -> Unit
): AvatarPickerLauncher
