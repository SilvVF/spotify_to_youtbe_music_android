package io.silv.ui

import ButtonPlaceholder
import ListItemPlaceHolder
import ShimmerHost
import SongItem
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import coil.imageLoader
import coil.request.ImageRequest
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
import io.silv.ui.layout.SpotifyTopBarLayout
import io.silv.ui.layout.SpotifyTopBarLayoutFullPoster
import io.silv.ui.layout.rememberTopBarState
import io.silv.ui.theme.SeededMaterialTheme
import io.silv.ui.theme.rememberDominantColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.net.URLDecoder

@Composable
fun PlaylistViewScreen(
    viewModel: PlaylistViewmodel = viewModel<PlaylistViewmodel>(
        factory = PlaylistViewmodel.factory
    ),
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Main.immediate) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PlaylistEvent.CreateError -> {
                            val r = snackBarHostState.showSnackbar(
                                event.message.orEmpty(),
                                actionLabel = "retry"
                            )
                            when (r) {
                                SnackbarResult.Dismissed -> Unit
                                SnackbarResult.ActionPerformed -> viewModel.create()
                            }
                        }
                        PlaylistEvent.Created -> {
                            snackBarHostState.showSnackbar(
                                "created playlist",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }

    val banner = remember(state.success?.playlist) {
        state.success?.playlist?.images?.maxByOrNull { (it.height ?: 0) * (it.width ?: 0) }?.url
    }
    val dominantColor by rememberDominantColor(banner)

    SeededMaterialTheme(seedColor = dominantColor) {
        when (val s = state) {
            is PlaylistViewState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message.orEmpty())
                    Button(
                        onClick = viewModel::refresh
                    ) {
                        Text("Retry")
                    }
                }
            }
            PlaylistViewState.Loading -> ListLoadingScreen(Modifier.fillMaxSize())
            is PlaylistViewState.Success ->{
                if (!dominantColor.isSpecified) {
                    ListLoadingScreen(Modifier.fillMaxSize())
                    return@SeededMaterialTheme
                }
                PlaylistSuccessScreen(
                    state = s,
                    query = { viewModel.query },
                    onQueryChange = { viewModel.query = it },
                    onBack = onBack,
                    onCreate = viewModel::create,
                    setClosest = viewModel::setClosest,
                    snackbarHostState = snackBarHostState
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistSuccessScreen(
    state: PlaylistViewState.Success,
    snackbarHostState: SnackbarHostState,
    query: () -> String,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    setClosest: (SpotifyPlaylist.Tracks.Item, SongItem) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val topBarState = rememberTopBarState(lazyListState)

    val banner = remember(state.playlist) {
        state.playlist.images.maxByOrNull { (it.height ?: 0) * (it.width ?: 0) }?.url
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
                query = query,
                topBarState = topBarState,
                name = state.playlist.name,
            )
        },
        info = {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp),) {
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
                    maxLines = 3,
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
                  Icon(
                      modifier = Modifier,
                      imageVector = Icons.Filled.Add,
                      contentDescription = null
                  )
                }
            }
        }
    ) { paddingValues ->
        val search = state.searchSongsResult
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            state = lazyListState
        ) {
            if (search is SearchSongState.Loading) {
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
                                progress = { search.complete / search.total.toFloat() }
                            )
                            Text(
                                "${search.complete} / ${search.total}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
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
                        onLongClick = {},
                        badge = {
                            IconButton(
                                onClick = {}
                            ) {
                                val hasMatch by remember(search.success?.result) {
                                    derivedStateOf {
                                        search.success?.result?.matches?.get(item) != null
                                    }
                                }
                                Icon(
                                    imageVector = if (hasMatch) {
                                        Icons.Filled.CheckCircle
                                    } else Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = if (hasMatch) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
                    when (search) {
                        is SearchSongState.Error -> {}
                        is SearchSongState.Loading -> {
                            ShimmerHost {
                                repeat(if (expanded) 4 else 0) {
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
                                val songs = remember(expanded, match) {
                                    if (!expanded)
                                        emptyList()
                                    else
                                        buildList { add(match); addAll(additional.filterNot { it == match }) }
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
                                            title = song.title + song.id,
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
