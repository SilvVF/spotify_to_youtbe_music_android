package io.silv.sp2yt

import com.russhwolf.settings.ObservableSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.silv.sp2yt.api.SpotifyApi
import io.silv.sp2yt.api.YoutubeMusicApi
import kotlinx.serialization.json.Json

lateinit var appGraph: AppGraph

abstract class AppGraph {

    abstract val settings: ObservableSettings

    val json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(Logging)
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    val spotifyApi by lazy {
        SpotifyApi(client, settings)
    }

    val ytMusicApi by lazy {
        YoutubeMusicApi(client, settings)
    }

    companion object {
        const val SETTINGS_NAME = "sp2yt_settings"
    }
}

object Keys {
    const val INNER_TUBE_COOKIE = "innerTubeCookie"
    const val SPOTIFY_CLIENT_ID = "spotifyClientId"
    const val SPOTIFY_CLIENT_SECRET = "spotifyClientSecret"
    const val YT_VISITOR = "visitorData"
}