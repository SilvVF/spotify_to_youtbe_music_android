package io.silv.sp2yt.ui.playlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import io.silv.sp2yt.api.SpotifyApi
import io.silv.sp2yt.api.YoutubeMusicApi
import io.silv.sp2yt.appGraph
import io.silv.sp2yt.types.Playlist
import io.silv.sp2yt.types.SongItem
import io.silv.sp2yt.types.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty
import kotlin.collections.set

data class PlaylistArgs(
    val id: String,
    val type: Type
) {

    enum class Type {
        Playlist, Album;

        companion object {
            fun fromString(str: String): Type {
                return when (str.lowercase()) {
                    "playlist" -> Playlist
                    "album" -> Album
                    else -> Playlist
                }
            }
        }
    }

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String>("id")!!,
        Type.fromString(savedStateHandle.get<String>("type")!!),
    )
}

sealed interface PlaylistEvent {
    data object Refresh : PlaylistEvent
    data object Create : PlaylistEvent
    data class OnQueryChange(val query: TextFieldValue) : PlaylistEvent
    data class SetClosest(val track: Track, val item: SongItem) : PlaylistEvent
}

sealed interface SearchSongState {
    data object None : SearchSongState
    data class Loading(val complete: Int, val total: Int) : SearchSongState
    data class Error(val message: String) : SearchSongState
    data class Success(val result: YoutubeMusicApi.PlaylistMatch) : SearchSongState

    val success get() = this as? Success
}

sealed class PlaylistViewState {
    data object Loading : PlaylistViewState()
    data class Error(val message: String?) : PlaylistViewState()
    data class Success(
        val playlist: Playlist,
        val searchSongsResult: SearchSongState,
        val filteredTracks: List<Track>,
        val creating: Boolean = false,
        val query: TextFieldValue,
    ) : PlaylistViewState()

    val success
        get() = this as? Success
}


class PlaylistViewmodel(
    private val spotifyApi: SpotifyApi = appGraph.spotifyApi,
    private val ytMusicApi: YoutubeMusicApi = appGraph.ytMusicApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val navArgs = PlaylistArgs(savedStateHandle)

    val events = MutableSharedFlow<PlaylistEvent>(extraBufferCapacity = 20)

    val snackBarHostState = SnackbarHostState()
    val scope = CoroutineScope(viewModelScope.coroutineContext + Dispatchers.Main)

    val state = scope.launchMolecule(RecompositionMode.Immediate) {
        present()
    }

    @Composable
    fun present(): PlaylistViewState {

        var query by remember { mutableStateOf(TextFieldValue()) }
        var fetchId by remember { mutableIntStateOf(0) }
        var playlistResponse by remember { mutableStateOf<Result<Playlist>?>(null) }
        var searchSongState by remember { mutableStateOf<SearchSongState>(SearchSongState.None) }
        var creating by remember { mutableStateOf(false) }

        LaunchedEffect(fetchId) {
            Snapshot.withMutableSnapshot {
                playlistResponse = null
                searchSongState = SearchSongState.None
            }

            playlistResponse = runCatching {
                when (navArgs.type) {
                    PlaylistArgs.Type.Playlist -> spotifyApi.playlistTracks(navArgs.id)!!.let(::Playlist)
                    PlaylistArgs.Type.Album -> spotifyApi.albumTracks(navArgs.id)!!.let(::Playlist)
                }
            }

            val result = runCatching {
                ytMusicApi.searchSongs(
                    playlistResponse?.getOrNull()?.items.orEmpty(),
                    onProgress = { complete, total ->
                        searchSongState = SearchSongState.Loading(
                            complete,
                            total
                        )
                    }
                )
            }
            searchSongState = result.fold(
                onSuccess = { SearchSongState.Success(it) },
                onFailure = { t ->
                    SearchSongState.Error(t.message.orEmpty())
                }
            )
        }

        LaunchedEffect(Unit) {
            events.collect { event ->
                launch {
                    when (event) {
                        PlaylistEvent.Create -> {
                            val playlist = playlistResponse?.getOrNull() ?: return@launch
                            val ids = searchSongState.success?.result?.matches?.mapNotNull { (_, result) ->
                                result.closest?.id
                            }?.takeIf { it.isNotEmpty() } ?: return@launch

                            creating = true

                            try {
                                ytMusicApi.createPlaylist(
                                    name = playlist.name,
                                    description = playlist.description,
                                    privacyStatus = YoutubeMusicApi.PrivacyStatus.PRIVACY_PRIVATE,
                                    ids = ids
                                )
                            } finally {
                                creating = false
                            }
                        }

                        is PlaylistEvent.OnQueryChange -> query = event.query
                        PlaylistEvent.Refresh -> fetchId++
                        is PlaylistEvent.SetClosest -> {
                            val search = searchSongState.success ?: return@launch
                            searchSongState = search.copy(
                                result = search.result.copy(
                                    matches = search.result.matches.toMutableMap().apply {
                                        this[event.track] = this[event.track]!!.copy(
                                            closest = event.item
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        val filteredTracks by produceState(emptyList()) {
            snapshotFlow { query.text }.combine(
                snapshotFlow { playlistResponse },
                ::Pair
            )
                .collectLatest { (query, response) ->
                    val items = response?.getOrNull()?.items ?: return@collectLatest
                    value = items.filter {
                        query.isEmpty()
                                || it.name.contains(query, true)
                                || it.artists.any { artist -> artist.contains(query, true) }
                                || it.album.contains(query, true)
                    }
                }
        }

        return playlistResponse?.fold(
            onFailure = { PlaylistViewState.Error(it.message.orEmpty()) },
            onSuccess = {
                PlaylistViewState.Success(
                    playlist = it,
                    searchSongsResult = searchSongState,
                    filteredTracks = filteredTracks,
                    creating = creating,
                    query = query,
                )
            }
        )
            ?: PlaylistViewState.Loading
    }
}
