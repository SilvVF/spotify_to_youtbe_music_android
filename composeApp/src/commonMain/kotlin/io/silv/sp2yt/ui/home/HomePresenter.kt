package io.silv.sp2yt.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import io.silv.sp2yt.api.SpotifyApi
import kotlinx.coroutines.launch

sealed interface HomeEvent {
    data class UpdateQuery(val query: String): HomeEvent
    data object Convert: HomeEvent
}

data class HomeState(
    val query: String,
    val snackbarHostState: SnackbarHostState,
    val events: (HomeEvent) -> Unit
)

@Composable
fun homePresenter(
    navController: NavController,
): HomeState {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }

    return HomeState(
        snackbarHostState = snackbarHostState,
        query = query
    ) { event ->
        when(event) {
            HomeEvent.Convert -> {
                try {
                    val (type, playlistId) = SpotifyApi.extractPlaylistIdFromUrl(query)
                    navController.navigate("playlist/${type!!}/${playlistId!!}")
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Failed to extract playlist from url $query.")
                    }
                }
            }
            is HomeEvent.UpdateQuery -> query = event.query
        }
    }
}