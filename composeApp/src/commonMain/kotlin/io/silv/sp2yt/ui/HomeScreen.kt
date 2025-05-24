package io.silv.sp2yt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.silv.sp2yt.api.SpotifyApi
import io.silv.sp2yt.appGraph
import io.silv.sp2yt.layout.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToPlaylist: (type: String, id: String) -> Unit,
    navigateToYtMusicLogin: () -> Unit,
    navigateToSpotifyLogin: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sp2YT",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            """
                convert spotify playlists and albums to youtube music playlists
            """.trimIndent(),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = text,
                    onQueryChange = { text = it },
                    onSearch = { text ->
                        val (type, playlistId) = SpotifyApi.extractPlaylistIdFromUrl(text)
                        if (playlistId != null && type != null) {
                            navigateToPlaylist(type, playlistId)
                        }
                    },
                    placeholder = {
                        Text("Enter a spotify url to convert")
                    },
                    expanded = false,
                    onExpandedChange = { expanded -> },
                )
            },
            onExpandedChange = { },
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth(0.8f),
            content = {},
            expanded = false
        )
        Button(
            onClick = {
                val (type, playlistId) = SpotifyApi.extractPlaylistIdFromUrl(text)
                if (playlistId != null && type != null) {
                    navigateToPlaylist(type, playlistId)
                }
            }
        ) {
            Text("Convert to Youtube Music")
        }
        Button(
            onClick = {
                navigateToYtMusicLogin()
            }
        ) {
            Text("Login to Youtube Music")
        }
        Button(
            modifier = Modifier,
            onClick = {
                navigateToSpotifyLogin()
            }
        ) {
            Text("Login to spotify")
        }
    }
}