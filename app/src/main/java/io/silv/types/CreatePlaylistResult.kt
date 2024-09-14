package io.silv.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistResult(
    @SerialName( "etag")
    val etag: String,
    @SerialName( "id")
    val id: String,
    @SerialName( "kind")
    val kind: String,
    @SerialName( "snippet")
    val snippet: Snippet,
    @SerialName( "status")
    val status: Status
) {
    @Serializable
    data class Snippet(
        @SerialName( "channelId")
        val channelId: String,
        @SerialName( "channelTitle")
        val channelTitle: String,
        @SerialName( "defaultLanguage")
        val defaultLanguage: String,
        @SerialName( "description")
        val description: String,
        @SerialName( "localized")
        val localized: Localized,
        @SerialName( "publishedAt")
        val publishedAt: String,
        @SerialName( "thumbnails")
        val thumbnails: Thumbnails,
        @SerialName( "title")
        val title: String
    ) {
        @Serializable
        data class Localized(
            @SerialName( "description")
            val description: String,
            @SerialName( "title")
            val title: String
        )

        @Serializable
        data class Thumbnails(
            @SerialName( "default")
            val default: Default,
            @SerialName( "high")
            val high: High,
            @SerialName( "medium")
            val medium: Medium
        ) {
            @Serializable
            data class Default(
                @SerialName( "height")
                val height: Int,
                @SerialName( "url")
                val url: String,
                @SerialName( "width")
                val width: Int
            )

            @Serializable
            data class High(
                @SerialName( "height")
                val height: Int,
                @SerialName( "url")
                val url: String,
                @SerialName( "width")
                val width: Int
            )

            @Serializable
            data class Medium(
                @SerialName( "height")
                val height: Int,
                @SerialName( "url")
                val url: String,
                @SerialName( "width")
                val width: Int
            )
        }
    }

    @Serializable
    data class Status(
        @SerialName( "privacyStatus")
        val privacyStatus: String
    )
}