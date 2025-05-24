package io.silv.sp2yt

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import java.security.MessageDigest

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()


@Composable
actual fun BackHandler(
    enabled: Boolean,
    callback: @DisallowComposableCalls (() -> Unit)
) {
    BackHandler(enabled, callback)
}

actual fun sha1(value: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}