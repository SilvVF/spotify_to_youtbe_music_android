package io.silv

import android.content.SharedPreferences
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import io.silv.types.SpotifyPlaylist
import kotlinx.coroutines.sync.Mutex
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
            Log.d("TOKEN", "expr: $expiresAt, time: ${epochSeconds() - 5.minutes.inWholeSeconds} expired: $expired")
            if (token.isEmpty() || expired) {
                refreshAuthToken()
            }
        }

        check(token.isNotEmpty()) { "Auth token was not set" }
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
                        .add("grant_type", "client_credentials")
                        .add("client_id", CLIENT_ID)
                        .add("client_secret", CLIENT_SECRET)
                        .build()
                )
                .build()
                .also { println(it) }
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
        private const val CLIENT_ID = "e6f8c62b14de4e75972c8a38e3628eaa"
        private const val CLIENT_SECRET = "01bbb0ccef5e4ac5ae6347f2cf180475"

        fun extractPlaylistIdFromUrl(url: String): String? {
            val regex = Regex("playlist/(?<id>\\w{22})\\W?")
            val match = regex.find(url)

            return if (match != null) {
                match.groups["id"]?.value
            } else {
                val regexAlt = Regex("playlist/(?<id>\\w+)\\W?")
                val matchAlt = regexAlt.find(url)

                 matchAlt?.groups?.get("id")?.value
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


suspend fun SpotifyApi.playlist(
    id: String,
    market: String? = null,
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyPlaylist? {

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