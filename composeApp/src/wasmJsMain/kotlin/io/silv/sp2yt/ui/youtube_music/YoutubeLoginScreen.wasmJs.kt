package io.silv.sp2yt.ui.youtube_music

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun YoutubeLoginScreen(
    state: YoutubeLoginState,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    InputLoginScreen(
        state,
        openUrl = {
            uriHandler.openUri(uri = it)
        },
        onBack = onBack
    )
}