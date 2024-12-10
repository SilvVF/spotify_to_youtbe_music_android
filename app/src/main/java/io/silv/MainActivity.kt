package io.silv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.silv.ui.HomeScreen
import io.silv.ui.LoginScreen
import io.silv.ui.PlaylistViewScreen
import io.silv.ui.SetupSpotifyScreen
import io.silv.ui.layout.EntryListItem
import io.silv.ui.theme.SptoytTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            val clientSecret = remember { SpotifyApi.CLIENT_SECRET }
            val clientId = remember { SpotifyApi.CLIENT_ID }

            SptoytTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                ) {
                    CompositionLocalProvider(LocalSharedPreferences provides App.store) {
                        NavHost(
                            navController,
                            startDestination = if (clientSecret.isEmpty() || clientId.isEmpty()) "setup" else "home"
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
                                PlaylistViewScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ContentListItem(
    title: String,
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badge: (@Composable RowScope.() -> Unit) = {},
    content: (@Composable () -> Unit)? = null,
) {
    EntryListItem(
        title = title,
        coverData = url,
        coverAlpha = 1f,
        onLongClick = onLongClick,
        onClick = onClick,
        badge = badge,
        endButton = content,
    )
}
