package io.silv.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
            model = ImageRequest.Builder(LocalContext.current)
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
) = this then drawBehind { if (isSelected) drawRect(color = color) }

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
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Resume",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}