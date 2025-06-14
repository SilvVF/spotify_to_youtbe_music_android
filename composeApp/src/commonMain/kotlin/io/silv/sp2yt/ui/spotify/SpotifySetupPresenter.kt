package io.silv.sp2yt.ui.spotify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.silv.sp2yt.api.SpotifyApi
import io.silv.sp2yt.appGraph
import kotlinx.coroutines.flow.combine

sealed interface SpotifySetupEvent {
    data object ConfirmChanges : SpotifySetupEvent
    data class ChangeSecret(val secret: String) : SpotifySetupEvent
    data class ChangeClientId(val clientId: String) : SpotifySetupEvent
}

data class SpotifySetupState(
    val clientId: String,
    val secret: String,
    val events: (SpotifySetupEvent) -> Unit
)

@Composable
fun spotifySetupPresenter(
    spotifyApi: SpotifyApi = appGraph.spotifyApi
): SpotifySetupState {

    var cid by rememberSaveable { mutableStateOf(spotifyApi.clientId) }
    var secret by rememberSaveable { mutableStateOf(spotifyApi.clientSecret) }

    LaunchedEffect(Unit) {
        combine(
            snapshotFlow { cid },
            snapshotFlow { secret },
            ::Pair
        ).collect { (clientId, sec) ->
            cid = clientId
            secret = sec
        }
    }

    return SpotifySetupState(
        clientId = cid,
        secret = secret
    ) { event ->
        when (event) {
            is SpotifySetupEvent.ChangeClientId -> cid = event.clientId
            is SpotifySetupEvent.ChangeSecret -> secret = event.secret
            SpotifySetupEvent.ConfirmChanges -> {
                spotifyApi.clientSecret = secret
                spotifyApi.clientId = cid
            }
        }
    }
}