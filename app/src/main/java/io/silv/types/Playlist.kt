package io.silv.types

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
@Stable
data class Image(
    val url: String,
    val w: Int?,
    val h: Int?
)

@Immutable
@Stable
data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val images: List<Image>,
    val items: List<Track>
) {

    constructor(spotifyAlbum: SpotifyAlbum): this (
        id = spotifyAlbum.id ?: "",
        name = spotifyAlbum.name ?: "",
        description = spotifyAlbum.label ?: "",
        images = spotifyAlbum.images.orEmpty().map { Image(it?.url.orEmpty(), it?.height, it?.width) },
        items = spotifyAlbum.tracks?.items.orEmpty().mapNotNull {
            if (it != null && spotifyAlbum.name != null) {
                Track(
                    it,
                    spotifyAlbum.name,
                    spotifyAlbum.images.orEmpty().map { Image(it?.url.orEmpty(), it?.height, it?.width) }
                )
            } else null
        }
    )

    constructor(spotifyPlaylist: SpotifyPlaylist): this (
        id = spotifyPlaylist.id,
        name = spotifyPlaylist.name,
        description = spotifyPlaylist.description,
        images = spotifyPlaylist.images.map { Image(it.url, it.height, it.width) },
        items = spotifyPlaylist.tracks.items.map(::Track)
    )
}

@Immutable
@Stable
data class Track(
    val id: String,
    val name: String,
    val album: String,
    val images: List<Image>,
    val artists: List<String>,
    val durationMs: Duration
) {
    constructor(track: SpotifyAlbum.Tracks.Item, album: String = "", images: List<Image>): this(
        id = track.id ?: "",
        name = track.name?: "",
        images = images,
        album = album,
        artists = track.artists.orEmpty().mapNotNull { it?.name },
        durationMs = (track.durationMs?:0).milliseconds
    )
    constructor(track: SpotifyPlaylist.Tracks.Item): this(
        id = track.track.id,
        name = track.track.name,
        images = track.track.album.images.map { Image(it.url, it.height, it.width) },
        album = track.track.album.name,
        artists = track.track.artists.map { it.name },
        durationMs = track.track.durationMs.milliseconds
    )
}