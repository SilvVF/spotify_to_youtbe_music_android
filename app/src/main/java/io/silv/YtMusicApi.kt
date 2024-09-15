package io.silv

import AccountInfo
import AccountMenuBody
import AccountMenuResponse
import SearchPage
import SearchResponse
import SearchResult
import SongItem
import android.content.SharedPreferences
import android.util.Log
import getContinuation
import io.silv.types.SpotifyPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@Serializable
data class YouTubeLocale(
    val gl: String = Locale.getDefault().country,
    val hl: String = Locale.getDefault().toLanguageTag()
)

@Serializable
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
    val store: SharedPreferences,
    val builder: OkHttpClient.Builder.() -> Unit =  {}
) {
    var visitorData: String by store.stored<String>("visitorData")
    val WEB_REMIX = YouTubeClient(
        clientName = "WEB_REMIX",
        clientVersion = "1.20220606.03.00",
        api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        userAgent = USER_AGENT_WEB,
        referer = REFERER_YOUTUBE_MUSIC
    )

    fun Request.Builder.defaultHeaders(setLogin: Boolean = false) = apply {
        addHeader("X-Goog-Api-Format-Version", "1")
        addHeader("X-YouTube-Client-Name", WEB_REMIX.clientName)
        addHeader("X-YouTube-Client-Version", WEB_REMIX.clientVersion)
        addHeader("x-origin", "https://music.youtube.com")

        if (setLogin) {
            cookie?.let { cookie ->
                addHeader("Cookie", cookie)
                if ("SAPISID" !in cookieMap) return@let
                val currentTime = System.currentTimeMillis() / 1000
                val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} https://music.youtube.com")
                addHeader("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
            }
        }
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

        var cookie: String? = null
            set(value) {
                field = value
                cookieMap = if (value == null) emptyMap() else parseCookieString(value)
            }
        private var cookieMap = emptyMap<String, String>()

        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"
        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
    }
}


suspend fun YtMusicApi.get(url: HttpUrl.Builder.() -> Unit): Response {
    return client.newCall(
        Request.Builder()
            .defaultHeaders()
            .url(defaultHttpUrl(url))
            .build()
    )
        .await()
}

suspend inline fun<reified T> YtMusicApi.post(
    body: T,
    setLogin: Boolean = false,
    noinline url: HttpUrl.Builder.() -> Unit,
): Response {

    return client.newCall(
        Request.Builder()
            .defaultHeaders(setLogin)
            .url(defaultHttpUrl(url))
            .post(json.encodeToString(body).toRequestBody("application/json".toMediaType()))
            .build()
            .also { println(it.toString()) }
    )
        .await()
}


suspend fun YtMusicApi.searchSongs(
    tracks: SpotifyPlaylist.Tracks,
    onProgress: (complete: Int, total: Int) -> Unit = {_,_ ->}
): SearchSongsResult {
    val songs = tracks.items
    val notFound = mutableListOf<SpotifyPlaylist.Tracks.Item>()
    val matches = mutableMapOf<SpotifyPlaylist.Tracks.Item, Pair<SongItem, List<SongItem>>>()

    val completed = AtomicInteger(0)

    songs.pForEach(dispatcher = Dispatchers.IO) { i, song ->
        val name = song.track.name
            .replace(Regex(""" \(feat.*\..+\)"""), "")

        val names = song.track.artists
            .joinToString(" ") { it.name } + " " + name
        val query = names.replace(" &", "")
        val seen = mutableSetOf<String>()
        val results = search(query, filter = SearchFilter.FILTER_SONG)
            .getOrNull()?.items.orEmpty()
            .filter { seen.add(it.id) }
            .filterIsInstance<SongItem>()
        Log.d("Results", results.toString())
        val target = getBestFitSongId(results, song)
        if (target != null) {
            matches[song] = results.first { it.id == target } to results
        } else  {
            notFound.add(song)
        }
        onProgress(completed.getAndIncrement(), songs.size)
    }
    return SearchSongsResult(
        notFound = notFound,
        matches = matches.mapValues { (_, v) -> SearchSongsResult.Match(v.first, v.second) }
    )
}

suspend fun YtMusicApi.search(query: String, filter: SearchFilter) = runCatching {
    val response = internalSearch(query, filter.value).decode<SearchResponse>()
    SearchResult(
        items = response?.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
            ?.musicShelfRenderer?.contents?.mapNotNull {
                SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
            }.orEmpty(),
        continuation = response?.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
            ?.musicShelfRenderer?.continuations?.getContinuation()
    )
}

suspend fun YtMusicApi.accountMenu(): AccountInfo? {
    return post(
        body = AccountMenuBody(
            context = WEB_REMIX.toContext(
                visitorData.ifEmpty { "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" }
            )
        ),
        setLogin = true
    ) {
        addPathSegment("account")
        addPathSegment("account_menu")
    }
        .decode<AccountMenuResponse>()
        ?.actions?.get(0)?.openPopupAction?.popup?.multiPageMenuRenderer
        ?.header?.activeAccountHeaderRenderer
        ?.toAccountInfo()
}

@Serializable
data class PlaylistCreateRequest(
    val title: String,
    val description: String,
    val privacyStatus: String,
    val videoIds: List<String>,
    val context: Context
)

suspend fun YtMusicApi.createPlaylist(
    name: String,
    description: String,
    privacyStatus: PrivacyStatus,
    ids: List<String>
): Response {
    return post(
        body = PlaylistCreateRequest(
            name,
            description,
            privacyStatus.value,
            ids,
            WEB_REMIX.toContext(visitorData.ifEmpty { "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" })
        ),
        setLogin = true
    ) {
        addPathSegment("playlist")
        addPathSegment("create")
    }
}

private suspend fun YtMusicApi.internalSearch(
    query: String? = null,
    params: String? = null,
    continuation: String? = null,
): Response {
    return post(
        body = SearchBody(
            context = WEB_REMIX.toContext(visitorData.ifEmpty { "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" }),
            query = query,
            params = params
        )
    ) {
        addPathSegment("search")
        addQueryParameter("continuation", continuation)
        addQueryParameter("ctoken", continuation)
    }
}


fun getBestFitSongId(ytmResults: List<SongItem>, spoti: SpotifyPlaylist.Tracks.Item): String? {
    val matchScore = mutableMapOf<String, Double>()

    for (ytm in ytmResults) {
        if (ytm.title.isEmpty()) {
            continue
        }

        var durationMatchScore: Double? = null
        if (ytm.duration != null) {
            val sDur = spoti.track.durationMs / 1000.0
            durationMatchScore = 1 - abs(ytm.duration - sDur) * 2.0 / sDur
        }

        val artists = ytm.artists.joinToString(" ") { it.name }

        val scores = mutableListOf(
            similarity(ytm.title.lowercase(), spoti.track.name.lowercase()),
            similarity(artists.lowercase(), artists.lowercase())
        )
        if (durationMatchScore != null) {
            scores.add(durationMatchScore * 5)
        }

        if (ytm.album != null) {
            scores.add(
                similarity(ytm.album.name.lowercase(), spoti.track.album.name.lowercase())
            )
        }

        matchScore[ytm.id] = (scores.sum() / scores.size) * maxOf(1, if (ytm.album != null) 2 else 1)
    }

    if (matchScore.isEmpty()) {
        return null
    }

    return matchScore.maxByOrNull { it.value }?.key
}

fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
    }

    return dp[s1.length][s2.length]
}

fun similarity(s1: String, s2: String): Double {
    val maxLen = maxOf(s1.length, s2.length)
    return if (maxLen == 0) 1.0 else (maxLen - levenshteinDistance(s1, s2)) / maxLen.toDouble()
}

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null,
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val gl: String,
        val hl: String,
        val visitorData: String?,
    )
    @Serializable
    data class ThirdParty(
        val embedUrl: String,
    )
}

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)

@JvmInline
value class SearchFilter(val value: String) {
    companion object {
        val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
        val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
    }
}


data class SearchSongsResult(
    val notFound: List<SpotifyPlaylist.Tracks.Item>,
    val matches: Map<SpotifyPlaylist.Tracks.Item, Match>
) {

    data class Match(
        val closest: SongItem,
        val other: List<SongItem>
    )
}

@JvmInline
value class PrivacyStatus(val value: String) {
    companion object {
        val PRIVACY_PUBLIC = PrivacyStatus("PUBLIC")
        val PRIVACY_PRIVATE = PrivacyStatus("PRIVATE")
        val PRIVACY_UNLISTED = PrivacyStatus("UNLISTED")
    }
}

