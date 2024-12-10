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
import io.silv.types.Playlist
import io.silv.types.SpotifyPlaylist
import io.silv.types.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
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
    tracks: List<Track>,
    onProgress: (complete: Int, total: Int) -> Unit = {_,_ ->}
): PlaylistMatch {

    val notFound = mutableSetOf<Track>()
    val lowConfidence = mutableSetOf<Track>()
    val matches = mutableMapOf<Track, Pair<SongItem?, List<SongItem>>>()

    val completed = AtomicInteger(0)

    tracks.pForEach(dispatcher = Dispatchers.IO) { _, song ->
        val name = song.name
            .replace(Regex(""" \(feat.*\..+\)"""), "")
        val names = song.artists.joinToString(" ") + " " + name
        val query = names.replace(" &", "")

        val results = search(query, filter = SearchFilter.FILTER_SONG).getOrNull()
            ?.items.orEmpty()
            .filterIsInstance<SongItem>()

        val (best, scores) = getBestFitSongId(results, song)
        if (best != null) {
            val maxScore = scores.maxOf{ it.value }
            matches[song] = best.takeIf { maxScore > 1.3 } to results

            if (maxScore < 1.7) {
                lowConfidence.add(song)
            }
        } else  {
            notFound.add(song)
        }
        onProgress(completed.getAndIncrement(), tracks.size)
    }
    return PlaylistMatch(
        notFound = notFound,
        lowConfidence = lowConfidence,
        matches = matches.mapValues { (_, v) -> PlaylistMatch.Result(v.first, v.second) }
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


fun getBestFitSongId(
    songs: List<SongItem>,
    target: Track
): Pair<SongItem?, Map<SongItem, Double>> {

    val matchScore = mutableMapOf<SongItem, Double>()

    for (song in songs) {

        val scores = mutableListOf<Double>()

        val artists = song.artists.joinToString(" ") { it.name }
        val targetArtists = target.artists.joinToString(" ")

        val targetDuration = target.durationMs / 1000.0

        val titleScore = similarity(song.title.lowercase(), target.name.lowercase())
        val artistScore = similarity(targetArtists.lowercase(), artists.lowercase())

        if (song.duration != null) {
            val durationScore = 1 - abs(song.duration - targetDuration.inWholeMilliseconds) * 2.0 / targetDuration.inWholeMilliseconds
            scores.add(durationScore * 5)
        }

        if (song.album != null) {
            val albumScore = similarity(song.album.name.lowercase(), target.album.lowercase())
            scores.add(albumScore)
        }
        scores.addAll(titleScore, artistScore)
        matchScore[song] = (scores.sum() / scores.size)
    }

    return matchScore.maxByOrNull { (_, score) -> score }?.key to matchScore
}

private fun levenshteinDistance(s1: String, s2: String): Int {

    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
               dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }

    return dp[s1.length][s2.length]
}

private fun similarity(s1: String, s2: String): Double {
    val distance = levenshteinDistance(s1, s2)
    val maxLength = maxOf(s1.length, s2.length)
    return if (maxLength == 0) 1.0 else (1.0 - distance.toDouble() / maxLength)
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


data class PlaylistMatch(
    val lowConfidence: Set<Track>,
    val notFound: Set<Track>,
    val matches: Map<Track, Result>
) {

    data class Result(
        val closest: SongItem?,
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

