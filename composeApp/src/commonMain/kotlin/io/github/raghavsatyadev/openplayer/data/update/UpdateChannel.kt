package io.github.raghavsatyadev.openplayer.data.update

/**
 * Where builds of a channel are published. Both stable releases and alpha prereleases are
 * published on GitHub and signed with the same key, so moving between channels installs in place.
 */
enum class UpdateChannel(val storageKey: String) {
    STABLE("stable"),
    ALPHA("alpha");

    companion object {
        fun fromStorageKey(key: String?): UpdateChannel? = entries.firstOrNull { it.storageKey == key }

        /**
         * The channel the running build was distributed through. Alphas (and PR builds) carry
         * a pre-release suffix; stable releases use the plain version tag.
         */
        fun ofVersionName(versionName: String): UpdateChannel =
            if ('-' in versionName) ALPHA else STABLE
    }
}

/** `1.2.3` or `1.2.3-alpha.4` as published on GitHub releases. */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val alphaBuild: Int? = null,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val base = compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)
        if (base != 0) return base
        return when {
            this.alphaBuild == null && other.alphaBuild == null -> 0
            this.alphaBuild == null -> 1
            other.alphaBuild == null -> -1
            else -> this.alphaBuild.compareTo(other.alphaBuild)
        }
    }

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-alpha\.(\d+))?$""")

        fun parse(value: String): AppVersion? {
            val groups = PATTERN.matchEntire(value.trim())?.groupValues ?: return null
            return AppVersion(
                major = groups[1].toIntOrNull() ?: return null,
                minor = groups[2].toIntOrNull() ?: return null,
                patch = groups[3].toIntOrNull() ?: return null,
                alphaBuild = groups[4].takeIf { it.isNotEmpty() }?.toIntOrNull(),
            )
        }
    }
}

/** A published build that can be offered to the user. */
data class AppRelease(
    val channel: UpdateChannel,
    val versionName: String,
    /** Always null for GitHub releases. */
    val versionCode: Long?,
    val apkUrl: String,
    val apkSizeBytes: Long?,
    val pageUrl: String,
)

/** Whether [this] is newer than the running build. */
@Suppress("UNUSED_PARAMETER")
internal fun AppRelease.isNewerThan(installedVersionName: String, installedVersionCode: Long): Boolean {
    val candidate = AppVersion.parse(versionName) ?: return false
    val installed = AppVersion.parse(installedVersionName) ?: return true
    return candidate > installed
}
