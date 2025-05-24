package io.silv.sp2yt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun BackHandler(
    enabled: Boolean,
    callback: @DisallowComposableCalls (() -> Unit)
) {
}

actual fun sha1(value: String): String {
    TODO("Not yet implemented")
}