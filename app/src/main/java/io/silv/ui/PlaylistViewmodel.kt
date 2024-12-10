package io.silv.ui

import SongItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.silv.App
import io.silv.PrivacyStatus
import io.silv.PlaylistMatch
import io.silv.SpotifyApi
import io.silv.Timber
import io.silv.YtMusicApi
import io.silv.album
import io.silv.createPlaylist
import io.silv.playlist
import io.silv.searchSongs
import io.silv.types.Playlist
import io.silv.types.Track
import io.silv.ui.PlaylistArgs.Type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.net.URLDecoder

data class PlaylistArgs(
    val id: String,
    val type: Type
): Serializable {

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

    constructor(savedStateHandle: SavedStateHandle): this(
        URLDecoder.decode(savedStateHandle.get<String>("id")!!, Charsets.UTF_8.name()),
        Type.fromString(URLDecoder.decode(savedStateHandle.get<String>("type")!!, Charsets.UTF_8.name())),
    )
}

sealed interface SearchSongState {
    data class Loading(val complete: Int, val total: Int): SearchSongState
    data class Error(val message: String): SearchSongState
    data class Success(val result: PlaylistMatch): SearchSongState

    val success get() = this as? Success
}

fun PlaylistViewState.Success.updateMatchesAndGet(
    update: MutableMap<Track, PlaylistMatch.Result>.() -> Unit
): SearchSongState {
    val r = this.searchSongsResult.success ?: return this.searchSongsResult
    return r.copy(
        result = r.result.copy(
            matches = r.result.matches.toMutableMap().apply(update).toMap()
        )
    )
}

sealed interface PlaylistEvent {
    data object Created: PlaylistEvent
    data class CreateError(val message: String?): PlaylistEvent
}

sealed class PlaylistViewState {
    data object Loading: PlaylistViewState()
    data class Error(val message: String?): PlaylistViewState()
    data class Success(
        val playlist: Playlist,
        val searchSongsResult: SearchSongState,
        val creating: Boolean = false
    ): PlaylistViewState()

    val success
        get() = this as? Success
}

class PlaylistViewmodel(
    private val ytMusicApi: YtMusicApi,
    private val spotifyApi: SpotifyApi,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    private val navArgs = PlaylistArgs(savedStateHandle)

    var query by mutableStateOf("")

    private val _events = Channel<PlaylistEvent>(capacity = 20)
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow<PlaylistViewState>(PlaylistViewState.Loading)
    val state: StateFlow<PlaylistViewState> get() = _state.asStateFlow()

    private var refreshJob: Job? = null

    private fun MutableStateFlow<PlaylistViewState>.updateSuccess(
        update: (PlaylistViewState.Success) -> PlaylistViewState
    ) {
        this.update { state ->
            when(state) {
                is PlaylistViewState.Success -> update(state)
                else -> state
            }
        }
    }

    private val playlistFlow = state.filterIsInstance<PlaylistViewState.Success>()
        .map { it.playlist }
        .distinctUntilChanged()

    init {
        playlistFlow.onEach { playlist ->
            val result = getClosestFrom(playlist)
            _state.updateSuccess { state ->
                state.copy(
                    searchSongsResult = result.fold(
                        onSuccess = { SearchSongState.Success(it) },
                        onFailure = { t ->
                            Timber.e(t)
                            SearchSongState.Error(t.message.orEmpty())
                        }
                    )
                )
            }
        }
            .launchIn(viewModelScope)

        refresh()
    }

    private suspend fun getClosestFrom(playlist: Playlist): Result<PlaylistMatch> {
        return withContext(Dispatchers.IO) {
            runCatching {
                ytMusicApi.searchSongs(
                    playlist.items,
                    onProgress = { complete, total ->
                        _state.updateSuccess { state ->
                            state.copy(
                                searchSongsResult = SearchSongState.Loading(
                                    complete,
                                    total
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    fun create() {
        viewModelScope.launch {
            _state.updateSuccess { s -> s.copy(creating = true) }
            val successState = state.value.success ?: return@launch
            val selectedIds = successState.searchSongsResult.success?.result?.matches
                ?.map { it.value.closest?.id }
                ?.filterNotNull()
                .orEmpty()

            val response = runCatching {
                ytMusicApi.createPlaylist(
                    successState.playlist.name,
                    successState.playlist.description,
                    PrivacyStatus.PRIVACY_PRIVATE,
                    selectedIds
                )
            }
            _events.send(
                response.fold(
                    onSuccess = { PlaylistEvent.Created },
                    onFailure = { PlaylistEvent.CreateError(it.message) }
                )
            )
            _state.updateSuccess { s -> s.copy(creating = false) }
        }
    }

    fun setClosest(
        track: Track,
        new: SongItem?
    ) = viewModelScope.launch {
        _state.updateSuccess { state ->
            state.copy(
                searchSongsResult = state.updateMatchesAndGet {
                    this[track] = PlaylistMatch.Result(
                        closest = if (new == this[track]?.closest) null else new,
                        other = this[track]?.other.orEmpty()
                    )
                }
            )
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { PlaylistViewState.Loading }
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    when(navArgs.type) {
                        Playlist -> spotifyApi.playlist(navArgs.id)!!.let(::Playlist)
                        Album -> spotifyApi.album(navArgs.id)!!.let(::Playlist)
                    }
                }
            }
            _state.value = res.fold(
                onFailure = {
                    Timber.e(it)
                    PlaylistViewState.Error(it.message)
                },
                onSuccess = { PlaylistViewState.Success(
                    playlist = it,
                    searchSongsResult = SearchSongState.Loading(0, it.items.size)
                )}
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    companion object {
        val factory = object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return PlaylistViewmodel(
                    spotifyApi = App.spotifyApi,
                    ytMusicApi = App.ytMusicApi,
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }
}
