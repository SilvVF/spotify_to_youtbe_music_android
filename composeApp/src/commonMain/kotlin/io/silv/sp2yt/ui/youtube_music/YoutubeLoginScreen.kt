package io.silv.sp2yt.ui.youtube_music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.silv.sp2yt.appGraph


@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun YoutubeLoginScreen(
    state: YoutubeLoginState,
    onBack: () -> Unit,
)

private const val YT_AUTH_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputLoginScreen(
    state: YoutubeLoginState,
    openUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
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
                        openUrl(YT_AUTH_URL)
                    },
                    icon = {},
                    text = {
                        Text("open yt music", color = LocalContentColor.current)
                    }
                )
                Spacer(Modifier.height(6.dp))
                ExtendedFloatingActionButton(
                    onClick = {
                        state.events(YoutubeLoginEvent.ConfirmValues)
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
                value = state.visitorData,
                onValueChange = {
                    state.events(YoutubeLoginEvent.SetVisitorData(it))
                }
            )
            OutlinedTextField(
                label = {
                    Text("cookies", color = LocalContentColor.current)
                },
                colors = OutlinedTextFieldDefaults.colors(),
                value = state.cookie,
                onValueChange = {
                    state.events(YoutubeLoginEvent.SetCookie(it))
                }
            )
        }
    }
}