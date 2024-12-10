package io.silv.ui.theme

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Composable
fun rememberDominantColor(data: Any?): State<Color> {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val color = rememberSaveable(
        saver = Saver(
            save = { it.value.toArgb() },
            restore = {
                mutableStateOf(Color(it))
            }
        )
    ) { mutableStateOf(Color.Unspecified) }

    val scope = rememberCoroutineScope()

    DisposableEffect(data) {
        val job = scope.launch {
            runCatching {

                if (data == null) error("null data")

                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .allowHardware(false) // Disable hardware bitmaps.
                    .build()

                val result = (loader.execute(request) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                val palette = Palette.from(bitmap).generate()
                val dominant = palette.getDominantColor(color.value.toArgb())

                color.value = Color(dominant)
            }
                .onFailure {
                    color.value = primary
                }
        }
        onDispose { job.cancel() }
    }

    return color
}

@Composable
fun Color.isLight() = this.luminance() > 0.5