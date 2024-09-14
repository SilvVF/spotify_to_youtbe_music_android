@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.ui.layout


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp


@Composable
fun PinnedTopBar(
    onBackPressed: () -> Unit,
    topBarState: TopBarState,
    query: () -> String,
    onQueryChanged: (String) -> Unit,
    name: String,
) {
    val fractionLerp by rememberUpdatedState(
        lerp(
            0f,
            1f,
            FastOutLinearInEasing.transform((topBarState.fraction / 0.2f).coerceIn(0f..1f))
        )
    )
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    if (topBarState.searching) {
                        topBarState.searching = false
                    } else {
                        onBackPressed()
                    }
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        },
        actions = {},
        title = {
            val focusRequester = remember { FocusRequester() }
            if (topBarState.searching) {

                LaunchedEffect(focusRequester) {
                    focusRequester.requestFocus()
                }
                val query = query()
                TextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    placeholder = { Text("Search...") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedVisibility(visible = query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                            IconButton(onClick = { onQueryChanged(query) }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .focusable()
                        .focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onQueryChanged(query)
                            focusRequester.freeFocus()
                        }
                    )
                )
                return@TopAppBar
            }
           Text(name,  modifier = Modifier.graphicsLayer { alpha = 1f - fractionLerp })
        },
        colors = TopAppBarDefaults.topAppBarColors(
            MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 1f - fractionLerp
            )
        )
    )
}