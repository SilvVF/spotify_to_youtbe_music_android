package io.silv.sp2yt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import io.silv.sp2yt.api.SpotifyApi

@Composable
fun App() {

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(httpClient = { appScope.client })
                )
            }
            .crossfade(true)
            .build()
    }

    MaterialTheme {
        val navController = rememberNavController()
        val setupRequired by remember {
            derivedStateOf {
                SpotifyApi.clientSecret.orEmpty().isEmpty() || SpotifyApi.clientId.orEmpty().isEmpty()
            }
        }

        Surface(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            NavHost(
                navController,
                startDestination = if (setupRequired) "setup" else "home"
            ) {
                composable("home") {
                    HomeScreen(
                        navigateToPlaylist = { type, playlistId ->
                            navController.navigate("playlist/$type/$playlistId")
                        },
                        navigateToYtMusicLogin = {
                            navController.navigate("login")
                        },
                        navigateToSpotifyLogin = {
                            navController.navigate("setup")
                        }
                    )
                }
                composable("setup") {
                    SetupSpotifyScreen {
                        navController.navigate("home")
                    }
                }
                composable("login") {
                    LoginScreen {
                        navController.popBackStack()
                    }
                }
                composable(
                    route = "playlist/{type}/{id}",
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType }
                    )
                ) {

                    val viewModel = viewModel<PlaylistViewmodel>(
                        factory = viewModelFactory {
                            initializer {
                                PlaylistViewmodel(savedStateHandle = createSavedStateHandle())
                            }
                        }
                    )

                    PlaylistViewScreen(
                        viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
expect fun BackHandler(enabled: Boolean, callback: @DisallowComposableCalls () -> Unit)

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

@Composable
fun SetupSpotifyScreen(
    onBack: () -> Unit,
) {

    var cid by remember(SpotifyApi.clientId) { mutableStateOf(SpotifyApi.clientId.orEmpty()) }
    var secret by remember { mutableStateOf(SpotifyApi.clientSecret.orEmpty()) }

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
                SpotifyApi.clientSecret = secret
                SpotifyApi.clientId = cid

                onBack()
            }
        ) {
            Text("Set values")
        }
    }
}