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
fun SetupSpotifyScreen(
    onBack: () -> Unit,
) {
    var cid by remember { mutableStateOf(SpotifyApi.CLIENT_ID) }
    var secret by remember { mutableStateOf(SpotifyApi.CLIENT_SECRET) }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = cid,
            onValueChange = { cid = it },
            label = { Text("Client id") }
        )
        TextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text("Secret") }
        )
        Button(
            onClick = {
                SpotifyApi.CLIENT_ID = cid
                SpotifyApi.CLIENT_SECRET = secret
                onBack()
            }
        ) {
            Text("Set values")
        }
    }
}