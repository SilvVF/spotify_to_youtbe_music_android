package io.silv.ui

import ButtonPlaceholder
import ListItemPlaceHolder
import ShimmerHost
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.silv.App
import io.silv.ContentListItem
import io.silv.PrivacyStatus
import io.silv.SearchSongsResult
import io.silv.SpotifyApi
import io.silv.YtMusicApi
import io.silv.createPlaylist
import io.silv.playlist
import io.silv.removeHtmlTags
import io.silv.searchSongs
import io.silv.types.SpotifyPlaylist
import io.silv.ui.layout.PinnedTopBar
import io.silv.ui.layout.SearchField
import io.silv.ui.layout.SpotifyTopBarLayoutFullPoster
import io.silv.ui.layout.rememberTopBarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.net.URLDecoder

data class PlaylistArgs(
    val playlistId: String
): Serializable {

    constructor(savedStateHandle: SavedStateHandle): this(
        URLDecoder.decode(savedStateHandle.get<String>("id")!!, Charsets.UTF_8.name())
    )
}

sealed interface SearchSongState {
    data object Loading: SearchSongState
    data class Error(val message: String): SearchSongState
    data class Success(val result: SearchSongsResult): SearchSongState

    val success get() = this as? Success
}

sealed class PlaylistViewState {
    data object Loading: PlaylistViewState()
    data class Error(val message: String?): PlaylistViewState()
    data class Success(
        val playlist: SpotifyPlaylist,
        val searchSongsResult: SearchSongState = SearchSongState.Loading,
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

    private val _state = MutableStateFlow<PlaylistViewState>(PlaylistViewState.Loading)
    val state: StateFlow<PlaylistViewState> get() = _state.asStateFlow()

    var refreshJob: Job? = null

    private val tracksFlow = state.filterIsInstance<PlaylistViewState.Success>()
        .map { it.playlist.tracks }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            launch {
                tracksFlow.collectLatest { tracks ->
                    val result = withContext(Dispatchers.IO) {
                        runCatching { ytMusicApi.searchSongs(tracks) }
                    }
                    _state.update { state ->
                        state.success?.let { s ->
                            s.copy(
                                searchSongsResult = result.fold(
                                    onSuccess = { SearchSongState.Success(it) },
                                    onFailure = { t -> SearchSongState.Error(t.message.orEmpty()) }
                                )
                            )
                        } ?: state
                    }
                }
            }
            refresh()
        }
    }

    fun create() {
        viewModelScope.launch {
            state.value.success?.let { s ->
                val ids = s.searchSongsResult.success?.result?.matches?.map { it.value.closest.id } ?: return@let
                val result = ytMusicApi.createPlaylist(
                    s.playlist.name,
                    s.playlist.description,
                    PrivacyStatus.PRIVACY_PRIVATE,
                    ids
                )
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { PlaylistViewState.Loading }
            val res = withContext(Dispatchers.IO) {
                runCatching { spotifyApi.playlist(navArgs.playlistId)!! }
            }
            _state.value = res.fold(
                onFailure = { PlaylistViewState.Error(it.message) },
                onSuccess = { PlaylistViewState.Success(it, SearchSongState.Loading) }
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

@Composable
fun PlaylistViewScreen(
    viewModel: PlaylistViewmodel = viewModel<PlaylistViewmodel>(
        factory = PlaylistViewmodel.factory
    ),
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is PlaylistViewState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.message.orEmpty())
                Button(
                    onClick =  { viewModel.refresh() }
                ) {
                    Text("Retry")
                }
            }
        }
        PlaylistViewState.Loading -> ListLoadingScreen(Modifier.fillMaxSize())
        is PlaylistViewState.Success -> PlaylistSuccessScreen(
            state = s,
            query = { viewModel.query },
            onQueryChange = { viewModel.query = it },
            onBack = onBack,
            onCreate = { viewModel.create() }
        )
    }
}

@Composable
private fun PlaylistSuccessScreen(
    state: PlaylistViewState.Success,
    query: () -> String,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val topBarState = rememberTopBarState(lazyListState)

    SpotifyTopBarLayoutFullPoster(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarState.connection)
            .imePadding(),
        topBarState = topBarState,
        search = {
            SearchField(
                topBarState = topBarState,
                modifier = Modifier.padding(horizontal = 18.dp),
                background = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    .copy(alpha = 0.6f)
            )
        },
        poster = {
            AsyncImage(
                model = state.playlist.images.firstOrNull()?.url.orEmpty(),
                modifier = Modifier.fillMaxHeight(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        },
        topAppBar = {
            PinnedTopBar(
                onBackPressed = onBack,
                onQueryChanged = onQueryChange,
                query = query,
                topBarState = topBarState,
                name = state.playlist.name
            )
        },
        info = {
            Column {
                Text(
                    text = state.playlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
                Text(
                    remember(state.playlist.description) { state.playlist.description.removeHtmlTags() },
                    maxLines = 2
                )
            }
        },
        pinnedButton = {
            IconButton(
                onClick = onCreate,
                modifier = Modifier
                    .size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor =  MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        .copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    modifier = Modifier,
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            state = lazyListState
        ) {
            items(
                items = state.playlist.tracks.items,
                key = { it.track.id }
            ) { item ->
                var expanded by rememberSaveable { mutableStateOf(false) }
                Column(
                    Modifier.animateContentSize()
                ) {
                    ContentListItem(
                        title = item.track.name,
                        url = item.track.album.images.firstOrNull()?.url.orEmpty(),
                        onClick = {},
                        onLongClick = {}
                    ) {
                        IconButton(
                            onClick = { expanded = !expanded }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                modifier = Modifier.rotate(if (expanded) 180f else 0f),
                                contentDescription = null
                            )
                        }
                    }
                    val scaleModifier = Modifier.graphicsLayer {
                        scaleX = 0.9f
                        scaleY = 0.9f
                        alpha = 0.9f
                    }
                    when (val search = state.searchSongsResult) {
                        is SearchSongState.Error -> {}
                        SearchSongState.Loading -> {
                            ShimmerHost {
                                repeat(if (expanded) 4 else 1) {
                                    Box(scaleModifier) {
                                        Box(
                                            modifier = Modifier
                                                .height(56.dp)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.onSurface),
                                        )
                                    }
                                }
                            }
                        }
                        is SearchSongState.Success -> {
                            search.result.matches[item]?.let { (match, additional) ->
                                val items = remember(expanded) {
                                    if (!expanded) listOf(match) else additional
                                }
                                items.map { item ->
                                    Box(scaleModifier) {
                                        ContentListItem(
                                            title = item.title + item.id,
                                            url = item.thumbnail,
                                            onClick = {},
                                            onLongClick = {},
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListLoadingScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier) { paddingValues ->
        ShimmerHost(
            Modifier.padding(paddingValues)
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier
                            .size(244.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                Spacer(Modifier.padding(8.dp))


                Row {
                    ButtonPlaceholder(
                        Modifier.weight(0.8f)
                    )
                    Spacer(Modifier.padding(8.dp))
                    ButtonPlaceholder(
                        Modifier
                            .weight(0.2f)
                            .clip(CircleShape)
                    )
                }
            }

            repeat(6) {
                ListItemPlaceHolder()
            }
        }
    }
}
