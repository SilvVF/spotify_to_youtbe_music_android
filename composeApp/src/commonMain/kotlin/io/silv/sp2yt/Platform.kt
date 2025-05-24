package io.silv.sp2yt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls

interface Platform {
    val name: String
}

expect fun sha1(value: String): String

expect fun getPlatform(): Platform

@Composable
expect fun BackHandler(enabled: Boolean, callback: @DisallowComposableCalls () -> Unit)