package io.github.raghavsatyadev.openplayer.data.image.online

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.github.raghavsatyadev.openplayer.data.network.deezer.DeezerApiService
import io.github.raghavsatyadev.openplayer.data.network.itunes.ITunesSearchClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OnlineCoverLookupTest {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val deezerApiService: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DeezerApiService::class.java)
    }

    private val itunesSearchClient: ITunesSearchClient by lazy {
        ITunesSearchClient(okHttpClient, gson)
    }

    @Before
    fun checkNetworkAvailable() {
        val hasNetwork = runCatching {
            val request = Request.Builder()
                .url("https://api.deezer.com/")
                .head()
                .build()
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)

        assumeTrue("Internet connection not available, skipping network lookup test", hasNetwork)
    }

    @Test
    fun deezer_searchTrack_returnsAcceptedTrackWithCoverXl() = runBlocking {
        val query = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )

        val response = deezerApiService.searchTrack("artist:\"The Weeknd\" track:\"Blinding Lights\"", 5)
        // Deezer returns an empty track list in regions where it has no catalogue (e.g. India).
        assumeTrue("Deezer has no track results in this region", response.data.isNotEmpty())

        val matched = response.data.firstOrNull { track ->
            CoverMatch.accepts(
                query = query,
                title = track.title,
                artist = track.artist?.name.orEmpty(),
                durationMs = track.duration * 1000L
            )
        }
        assertThat(matched).isNotNull()
        assertThat(matched?.album?.coverXl).isNotNull()
        assertThat(matched?.album?.coverXl).isNotEmpty()
    }

    @Test
    fun itunes_searchSongs_returnsAcceptedTrackWithBigArtwork() = runBlocking {
        val query = CoverQuery(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = null,
            durationMs = 200_000L
        )

        val results = itunesSearchClient.searchSongs("The Weeknd Blinding Lights", 5)
        assertThat(results).isNotEmpty()

        val matched = results.firstOrNull { song ->
            CoverMatch.accepts(
                query = query,
                title = song.trackName.orEmpty(),
                artist = song.artistName.orEmpty(),
                durationMs = song.trackTimeMillis
            )
        }
        assertThat(matched).isNotNull()
        val bigUrl = matched?.bigArtworkUrl()
        assertThat(bigUrl).isNotNull()
        assertThat(bigUrl).contains("600x600")
    }
}
