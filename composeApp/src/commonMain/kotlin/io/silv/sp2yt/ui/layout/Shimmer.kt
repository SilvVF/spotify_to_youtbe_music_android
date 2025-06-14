package io.silv.sp2yt.ui.layout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.shimmer
import kotlin.random.Random

@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .shimmer()
            .graphicsLayer(alpha = 0.99f)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent)),
                    blendMode = BlendMode.DstIn
                )
            },
        content = content
    )
}

val ShimmerTheme = defaultShimmerTheme.copy(
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 800,
            easing = LinearEasing,
            delayMillis = 250,
        ),
        repeatMode = RepeatMode.Restart
    ),
    shaderColors = listOf(
        Color.Unspecified.copy(alpha = 0.25f),
        Color.Unspecified.copy(alpha = 0.50f),
        Color.Unspecified.copy(alpha = 0.25f),
    ),
)

@Composable
fun TextPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Spacer(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.onSurface)
            .fillMaxWidth(remember { 0.25f + Random.nextFloat() * 0.5f })
            .height(height)
    )
}

@Composable
fun ListItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(6.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(64.dp)
            .padding(horizontal = 6.dp),
    ) {
        Spacer(
            modifier = Modifier
                .padding(6.dp)
                .size(48.dp)
                .clip(thumbnailShape)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            TextPlaceholder()
        }
        ButtonPlaceholder(
            Modifier
                .clip(CircleShape)
                .size(32.dp)
                .padding(6.dp)
        )
    }
}

@Composable
fun GridItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(6.dp),
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier = if (fillMaxWidth) {
            modifier
                .padding(12.dp)
                .fillMaxWidth()
        } else {
            modifier
                .padding(12.dp)
                .width(128.dp)
        }
    ) {
        Spacer(
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(128.dp)
            }
                .aspectRatio(1f)
                .clip(thumbnailShape)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        Spacer(modifier = Modifier.height(6.dp))

        TextPlaceholder()

        TextPlaceholder()
    }
}

@Composable
fun ButtonPlaceholder(
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier
            .height(ButtonDefaults.MinHeight)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface))
}