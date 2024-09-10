package io.silv

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.moshi.Moshi
import io.silv.types.SpotifyPlaylist
import io.silv.ui.layout.EntryListItem
import io.silv.ui.layout.PinnedTopBar
import io.silv.ui.layout.Poster
import io.silv.ui.layout.SpotifyTopBarLayout
import io.silv.ui.layout.rememberTopBarState
import io.silv.ui.theme.SptoytTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val moshi = Moshi.Builder()
    .build()

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api by lazy { SpotifyApi(application) }
        val ytApi by lazy { YtMusicApi() }

        enableEdgeToEdge()
        setContent {

            val resp by produceState<SpotifyPlaylist?>(null) {
                val r = withContext(Dispatchers.IO) { api.playlist("3cEYpjA9oz9GiPac4AsH4n") }
                value = r
            }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        Log.d("YtMusicApi", ytApi.search("ado").body?.string().orEmpty())
                    }.logError("YtMusicApi")
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
                        contentPadding = paddingValues
                    ) {
                        items(resp?.tracks?.items.orEmpty()) { item ->
                            ContentListItem(
                                title = item.track.name,
                                url = item.track.album.images.firstOrNull()?.url.orEmpty(),
                                onClick = {},
                                onLongClick = {}
                            )
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