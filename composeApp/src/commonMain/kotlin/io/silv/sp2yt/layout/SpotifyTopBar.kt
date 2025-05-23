package io.silv.sp2yt.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.silv.sp2yt.BackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

val TopAppBarHeight = 64.dp
val TopBarMaxHeight = 482.dp

val SearchBarHeight = 38.dp

enum class ItemCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    Rect(16f / 9f);

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            placeholder = remember { ColorPainter(CoverPlaceholderColor) },
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentScale = ContentScale.Crop,
        )
    }
}

val CoverPlaceholderColor = Color(0x1F888888)

data class PosterData(
    val id: Long,
    val url: String?,
    val title: String,
    val favorite: Boolean,
    val isMovie: Boolean,
    val lastModified: Long,
    val inList: Boolean
)

object CommonEntryItemDefaults {
    val GridHorizontalSpacer = 4.dp
    val GridVerticalSpacer = 4.dp

    const val BrowseFavoriteCoverAlpha = 0.34f
}

private val ContinueViewingButtonSize = 28.dp
private val ContinueViewingButtonGridPadding = 6.dp
private val ContinueViewingButtonListSpacing = 8.dp

private const val GridSelectedCoverAlpha = 0.76f

/**
 * Layout of grid list item with title overlaying the cover.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun EntryCompactGridItem(
    coverData: PosterData,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    title: String? = null,
    coverAlpha: Float = 1f,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        EntryGridCover(
            cover = {
                ItemCover.Book(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isSelected) GridSelectedCoverAlpha else coverAlpha),
                    data = coverData,
                )
            },
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            content = {
                if (title != null) {
                    CoverTextOverlay(
                        title = title,
                        content = content,
                    )
                } else if (content != null) {
                    Box(
                        modifier =  Modifier
                            .padding(ContinueViewingButtonGridPadding)
                            .align(Alignment.BottomEnd)
                    ) {
                        content()
                    }
                }
            },
        )
    }
}

/**
 * Title overlay for [EntryCompactGridItem]
 */
@Composable
private fun BoxScope.CoverTextOverlay(
    title: String,
    content: (@Composable () -> Unit)?
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .fillMaxWidth()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.9f),
                                    ),
                            ),
                    )
                }
            }
            .fillMaxHeight(0.33f)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    )
    Row(
        modifier = Modifier.align(Alignment.BottomStart),
        verticalAlignment = Alignment.Bottom,
    ) {
        GridItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            title = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 4f,
                ),
            ),
            minLines = 1,
        )
        if (content != null) {
            Box(
                modifier = Modifier.padding(
                    end = ContinueViewingButtonGridPadding,
                    bottom = ContinueViewingButtonGridPadding,
                )
            ) {
                content()
            }
        }
    }
}

/**
 * Layout of grid list item with title below the cover.
 */
@Composable
fun EntryComfortableGridItem(
    isSelected: Boolean = false,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    titleMaxLines: Int = 2,
    coverData: PosterData,
    coverAlpha: Float = 1f,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column {
            EntryGridCover(
                cover = {
                    ItemCover.Book(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isSelected) GridSelectedCoverAlpha else coverAlpha),
                        data = coverData,
                    )
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
                content = {
                    if (content != null) {
                        Box(
                            modifier =  Modifier
                                .padding(ContinueViewingButtonGridPadding)
                                .align(Alignment.BottomEnd)
                        ) {
                            content()
                        }
                    }
                },
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = titleMaxLines,
            )
        }
    }
}

/**
 * Common cover layout to add contents to be drawn on top of the cover.
 */
@Composable
private fun EntryGridCover(
    modifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit = {},
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ItemCover.Book.ratio),
    ) {
        cover()
        content?.invoke(this)
        if (badgesStart != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

@Composable
fun BadgeGroup(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier.clip(shape)) {
        content()
    }
}

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSecondary,
    shape: Shape = RectangleShape,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        color = textColor,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun Badge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    iconColor: Color = MaterialTheme.colorScheme.onSecondary,
    shape: Shape = RectangleShape,
) {
    val iconContentPlaceholder = "[icon]"
    val text = buildAnnotatedString {
        appendInlineContent(iconContentPlaceholder)
    }
    val inlineContent = mapOf(
        Pair(
            iconContentPlaceholder,
            InlineTextContent(
                Placeholder(
                    width = MaterialTheme.typography.bodySmall.fontSize,
                    height = MaterialTheme.typography.bodySmall.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Icon(
                    imageVector = imageVector,
                    tint = iconColor,
                    contentDescription = null,
                )
            },
        ),
    )

    Text(
        text = text,
        inlineContent = inlineContent,
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        color = iconColor,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun GridItemTitle(
    title: String,
    style: TextStyle,
    minLines: Int,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    Text(
        modifier = modifier,
        text = title,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        minLines = minLines,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}

/**
 * Wrapper for grid items to handle selection state, click and long click.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary)
            .padding(4.dp),
    ) {
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * @see GridItemSelectable
 */
private fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = this.then(Modifier.drawBehind { if (isSelected) drawRect(color = color) })

/**
 * Layout of list item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryListItem(
    isSelected: Boolean = false,
    title: String,
    coverData: String,
    coverAlpha: Float = 1f,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    badge: @Composable (RowScope.() -> Unit),
    endButton: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .selectedBackground(isSelected)
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Square(
            modifier = Modifier
                .fillMaxHeight()
                .alpha(coverAlpha),
            data = coverData,
        )
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        BadgeGroup(content = badge)
        endButton?.let { button ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .padding(ContinueViewingButtonListSpacing)
                    .size(ContinueViewingButtonSize),
                contentAlignment = Alignment.Center
            ) {
                button()
            }
        }
    }
}

@Composable
private fun ContinueViewingButton(
    modifier: Modifier = Modifier,
    onClickContinueViewing: () -> Unit,
) {
    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClickContinueViewing,
            modifier = Modifier.size(ContinueViewingButtonSize),
            shape = MaterialTheme.shapes.small,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            ),
        ) {
//            Icon(
//                imageVector = Icons.Filled.PlayArrow,
//                contentDescription = "Resume",
//                modifier = Modifier.size(16.dp),
//            )
        }
    }
}


fun Modifier.selectedBackground(isSelected: Boolean): Modifier = if (isSelected) {
    composed {
        val alpha = if (isSystemInDarkTheme()) 0.16f else 0.22f
        val color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
        Modifier.drawBehind {
            drawRect(color)
        }
    }
} else {
    this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedTopBar(
    onBackPressed: () -> Unit,
    topBarState: TopBarState,
    query: () -> String,
    onQueryChanged: (String) -> Unit,
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
            TextButton(
                onClick = {
                    if (topBarState.searching) {
                        topBarState.searching = false
                    } else {
                        onBackPressed()
                    }
                }
            ) {
                Text("<")
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
                                TextButton(onClick = { onQueryChanged("") }) {
                                    Text(
                                        "X"
                                    )
                                }
                            }
                            TextButton(onClick = { onQueryChanged(query) }) {
                               Text("()--")
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

@Composable
fun rememberTopBarState(
    scrollableState: ScrollableState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): TopBarState {

    val density = LocalDensity.current
    val inset = WindowInsets.systemBars.getTop(density)

    val appBarMaxHeightPx = with(density)  { TopBarMaxHeight.toPx() }
    val topAppBarHeightPx = with(density) { TopAppBarHeight.toPx() + inset }
    val appBarPinnedHeightPx = with(density) { topAppBarHeightPx + SearchBarHeight.toPx() }
    val snapAnimationSpec = tween<Float>()
    val flingAnimationSpec = rememberSplineBasedDecay<Float>()

    return rememberSaveable(
        scrollableState,
        appBarPinnedHeightPx,
        saver = Saver(
            save = { arrayOf(it.fraction, it.searching) },
            restore = { (fraction, searching ) ->
                TopBarState(
                    scrollableState,
                    if ((searching as? Boolean) == true) {
                        -appBarMaxHeightPx + topAppBarHeightPx
                    } else {
                        if (!scrollableState.canScrollBackward) {
                            (-appBarMaxHeightPx + topAppBarHeightPx) * (1f - ((fraction as? Float) ?: 0f))
                        } else  {
                            (-appBarMaxHeightPx + topAppBarHeightPx)
                        }
                    },
                    searching as? Boolean == true,
                    appBarMaxHeightPx,
                    appBarPinnedHeightPx,
                    topAppBarHeightPx,
                    snapAnimationSpec,
                    flingAnimationSpec,
                    coroutineScope
                )
            }
        ),
    ) {
        TopBarState(
            scrollableState,
            -appBarPinnedHeightPx,
            false,
            appBarMaxHeightPx,
            appBarPinnedHeightPx,
            topAppBarHeightPx,
            snapAnimationSpec,
            flingAnimationSpec,
            coroutineScope
        )
    }
}

class TopBarState(
    val scrollableState: ScrollableState,
    initialHeight: Float,
    initialSearching: Boolean,
    val maxHeightPx: Float,
    val pinnedHeightPx: Float,
    topAppBarHeightPx: Float,
    snapAnimationSpec: AnimationSpec<Float>,
    flingAnimationSpec: DecayAnimationSpec<Float>,
    scope: CoroutineScope,
) {
    var searching by mutableStateOf(initialSearching)

    val connection = CollapsingAppBarNestedScrollConnection(
        initialHeight,
        maxHeightPx - topAppBarHeightPx,
        pinnedHeightPx,
        {
            if(searching) {
                return@CollapsingAppBarNestedScrollConnection false
            }

            !scrollableState.canScrollBackward
        },
        snapAnimationSpec,
        flingAnimationSpec
    )

    init {
        scope.launch {
            snapshotFlow { searching }
                .drop(1)
                .collectLatest {
                    animate(
                        initialValue = connection.appBarOffset,
                        targetValue = if (searching) -maxHeightPx + topAppBarHeightPx else -pinnedHeightPx,
                        block = { value, _ ->
                            connection.appBarOffset = value
                        }
                    )
                }
        }
    }

    val fraction by derivedStateOf {
        (maxHeightPx - topAppBarHeightPx + connection.appBarOffset) / (maxHeightPx - topAppBarHeightPx)
    }

    val spaceHeightPx by derivedStateOf {
        maxHeightPx + connection.appBarOffset
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTopBarLayout(
    topBarState: TopBarState,
    modifier: Modifier = Modifier,
    gradiant: Brush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            Color.Transparent
        )
    ),
    snackBarHost:  @Composable () -> Unit = {},
    pinnedButton: @Composable () -> Unit = {},
    search: @Composable () -> Unit = { SearchField(Modifier.padding(horizontal = 18.dp), topBarState) },
    topAppBar: @Composable () -> Unit = { TopAppBar(title = { Text("Title") })},
    poster: @Composable () -> Unit = {
        Poster(
            modifier = Modifier
                .fillMaxHeight()
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
                            brush = gradiant
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
            modifier = Modifier.fillMaxSize()
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
                constraints.maxHeight
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
    val applyAlpha by remember(state.spaceHeightPx, state.pinnedHeightPx) {
        derivedStateOf {
            (state.maxHeightPx - state.pinnedHeightPx) > state.spaceHeightPx
        }
    }
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
                            .takeIf { applyAlpha }
                            ?: 1f
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
        val topPaddingPx = 12.dp.roundToPx()

        val searchY =
            state.connection.appBarPinnedHeight + state.connection.appBarOffset - SearchBarHeight.toPx()

        val posterMaxHeight =
            minOf(
                (state.spaceHeightPx - infoPlaceable.height - topPaddingPx - inset),
                (state.maxHeightPx - infoPlaceable.height - state.connection.appBarPinnedHeight - topPaddingPx - inset),
            )

        val posterMinHeight = minOf(
            state.connection.appBarPinnedHeight
        )
            .coerceAtLeast(0f)

        val posterPlaceable = posterM.measure(
            constraints.copy(
                minHeight = posterMinHeight
                    .roundToInt()
                    .coerceAtLeast(0),
                maxHeight = maxOf(posterMinHeight, posterMaxHeight)
                    .coerceAtLeast(posterMinHeight)
                    .roundToInt()
            )
        )


        val posterY = (constraints.maxHeight - posterPlaceable.height - infoPlaceable.height)
            .coerceAtLeast(topPaddingPx + inset)

        val infoY = constraints.maxHeight - infoPlaceable.height

        val posterOffset =  (infoY - (posterY + posterPlaceable.height))
            .coerceAtMost(0) * 0.6f

        layout(constraints.maxWidth, constraints.maxHeight) {

            searchPlaceable.placeRelative(
                constraints.maxWidth / 2 - searchPlaceable.width / 2,
                searchY.roundToInt()
            )

            posterPlaceable.placeRelative(
                constraints.maxWidth / 2 - posterPlaceable.width / 2,
                posterY + posterOffset.roundToInt()
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
                    (infoY + infoPlaceable.height / 2)
                        .coerceAtLeast(
                            TopAppBarHeight.roundToPx() + inset
                        ) - pinnedPlaceable.height / 2,
                    2f
                )
            }
        }
    }
}


fun Modifier.appBarDraggable(
    topBarState: TopBarState,
): Modifier = this.composed {
    this.draggable(
        enabled = !topBarState.searching,
        state = rememberDraggableState {
            if (topBarState.connection.canConsume() || topBarState.connection.appBarOffset > -topBarState.connection.appBarMaxHeight) {
                val newOffset = topBarState.connection.appBarOffset + it
                topBarState.connection.appBarOffset = newOffset.coerceIn(-topBarState.connection.appBarMaxHeight, 0f)
            } else {
                topBarState.scrollableState.dispatchRawDelta(-it)
            }
        },
        onDragStopped = { v ->
            if (topBarState.connection.canConsume() || topBarState.connection.appBarOffset > -topBarState.connection.appBarMaxHeight) {
                with(topBarState.connection) {
                    settleBar(
                        { appBarOffset },
                        appBarPinnedHeight,
                        appBarMaxHeight,
                        Velocity(0f, v),
                        flingAnimationSpec,
                        snapAnimationSpec,
                    ) { value ->
                        appBarOffset = value
                    }
                }
            } else {
                topBarState.scrollableState.scrollBy(
                    topBarState.connection.flingAnimationSpec.calculateTargetValue(0f, -v)
                )
            }
        },
        orientation = Orientation.Vertical
    )
}

@Composable
fun SearchField(modifier: Modifier = Modifier, topBarState: TopBarState, background: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)) {
    Row(modifier) {
        Box(
            Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .background(background)
                .clickable {
                    topBarState.searching = !topBarState.searching
                }
                .padding(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "()---",
                    modifier = Modifier.size(18.dp)
                )
                Text("Find in list", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun Poster(
    modifier: Modifier,
    url: String? = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRacaxFiAzxNzKWQNWZVgfTfLXKti0MZE5HXn6_GdG0JA&s"
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        placeholder = remember { ColorPainter(Color.Black) },
        contentScale = ContentScale.FillHeight,
        modifier = modifier
    )
}

class CollapsingAppBarNestedScrollConnection internal constructor(
    initialHeight: Float,
    val appBarMaxHeight: Float,
    val appBarPinnedHeight: Float,
    val canConsume: () -> Boolean,
    val snapAnimationSpec: AnimationSpec<Float>,
    val flingAnimationSpec: DecayAnimationSpec<Float>
) : NestedScrollConnection {

    var appBarOffset: Float by mutableFloatStateOf(initialHeight)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        return if (
            canConsume() || appBarOffset > -appBarMaxHeight
        ) {
            val newOffset = appBarOffset + delta
            val previousOffset = appBarOffset
            appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0f)
            val consumed = appBarOffset - previousOffset
            Offset(
                x = available.x,
                y = consumed
            )
        } else {
            appBarOffset = -appBarMaxHeight
            super.onPreScroll(available, source)
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return if (
            canConsume() || appBarOffset > -appBarMaxHeight
        ) {
            val newOffset = appBarOffset + delta
            val previousOffset = appBarOffset
            appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0f)
            val consumed = appBarOffset - previousOffset
            Offset(
                x = available.x,
                y = consumed
            )
        } else {
            appBarOffset = -appBarMaxHeight
            super.onPostScroll(consumed, available, source)
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val superConsumed = super.onPostFling(consumed, available)
        return superConsumed + if (
            canConsume() || appBarOffset > -appBarMaxHeight
        ) {
            settleBar(
                { appBarOffset },
                appBarPinnedHeight,
                appBarMaxHeight,
                superConsumed,
                flingAnimationSpec,
                snapAnimationSpec
            ) {
                appBarOffset = it
            }
        } else {
            appBarOffset = -appBarMaxHeight
            Velocity.Zero
        }
    }
}


suspend fun settleBar(
    appBarOffset: () -> Float,
    appBarPinnedHeight: Float,
    appBarMaxHeight: Float,
    superConsumed: Velocity,
    flingAnimationSpec: DecayAnimationSpec<Float>,
    snapAnimationSpec: AnimationSpec<Float>,
    setHeightOffset: (Float) -> Unit,
): Velocity {
    val initial = appBarOffset()
    if (initial > -appBarMaxHeight && initial < 0) {
        var remainingVelocity = superConsumed.y
        // In case there is an initial velocity that was left after a previous user fling, animate to
        // continue the motion to expand or collapse the app bar.
        if (abs(superConsumed.y) > 1f) {
            animate(
                initial,
                flingAnimationSpec.calculateTargetValue(initial, superConsumed.y)
                    .coerceIn(-appBarMaxHeight..0f),
                block = { value, velocity ->
                    remainingVelocity = velocity
                    setHeightOffset(value)
                }
            )
        }
        if (abs(appBarOffset()) <= appBarPinnedHeight) {
            AnimationState(
                initialValue = appBarOffset(),
                initialVelocity = remainingVelocity
            )
                .animateTo(
                    if (abs(appBarOffset()) <= appBarPinnedHeight / 2) {
                        0f
                    } else {
                        -appBarPinnedHeight
                    },
                    sequentialAnimation = true,
                    animationSpec = snapAnimationSpec
                ) {
                    setHeightOffset(value)
                }
        }
        Velocity(0f, remainingVelocity)
    }
    return superConsumed
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTopBarLayoutFullPoster(
    topBarState: TopBarState,
    modifier: Modifier = Modifier,
    gradiant: Brush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            Color.Transparent
        )
    ),
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
    title: @Composable () -> Unit = {},
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
                    Modifier.background(brush = gradiant),
                    pinnedButton,
                    search,
                    topAppBar,
                    info,
                    title,
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
    title: @Composable () -> Unit,
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
                .layoutId("title")
            ) {
            }
            Column(Modifier.layoutId("info")) {
                title()
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
        val title = measurables.first { it.layoutId == "title" }

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

        val minHInfo = infoM.minIntrinsicHeight(
            constraints.maxWidth - pinnedPlaceable.width - pinnedPadding
        )
        val minHTitle = infoM.minIntrinsicHeight(
            constraints.maxWidth - pinnedPlaceable.width - pinnedPadding
        )

        val titlePlaceable = title.measure(
            constraints.copy(
                minHeight = minHTitle,
                maxHeight = minHTitle,
                minWidth = 0,
                maxWidth = constraints.maxWidth - pinnedPlaceable.width - pinnedPadding
            )
        )

        val infoPlaceable = infoM.measure(
            constraints.copy(
                minHeight = minHInfo,
                maxHeight = minHInfo,
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
                5f
            )

            titlePlaceable.placeRelative(
                0,
                constraints.maxHeight - infoPlaceable.height - titlePlaceable.height,
                3f
            )

            if (!state.searching) {
                pinnedPlaceable.placeRelative(
                    constraints.maxWidth - pinnedPlaceable.width - pinnedPadding,
                    (constraints.maxHeight - infoPlaceable.height - pinnedPlaceable.height / 2)
                        .coerceAtLeast(
                            TopAppBarHeight.roundToPx() + inset - pinnedPlaceable.height / 2
                        ),
                    10f
                )
            }
        }
    }
}