package io.silv.sp2yt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.silv.sp2yt.api.YtMusicApi
import java.awt.Desktop
import java.net.URI


private const val YT_AUTH_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtMusciDesktopWebView(onBack: () -> Unit) {

    var visitorData by remember { mutableStateOf(YtMusicApi.visitorData) }
    var cookie by remember { mutableStateOf(YtMusicApi.cookie) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            Column {
                Button(
                    onClick = {
                        val url = URI.create(YT_AUTH_URL)
                        Desktop.getDesktop().browse(url)

                    }
                ) {
                    Text("open yt music")
                }
                Button(
                    onClick = {
                        YtMusicApi.visitorData = visitorData
                        YtMusicApi.cookie = cookie
                    }
                ) {
                    Text("Set values")
                }
                TextField(
                    label = {
                        Text("visitor data")
                    },
                    value = visitorData,
                    onValueChange = {
                        visitorData = it
                    }
                )
                TextField(
                    label = {
                        Text("cookies")
                    },
                    value = cookie,
                    onValueChange = {
                        cookie = it
                    }
                )
            }
        }
    }
}

