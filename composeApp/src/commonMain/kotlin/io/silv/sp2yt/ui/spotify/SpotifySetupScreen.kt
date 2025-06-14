package io.silv.sp2yt.ui.spotify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifySetupScreen(
    state: SpotifySetupState,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Spotify Setup")
                },
                navigationIcon = {
                    TextButton(
                        onClick = { onBack() }
                    ) {
                        Text("<")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    state.events(SpotifySetupEvent.ConfirmChanges)
                    onBack()
                },
                icon = {},
                text = {
                    Text("Set values")
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = state.clientId,
                onValueChange = { state.events(SpotifySetupEvent.ChangeClientId(it)) },
                label = { Text("Client id") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.secret,
                onValueChange = { state.events(SpotifySetupEvent.ChangeSecret(it)) },
                label = { Text("Secret") }
            )
        }
    }
}