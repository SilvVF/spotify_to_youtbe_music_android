package io.silv.sp2yt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@Composable
actual fun BackHandler(
    enabled: Boolean,
    callback: @DisallowComposableCalls (() -> Unit)
) {
}

actual fun sha1(value: String): String {
    TODO("Not yet implemented")
}