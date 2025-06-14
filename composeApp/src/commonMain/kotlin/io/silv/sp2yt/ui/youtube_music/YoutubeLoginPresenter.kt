package io.silv.sp2yt.ui.youtube_music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.silv.sp2yt.api.YoutubeMusicApi
import io.silv.sp2yt.appGraph
import kotlinx.coroutines.flow.combine

sealed interface YoutubeLoginEvent {
    data class SetVisitorData(val data: String): YoutubeLoginEvent
    data class SetCookie(val cookie: String): YoutubeLoginEvent
    data object ConfirmValues: YoutubeLoginEvent
}

data class YoutubeLoginState(
    val cookie: String,
    val visitorData: String,
    val events: (YoutubeLoginEvent) -> Unit
)

@Composable
fun youtubeLoginPresenter(
    api: YoutubeMusicApi
): YoutubeLoginState {

    var visitorData by rememberSaveable { mutableStateOf(api.visitorData) }
    var cookie by rememberSaveable { mutableStateOf(api.cookie) }

    LaunchedEffect(Unit) {
        combine(
            snapshotFlow { api.visitorData },
            snapshotFlow { api.cookie },
            ::Pair
        ).collect { (d, c) ->
            api.visitorData = d
            api.cookie = c
        }
    }

    return YoutubeLoginState(
        visitorData = visitorData,
        cookie = cookie
    ) { event ->
        when(event) {
            YoutubeLoginEvent.ConfirmValues -> {
                api.visitorData = visitorData
                api.cookie = cookie
            }
            is YoutubeLoginEvent.SetCookie -> cookie = event.cookie
            is YoutubeLoginEvent.SetVisitorData -> visitorData = event.data
        }
    }
}