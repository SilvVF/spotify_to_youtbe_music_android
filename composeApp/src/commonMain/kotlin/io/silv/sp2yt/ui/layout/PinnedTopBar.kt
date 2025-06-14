package io.silv.sp2yt.ui.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.Search
import compose.icons.fontawesomeicons.solid.Undo
import kotlin.math.ln

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedTopBar(
    onBackPressed: () -> Unit,
    topBarState: TopBarState,
    query: () -> TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    name: String,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
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
                Icon(
                    imageVector = FontAwesomeIcons.Solid.ArrowLeft,
                    contentDescription = null
                )
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
                            AnimatedVisibility(visible = query.text.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged(TextFieldValue()) }) {
                                    Icon(
                                        imageVector = FontAwesomeIcons.Solid.Undo,
                                        contentDescription = null
                                    )
                                }
                            }
                            IconButton(onClick = { onQueryChanged(query) }) {
                                Icon(
                                    imageVector = FontAwesomeIcons.Solid.Search,
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
        colors = colors.copy(
            colors.containerColor.atElevation(3.dp).copy(alpha = 1f - fractionLerp)
        )
    )
}

@Stable
@Composable
fun Color.atElevation(
    elevation: Dp,
    surfaceTint: Color = MaterialTheme.colorScheme.surfaceTint
): Color {
    if (elevation == 0.dp) return this
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(this)
}