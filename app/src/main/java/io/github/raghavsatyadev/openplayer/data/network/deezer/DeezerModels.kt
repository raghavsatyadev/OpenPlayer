package io.github.raghavsatyadev.openplayer.data.network.deezer

import com.google.gson.annotations.SerializedName

/**
 * Response from Deezer artist search API.
 */
data class DeezerSearchResponse(
    @SerializedName("data") val data: List<DeezerArtist> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

/**
 * Artist data from Deezer API.
 * Contains multiple image sizes for different use cases.
 */
data class DeezerArtist(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picture") val picture: String? = null,
    @SerializedName("picture_small") val pictureSmall: String? = null,
    @SerializedName("picture_medium") val pictureMedium: String? = null,
    @SerializedName("picture_big") val pictureBig: String? = null,
    @SerializedName("picture_xl") val pictureXl: String? = null,
    @SerializedName("nb_album") val albumCount: Int = 0,
    @SerializedName("nb_fan") val fanCount: Int = 0
)

/**
 * Response from Deezer track search API.
 */
data class DeezerTrackSearchResponse(
    @SerializedName("data") val data: List<DeezerTrack> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("error") val error: DeezerError? = null
)

/**
 * Track data from Deezer API.
 */
data class DeezerTrack(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("title") val title: String = "",
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("artist") val artist: DeezerTrackArtist? = null,
    @SerializedName("album") val album: DeezerTrackAlbum? = null
)

/**
 * Track artist data from Deezer API.
 */
data class DeezerTrackArtist(
    @SerializedName("name") val name: String = ""
)

/**
 * Track album data from Deezer API containing cover URLs.
 */
data class DeezerTrackAlbum(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("title") val title: String = "",
    @SerializedName("cover_medium") val coverMedium: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null,
    @SerializedName("cover_xl") val coverXl: String? = null
)

/** Album details from Deezer's album/{id} endpoint; only the genres are read. */
data class DeezerAlbum(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("genres") val genres: DeezerGenreList? = null,
    @SerializedName("error") val error: DeezerError? = null
)

data class DeezerGenreList(
    @SerializedName("data") val data: List<DeezerGenre> = emptyList()
)

data class DeezerGenre(
    @SerializedName("name") val name: String = ""
)

/** Deezer reports errors, including its quota limit, as HTTP 200 with this body. */
data class DeezerError(
    @SerializedName("type") val type: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: Int = 0
)
