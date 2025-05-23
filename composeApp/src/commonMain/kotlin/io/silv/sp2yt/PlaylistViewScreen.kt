package io.silv.sp2yt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import coil3.compose.AsyncImage
import io.silv.sp2yt.api.*
import io.silv.sp2yt.layout.EntryListItem
import io.silv.sp2yt.layout.PinnedTopBar
import io.silv.sp2yt.layout.SearchField
import io.silv.sp2yt.layout.SpotifyTopBarLayout
import io.silv.sp2yt.layout.rememberTopBarState
import io.silv.sp2yt.types.Playlist
import io.silv.sp2yt.types.SongItem
import io.silv.sp2yt.types.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.contains

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
    data class OnQueryChange(val query: String) : PlaylistEvent
    data class SetClosest(val track: Track, val item: SongItem) : PlaylistEvent
}

sealed interface SearchSongState {
    data object None : SearchSongState
    data class Loading(val complete: Int, val total: Int) : SearchSongState
    data class Error(val message: String) : SearchSongState
    data class Success(val result: PlaylistMatch) : SearchSongState

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
        val query: String,
    ) : PlaylistViewState()

    val success
        get() = this as? Success
}

class PlaylistViewmodel(
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

        var query by remember { mutableStateOf("") }
        var playlistResponse by remember { mutableStateOf<Result<Playlist>?>(null) }
        var searchSongState by remember { mutableStateOf<SearchSongState>(SearchSongState.None) }
        var creating by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            playlistResponse = null
            searchSongState = SearchSongState.None

            playlistResponse = runCatching {
                when (navArgs.type) {
                    PlaylistArgs.Type.Playlist -> SpotifyApi.playlistTracks(navArgs.id)!!.let(::Playlist)
                    PlaylistArgs.Type.Album -> SpotifyApi.albumTracks(navArgs.id)!!.let(::Playlist)
                }
            }
            launch {
                val result = runCatching {
                    YtMusicApi.searchSongs(
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
        }

        val filteredTracks by produceState(emptyList<Track>()) {
            snapshotFlow { query }.combine(
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

@Composable
fun PlaylistViewScreen(
    viewModel: PlaylistViewmodel,
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface {
        when (val s = state) {
            is PlaylistViewState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message.orEmpty())
                    Button(
                        onClick = { viewModel.events.tryEmit(PlaylistEvent.Refresh) }
                    ) {
                        Text("Retry")
                    }
                }
            }

            PlaylistViewState.Loading -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is PlaylistViewState.Success -> {
                PlaylistSuccessScreen(
                    state = s,
                    onQueryChange = { viewModel.events.tryEmit(PlaylistEvent.OnQueryChange(it)) },
                    onBack = onBack,
                    onCreate = { viewModel.events.tryEmit(PlaylistEvent.Create) },
                    setClosest = { t, i -> viewModel.events.tryEmit(PlaylistEvent.SetClosest(t, i)) },
                    snackbarHostState = viewModel.snackBarHostState
                )
            }
        }
    }
}

fun String.removeHtmlTags(): String {
    val regex = Regex("<[^>]+>")
    var result = this
    val tags = regex.findAll(this).map { it.value }
    for (tag in tags) {
        result = result.replace(tag, "")
    }
    return result
}

@Composable
fun ContentListItem(
    title: String,
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badge: (@Composable RowScope.() -> Unit) = {},
    content: (@Composable () -> Unit)? = null,
) {
    EntryListItem(
        title = title,
        coverData = url,
        coverAlpha = 1f,
        onLongClick = onLongClick,
        onClick = onClick,
        badge = badge,
        endButton = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSuccessScreen(
    state: PlaylistViewState.Success,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    setClosest: (Track, SongItem) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val topBarState = rememberTopBarState(lazyListState)

    val banner = remember(state.playlist) {
        state.playlist.images.maxByOrNull { (it.h ?: 0) * (it.w ?: 0) }?.url
    }

    SpotifyTopBarLayout(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarState.connection)
            .imePadding(),
        snackBarHost = { SnackbarHost(snackbarHostState) },
        topBarState = topBarState,
        search = {
            SearchField(
                topBarState = topBarState,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        },
        poster = {
            AsyncImage(
                model = banner,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentDescription = null,
                placeholder = remember { ColorPainter(Color.Black) },
                contentScale = ContentScale.Crop
            )
        },
        topAppBar = {
            PinnedTopBar(
                onBackPressed = onBack,
                onQueryChanged = onQueryChange,
                query = { state.query },
                topBarState = topBarState,
                name = state.playlist.name,
            )
        },
        info = {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = state.playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    remember(state.playlist.description) { state.playlist.description.removeHtmlTags() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Total: ${state.playlist.items.size}, Missing: ${state.searchSongsResult.success?.result?.notFound?.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        pinnedButton = {
            FilledIconButton(
                onClick = onCreate,
                enabled = !state.creating,
                modifier = Modifier
                    .size(48.dp),
            ) {
                if (state.creating) {
                    CircularProgressIndicator()
                } else {
                    Text("+")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            state = lazyListState
        ) {
            if (state.searchSongsResult is SearchSongState.Error) {
                item(
                    key = "error-results"
                ) {
                    Column {
                        Text(
                            "Error ${state.searchSongsResult.message}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            if (state.searchSongsResult is SearchSongState.Loading) {
                item(
                    key = "loading-results"
                ) {
                    Column {
                        Text(
                            "generating matches",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                progress = { state.searchSongsResult.complete / state.searchSongsResult.total.toFloat() }
                            )
                            Text(
                                "${state.searchSongsResult.complete} / ${state.searchSongsResult.total}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            items(
                items = state.filteredTracks,
                key = { it.id }
            ) { item ->
                var expanded by rememberSaveable { mutableStateOf(false) }

                Column(
                    Modifier.animateContentSize()
                ) {
                    ContentListItem(
                        title = item.name + " - " + item.artists.joinToString(", "),
                        url = item.images.firstOrNull()?.url.orEmpty(),
                        onClick = {},
                        onLongClick = {},
                        badge = {
                            IconButton(
                                onClick = {}
                            ) {
                                val status by remember(state.searchSongsResult.success?.result) {
                                    derivedStateOf {
                                        val result = state.searchSongsResult.success?.result
                                        Pair(
                                            result?.matches?.get(item)?.closest != null,
                                            state.searchSongsResult.success?.result?.lowConfidence?.contains(item) == true
                                        )
                                    }
                                }

                                Text(
                                    text = when {
                                        status.first && !status.second -> "(v)"
                                        status.first -> "(i)"
                                        else -> "(!)"
                                    },
                                    color = when {
                                        status.first && !status.second -> MaterialTheme.colorScheme.primary
                                        status.first -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    ) {
                        TextButton(
                            onClick = { expanded = !expanded }
                        ) {
                            Text(
                                text = "v",
                                modifier = Modifier.rotate(if (expanded) 180f else 0f),
                            )
                        }
                    }
                    val scaleModifier = Modifier.graphicsLayer {
                        scaleX = 0.9f
                        scaleY = 0.9f
                        alpha = 0.9f
                    }
                    (state.searchSongsResult as? SearchSongState.Success)?.result?.matches[item]?.let { (match, additional) ->
                        val songs = remember(expanded, match) {
                            if (!expanded)
                                emptyList()
                            else
                                buildList {
                                    match?.let(::add)
                                    addAll(additional.filterNot { it == match })
                                }
                        }
                        songs.map { song ->
                            Box(
                                scaleModifier.then(
                                    if (song == match)
                                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
                                    else Modifier
                                )
                            ) {
                                ContentListItem(
                                    title = "${song.title} - ${song.artists.joinToString(", ") { it.name }}",
                                    url = song.thumbnail,
                                    onClick = { setClosest(item, song) },
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