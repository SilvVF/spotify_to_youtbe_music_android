package io.silv.sp2yt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

@Composable
actual fun BackHandler(
    enabled: Boolean,
    callback: @DisallowComposableCalls (() -> Unit)
) {
}