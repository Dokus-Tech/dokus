package ai.thepredict.app.platform

import android.os.Build

internal actual fun isDebugBuild(): Boolean {
    return Build.TYPE == "eng" || Build.TYPE == "userdebug"
}
