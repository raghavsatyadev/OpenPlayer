package io.github.raghavsatyadev.openplayer.data.update

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.junit.jupiter.api.Test

class AppReleaseSourceTest {

    @Test
    fun `newest alpha is picked by version even when the API lists it later`() {
        val releases = JSONArray(
            listOf(
                release("v0.3.0-alpha.9"),
                release("v0.3.0-alpha.16"),
                release("v0.3.0-alpha.15"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        val latest = AppReleaseSource.parseLatest(releases, UpdateChannel.ALPHA, listOf("arm64-v8a"))!!

        assertThat(latest.versionName).isEqualTo("0.3.0-alpha.16")
        assertThat(latest.apkUrl).endsWith("/OpenPlayer-0.3.0-alpha.16-arm64-v8a.apk")
    }

    @Test
    fun `alpha asset follows the device ABI preference order`() {
        val releases = JSONArray("[${release("v0.3.0-alpha.16")}]")

        val latest = AppReleaseSource.parseLatest(releases, UpdateChannel.ALPHA, listOf("armeabi-v7a", "arm64-v8a"))!!

        assertThat(latest.apkUrl).endsWith("-armeabi-v7a.apk")
    }

    @Test
    fun `drafts, stable tags and releases without a matching APK are not offered`() {
        val releases = JSONArray(
            listOf(
                release("v0.3.0-alpha.20", draft = true),
                release("v0.4.0", prerelease = false),
                release("v0.3.0-alpha.19", abis = listOf("x86_64")),
                release("v0.3.0-alpha.18"),
            ).joinToString(prefix = "[", postfix = "]")
        )

        assertThat(AppReleaseSource.parseLatest(releases, UpdateChannel.ALPHA, listOf("arm64-v8a"))!!.versionName)
            .isEqualTo("0.3.0-alpha.18")
        assertThat(AppReleaseSource.parseLatest(releases, UpdateChannel.ALPHA, listOf("x86"))).isNull()
    }

    @Test
    fun `stable channel picks the newest non-prerelease and ignores prereleases`() {
        val releases = JSONArray(
            listOf(
                release("v0.5.0-alpha.1"),
                release("v0.4.0", prerelease = false),
                release("v0.4.1", prerelease = false, abis = listOf("x86_64")),
                release("v0.3.2", prerelease = false),
                release("v0.9.0", prerelease = false, draft = true),
            ).joinToString(prefix = "[", postfix = "]")
        )

        val latest = AppReleaseSource.parseLatest(releases, UpdateChannel.STABLE, listOf("arm64-v8a"))!!

        assertThat(latest.versionName).isEqualTo("0.4.0")
        assertThat(latest.channel).isEqualTo(UpdateChannel.STABLE)
        assertThat(latest.apkUrl).endsWith("/OpenPlayer-0.4.0-arm64-v8a.apk")
    }

    @Test
    fun `app versions rank stable above alpha of the same base`() {
        assertThat(stableRelease("1.2.0").isNewerThan("1.2.0-alpha.9", 0)).isTrue()
        assertThat(alphaRelease("1.2.0-alpha.2").isNewerThan("1.1.9", 0)).isTrue()
        assertThat(alphaRelease("1.2.0-alpha.3").isNewerThan("1.2.0-alpha.2", 0)).isTrue()
        assertThat(stableRelease("1.2.0").isNewerThan("1.2.0", 0)).isFalse()
        assertThat(alphaRelease("1.2.0-alpha.2").isNewerThan("1.2.0", 0)).isFalse()
        assertThat(stableRelease("1.2.0").isNewerThan("garbage", 0)).isTrue()
    }

    @Test
    fun `alpha versions order by base version before build number`() {
        val olderBaseHigherBuild = alphaRelease("0.3.0-alpha.99")
        val newerBase = alphaRelease("0.4.0-alpha.1")

        assertThat(newerBase.isNewerThan("0.3.0-alpha.99", installedVersionCode = 300099)).isTrue()
        assertThat(olderBaseHigherBuild.isNewerThan("0.4.0-alpha.1", installedVersionCode = 400001)).isFalse()
        assertThat(alphaRelease("0.3.0-alpha.16").isNewerThan("0.3.0-alpha.16", 300016)).isFalse()
    }

    @Test
    fun `PR builds on the alpha key are offered any published alpha`() {
        assertThat(alphaRelease("0.3.0-alpha.16").isNewerThan("0.3.0-pr.137.ba94188", 3)).isTrue()
    }

    @Test
    fun `installed channel follows the version name published by each pipeline`() {
        assertThat(UpdateChannel.ofVersionName("0.3.0")).isEqualTo(UpdateChannel.STABLE)
        assertThat(UpdateChannel.ofVersionName("0.3.0-alpha.16")).isEqualTo(UpdateChannel.ALPHA)
        assertThat(UpdateChannel.ofVersionName("0.3.0-pr.137.ba94188")).isEqualTo(UpdateChannel.ALPHA)
    }

    private fun stableRelease(versionName: String) = alphaRelease(versionName).copy(channel = UpdateChannel.STABLE)

    private fun alphaRelease(versionName: String) = AppRelease(
        channel = UpdateChannel.ALPHA,
        versionName = versionName,
        versionCode = null,
        apkUrl = "https://example.invalid/app.apk",
        apkSizeBytes = null,
        pageUrl = "https://example.invalid",
    )

    private fun release(
        tag: String,
        prerelease: Boolean = true,
        draft: Boolean = false,
        abis: List<String> = listOf("arm64-v8a", "armeabi-v7a"),
    ): String {
        val name = tag.removePrefix("v")
        val assets = abis.joinToString(prefix = "[", postfix = "]") { abi ->
            """{"name":"OpenPlayer-$name-$abi.apk","size":37082259,
               "browser_download_url":"https://github.com/raghavsatyadev/OpenPlayer/releases/download/$tag/OpenPlayer-$name-$abi.apk"}"""
        }
        return """{"tag_name":"$tag","prerelease":$prerelease,"draft":$draft,
                   "html_url":"https://github.com/raghavsatyadev/OpenPlayer/releases/tag/$tag","assets":$assets}"""
    }
}
