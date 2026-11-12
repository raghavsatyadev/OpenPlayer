package io.github.raghavsatyadev.openplayer.data.update

import android.os.Build
import io.github.raghavsatyadev.openplayer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Looks up the newest published build of each channel. */
@Singleton
class AppReleaseSource @Inject constructor(
    baseClient: OkHttpClient,
) {
    private val client = baseClient.newBuilder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "OpenPlayer/${BuildConfig.VERSION_NAME} ($PROJECT_URL)")
                    .build()
            )
        }
        .build()

    /** Newest release of [channel], or null when the channel has nothing installable for this device. */
    suspend fun latest(channel: UpdateChannel): AppRelease? = withContext(Dispatchers.IO) {
        parseLatest(
            JSONArray(getBody(GITHUB_RELEASES_API)),
            channel,
            Build.SUPPORTED_ABIS.toList(),
        )
    }

    private fun getBody(url: String): String {
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $url")
            return response.body.string()
        }
    }

    companion object {
        private const val PROJECT_URL = "https://github.com/raghavsatyadev/OpenPlayer"

        /** Release applicationId; debug builds add a suffix but should still see real releases. */
        internal const val PUBLISHED_PACKAGE = "io.github.raghavsatyadev.openplayer"
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/raghavsatyadev/OpenPlayer/releases?per_page=30"

        /**
         * Newest GitHub release of [channel] (stable = non-prerelease `1.2.3`, alpha = prerelease
         * `1.2.3-alpha.4`) with an APK for one of [supportedAbis] (in device preference order).
         * Assets are named `OpenPlayer-<version>-<abi>.apk`.
         */
        internal fun parseLatest(
            releases: JSONArray,
            channel: UpdateChannel,
            supportedAbis: List<String>,
        ): AppRelease? {
            val wantAlpha = channel == UpdateChannel.ALPHA
            return (0 until releases.length())
                .mapNotNull { releases.optJSONObject(it) }
                .filter { !it.optBoolean("draft") && it.optBoolean("prerelease") == wantAlpha }
                .mapNotNull { release ->
                    val tag = release.optString("tag_name")
                    val version = AppVersion.parse(tag) ?: return@mapNotNull null
                    if ((version.alphaBuild != null) != wantAlpha) return@mapNotNull null
                    val asset = pickAsset(release.optJSONArray("assets"), supportedAbis) ?: return@mapNotNull null
                    version to AppRelease(
                        channel = channel,
                        versionName = tag.removePrefix("v"),
                        versionCode = null,
                        apkUrl = asset.optString("browser_download_url"),
                        apkSizeBytes = asset.optLong("size", -1L).takeIf { it > 0 },
                        pageUrl = release.optString("html_url").ifBlank { "$PROJECT_URL/releases/tag/$tag" },
                    )
                }
                .maxByOrNull { (version, _) -> version }
                ?.second
        }

        private fun pickAsset(assets: JSONArray?, supportedAbis: List<String>): JSONObject? {
            if (assets == null) return null
            val apks = (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .filter {
                    it.optString("name").endsWith(".apk") && it.optString("browser_download_url").isNotBlank()
                }
            return supportedAbis.firstNotNullOfOrNull { abi ->
                apks.firstOrNull { it.optString("name").endsWith("-$abi.apk") }
            }
        }
    }
}
