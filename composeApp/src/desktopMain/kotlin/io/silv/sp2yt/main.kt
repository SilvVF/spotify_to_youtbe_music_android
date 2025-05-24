package io.silv.sp2yt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings

fun main() = run {
    appGraph = object : AppGraph() {
        override val settings: ObservableSettings =
            PreferencesSettings.Factory().create(SETTINGS_NAME)

    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sp2yt",
        ) {
            App()
        }
    }
}