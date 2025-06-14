package io.silv.sp2yt.ui.youtube_music

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import io.silv.sp2yt.appGraph
import java.awt.Desktop
import java.net.URI

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun YoutubeLoginScreen(
    state: YoutubeLoginState,
    onBack: () -> Unit
) {
    InputLoginScreen(
        onBack = onBack,
        openUrl = {
            val url = URI.create(it)
            Desktop.getDesktop().browse(url)
        },
        state = state
    )
}