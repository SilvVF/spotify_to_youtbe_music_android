package io.silv.sp2yt.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import io.silv.sp2yt.Keys
import io.silv.sp2yt.appGraph
import io.silv.sp2yt.settingsMutableState
import io.silv.sp2yt.sha1
import io.silv.sp2yt.similarity
import io.silv.sp2yt.types.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class YoutubeMusicApi(
    val client: HttpClient,
    store: Settings,
) {
    var visitorData by store.settingsMutableState<String>(Keys.YT_VISITOR, "")

    @Serializable
    private data class YouTubeLocale(
        val gl: String = Locale.current.region,
        val hl: String = Locale.current.toLanguageTag()
    )

    @Serializable
    private data class YouTubeClient(
        val clientName: String,
        val clientVersion: String,
        @SerialName("api_key") val apiKey: String,
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


    private val WEB_REMIX = YouTubeClient(
        clientName = "WEB_REMIX",
        clientVersion = "1.20220606.03.00",
        apiKey = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        userAgent = USER_AGENT_WEB,
        referer = REFERER_YOUTUBE_MUSIC
    )

    @OptIn(ExperimentalTime::class)
    fun HeadersBuilder.defaultHeaders(setLogin: Boolean = false) = apply {
        append("X-Goog-Api-Format-Version", "1")
        append("X-YouTube-Client-Name", WEB_REMIX.clientName)
        append("X-YouTube-Client-Version", WEB_REMIX.clientVersion)
        append("x-origin", "https://music.youtube.com")

        if (setLogin) {
            append("Cookie", cookie)

            cookieMap["SAPISID"]?.let { sapisid ->
                val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
                val sapisidHash = sha1("$currentTime $sapisid https://music.youtube.com")
                append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
            }
        }
    }

    fun defaultHttpUrl(additional: URLBuilder.() -> Unit): Url =
        buildUrl {
            set(
                scheme = "https",
                host = "music.youtube.com"
            )
            appendPathSegments("youtubei", "v1")
            parameters {
                append("key", WEB_REMIX.apiKey)
                append("prettyPrint", "false")
            }
            additional()
        }

    fun parseCookieString(cookie: String): Map<String, String> =
        cookie.split("; ")
            .filter { it.isNotEmpty() }
            .associate {
                val (key, value) = it.split("=")
                key to value
            }

    var cookie: String by store.settingsMutableState(Keys.INNER_TUBE_COOKIE, "")
    val cookieMap get() = if (cookie.isEmpty()) emptyMap() else parseCookieString(cookie)

    private suspend fun get(url: URLBuilder.() -> Unit): HttpResponse {
        return client.get(url = defaultHttpUrl(url)) {
            headers {
                defaultHeaders()
            }
        }
    }

    private suspend inline fun <reified T> post(
        body: T,
        setLogin: Boolean = false,
        noinline url: URLBuilder.() -> Unit,
    ): HttpResponse {
        return client.post(url = defaultHttpUrl(url)) {
            headers {
                defaultHeaders(setLogin)
            }
            contentType(ContentType.Application.Json)
            setBody(appGraph.json.encodeToString(body))
        }
    }


    @OptIn(ExperimentalAtomicApi::class)
    suspend fun searchSongs(
        tracks: List<Track>,
        onProgress: (complete: Int, total: Int) -> Unit = { _, _ -> }
    ): PlaylistMatch {

        val notFound = mutableSetOf<Track>()
        val lowConfidence = mutableSetOf<Track>()
        val matches = mutableMapOf<Track, Pair<SongItem?, List<SongItem>>>()

        val completed = AtomicInt(value = 0)
        val semaphore = Semaphore(4)
        val jobs = mutableListOf<Job>()

        supervisorScope {
            tracks.forEach { song ->
                semaphore.withPermit {
                    val job = launch {
                        val name = song.name
                            .replace(Regex(""" \(feat.*\..+\)"""), "")
                        val names = song.artists.joinToString(" ") + " " + name
                        val query = names.replace(" &", "")

                        val results = search(query, filter = SearchFilter.FILTER_SONG).getOrNull()
                            ?.items.orEmpty()
                            .filterIsInstance<SongItem>()

                        val (best, scores) = getBestFitSongId(results, song)
                        if (best != null) {
                            val maxScore = scores.maxOf { it.value }
                            matches[song] = best.takeIf { maxScore > 1.3 } to results

                            if (maxScore < 1.7) {
                                lowConfidence.add(song)
                            }
                        } else {
                            notFound.add(song)
                        }
                    }
                    jobs.add(job)
                }
                onProgress(completed.fetchAndAdd(1), tracks.size)
            }
        }

        jobs.joinAll()

        return PlaylistMatch(
            notFound = notFound,
            lowConfidence = lowConfidence,
            matches = matches.mapValues { (_, v) -> PlaylistMatch.Result(v.first, v.second) }
        )
    }

    suspend fun search(query: String, filter: SearchFilter) = runCatching {
        val response = internalSearch(query, filter.value).body<SearchResponse>()
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.continuations?.getContinuation()
        )
    }

    suspend fun accountMenu(): AccountInfo? {
        return post(
            body = AccountMenuBody(
                context = WEB_REMIX.toContext(
                    visitorData.ifEmpty { "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" }
                )
            ),
            setLogin = true
        ) {
            appendPathSegments("account", "account_menu")
        }
            .body<AccountMenuResponse>()
            .actions.getOrNull(0)?.openPopupAction?.popup?.multiPageMenuRenderer
            ?.header?.activeAccountHeaderRenderer
            ?.toAccountInfo()
    }

    @Serializable
    private data class PlaylistCreateRequest(
        val title: String,
        val description: String,
        val privacyStatus: String,
        val videoIds: List<String>,
        val context: Context
    )

    suspend fun createPlaylist(
        name: String,
        description: String,
        privacyStatus: PrivacyStatus,
        ids: List<String>
    ): HttpResponse {
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
            appendPathSegments("playlist", "create")
        }
    }

    @Serializable
    private data class SearchBody(
        val context: Context,
        val query: String?,
        val params: String?,
    )

    private suspend fun internalSearch(
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): HttpResponse {
        return post(
            setLogin = true,
            body = SearchBody(
                context = WEB_REMIX.toContext(visitorData.ifEmpty { "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" }),
                query = query,
                params = params
            )
        ) {
            appendPathSegments("search")
            parameters {
                if (continuation != null) {
                    append("continuation", continuation)
                    append("ctoken", continuation)
                }
            }
        }
    }


    private fun getBestFitSongId(
        songs: List<SongItem>,
        target: Track
    ): Pair<SongItem?, Map<SongItem, Double>> {

        val matchScore = mutableMapOf<SongItem, Double>()

        for (song in songs) {

            val scores = mutableListOf<Double>()

            val artists = song.artists.joinToString(" ") { it.name }
            val targetArtists = target.artists.joinToString(" ")

            val targetDuration = target.durationMs / 1000.0

            val titleScore = song.title.similarity(target.name, true)
            val artistScore = targetArtists.similarity(artists, true)

            if (song.duration != null) {
                val durationScore =
                    1 - abs(song.duration - targetDuration.inWholeMilliseconds) * 2.0 / targetDuration.inWholeMilliseconds
                scores.add(durationScore * 5)
            }

            if (song.album != null) {
                val albumScore = song.album.name.similarity(target.album, true)
                scores.add(albumScore)
            }
            scores.add(titleScore)
            scores.add(artistScore)

            matchScore[song] = (scores.sum() / scores.size)
        }

        return matchScore.maxByOrNull { (_, score) -> score }?.key to matchScore
    }

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

    companion object {
        const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"
        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
    }
}
