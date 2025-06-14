package io.silv.sp2yt.ui.playlist

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
import androidx.compose.ui.graphics.Brush
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
import compose.icons.AllIcons
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.CheckCircle
import compose.icons.fontawesomeicons.solid.ExclamationTriangle
import compose.icons.fontawesomeicons.solid.Info
import compose.icons.fontawesomeicons.solid.InfoCircle
import io.silv.sp2yt.removeHtmlTags
import io.silv.sp2yt.types.SongItem
import io.silv.sp2yt.types.Track
import io.silv.sp2yt.ui.layout.ButtonPlaceholder
import io.silv.sp2yt.ui.layout.EntryListItem
import io.silv.sp2yt.ui.layout.ListItemPlaceHolder
import io.silv.sp2yt.ui.layout.PinnedTopBar
import io.silv.sp2yt.ui.layout.SearchField
import io.silv.sp2yt.ui.layout.ShimmerHost
import io.silv.sp2yt.ui.layout.SpotifyTopBarLayout
import io.silv.sp2yt.ui.layout.rememberTopBarState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistViewScreen(
    viewModel: PlaylistViewmodel,
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface {
        when (val s = state) {
            is PlaylistViewState.Error, PlaylistViewState.Loading -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        onBack()
                                    }
                                ) {
                                    Icon(
                                        imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                ) {
                    if (state is PlaylistViewState.Loading) {
                        ListLoadingScreen(Modifier.padding(it))
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text((s as? PlaylistViewState.Error)?.message.orEmpty())
                            Button(
                                onClick = { viewModel.events.tryEmit(PlaylistEvent.Refresh) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is PlaylistViewState.Success -> {
                PlaylistSuccessScreen(
                    state = s,
                    onQueryChange = { viewModel.events.tryEmit(PlaylistEvent.OnQueryChange(it)) },
                    onBack = onBack,
                    onCreate = { viewModel.events.tryEmit(PlaylistEvent.Create) },
                    setClosest = { t, i ->
                        viewModel.events.tryEmit(
                            PlaylistEvent.SetClosest(t, i)
                        )
                    },
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

    val color = MaterialTheme.colorScheme.primary

    SpotifyTopBarLayout(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarState.connection)
            .imePadding(),
        gradiant = Brush.verticalGradient(
            listOf(
                color,
                Color.Transparent
            )
        ),
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
                YoutubeListItem(
                    state,
                    item,
                    setClosest
                )
            }
        }
    }
}

@Composable
private fun YoutubeListItem(
    state: PlaylistViewState.Success,
    item: Track,
    setClosest: (Track, SongItem) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val status by remember(state.searchSongsResult) {
        derivedStateOf {
            val result = state.searchSongsResult.success?.result
            Pair(
                result?.matches?.get(item)?.closest != null,
                state.searchSongsResult.success?.result?.lowConfidence?.contains(
                    item
                ) == true
            )
        }
    }

    val thumbnail by remember(state.searchSongsResult, item) {
        derivedStateOf {
            val match = state.searchSongsResult.success?.result?.matches?.get(item)
            match?.closest?.thumbnail ?: item.images.firstOrNull()?.url
        }
    }

    Column(
        Modifier.animateContentSize()
    ) {
        ContentListItem(
            title = item.name + " - " + item.artists.joinToString(", "),
            url = thumbnail.orEmpty(),
            onClick = { expanded = !expanded },
            onLongClick = {},
            badge = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = when {
                        status.first && !status.second -> FontAwesomeIcons.Solid.CheckCircle
                        status.first -> FontAwesomeIcons.Solid.InfoCircle
                        else -> FontAwesomeIcons.Solid.ExclamationTriangle
                    },
                    tint = when {
                        status.first && !status.second -> MaterialTheme.colorScheme.primary
                        status.first -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    },
                    contentDescription = null
                )
            }
        )
        YoutubeItemAlternatives(
            expanded = expanded,
            track = item,
            state = state.searchSongsResult,
            setClosest = {
                setClosest(item, it)
            }
        )
    }
}

@Composable
private fun YoutubeItemAlternatives(
    setClosest: (SongItem) -> Unit,
    track: Track,
    state: SearchSongState,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val scaleModifier = Modifier.graphicsLayer {
        scaleX = 0.9f
        scaleY = 0.9f
        alpha = 0.9f
    }
    Column(modifier) {
        when (state) {
            is SearchSongState.Error -> Text("Failed to load songs: ${state.message}")
            is SearchSongState.Loading,
            SearchSongState.None -> {
                ListItemPlaceHolder()
            }

            is SearchSongState.Success -> {
                state.result.matches[track]?.let { (match, additional) ->
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
                                onClick = { setClosest(song) },
                                onLongClick = {},
                            )
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