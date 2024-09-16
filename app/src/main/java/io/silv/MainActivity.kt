package io.silv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.silv.ui.LoginScreen
import io.silv.ui.PlaylistViewScreen
import io.silv.ui.layout.EntryListItem
import io.silv.ui.theme.SptoytTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navHostController = rememberNavController()
            val clientSecret = remember { SpotifyApi.CLIENT_SECRET }
            val clientId = remember { SpotifyApi.CLIENT_ID }

            SptoytTheme {
                Surface {
                    NavHost(
                        navHostController,
                        startDestination = if (clientSecret.isEmpty() || clientId.isEmpty()) "setup" else "home"
                    ) {
                        composable("home") {

                            var text by remember { mutableStateOf("") }

                            Column(
                                modifier = Modifier.fillMaxSize().imePadding(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextField(
                                    value = text,
                                    onValueChange = { text = it }
                                )
                                Button(
                                    onClick = {
                                        val playlistId = SpotifyApi.extractPlaylistIdFromUrl(text)
                                        Log.d("PlaylistId", playlistId.orEmpty())
                                        if (playlistId != null) {
                                            navHostController.navigate("playlist/$playlistId")
                                        }
                                    }
                                ) {
                                    Text("Convert to Youtube Music")
                                }
                                Button(
                                    onClick = {
                                        navHostController.navigate("login")
                                    }
                                ) {
                                    Text("Login to Youtube Music")
                                }
                                Button(
                                    onClick = {
                                        navHostController.navigate("setup")
                                    }
                                ) {
                                    Text("Login to spotify")
                                }
                            }
                        }
                        composable("setup") {
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
                                        navHostController.navigate("home")
                                    }
                                ) {
                                    Text("Set values")
                                }
                            }
                        }
                        composable("login") {
                            LoginScreen {
                                navHostController.popBackStack()
                            }
                        }
                        composable(
                            route = "playlist/{id}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType }
                            )
                        ) {
                            PlaylistViewScreen(
                                onBack = { navHostController.popBackStack() }
                            )
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
