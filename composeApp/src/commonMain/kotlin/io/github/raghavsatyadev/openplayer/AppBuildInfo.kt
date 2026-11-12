package io.github.raghavsatyadev.openplayer

/**
 * Build facts that used to come from :app's BuildConfig. :composeApp is a KMP library with no
 * BuildConfig, so the application module fills this in from its own BuildConfig at startup
 * (androidCMP's OpenPlayerApp.attachBaseContext), before anything else reads it.
 *
 * The defaults are the safe release values: if init were ever skipped, debug-only behaviour
 * (destructive DB migration fallback, verbose logging) stays off.
 */
object AppBuildInfo {
    var applicationId: String = ""
        private set
    var buildType: String = "release"
        private set
    var debug: Boolean = false
        private set
    var versionCode: Int = 0
        private set
    var versionName: String = ""
        private set

    fun init(
        applicationId: String,
        buildType: String,
        debug: Boolean,
        versionCode: Int,
        versionName: String,
    ) {
        this.applicationId = applicationId
        this.buildType = buildType
        this.debug = debug
        this.versionCode = versionCode
        this.versionName = versionName
    }
}
