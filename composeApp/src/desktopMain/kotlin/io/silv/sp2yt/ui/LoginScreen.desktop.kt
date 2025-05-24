package io.silv.sp2yt.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import io.silv.sp2yt.YtMusciDesktopWebView

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun LoginScreen(onBack: () -> Unit) {
    YtMusciDesktopWebView(onBack)
}