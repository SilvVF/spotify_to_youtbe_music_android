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
import io.silv.sp2yt.ui.HomeScreen
import io.silv.sp2yt.ui.LoginScreen
import io.silv.sp2yt.ui.PlaylistViewScreen
import io.silv.sp2yt.ui.SetupSpotifyScreen
import io.silv.sp2yt.ui.theme.AppTheme

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(httpClient = { appGraph.client })
                )
            }
            .crossfade(true)
            .build()
    }

    AppTheme {
        val navController = rememberNavController()
        val setupRequired by remember {
            derivedStateOf {
                appGraph.spotifyApi.clientSecret.isEmpty() || appGraph.spotifyApi.clientId.isEmpty()
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
