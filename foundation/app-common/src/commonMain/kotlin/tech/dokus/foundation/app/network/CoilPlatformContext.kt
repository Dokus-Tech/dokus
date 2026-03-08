package tech.dokus.foundation.app.network

import coil3.PlatformContext

/**
 * Provides Coil's [PlatformContext] outside of Compose context (for Koin registration).
 * On Android this delegates to the Koin-registered Application Context.
 * On other platforms it returns [PlatformContext.INSTANCE] (no-op).
 */
internal expect fun coilPlatformContext(): PlatformContext
