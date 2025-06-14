package io.silv.sp2yt.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.silv.sp2yt.api.SpotifyApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    navigateToYtMusicLogin: () -> Unit,
    navigateToSpotifyLogin: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(state.snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.query,
                        onQueryChange = { state.events(HomeEvent.UpdateQuery(it)) },
                        onSearch = { text ->
                            state.events(HomeEvent.Convert)
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
            Spacer(Modifier.height(48.dp))
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavigationButton(
                    text = "Convert to Youtube Music",
                    onClick = {
                        state.events(HomeEvent.Convert)
                    }
                )
                NavigationButton(
                    text = "Login to Youtube Music",
                    onClick = {
                        navigateToYtMusicLogin()
                    }
                )
                NavigationButton(
                    text = "Login to Spotify",
                    onClick = {
                        navigateToSpotifyLogin()
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text)
    }
}