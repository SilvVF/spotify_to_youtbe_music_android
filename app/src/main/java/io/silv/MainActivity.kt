package io.silv

import SongItem
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import io.silv.types.SpotifyPlaylist
import io.silv.ui.layout.EntryListItem
import io.silv.ui.layout.PinnedTopBar
import io.silv.ui.layout.Poster
import io.silv.ui.layout.SpotifyTopBarLayout
import io.silv.ui.layout.rememberTopBarState
import io.silv.ui.theme.SptoytTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import okhttp3.Cache
import java.io.File

class MainActivity : ComponentActivity() {

    val cache by lazy { Cache(File(applicationContext.cacheDir, "net_cache"), 5000) }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api by lazy {
            SpotifyApi(application) { cache(cache) }
        }
        val ytApi by lazy {
            YtMusicApi { cache(cache) }
        }

        enableEdgeToEdge()
        setContent {

            val resp by produceState<SpotifyPlaylist?>(null) {
                val r = withContext(Dispatchers.IO) { api.playlist("37i9dQZF1EIXwW3DKBf9K8") }
                value = r
            }

            val searchResult by produceState<SearchSongsResult?>(null) {
                snapshotFlow { resp }.filterNotNull().collectLatest {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            resp?.let { r ->
                                val result = ytApi.searchSongs(r.tracks)
                                withContext(Dispatchers.Main) { value = result }
                            }
                        }.logError("YtMusicApi")
                    }
                }
            }

            SptoytTheme {

                val lazyListState = rememberLazyListState()
                val topBarState = rememberTopBarState(lazyListState)

                SpotifyTopBarLayout(
                    modifier =   Modifier.fillMaxSize()
                    .nestedScroll(topBarState.connection)
                    .imePadding(),
                    topBarState = topBarState,
                    poster = {
                        Poster(
                            url = resp?.images?.firstOrNull()?.url,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )
                    },
                    topAppBar = {
                        PinnedTopBar(
                            onBackPressed = {},
                            onQueryChanged = {},
                            query = "",
                            topBarState = topBarState,
                            name = resp?.name.orEmpty()
                        )
                    },
                    info = {
                        Text(
                            text = resp?.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    },
                    pinnedButton = {
                        FilledIconButton(
                            onClick = {},
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
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
                        items(resp?.tracks?.items.orEmpty()) { item ->
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
                                searchResult?.matches?.get(item)?.let { (match, additional) ->
                                    val items =  remember(expanded) {
                                        if (!expanded) listOf(match) else buildList {
                                            add(match)
                                            addAll(additional.filterNot { it.id == match.id })
                                        }
                                    }
                                    items.map { item ->
                                        Box(
                                            Modifier.graphicsLayer {
                                                scaleX = 0.9f
                                                scaleY = 0.9f
                                                alpha = 0.9f
                                            }
                                        ) {
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
}

@Composable
fun ContentListItem(
    title: String,
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    EntryListItem(
        title = title,
        coverData = url,
        coverAlpha = 1f,
        onLongClick = onLongClick,
        onClick = onClick,
        badge = {},
        endButton = content,
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SptoytTheme {
        Greeting("Android")
    }
}