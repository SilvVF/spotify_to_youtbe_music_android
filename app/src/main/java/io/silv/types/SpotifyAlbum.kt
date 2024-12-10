package io.silv.types


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAlbum(
    @SerialName("album_type")
    val albumType: String?,
    @SerialName("artists")
    val artists: List<Artist?>?,
    @SerialName("available_markets")
    val availableMarkets: List<String?>?,
    @SerialName("copyrights")
    val copyrights: List<Copyright?>?,
    @SerialName("external_ids")
    val externalIds: ExternalIds?,
    @SerialName("external_urls")
    val externalUrls: ExternalUrls?,
    @SerialName("genres")
    val genres: List<String?>?,
    @SerialName("href")
    val href: String?,
    @SerialName("id")
    val id: String?,
    @SerialName("images")
    val images: List<Image?>?,
    @SerialName("label")
    val label: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("popularity")
    val popularity: Int?,
    @SerialName("release_date")
    val releaseDate: String?,
    @SerialName("release_date_precision")
    val releaseDatePrecision: String?,
    @SerialName("restrictions")
    val restrictions: Restrictions?,
    @SerialName("total_tracks")
    val totalTracks: Int?,
    @SerialName("tracks")
    val tracks: Tracks?,
    @SerialName("type")
    val type: String?,
    @SerialName("uri")
    val uri: String?
) {
    @Serializable
    data class Artist(
        @SerialName("external_urls")
        val externalUrls: ExternalUrls?,
        @SerialName("href")
        val href: String?,
        @SerialName("id")
        val id: String?,
        @SerialName("name")
        val name: String?,
        @SerialName("type")
        val type: String?,
        @SerialName("uri")
        val uri: String?
    ) {
        @Serializable
        data class ExternalUrls(
            @SerialName("spotify")
            val spotify: String?
        )
    }

    @Serializable
    data class Copyright(
        @SerialName("text")
        val text: String?,
        @SerialName("type")
        val type: String?
    )

    @Serializable
    data class ExternalIds(
        @SerialName("ean")
        val ean: String?,
        @SerialName("isrc")
        val isrc: String?,
        @SerialName("upc")
        val upc: String?
    )

    @Serializable
    data class ExternalUrls(
        @SerialName("spotify")
        val spotify: String?
    )

    @Serializable
    data class Image(
        @SerialName("height")
        val height: Int?,
        @SerialName("url")
        val url: String?,
        @SerialName("width")
        val width: Int?
    )

    @Serializable
    data class Restrictions(
        @SerialName("reason")
        val reason: String?
    )

    @Serializable
    data class Tracks(
        @SerialName("href")
        val href: String?,
        @SerialName("items")
        val items: List<Item?>?,
        @SerialName("limit")
        val limit: Int?,
        @SerialName("next")
        val next: String?,
        @SerialName("offset")
        val offset: Int?,
        @SerialName("previous")
        val previous: String?,
        @SerialName("total")
        val total: Int?
    ) {
        @Serializable
        data class Item(
            @SerialName("artists")
            val artists: List<Artist?>?,
            @SerialName("available_markets")
            val availableMarkets: List<String?>?,
            @SerialName("disc_number")
            val discNumber: Int?,
            @SerialName("duration_ms")
            val durationMs: Int?,
            @SerialName("explicit")
            val explicit: Boolean?,
            @SerialName("external_urls")
            val externalUrls: ExternalUrls?,
            @SerialName("href")
            val href: String?,
            @SerialName("id")
            val id: String?,
            @SerialName("is_local")
            val isLocal: Boolean?,
            @SerialName("is_playable")
            val isPlayable: Boolean?,
            @SerialName("linked_from")
            val linkedFrom: LinkedFrom?,
            @SerialName("name")
            val name: String?,
            @SerialName("preview_url")
            val previewUrl: String?,
            @SerialName("restrictions")
            val restrictions: Restrictions?,
            @SerialName("track_number")
            val trackNumber: Int?,
            @SerialName("type")
            val type: String?,
            @SerialName("uri")
            val uri: String?
        ) {
            @Serializable
            data class Artist(
                @SerialName("external_urls")
                val externalUrls: ExternalUrls?,
                @SerialName("href")
                val href: String?,
                @SerialName("id")
                val id: String?,
                @SerialName("name")
                val name: String?,
                @SerialName("type")
                val type: String?,
                @SerialName("uri")
                val uri: String?
            ) {
                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify")
                    val spotify: String?
                )
            }

            @Serializable
            data class ExternalUrls(
                @SerialName("spotify")
                val spotify: String?
            )

            @Serializable
            data class LinkedFrom(
                @SerialName("external_urls")
                val externalUrls: ExternalUrls?,
                @SerialName("href")
                val href: String?,
                @SerialName("id")
                val id: String?,
                @SerialName("type")
                val type: String?,
                @SerialName("uri")
                val uri: String?
            ) {
                @Serializable
                data class ExternalUrls(
                    @SerialName("spotify")
                    val spotify: String?
                )
            }

            @Serializable
            data class Restrictions(
                @SerialName("reason")
                val reason: String?
            )
        }
    }
}