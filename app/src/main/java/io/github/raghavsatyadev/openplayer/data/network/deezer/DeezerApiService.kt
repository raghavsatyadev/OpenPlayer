package io.github.raghavsatyadev.openplayer.data.network.deezer

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Deezer API.
 * Used primarily for fetching artist artwork.
 */
interface DeezerApiService {

    /**
     * Search for an artist by name.
     * @param query Artist name to search for
     * @param limit Maximum number of results to return
     * @return Search response containing list of matching artists
     */
    @GET("search/artist")
    suspend fun searchArtist(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1
    ): DeezerSearchResponse

    /**
     * Search for tracks by query string.
     * @param query Search query (e.g. artist and track title)
     * @param limit Maximum number of results to return
     * @return Search response containing matching tracks
     */
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): DeezerTrackSearchResponse

    /** Album details, read for the album's genres. */
    @GET("album/{id}")
    suspend fun getAlbum(@Path("id") id: Long): DeezerAlbum
}
