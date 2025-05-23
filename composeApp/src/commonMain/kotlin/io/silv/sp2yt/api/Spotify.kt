package io.silv.sp2yt.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.path
import io.ktor.http.set
import io.silv.sp2yt.Keys
import io.silv.sp2yt.appScope
import io.silv.sp2yt.epochSeconds
import io.silv.sp2yt.getValue
import io.silv.sp2yt.setValue
import io.silv.sp2yt.settingsMutableState
import io.silv.sp2yt.stored
import io.silv.sp2yt.types.SpotifyAlbum
import io.silv.sp2yt.types.SpotifyPlaylist
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.EmptyCoroutineContext.get
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

object SpotifyApi {

    private val store = appScope.settings
    val mutex = Mutex()

    val client = appScope.client

    var clientId by store.settingsMutableState<String>(Keys.SPOTIFY_CLIENT_ID, "")
    var clientSecret by store.settingsMutableState<String>(Keys.SPOTIFY_CLIENT_SECRET, "")

    private var expiresAt by store.stored<Long>("expires_at", -1)
    private var token by store.stored<String>("token", "")

    suspend fun HttpRequestBuilder.authConfig() {
        mutex.withLock {
            val expired = expiresAt < (epochSeconds() - 5.minutes.inWholeSeconds)
            if (token.isEmpty() || expired) {
                refreshAuthToken()
            }
        }
        check(token.isNotEmpty()) { "Auth token was not set" }
        headers {
            append("Authorization", "Bearer $token")
        }
    }

    @Serializable
    data class AuthToken(
        @SerialName("access_token")
        val accessToken: String,
        @SerialName("expires_in")
        val expiresIn: Long
    ) {
        val expiresAt get() = epochSeconds() + expiresIn
    }

    suspend fun refreshAuthToken() {
        val res = client.post("https://accounts.spotify.com/api/token") {
            headers {
                append("Content-Type", "application/x-www-form-urlencoded")
                basicAuth(clientId, clientSecret)
            }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
            }))
        }
            .body<AuthToken>()

        expiresAt = res.expiresAt
        token = res.accessToken
    }

    fun extractPlaylistIdFromUrl(url: String): Pair<String?, String?> {
        val regex = Regex("(?<type>playlist|album)/(?<id>\\w{22})\\W?")
        val match = regex.find(url)

        return if (match != null) {
            match.groups["type"]?.value to match.groups["id"]?.value
        } else {
            val regexAlt = Regex("(?<type>playlist|album)/(?<id>\\w+)\\W?")
            val matchAlt = regexAlt.find(url)
            matchAlt?.let {
                matchAlt.groups["type"]?.value to matchAlt.groups["id"]?.value
            } ?: Pair(null, null)
        }
    }
}

private suspend fun SpotifyApi.get(url: URLBuilder.() -> Unit): HttpResponse {
    val res = client.get {
        url {
            set(
                scheme = "https",
                host = "api.spotify.com",
            )
            path("v1")
            url()
        }
        authConfig()
    }
    return if (res.status == HttpStatusCode.Unauthorized) {
        refreshAuthToken()
        get(url)
    } else {
        res
    }
}

private suspend fun SpotifyApi.get(url: String): HttpResponse {
    val res = client.get(urlString = url) {
        authConfig()
    }
    return if (res.status == HttpStatusCode.Unauthorized) {
        refreshAuthToken()
        get(url)
    } else {
        res
    }
}

suspend fun SpotifyApi.album(
    id: String,
    offset: Int,
    limit: Int = 100,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
) = get {
    appendPathSegments("albums", id)
    parameters {
        if (market != null) {
            append("market", market)
        }
        if (additionalTypes.isNotEmpty()) {
            append(
                "additional_types",
                additionalTypes.joinToString(",")
            )
        }
        if (fields.isNotEmpty()) {
            append(
                "fields",
                fields.joinToString(",")
            )
        }
        append("offset", "$offset")
        append("limit", "$limit")
    }
}

suspend fun SpotifyApi.albumTracks(
    id: String,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyAlbum? {
    var curr = album(id, 0, 100, market, additionalTypes, *fields)
        .body<SpotifyAlbum>()

    while (curr.tracks!!.items!!.size < curr.tracks.total!!) {
        val r = get {
            appendPathSegments("albums", curr.id!!, "tracks")
            parameters {
                append(
                    "offset", "${curr.tracks!!.items!!.size}"
                )
                append("limit", "100")
                append(
                    "additional_types",
                    "track"
                )
            }
        }
            .body<SpotifyAlbum.Tracks>()

        curr = curr.copy(
            tracks = curr.tracks.copy(
                items = curr.tracks.items.filterNotNull() + r.items!!.filterNotNull(),
            )
        )
    }

    return curr
}

suspend fun SpotifyApi.playlist(
    id: String,
    offset: Int,
    limit: Int = 100,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): HttpResponse {
    return get {
        appendPathSegments("playlists", id)
        parameters {
            if (market != null) {
                append("market", market)
            }
            if (additionalTypes.isNotEmpty()) {
                append(
                    "additional_types",
                    additionalTypes.joinToString(",")
                )
            }
            if (fields.isNotEmpty()) {
                append(
                    "fields",
                    fields.joinToString(",")
                )
            }
            append("offset", "$offset")
            append("limit", "$limit")
        }
    }
}

suspend fun SpotifyApi.playlistTracks(
    id: String,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyPlaylist? {
    var curr = playlist(id, 0, 100, market, additionalTypes, *fields)
        .body<SpotifyPlaylist>()
    while (curr.tracks.items.size < curr.tracks.total) {
        val r = get {
            appendPathSegments("playlists", curr.id, "tracks")
            parameters {
                append(
                    "offset", "${curr.tracks.items.size}"
                )
                append(
                    "limit", "100"
                )
                append(
                    "additional_types",
                    "track"
                )
            }
        }
            .body<SpotifyPlaylist.Tracks>()

        curr = curr.copy(
            tracks = curr.tracks.copy(
                items = curr.tracks.items + r.items,
            )
        )
    }

    return curr
}