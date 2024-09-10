package io.silv

import android.content.Context
import android.content.SharedPreferences
import android.util.JsonReader
import android.util.JsonToken
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.EncryptedSharedPreferences
import io.silv.types.SpotifyPlaylist
import kotlinx.coroutines.sync.Mutex
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.StringReader
import kotlin.time.Duration.Companion.minutes

data class SpotifyApi(
    val context: Context,
    val builder: OkHttpClient.Builder.() -> Unit = {}
) {
    private val store: SharedPreferences = EncryptedSharedPreferences.create(
        "spotify-access-prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val mutex = Mutex()

    private var expiresAt by store.stored<Long>("expires_at")
    private var token by store.stored<String>("token")

    private val refreshClient = OkHttpClient.Builder().build()

    val client = OkHttpClient.Builder().apply(builder).addInterceptor { chain ->
        mutex.withLock {
            if (token.isEmpty() || expiresAt < epochSeconds() - 5.minutes.inWholeSeconds) {
                refreshAuthToken()
            }
        }
        check(token.isNotEmpty()) { "Auth token was not set" }
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )
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
            val re1 = Regex("""playlist/(?<id>\w{22})\W?""")
            val re2 = Regex("""playlist/(?<id>\w+)\W?""")
            return when {
                re1.matches(url) -> re1.find(url)?.groups?.get("id")?.value
                re2.matches(url) -> re2.find(url)?.groups?.get("id")?.value
                else -> null
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


suspend fun SpotifyApi.playlist(
    id: String,
    market: String? = null,
    additionalTypes: List<String> = listOf("tracks"),
    vararg fields: String
): SpotifyPlaylist? {
    return get {
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
    }
        .decode<SpotifyPlaylist>()
}