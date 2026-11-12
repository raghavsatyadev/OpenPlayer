package io.github.raghavsatyadev.openplayer.app

import android.content.Context
import io.github.raghavsatyadev.openplayer.AppBuildInfo
import io.github.raghavsatyadev.openplayer.OpenPlayerApplication

/**
 * The application entry point. Its only job is to hand this module's BuildConfig to
 * :composeApp, which is a KMP library and has no BuildConfig of its own.
 */
class OpenPlayerApp : OpenPlayerApplication() {
    override fun attachBaseContext(base: Context) {
        AppBuildInfo.init(
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = BuildConfig.BUILD_TYPE,
            debug = BuildConfig.DEBUG,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
        )
        super.attachBaseContext(base)
    }
}
