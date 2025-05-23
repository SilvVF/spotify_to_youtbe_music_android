package io.silv.sp2yt.types


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAlbum(
    @SerialName("album_type") val albumType: String? = null,
    @SerialName("artists") val artists: List<Artist?>? = null,
    @SerialName("available_markets") val availableMarkets: List<String?>? = null,
    @SerialName("copyrights") val copyrights: List<Copyright?>? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
    @SerialName("external_urls") val externalUrls: ExternalUrls? = null,
    @SerialName("genres") val genres: List<String?>? = null,
    @SerialName("href") val href: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("images") val images: List<Image?>? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("popularity") val popularity: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_date_precision") val releaseDatePrecision: String? = null,
    @SerialName("restrictions") val restrictions: Restrictions? = null,
    @SerialName("total_tracks") val totalTracks: Int? = null,
    @SerialName("tracks") val tracks: Tracks? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("uri") val uri: String? = null
) {
    @Serializable
    data class Artist(
        @SerialName("external_urls") val externalUrls: ExternalUrls? = null,
        @SerialName("href") val href: String? = null,
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
        @SerialName("type") val type: String? = null,
        @SerialName("uri") val uri: String? = null
    ) {
        @Serializable
        data class ExternalUrls(
            @SerialName("spotify") val spotify: String? = null
        )
    }

    @Serializable
    data class Copyright(
        @SerialName("text") val text: String? = null,
        @SerialName("type") val type: String? = null
    )

    @Serializable
    data class ExternalIds(
        @SerialName("ean") val ean: String? = null,
        @SerialName("isrc") val isrc: String? = null,
        @SerialName("upc") val upc: String? = null
    )

    @Serializable
    data class ExternalUrls(
        @SerialName("spotify") val spotify: String? = null
    )

    @Serializable
    data class Image(
        @SerialName("height") val height: Int? = null,
        @SerialName("url") val url: String? = null,
        @SerialName("width") val width: Int? = null
    )

    @Serializable
    data class Restrictions(
        @SerialName("reason") val reason: String? = null
    )

    @Serializable
    data class Tracks(
        @SerialName("href") val href: String? = null,
        @SerialName("items") val items: List<Item?>? = null,
        @SerialName("limit") val limit: Int? = null,
        @SerialName("next") val next: String? = null,
        @SerialName("offset") val offset: Int? = null,
        @SerialName("previous") val previous: String? = null,
        @SerialName("total") val total: Int? = null
    ) {
        @Serializable
        data class Item(
            @SerialName("artists") val artists: List<Artist?>? = null,
            @SerialName("available_markets") val availableMarkets: List<String?>? = null,
            @SerialName("disc_number") val discNumber: Int? = null,
            @SerialName("duration_ms") val durationMs: Int? = null,
            @SerialName("explicit") val explicit: Boolean? = null,
            @SerialName("external_urls") val externalUrls: ExternalUrls? = null,
            @SerialName("href") val href: String? = null,
            @SerialName("id") val id: String? = null,
            @SerialName("is_local") val isLocal: Boolean? = null,
            @SerialName("is_playable") val isPlayable: Boolean? = null,
            @SerialName("linked_from") val linkedFrom: LinkedFrom? = null,
            @SerialName("name") val name: String? = null,
            @SerialName("preview_url") val previewUrl: String? = null,
            @SerialName("restrictions") val restrictions: Restrictions? = null,
            @SerialName("track_number") val trackNumber: Int? = null,
            @SerialName("type") val type: String? = null,
            @SerialName("uri") val uri: String? = null
        ) {
            @Serializable
            data class Artist(
                @SerialName("external_urls") val externalUrls: ExternalUrls? = null,
                @SerialName("href") val href: String? = null,
                @SerialName("id") val id: String? = null,
                @SerialName("name") val name: String? = null,
                @SerialName("type") val type: String? = null,
                @SerialName("uri") val uri: String? = null
            ) {
                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify") val spotify: String? = null
                )
            }

            @Serializable
            data class ExternalUrls(
                @SerialName("spotify") val spotify: String? = null
            )

            @Serializable
            data class LinkedFrom(
                @SerialName("external_urls") val externalUrls: ExternalUrls? = null,
                @SerialName("href") val href: String? = null,
                @SerialName("id") val id: String? = null,
                @SerialName("type") val type: String? = null,
                @SerialName("uri") val uri: String? = null
            ) {
                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify") val spotify: String? = null
                )
            }

            @Serializable
            data class Restrictions(
                @SerialName("reason") val reason: String? = null
            )
        }
    }
}
