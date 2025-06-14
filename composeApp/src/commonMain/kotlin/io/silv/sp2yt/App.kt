package io.silv.sp2yt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import io.silv.sp2yt.ui.home.HomeScreen
import io.silv.sp2yt.ui.youtube_music.YoutubeLoginScreen
import io.silv.sp2yt.ui.playlist.PlaylistViewScreen
import io.silv.sp2yt.ui.home.homePresenter
import io.silv.sp2yt.ui.playlist.PlaylistViewmodel
import io.silv.sp2yt.ui.spotify.SpotifySetupScreen
import io.silv.sp2yt.ui.spotify.spotifySetupPresenter
import io.silv.sp2yt.ui.theme.AppTheme
import io.silv.sp2yt.ui.youtube_music.youtubeLoginPresenter
import kotlin.math.PI
import kotlin.math.tan

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
                val spotifyApi = appGraph.spotifyApi
                spotifyApi.clientSecret.isEmpty() || spotifyApi.clientId.isEmpty()
            }
        }

        NiaGradientBackground(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            NavHost(
                navController,
                startDestination = if (setupRequired) "setup" else "home"
            ) {
                composable("home") {

                    val state = homePresenter(navController)

                    HomeScreen(
                        state = state,
                        navigateToYtMusicLogin = {
                            navController.navigate("login")
                        },
                        navigateToSpotifyLogin = {
                            navController.navigate("setup")
                        }
                    )
                }
                composable("setup") {
                    val state = spotifySetupPresenter()

                    SpotifySetupScreen(
                        state = state
                    ) {
                        navController.navigate("home")
                    }
                }
                composable("login") {

                    val state = youtubeLoginPresenter(appGraph.ytMusicApi)

                    YoutubeLoginScreen(state) {
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


/**
 * A gradient background for select screens. Uses [LocalBackgroundTheme] to set the gradient colors
 * of a [Box] within a [Surface].
 *
 * @param modifier Modifier to be applied to the background.
 * @param gradientColors The gradient colors to be rendered.
 * @param content The background content.
 */
@Composable
fun NiaGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentTopColor by rememberUpdatedState(MaterialTheme.colorScheme.inverseOnSurface)
    val currentBottomColor by rememberUpdatedState(MaterialTheme.colorScheme.primaryContainer)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    // Compute the start and end coordinates such that the gradients are angled 11.06
                    // degrees off the vertical axis
                    val offset: Float = size.height * tan(
                        11.06f  * PI / 180f,
                    ).toFloat()

                    val start = Offset(size.width / 2 + offset / 2, 0f)
                    val end = Offset(size.width / 2 - offset / 2, size.height)

                    // Create the top gradient that fades out after the halfway point vertically
                    val topGradient = Brush.linearGradient(
                        0f to if (currentTopColor == Color.Unspecified) {
                            Color.Transparent
                        } else {
                            currentTopColor
                        },
                        0.724f to Color.Transparent,
                        start = start,
                        end = end,
                    )
                    // Create the bottom gradient that fades in before the halfway point vertically
                    val bottomGradient = Brush.linearGradient(
                        0.2552f to Color.Transparent,
                        1f to if (currentBottomColor == Color.Unspecified) {
                            Color.Transparent
                        } else {
                            currentBottomColor
                        },
                        start = start,
                        end = end,
                    )

                    onDrawBehind {
                        // There is overlap here, so order is important
                        drawRect(topGradient)
                        drawRect(bottomGradient)
                    }
                },
        ) {
            content()
        }
    }
}
