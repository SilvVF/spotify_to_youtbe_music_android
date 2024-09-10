package io.silv.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

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

fun Modifier.conditional(condition : Boolean, modifier : Modifier.() -> Modifier) : Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

fun Modifier.colorClickable(
    color: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = this.composed {
    val rippleColor = color ?: MaterialTheme.colorScheme.primary
    val interactionSource =  remember { MutableInteractionSource() }
    val indication = rememberRipple(color = rippleColor)

    clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = indication,
        onClick = onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.clickableNoIndication(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this.composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}