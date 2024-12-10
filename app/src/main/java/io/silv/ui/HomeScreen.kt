package io.silv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.silv.SpotifyApi

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
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = text,
            onValueChange = { text = it }
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