package io.silv.ui.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

val TopAppBarHeight = 64.dp
val TopBarMaxHeight
    @Composable get() = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.5f, 482.dp)
val SearchBarHeight = 38.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTopBarLayoutFullPoster(
    topBarState: TopBarState,
    modifier: Modifier = Modifier,
    snackBarHost:  @Composable () -> Unit = {},
    pinnedButton: @Composable () -> Unit = {},
    search: @Composable () -> Unit = { SearchField(Modifier.padding(horizontal = 18.dp), topBarState) },
    topAppBar: @Composable () -> Unit = { TopAppBar(title = { Text("Title") })},
    poster: @Composable () -> Unit = {
        Poster(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    },
    info: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BackHandler(topBarState.searching) {
        topBarState.searching = false
    }

    Scaffold(
        snackbarHost = snackBarHost,
        modifier = modifier
    ) { paddingValues ->
        Layout(
            {
                TopBarLayout(
                    topBarState,
                    Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color.Transparent
                                )
                            )
                        ),
                    pinnedButton,
                    search,
                    topAppBar,
                    info,
                    poster
                )
                content(
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val topBar = measurables[0]

            val tbp = topBar.measure(
                constraints.copy(
                    minHeight = 0,
                    maxHeight = topBarState.maxHeightPx.roundToInt()
                )
            )
            val content = measurables[1].measure(constraints.copy(
                minHeight = 0,
                maxHeight = (constraints.maxHeight - tbp.height).coerceAtLeast(0),
            ))

            layout(
                constraints.maxWidth,
                constraints.maxHeight,
            ) {
                tbp.place(0, 0,  1f)
                content.place(
                    0,
                    tbp.height,
                    0f
                )
            }
        }
    }
}

@Composable
private fun TopBarLayout(
    state: TopBarState,
    modifier: Modifier,
    pinnedButton: @Composable () -> Unit,
    search: @Composable () -> Unit,
    topAppBar: @Composable () -> Unit,
    info: @Composable () -> Unit,
    poster: @Composable () -> Unit,
) {
    val inset = WindowInsets.systemBars.getTop(LocalDensity.current)
    Layout(
        {
            Box(Modifier
                .layoutId("topBar")
            ) {
                topAppBar()
            }
            Box(Modifier
                .layoutId("info")
            ) {
                info()
            }
            Box(Modifier.layoutId("search")) {
                search()
            }
            Box(Modifier.layoutId("pinned")) {
                pinnedButton()
            }
            Box(
                Modifier
                    .layoutId("poster")
                    .wrapContentWidth()
                    .graphicsLayer {
                        alpha = lerp(
                            0f,
                            1f,
                            FastOutLinearInEasing.transform(
                                (state.fraction / 0.6f - 0.1f).coerceIn(0f..1f)
                            )
                        )
                    }
                    .clipToBounds()
            ) {
                poster()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .appBarDraggable(state)
            .height(
                with(LocalDensity.current) { state.spaceHeightPx.toDp() }
            )
    ) { measurables, constraints ->
        val searchM = measurables.first { it.layoutId == "search" }
        val pinned = measurables.first { it.layoutId == "pinned" }
        val posterM = measurables.first { it.layoutId == "poster" }
        val infoM = measurables.first { it.layoutId == "info" }
        val topBar = measurables.first { it.layoutId == "topBar" }

        val topBarPlaceable = topBar.measure(constraints)

        val searchPlaceable = searchM.measure(
            constraints.copy(
                maxHeight = SearchBarHeight.roundToPx(),
                minHeight = 0
            )
        )
        val pinnedPlaceable = pinned.measure(constraints.copy(
            minWidth = 0,
            minHeight = 0
        ))
        val pinnedPadding = 14.dp.roundToPx()
        val minH = infoM.minIntrinsicHeight(
            constraints.maxWidth - pinnedPlaceable.width - pinnedPadding
        )
        val infoPlaceable = infoM.measure(
            constraints.copy(
                minHeight = minH,
                maxHeight = minH,
                minWidth = 0,
                maxWidth = constraints.maxWidth - pinnedPlaceable.width - pinnedPadding
            )
        )

        val searchY =
            state.connection.appBarPinnedHeight + state.connection.appBarOffset - SearchBarHeight.toPx()

        val posterMaxHeight = (constraints.maxHeight - infoPlaceable.height)

        val posterMinHeight = minOf(
            state.connection.appBarPinnedHeight
        )
            .coerceAtLeast(0f)

        val posterPlaceable = posterM.measure(
            constraints.copy(
                minHeight = posterMinHeight
                    .roundToInt()
                    .coerceAtLeast(0),
                maxHeight = posterMaxHeight.coerceAtLeast(posterMinHeight.roundToInt())
            )
        )


        val posterY = constraints.maxHeight - posterPlaceable.height - infoPlaceable.height

        val infoY = constraints.maxHeight - infoPlaceable.height

        val posterOffset =  (infoY - (posterY + posterPlaceable.height)).coerceAtMost(0)

        layout(constraints.maxWidth, constraints.maxHeight) {

            searchPlaceable.placeRelative(
                constraints.maxWidth / 2 - searchPlaceable.width / 2,
                searchY.roundToInt(),
                zIndex = 10f
            )

            posterPlaceable.placeRelative(
                constraints.maxWidth / 2 - posterPlaceable.width / 2,
                posterY + posterOffset
            )

            infoPlaceable.placeRelative(
                0,
                infoY
            )

            topBarPlaceable.placeRelative(
                0,
                0,
                1f
            )

            if (!state.searching) {
                pinnedPlaceable.placeRelative(
                    constraints.maxWidth - pinnedPlaceable.width - pinnedPadding,
                    (constraints.maxHeight - infoPlaceable.height - pinnedPlaceable.height / 2)
                        .coerceAtLeast(
                            TopAppBarHeight.roundToPx() + inset - pinnedPlaceable.height / 2
                        ),
                    2f
                )
            }
        }
    }
}


