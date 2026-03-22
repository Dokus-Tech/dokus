@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package tech.dokus.foundation.app.download

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class FileSaver {

    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun saveFile(filename: String, bytes: ByteArray) {
        val base64 = Base64.encode(bytes)
        triggerDownload(filename, base64)
    }
}

@JsFun("""(filename, base64) => {
    var binary = atob(base64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    var blob = new Blob([bytes], { type: 'application/octet-stream' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}""")
private external fun triggerDownload(filename: String, base64: String)
