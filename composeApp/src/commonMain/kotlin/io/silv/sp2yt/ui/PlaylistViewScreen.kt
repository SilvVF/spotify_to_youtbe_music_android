package io.silv.sp2yt.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.silv.sp2yt.PlaylistEvent
import io.silv.sp2yt.PlaylistViewState
import io.silv.sp2yt.PlaylistViewmodel
import io.silv.sp2yt.SearchSongState
import io.silv.sp2yt.layout.*
import io.silv.sp2yt.removeHtmlTags
import io.silv.sp2yt.types.SongItem
import io.silv.sp2yt.types.Track


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
    onQueryChange: (TextFieldValue) -> Unit,
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