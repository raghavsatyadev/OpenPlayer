package io.github.raghavsatyadev.openplayer.data.update

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpToDate(val channel: UpdateChannel) : UpdateStatus

    /**
     * [requiresReinstall] means the release is signed differently from this install (other
     * channel, or a side-loaded build), so Android refuses to install it over the app.
     */
    data class Available(val release: AppRelease, val requiresReinstall: Boolean) : UpdateStatus
    data class Downloading(val release: AppRelease, val progress: Float?) : UpdateStatus
    data class AwaitingInstallPermission(val release: AppRelease) : UpdateStatus
    data class AwaitingConfirmation(val release: AppRelease, /** Platform install prompt; an android.content.Intent on Android. */ val confirmIntent: Any) : UpdateStatus
    data class Installing(val release: AppRelease) : UpdateStatus
    data class Failed(val error: UpdateError, val release: AppRelease?) : UpdateStatus
}

enum class UpdateError {
    CHECK_FAILED,
    NOTHING_PUBLISHED,
    DOWNLOAD_FAILED,
    INVALID_APK,
    NOT_NEWER,
    INSTALL_FAILED,
}
