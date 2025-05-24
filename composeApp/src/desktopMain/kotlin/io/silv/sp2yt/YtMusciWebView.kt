package io.silv.sp2yt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI


private const val YT_AUTH_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtMusciDesktopWebView(onBack: () -> Unit) {

    var visitorData by remember { mutableStateOf(appGraph.ytMusicApi.visitorData) }
    var cookie by remember { mutableStateOf(appGraph.ytMusicApi.cookie) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Login",
                        color = LocalContentColor.current,
                        style = LocalTextStyle.current
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("<", color = LocalContentColor.current)
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                ExtendedFloatingActionButton(
                    onClick = {
                        val url = URI.create(YT_AUTH_URL)
                        Desktop.getDesktop().browse(url)
                    },
                    icon = {},
                    text = {
                        Text("open yt music", color = LocalContentColor.current)
                    }
                )
                Spacer(Modifier.height(6.dp))
                ExtendedFloatingActionButton(
                    onClick = {
                        appGraph.ytMusicApi.visitorData = visitorData
                        appGraph.ytMusicApi.cookie = cookie
                        onBack()
                    },
                    icon = {},
                    text = {
                        Text("Set values", color = LocalContentColor.current)
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                label = {
                    Text("visitor data", color = LocalContentColor.current)
                },
                colors = OutlinedTextFieldDefaults.colors(),
                value = visitorData,
                onValueChange = {
                    visitorData = it
                }
            )
            OutlinedTextField(
                label = {
                    Text("cookies", color = LocalContentColor.current)
                },
                colors = OutlinedTextFieldDefaults.colors(),
                value = cookie,
                onValueChange = {
                    cookie = it
                }
            )
        }
    }
}

