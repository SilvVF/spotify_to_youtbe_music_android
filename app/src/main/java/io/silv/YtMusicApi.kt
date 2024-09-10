package io.silv

import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapter
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Locale

@JsonClass(generateAdapter = true)
data class YouTubeLocale(
    val gl: String = Locale.getDefault().country,
    val hl: String = Locale.getDefault().toLanguageTag()
)

@JsonClass(generateAdapter = true)
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val api_key: String,
    val userAgent: String,
    val referer: String? = null,
) {
    fun toContext(visitorData: String?, locale: YouTubeLocale = YouTubeLocale()) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )
}

data class YtMusicApi(
    val builder: OkHttpClient.Builder.() -> Unit =  {}
) {
    var visitorData: String = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
    val WEB_REMIX = YouTubeClient(
        clientName = "WEB_REMIX",
        clientVersion = "1.20220606.03.00",
        api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        userAgent = USER_AGENT_WEB,
        referer = REFERER_YOUTUBE_MUSIC
    )

    fun Request.Builder.defaultHeaders() = apply {
        addHeader("X-Goog-Api-Format-Version", "1")
        addHeader("X-YouTube-Client-Name", WEB_REMIX.clientName)
        addHeader("X-YouTube-Client-Version", WEB_REMIX.clientVersion)
        addHeader("x-origin", "https://music.youtube.com")
    }

    fun defaultHttpUrl(additional:  HttpUrl.Builder.() -> Unit) = HttpUrl.Builder()
        .scheme("https")
        .host("music.youtube.com")
        .addPathSegment("youtubei")
        .addPathSegment("v1")
        .addQueryParameter("key", WEB_REMIX.api_key)
        .addQueryParameter("prettyPrint", "false")
        .apply(additional)
        .build()

    val client = OkHttpClient.Builder().apply(builder).build()

    companion object {
        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"
        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
    }
}

suspend fun YtMusicApi.get(url: HttpUrl.Builder.() -> Unit): Response {

    val httpUrl = HttpUrl.Builder().apply(url).build()

    return client.newCall(
        Request.Builder()
            .defaultHeaders()
            .url(defaultHttpUrl(url))
            .build()
    )
        .await()
}

suspend fun YtMusicApi.post(
    body: RequestBody,
    url: HttpUrl.Builder.() -> Unit
): Response {

    return client.newCall(
        Request.Builder()
            .defaultHeaders()
            .url(defaultHttpUrl(url))
            .post(body)
            .build()
    )
        .await()
}

@JsonClass(generateAdapter = true)
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
) {
    @JsonClass(generateAdapter = true)
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val gl: String,
        val hl: String,
        val visitorData: String?,
    )
    @JsonClass(generateAdapter = true)
    data class ThirdParty(
        val embedUrl: String,
    )
}

@JsonClass(generateAdapter = true)
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)

@OptIn(ExperimentalStdlibApi::class)
suspend fun YtMusicApi.search(
    query: String? = null,
    params: String? = null,
    continuation: String? = null,
): Response {
    return post(
        body = moshi.adapter<SearchBody>().toJson(
            SearchBody(
                context = WEB_REMIX.toContext(visitorData),
                query = query,
                params = params
            )
        )
            .toRequestBody("application/json".toMediaType())
    ) {
        addPathSegment("search")
        addQueryParameter("continuation", continuation)
        addQueryParameter("ctoken", continuation)
    }
}
