package io.silv.types


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyPlaylist(
    @Json(name = "collaborative")
    val collaborative: Boolean,
    @Json(name = "description")
    val description: String,
    @Json(name = "external_urls")
    val externalUrls: ExternalUrls,
    @Json(name = "followers")
    val followers: Followers,
    @Json(name = "href")
    val href: String,
    @Json(name = "id")
    val id: String,
    @Json(name = "images")
    val images: List<Image>,
    @Json(name = "name")
    val name: String,
    @Json(name = "owner")
    val owner: Owner,
    @Json(name = "primary_color")
    val primaryColor: Any?,
    @Json(name = "public")
    val `public`: Boolean,
    @Json(name = "snapshot_id")
    val snapshotId: String,
    @Json(name = "tracks")
    val tracks: Tracks,
    @Json(name = "type")
    val type: String,
    @Json(name = "uri")
    val uri: String
) {
    @JsonClass(generateAdapter = true)
    data class ExternalUrls(
        @Json(name = "spotify")
        val spotify: String
    )

    @JsonClass(generateAdapter = true)
    data class Followers(
        @Json(name = "href")
        val href: Any?,
        @Json(name = "total")
        val total: Int
    )

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "height")
        val height: Any?,
        @Json(name = "url")
        val url: String,
        @Json(name = "width")
        val width: Any?
    )

    @JsonClass(generateAdapter = true)
    data class Owner(
        @Json(name = "display_name")
        val displayName: String,
        @Json(name = "external_urls")
        val externalUrls: ExternalUrls,
        @Json(name = "href")
        val href: String,
        @Json(name = "id")
        val id: String,
        @Json(name = "type")
        val type: String,
        @Json(name = "uri")
        val uri: String
    ) {
        @JsonClass(generateAdapter = true)
        data class ExternalUrls(
            @Json(name = "spotify")
            val spotify: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class Tracks(
        @Json(name = "href")
        val href: String,
        @Json(name = "items")
        val items: List<Item>,
        @Json(name = "limit")
        val limit: Int,
        @Json(name = "next")
        val next: Any?,
        @Json(name = "offset")
        val offset: Int,
        @Json(name = "previous")
        val previous: Any?,
        @Json(name = "total")
        val total: Int
    ) {
        @JsonClass(generateAdapter = true)
        data class Item(
            @Json(name = "added_at")
            val addedAt: String,
            @Json(name = "added_by")
            val addedBy: AddedBy,
            @Json(name = "is_local")
            val isLocal: Boolean,
            @Json(name = "primary_color")
            val primaryColor: Any?,
            @Json(name = "track")
            val track: Track,
            @Json(name = "video_thumbnail")
            val videoThumbnail: VideoThumbnail
        ) {
            @JsonClass(generateAdapter = true)
            data class AddedBy(
                @Json(name = "external_urls")
                val externalUrls: ExternalUrls,
                @Json(name = "href")
                val href: String,
                @Json(name = "id")
                val id: String,
                @Json(name = "type")
                val type: String,
                @Json(name = "uri")
                val uri: String
            ) {
                @JsonClass(generateAdapter = true)
                data class ExternalUrls(
                    @Json(name = "spotify")
                    val spotify: String
                )
            }

            @JsonClass(generateAdapter = true)
            data class Track(
                @Json(name = "album")
                val album: Album,
                @Json(name = "artists")
                val artists: List<Artist>,
                @Json(name = "available_markets")
                val availableMarkets: List<String>,
                @Json(name = "disc_number")
                val discNumber: Int,
                @Json(name = "duration_ms")
                val durationMs: Int,
                @Json(name = "episode")
                val episode: Boolean,
                @Json(name = "explicit")
                val explicit: Boolean,
                @Json(name = "external_ids")
                val externalIds: ExternalIds,
                @Json(name = "external_urls")
                val externalUrls: ExternalUrls,
                @Json(name = "href")
                val href: String,
                @Json(name = "id")
                val id: String,
                @Json(name = "is_local")
                val isLocal: Boolean,
                @Json(name = "name")
                val name: String,
                @Json(name = "popularity")
                val popularity: Int,
                @Json(name = "preview_url")
                val previewUrl: String?,
                @Json(name = "track")
                val track: Boolean,
                @Json(name = "track_number")
                val trackNumber: Int,
                @Json(name = "type")
                val type: String,
                @Json(name = "uri")
                val uri: String
            ) {
                @JsonClass(generateAdapter = true)
                data class Album(
                    @Json(name = "album_type")
                    val albumType: String,
                    @Json(name = "artists")
                    val artists: List<Artist>,
                    @Json(name = "available_markets")
                    val availableMarkets: List<String>,
                    @Json(name = "external_urls")
                    val externalUrls: ExternalUrls,
                    @Json(name = "href")
                    val href: String,
                    @Json(name = "id")
                    val id: String,
                    @Json(name = "images")
                    val images: List<Image>,
                    @Json(name = "name")
                    val name: String,
                    @Json(name = "release_date")
                    val releaseDate: String,
                    @Json(name = "release_date_precision")
                    val releaseDatePrecision: String,
                    @Json(name = "total_tracks")
                    val totalTracks: Int,
                    @Json(name = "type")
                    val type: String,
                    @Json(name = "uri")
                    val uri: String
                ) {
                    @JsonClass(generateAdapter = true)
                    data class Artist(
                        @Json(name = "external_urls")
                        val externalUrls: ExternalUrls,
                        @Json(name = "href")
                        val href: String,
                        @Json(name = "id")
                        val id: String,
                        @Json(name = "name")
                        val name: String,
                        @Json(name = "type")
                        val type: String,
                        @Json(name = "uri")
                        val uri: String
                    ) {
                        @JsonClass(generateAdapter = true)
                        data class ExternalUrls(
                            @Json(name = "spotify")
                            val spotify: String
                        )
                    }

                    @JsonClass(generateAdapter = true)
                    data class ExternalUrls(
                        @Json(name = "spotify")
                        val spotify: String
                    )

                    @JsonClass(generateAdapter = true)
                    data class Image(
                        @Json(name = "height")
                        val height: Int,
                        @Json(name = "url")
                        val url: String,
                        @Json(name = "width")
                        val width: Int
                    )
                }

                @JsonClass(generateAdapter = true)
                data class Artist(
                    @Json(name = "external_urls")
                    val externalUrls: ExternalUrls,
                    @Json(name = "href")
                    val href: String,
                    @Json(name = "id")
                    val id: String,
                    @Json(name = "name")
                    val name: String,
                    @Json(name = "type")
                    val type: String,
                    @Json(name = "uri")
                    val uri: String
                ) {
                    @JsonClass(generateAdapter = true)
                    data class ExternalUrls(
                        @Json(name = "spotify")
                        val spotify: String
                    )
                }

                @JsonClass(generateAdapter = true)
                data class ExternalIds(
                    @Json(name = "isrc")
                    val isrc: String
                )

                @JsonClass(generateAdapter = true)
                data class ExternalUrls(
                    @Json(name = "spotify")
                    val spotify: String
                )
            }

            @JsonClass(generateAdapter = true)
            data class VideoThumbnail(
                @Json(name = "url")
                val url: String?
            )
        }
    }
}