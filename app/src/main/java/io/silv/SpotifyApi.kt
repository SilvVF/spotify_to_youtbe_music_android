package io.silv

import android.content.SharedPreferences
import android.util.JsonReader
import android.util.JsonToken
import io.silv.types.SpotifyAlbum
import io.silv.types.SpotifyPlaylist
import io.silv.ui.PlaylistArgs
import kotlinx.coroutines.sync.Mutex
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.StringReader
import kotlin.time.Duration.Companion.minutes

data class SpotifyApi(
    val store: SharedPreferences,
    val builder: OkHttpClient.Builder.() -> Unit = {}
) {

    private val mutex = Mutex()

    private var expiresAt by store.stored<Long>("expires_at")
    private var token by store.stored<String>("token")

    private val refreshClient = OkHttpClient.Builder().build()

    val client = OkHttpClient.Builder().apply(builder).addInterceptor { chain ->

        mutex.withLock {
            val expired = expiresAt < epochSeconds() - 5.minutes.inWholeSeconds
            if (token.isEmpty() || expired) {
                refreshAuthToken()
            }
        }

        check(token.isNotEmpty()) { "Auth token was not set" }
        Timber.d { token }
        var res = chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )
        if (!res.isSuccessful && res.code == 401) {
            refreshAuthToken()
            res.close()
            res = chain.proceed(
                chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            )
        }

        res
    }
        .build()

    private fun refreshAuthToken() {
        val res = refreshClient.newCall(
            Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(
                    FormBody.Builder()
                        .addEncoded("grant_type", "client_credentials")
                        .build()
                )
                .header(
                    name = "Authorization",
                    value = Credentials.basic(CLIENT_ID, CLIENT_SECRET)
                )
                .build()
        )
            .execute()

        JsonReader(StringReader(res.body!!.string())).use { r ->
            r.beginObject()
            while(r.hasNext()) {
                val k = r.nextName()
                when(r.peek()) {
                    JsonToken.STRING -> {
                       val s = r.nextString()
                       if (k == "access_token") {
                           token = s
                       }
                    }
                    JsonToken.NUMBER -> {
                        val n = r.nextLong()
                        if (k == "expires_in") {
                            expiresAt = (epochSeconds() + n)
                        }
                    }
                    else -> r.skipValue()
                }
            }
        }
    }

    companion object {

        var CLIENT_ID by App.store.stored<String>("spotifyClientId")
        var CLIENT_SECRET by App.store.stored<String>("spotifyClientSecret")

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
}

private suspend fun SpotifyApi.get(url: HttpUrl.Builder.() -> Unit): Response {
    val httpUrl = HttpUrl.Builder().apply {
        scheme("https")
        host("api.spotify.com")
        addPathSegment("v1")
        url()
    }
    return client.newCall(
        Request.Builder()
            .url(httpUrl.build().also { println(it.toUrl().toString()) })
            .build()
    ).await()
}

private suspend fun SpotifyApi.get(url: String): Response {
    return client.newCall(
        Request.Builder()
            .url(url.also { println(it) })
            .build()
    ).await()
}


suspend fun SpotifyApi.album(
    id: String,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyAlbum? {
    // Looks sus but album doesn't have all the tracks after getting album
    // fetch only the tracks if more than 100 exist
    suspend fun album(offset: Int, limit: Int = 100) = get {
        addPathSegment("albums")
        addPathSegment(id)
        addQueryParameter("market", market)
        addQueryParameter(
            "additional_types",
            additionalTypes.joinToString(",")
        )
        addQueryParameter(
            "fields",
            fields.joinToString(",")
        )
        addQueryParameter("offset", "$offset")
        addQueryParameter("limit", "$limit")
    }

    var curr = album(0).decode<SpotifyAlbum>() ?: return null
    while (curr.tracks!!.items!!.size < curr.tracks!!.total!!) {
        val r = get {
            addPathSegment("albums")
            addPathSegment(curr.id!!)
            addPathSegment("tracks")
            addQueryParameter("offset", "${curr.tracks!!.items!!.size}")
            addQueryParameter("limit", "100")
            addQueryParameter(
                "additional_types",
                "track"
            )
        }
            .decode<SpotifyAlbum.Tracks>()!!
        curr = curr.copy(
            tracks = curr.tracks!!.copy(
                items = curr.tracks!!.items!!.filterNotNull() + r.items!!.filterNotNull(),
            )
        )
    }

    return curr
}

suspend fun SpotifyApi.playlist(
    id: String,
    market: String? = "US",
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyPlaylist? {
    // Looks sus but playlist doesn't have all the tracks after getting album
    // fetch only the tracks if more than 100 exist
    suspend fun playlist(offset: Int, limit: Int = 100) = get {
        addPathSegment("playlists")
        addPathSegment(id)
        addQueryParameter("market", market)
        addQueryParameter(
            "additional_types",
            additionalTypes.joinToString(",")
        )
        addQueryParameter(
            "fields",
            fields.joinToString(",")
        )
        addQueryParameter("offset", "$offset")
        addQueryParameter("limit", "$limit")
    }

    var curr = playlist(0).decode<SpotifyPlaylist>() ?: return null
    while (curr.tracks.items.size < curr.tracks.total) {
        val r = get {
            addPathSegment("playlists")
            addPathSegment(curr.id)
            addPathSegment("tracks")
            addQueryParameter("offset", "${curr.tracks.items.size}")
            addQueryParameter("limit", "100")
            addQueryParameter(
                "additional_types",
                "track"
            )
        }
            .decode<SpotifyPlaylist.Tracks>()!!
        curr = curr.copy(
            tracks = curr.tracks.copy(
                items = curr.tracks.items + r.items,
            )
        )
    }

    return curr
}