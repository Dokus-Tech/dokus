package tech.dokus.app.screens.console

import tech.dokus.foundation.app.shell.UserAccessContext

internal fun isConsoleAccessDenied(accessContext: UserAccessContext): Boolean {
    return accessContext.isSurfaceAvailabilityResolved && !accessContext.canBookkeeperConsole
}

internal fun canRenderConsoleContent(accessContext: UserAccessContext): Boolean {
    return accessContext.isSurfaceAvailabilityResolved && accessContext.canBookkeeperConsole
}
