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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
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
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

class MainActivity : ComponentActivity() {

    private var token by App.store.stored<String>("oauthToken")
    private var refresh by App.store.stored<String>("refreshToken")
    private var expiration by App.store.stored<Long>("oauthTokenExpiration")

    private lateinit var authorizationService: AuthorizationService

    private val authRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        callback = { result ->
            val data = result.data ?: return@registerForActivityResult

            val response = AuthorizationResponse.fromIntent(data)
            val ex = AuthorizationException.fromIntent(data)

            if (response != null && ex == null) {
                authorizationService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, _ ->
                    if (tokenResponse != null) {
                        // Access and refresh tokens are available here
                        val accessToken = tokenResponse.accessToken
                        val refreshToken = tokenResponse.refreshToken

                        expiration = tokenResponse.accessTokenExpirationTime ?: 0L
                        // Use the access token to make authenticated requests to the YouTube Data API
                        Log.d("TOKEN", "$accessToken, $refreshToken")
                        token = accessToken.orEmpty()
                        refresh = refreshToken.orEmpty()
                    } else {
                        // Handle token exchange error
                        Log.d("TOKEN", "error ex")
                    }
                }
            } else {
                // Handle authorization error
                Log.d("AuthRequestError", "${ex?.error}")
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (expiration < epochSeconds()) {
            authorizationService = AuthorizationService(this)

            val authRequest = authenticationIntent(authorizationService)
            authRequestLauncher.launch(authRequest)
        }

        setContent {

            val navHostController = rememberNavController()

            SptoytTheme {
                NavHost(
                    navHostController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        var text by remember { mutableStateOf("") }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(token)
                            Text(refresh)
                            Text(expiration.toString())
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

@Composable
fun ContentListItem(
    title: String,
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    EntryListItem(
        title = title,
        coverData = url,
        coverAlpha = 1f,
        onLongClick = onLongClick,
        onClick = onClick,
        badge = {},
        endButton = content,
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SptoytTheme {
        Greeting("Android")
    }
}