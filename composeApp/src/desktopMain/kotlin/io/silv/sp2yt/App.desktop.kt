package io.silv.sp2yt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls

@Composable
actual fun BackHandler(
    enabled: Boolean,
    callback: @DisallowComposableCalls (() -> Unit)
) {
}