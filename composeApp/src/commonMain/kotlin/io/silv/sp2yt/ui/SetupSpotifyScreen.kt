package io.silv.sp2yt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.silv.sp2yt.NiaGradientBackground
import io.silv.sp2yt.appGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSpotifyScreen(
    onBack: () -> Unit,
) {
    var cid by remember { mutableStateOf(appGraph.spotifyApi.clientId) }
    var secret by remember { mutableStateOf(appGraph.spotifyApi.clientSecret) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Spotify Setup")
                },
                navigationIcon = {
                    TextButton(
                        onClick = { onBack() }
                    ) {
                        Text("<")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    appGraph.spotifyApi.clientSecret = secret
                    appGraph.spotifyApi.clientId = cid

                    onBack()
                },
                icon = {},
                text = {
                    Text("Set values")
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = cid,
                onValueChange = { cid = it },
                label = { Text("Client id") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("Secret") }
            )
        }
    }
}