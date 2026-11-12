package io.github.raghavsatyadev.openplayer.data.network.itunes

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Response from iTunes Search API.
 */
data class ITunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int = 0,
    @SerializedName("results") val results: List<ITunesSong> = emptyList()
)

/**
 * Song result from iTunes Search API.
 */
data class ITunesSong(
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("collectionName") val collectionName: String? = null,
    @SerializedName("artworkUrl100") val artworkUrl100: String? = null,
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long? = null,
    @SerializedName("primaryGenreName") val primaryGenreName: String? = null
) {
    /**
     * Returns high-resolution artwork URL by replacing 100x100bb dimension with 600x600bb.
     */
    fun bigArtworkUrl(): String? {
        return artworkUrl100?.replace("100x100bb", "600x600bb")
    }
}

/**
 * Client for searching songs and artwork on the iTunes Search API.
 */
class ITunesSearchClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    /**
     * Searches iTunes for songs matching the given term.
     * Returns an empty list on bad responses; throws IOException on network failure. With
     * [throwWhenThrottled], a rate limit or server error (403, 429, 5xx) also throws IOException,
     * so callers that cache misses do not mistake it for "nothing found".
     */
    suspend fun searchSongs(term: String, limit: Int = 5, throwWhenThrottled: Boolean = false): List<ITunesSong> = withContext(Dispatchers.IO) {
        try {
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("media", "music")
                .addQueryParameter("entity", "song")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("term", term)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    if (throwWhenThrottled && (code == 403 || code == 429 || code >= 500)) {
                        throw java.io.IOException("iTunes HTTP $code")
                    }
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                gson.fromJson(body, ITunesSearchResponse::class.java)?.results ?: emptyList()
            }
        } catch (e: java.io.IOException) {
            // Network failures propagate so the caller retries later instead of recording a miss.
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
}
