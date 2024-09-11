package io.silv.types


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SpotifyPlaylist(
    @SerialName("collaborative")
    val collaborative: Boolean,
    @SerialName("description")
    val description: String,
    @SerialName("external_urls")
    val externalUrls: ExternalUrls,
    @SerialName("followers")
    val followers: Followers,
    @SerialName("href")
    val href: String,
    @SerialName("id")
    val id: String,
    @SerialName("images")
    val images: List<Image>,
    @SerialName("name")
    val name: String,
    @SerialName("owner")
    val owner: Owner,
    @SerialName("primary_color")
    val primaryColor: String?,
    @SerialName("public")
    val `public`: Boolean,
    @SerialName("snapshot_id")
    val snapshotId: String,
    @SerialName("tracks")
    val tracks: Tracks,
    @SerialName("type")
    val type: String,
    @SerialName("uri")
    val uri: String
) {
    @Serializable
    data class ExternalUrls(
        @SerialName("spotify")
        val spotify: String
    )

    @Serializable
    data class Followers(
        @SerialName("href")
        val href: String?,
        @SerialName("total")
        val total: Int
    )

    @Serializable
    data class Image(
        @SerialName("height")
        val height: Int?,
        @SerialName("url")
        val url: String,
        @SerialName("width")
        val width: Int?
    )

    @Serializable
    data class Owner(
        @SerialName("display_name")
        val displayName: String,
        @SerialName("external_urls")
        val externalUrls: ExternalUrls,
        @SerialName("href")
        val href: String,
        @SerialName("id")
        val id: String,
        @SerialName("type")
        val type: String,
        @SerialName("uri")
        val uri: String
    ) {
        @Serializable
        data class ExternalUrls(
            @SerialName("spotify")
            val spotify: String
        )
    }

    @Serializable
    data class Tracks(
        @SerialName("href")
        val href: String,
        @SerialName("items")
        val items: List<Item>,
        @SerialName("limit")
        val limit: Int,
        @SerialName("next")
        val next: String?,
        @SerialName("offset")
        val offset: Int,
        @SerialName("previous")
        val previous: String?,
        @SerialName("total")
        val total: Int
    ) {
        @Serializable
        data class Item(
            @SerialName("added_at")
            val addedAt: String,
            @SerialName("added_by")
            val addedBy: AddedBy,
            @SerialName("is_local")
            val isLocal: Boolean,
            @SerialName("primary_color")
            val primaryColor: String?,
            @SerialName("track")
            val track: Track,
            @SerialName("video_thumbnail")
            val videoThumbnail: VideoThumbnail
        ) {
            @Serializable
            data class AddedBy(
                @SerialName("external_urls")
                val externalUrls: ExternalUrls,
                @SerialName("href")
                val href: String,
                @SerialName("id")
                val id: String,
                @SerialName("type")
                val type: String,
                @SerialName("uri")
                val uri: String
            ) {
                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify")
                    val spotify: String
                )
            }

            @Serializable
            data class Track(
                @SerialName("album")
                val album: Album,
                @SerialName("artists")
                val artists: List<Artist>,
                @SerialName("available_markets")
                val availableMarkets: List<String>,
                @SerialName("disc_number")
                val discNumber: Int,
                @SerialName("duration_ms")
                val durationMs: Int,
                @SerialName("episode")
                val episode: Boolean,
                @SerialName("explicit")
                val explicit: Boolean,
                @SerialName("external_ids")
                val externalIds: ExternalIds,
                @SerialName("external_urls")
                val externalUrls: ExternalUrls,
                @SerialName("href")
                val href: String,
                @SerialName("id")
                val id: String,
                @SerialName("is_local")
                val isLocal: Boolean,
                @SerialName("name")
                val name: String,
                @SerialName("popularity")
                val popularity: Int,
                @SerialName("preview_url")
                val previewUrl: String?,
                @SerialName("track")
                val track: Boolean,
                @SerialName("track_number")
                val trackNumber: Int,
                @SerialName("type")
                val type: String,
                @SerialName("uri")
                val uri: String
            ) {
                @Serializable
                data class Album(
                    @SerialName("album_type")
                    val albumType: String,
                    @SerialName("artists")
                    val artists: List<Artist>,
                    @SerialName("available_markets")
                    val availableMarkets: List<String>,
                    @SerialName("external_urls")
                    val externalUrls: ExternalUrls,
                    @SerialName("href")
                    val href: String,
                    @SerialName("id")
                    val id: String,
                    @SerialName("images")
                    val images: List<Image>,
                    @SerialName("name")
                    val name: String,
                    @SerialName("release_date")
                    val releaseDate: String,
                    @SerialName("release_date_precision")
                    val releaseDatePrecision: String,
                    @SerialName("total_tracks")
                    val totalTracks: Int,
                    @SerialName("type")
                    val type: String,
                    @SerialName("uri")
                    val uri: String
                ) {
                    @Serializable
                    data class Artist(
                        @SerialName("external_urls")
                        val externalUrls: ExternalUrls,
                        @SerialName("href")
                        val href: String,
                        @SerialName("id")
                        val id: String,
                        @SerialName("name")
                        val name: String,
                        @SerialName("type")
                        val type: String,
                        @SerialName("uri")
                        val uri: String
                    ) {
                        @Serializable
                        data class ExternalUrls(
                            @SerialName("spotify")
                            val spotify: String
                        )
                    }

                    @Serializable
                    data class ExternalUrls(
                        @SerialName("spotify")
                        val spotify: String
                    )

                    @Serializable
                    data class Image(
                        @SerialName("height")
                        val height: Int,
                        @SerialName("url")
                        val url: String,
                        @SerialName("width")
                        val width: Int
                    )
                }

                @Serializable
                data class Artist(
                    @SerialName("external_urls")
                    val externalUrls: ExternalUrls,
                    @SerialName("href")
                    val href: String,
                    @SerialName("id")
                    val id: String,
                    @SerialName("name")
                    val name: String,
                    @SerialName("type")
                    val type: String,
                    @SerialName("uri")
                    val uri: String
                ) {
                    @Serializable
                    data class ExternalUrls(
                        @SerialName("spotify")
                        val spotify: String
                    )
                }

                @Serializable
                data class ExternalIds(
                    @SerialName("isrc")
                    val isrc: String
                )

                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify")
                    val spotify: String
                )
            }

            @Serializable
            data class VideoThumbnail(
                @SerialName("url")
                val url: String?
            )
        }
    }
}